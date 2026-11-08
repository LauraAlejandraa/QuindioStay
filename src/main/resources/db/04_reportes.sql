-- =====================================================================
-- QuindíoStay · Bases de Datos I · Grupo 02D · 2026-2
-- Script 04: consultas de los 10 reportes (Entrega 2, punto 3)
-- Motor: MySQL 8.0.46
-- Estilo de la clase (Clase 15): alias de tabla sin AS, toda columna con
-- el alias de su tabla, y en cada ON siempre FK = PK (la foránea de la
-- tabla hija = la primaria de la tabla padre).
-- Ejecutar DESPUÉS de 00, 01 y 02. Cada consulta se puede correr sola.
-- En todos los reportes una boleta cuenta como vendida mientras no esté
-- anulada (estado 'vendida' o 'usada'), como se definió en la Entrega 2.
-- =====================================================================

USE quindiostay;

-- =====================================================================
-- REPORTES SIMPLES (consultas a una sola tabla)
-- =====================================================================

-- ---------------------------------------------------------------------
-- Reporte 1 · Atracciones registradas (simple)
-- Pregunta: ¿cuáles son las atracciones turísticas registradas en
-- QuindíoStay y qué información tiene el sistema de cada una?
-- Tabla: Atraccion.
-- ---------------------------------------------------------------------
SELECT id_atraccion,
       nombre,
       descripcion,
       ubicacion
FROM Atraccion
ORDER BY nombre ASC;

-- ---------------------------------------------------------------------
-- Reporte 2 · Funciones programadas (simple)
-- Pregunta: ¿qué funciones están programadas y cuándo son?
-- Tabla: Funcion. Solo las que tienen estado 'programada' (las futuras),
-- de la más próxima a la más lejana.
-- ---------------------------------------------------------------------
SELECT id_funcion,
       fecha,
       hora_apertura,
       hora_cierre,
       estado,
       id_atraccion
FROM Funcion
WHERE estado = 'programada'
ORDER BY fecha ASC, hora_apertura ASC;

-- ---------------------------------------------------------------------
-- Reporte 3 · Boletas por estado (simple)  [AJUSTADO en la Entrega 3]
-- Pregunta: ¿cuántas boletas se encuentran registradas en QuindíoStay
-- según su estado y cuáles son sus datos principales?
-- Tabla: Boleta.
-- La pregunta pide DOS cosas, por eso el reporte tiene dos partes, cada
-- una con una consulta a una sola tabla (sigue siendo nivel simple):
--   Parte A: el resumen, cuántas boletas hay en cada estado.
--   Parte B: el detalle, los datos principales de cada boleta.
-- Ajuste: en la Entrega 2 solo estaba la parte B, que no respondía
-- "cuántas". En la aplicación se muestran juntas: arriba el resumen y
-- debajo el detalle.
-- ---------------------------------------------------------------------

-- Reporte 3 · Parte A · Resumen por estado
SELECT estado,
       COUNT(*) AS cantidad_boletas
FROM Boleta
GROUP BY estado
ORDER BY cantidad_boletas DESC;

-- Reporte 3 · Parte B · Detalle de las boletas
-- Ordenadas por estado y, dentro de cada estado, las más recientes
-- primero (las de identificador más alto).
SELECT id_boleta,
       codigo,
       estado,
       id_reserva,
       id_zona,
       id_tipo_entrada,
       id_combo
FROM Boleta
ORDER BY estado ASC, id_boleta DESC;


-- =====================================================================
-- REPORTES INTERMEDIOS (consultas multitabla con WHERE, GROUP BY, etc.)
-- =====================================================================

-- ---------------------------------------------------------------------
-- Reporte 4 · Reservas realizadas por cliente (intermedio)  [AJUSTADO]
-- Pregunta: ¿qué reservas han realizado los clientes de QuindíoStay y
-- cuál es la información de cada una?
-- Tablas: Reserva y Cliente, unidas por id_cliente (INNER JOIN).
-- Ajuste: se agrega un WHERE con un rango de fechas de creación, para
-- que el administrador consulte un período (un mes, un año). En la
-- aplicación las dos fechas las escoge el usuario; aquí van unas que
-- cubren todos los datos, así el reporte responde la pregunta completa.
-- CONCAT_WS une el nombre completo y se salta los NULL (segundo nombre
-- y segundo apellido son opcionales).
-- ---------------------------------------------------------------------
SELECT r.id_reserva,
       c.id_cliente,
       CONCAT_WS(' ', c.primer_nombre, c.segundo_nombre,
                      c.primer_apellido, c.segundo_apellido) AS cliente,
       r.fecha_creacion,
       r.fecha_servicio,
       r.estado,
       r.valor_total
FROM Reserva r
INNER JOIN Cliente c
    ON r.id_cliente = c.id_cliente
WHERE r.fecha_creacion BETWEEN '2024-01-01' AND '2026-12-31'
ORDER BY r.fecha_creacion DESC, r.id_reserva DESC;

-- ---------------------------------------------------------------------
-- Reporte 5 · Aforo vendido y disponible por función y zona (intermedio)
-- Pregunta: ¿cuántas boletas se han vendido y cuántos cupos quedan
-- disponibles para cada función y zona?
-- Tablas: Funcion, Zona y Boleta.
-- LEFT JOIN con Boleta para que también salgan las zonas sin ventas
-- (con 0). La condición de "no anulada" va en el ON y no en el WHERE:
-- en el WHERE eliminaría esas zonas. La función de cada boleta se
-- obtiene por su zona (Boleta no guarda id_funcion).
-- ---------------------------------------------------------------------
SELECT f.id_funcion,
       z.id_zona,
       z.nombre                       AS zona,
       z.aforo                        AS aforo_maximo,
       COUNT(b.id_boleta)             AS boletas_vendidas,
       z.aforo - COUNT(b.id_boleta)   AS cupos_disponibles
FROM Funcion f
INNER JOIN Zona z
    ON z.id_funcion = f.id_funcion
LEFT JOIN Boleta b
    ON b.id_zona = z.id_zona
   AND b.estado <> 'anulada'
GROUP BY f.id_funcion, f.fecha, z.id_zona, z.nombre, z.aforo
ORDER BY f.fecha ASC, f.id_funcion ASC, z.nombre ASC;

-- ---------------------------------------------------------------------
-- Reporte 6 · Ventas por tipo de entrada (intermedio)  [AJUSTADO]
-- Pregunta: ¿cuántas boletas se han vendido de cada tipo de entrada y
-- cuáles son los tipos más solicitados? ¿Cuánto vendió cada tipo?
-- Tablas: Tipo_entrada, Boleta y Zona.
-- Ajuste: se agrega el valor vendido de cada tipo, porque en el RF13
-- (Entrega 1) prometimos "ingresos por atracción y tipo de entrada", y
-- el enunciado lo pide en lo que el negocio quiere ver del dominio 9.
-- Para el valor se une Zona: precio de la zona menos el descuento del
-- tipo, redondeado a la centena. Solo cuentan las boletas sueltas
-- (id_combo IS NULL): las de un combo no tienen precio propio, porque
-- el combo se cobra por unidad con su precio especial.
-- ---------------------------------------------------------------------
SELECT t.id_tipo_entrada,
       t.nombre                                      AS tipo_entrada,
       t.porcentaje_descuento,
       COUNT(b.id_boleta)                            AS boletas_vendidas,
       SUM(CASE
               WHEN b.id_combo IS NULL
               THEN ROUND(z.precio * (100 - t.porcentaje_descuento) / 100, -2)
               ELSE 0
           END)                                      AS valor_vendido_sueltas
FROM Tipo_entrada t
INNER JOIN Boleta b
    ON b.id_tipo_entrada = t.id_tipo_entrada
INNER JOIN Zona z
    ON b.id_zona = z.id_zona
WHERE b.estado <> 'anulada'
GROUP BY t.id_tipo_entrada, t.nombre, t.porcentaje_descuento
ORDER BY boletas_vendidas DESC;

-- ---------------------------------------------------------------------
-- Reporte 7 · Ingresos por plan turístico (intermedio)
-- Pregunta: ¿cuánto dinero han generado los pagos exitosos de cada plan
-- y cuántas reservas pagadas tiene cada uno?
-- Tablas: Plan, Reserva y Pago.
-- Solo cuentan los pagos 'exitoso' (no los fallidos, pendientes ni
-- reembolsados). COUNT(DISTINCT) cuenta cada reserva una sola vez
-- aunque tenga varios pagos (anticipo y saldo).
-- ---------------------------------------------------------------------
SELECT pl.id_plan,
       pl.nombre                      AS plan_turistico,
       COUNT(DISTINCT r.id_reserva)   AS reservas_pagadas,
       SUM(p.monto)                   AS ingresos
FROM Plan pl
INNER JOIN Reserva r
    ON r.id_plan = pl.id_plan
INNER JOIN Pago p
    ON p.id_reserva = r.id_reserva
WHERE p.estado = 'exitoso'
GROUP BY pl.id_plan, pl.nombre
ORDER BY ingresos DESC;


-- =====================================================================
-- REPORTES AVANZADOS (consultas con subconsultas)
-- =====================================================================

-- ---------------------------------------------------------------------
-- Reporte 8 · Combos más vendidos (avanzado)  [AJUSTADO]
-- Pregunta: ¿cuáles son los combos turísticos más vendidos y cuántas
-- veces ha sido comprado cada uno?
-- Tablas: Combo, Boleta y Combo_atraccion.
-- Ajuste: en la Entrega 2 se contaban las reservas con boletas del combo
-- y el resultado era una estimación (dos unidades en la misma reserva
-- contaban como una). Ahora se calculan las UNIDADES exactas:
--   cada unidad de combo genera una boleta por cada atracción del combo
--   (regla de la Entrega 1), así que
--   unidades = boletas no anuladas del combo / atracciones del combo.
-- Subconsultas (todas correlacionadas con el combo de la fila, c.id_combo):
--   compras            -> reservas distintas que compraron el combo
--   unidades_vendidas  -> boletas del combo / atracciones del combo
--   ingresos           -> unidades x precio especial (el combo se cobra
--                         por unidad, no por el precio de cada zona)
-- Aparecen todos los combos, también los que no se han vendido (con 0).
-- En MySQL la división "/" da un decimal (por ejemplo 5.0000); ROUND(..., 0)
-- lo deja como número entero de unidades y de pesos.
-- ---------------------------------------------------------------------
SELECT c.id_combo,
       c.nombre                    AS nombre_combo,
       c.precio_especial,
       c.estado,
       (SELECT COUNT(DISTINCT b.id_reserva)
        FROM Boleta b
        WHERE b.id_combo = c.id_combo
          AND b.estado <> 'anulada')                   AS compras,
       ROUND((SELECT COUNT(*)
              FROM Boleta b
              WHERE b.id_combo = c.id_combo
                AND b.estado <> 'anulada')
             /
             (SELECT COUNT(*)
              FROM Combo_atraccion ca
              WHERE ca.id_combo = c.id_combo), 0)      AS unidades_vendidas,
       ROUND(c.precio_especial *
             (SELECT COUNT(*)
              FROM Boleta b
              WHERE b.id_combo = c.id_combo
                AND b.estado <> 'anulada')
             /
             (SELECT COUNT(*)
              FROM Combo_atraccion ca
              WHERE ca.id_combo = c.id_combo), 0)      AS ingresos
FROM Combo c
ORDER BY unidades_vendidas DESC, compras DESC;

-- ---------------------------------------------------------------------
-- Reporte 9 · Atracciones con mayor cantidad de boletas vendidas
-- (avanzado)  [AJUSTADO]
-- Pregunta: ¿cuáles son las atracciones que registran más boletas
-- vendidas y cuánto vendió cada una?
-- Tablas: Atraccion, Funcion, Zona, Boleta y Tipo_entrada.
-- Cada subconsulta recorre Funcion -> Zona -> Boleta para la atracción
-- de la fila (f.id_atraccion = a.id_atraccion). La boleta no guarda la
-- función ni la atracción: se llega a ellas por la zona.
-- Ajuste: se agrega el valor vendido (boletas sueltas: precio de la zona
-- menos el descuento del tipo), porque en el RF13 prometimos "ingresos
-- por atracción y tipo de entrada" (el tipo está en el Reporte 6).
-- ---------------------------------------------------------------------
SELECT a.id_atraccion,
       a.nombre                    AS nombre_atraccion,
       a.ubicacion,
       (SELECT COUNT(b.id_boleta)
        FROM Funcion f
        INNER JOIN Zona z   ON z.id_funcion = f.id_funcion
        INNER JOIN Boleta b ON b.id_zona = z.id_zona
        WHERE f.id_atraccion = a.id_atraccion
          AND b.estado <> 'anulada')                   AS boletas_vendidas,
       (SELECT COALESCE(SUM(ROUND(z.precio * (100 - t.porcentaje_descuento) / 100, -2)), 0)
        FROM Funcion f
        INNER JOIN Zona z         ON z.id_funcion = f.id_funcion
        INNER JOIN Boleta b       ON b.id_zona = z.id_zona
        INNER JOIN Tipo_entrada t ON b.id_tipo_entrada = t.id_tipo_entrada
        WHERE f.id_atraccion = a.id_atraccion
          AND b.estado <> 'anulada'
          AND b.id_combo IS NULL)                      AS valor_vendido_sueltas
FROM Atraccion a
ORDER BY boletas_vendidas DESC;

-- ---------------------------------------------------------------------
-- Reporte 10 · Funciones con mayor ocupación (avanzado)
-- Pregunta: ¿cuáles funciones tienen mayor porcentaje de ocupación según
-- las boletas vendidas y su capacidad?
-- Tablas: Funcion, Atraccion, Zona y Boleta.
-- La consulta de adentro (tabla derivada t) arma una fila por función
-- con dos subconsultas: la capacidad total (suma del aforo de sus zonas)
-- y las boletas vendidas (no anuladas). La consulta de afuera calcula el
-- porcentaje. WHERE capacidad_total > 0 evita dividir entre cero.
-- ---------------------------------------------------------------------
SELECT t.id_funcion,
       t.nombre_atraccion,
       t.fecha,
       t.hora_apertura,
       t.hora_cierre,
       t.capacidad_total,
       t.boletas_vendidas,
       ROUND(t.boletas_vendidas * 100 / t.capacidad_total, 1) AS porcentaje_ocupacion
FROM (
    SELECT f.id_funcion,
           a.nombre AS nombre_atraccion,
           f.fecha,
           f.hora_apertura,
           f.hora_cierre,
           (SELECT SUM(z.aforo)
            FROM Zona z
            WHERE z.id_funcion = f.id_funcion)          AS capacidad_total,
           (SELECT COUNT(b.id_boleta)
            FROM Zona z
            INNER JOIN Boleta b ON b.id_zona = z.id_zona
            WHERE z.id_funcion = f.id_funcion
              AND b.estado <> 'anulada')                AS boletas_vendidas
    FROM Funcion f
    INNER JOIN Atraccion a ON f.id_atraccion = a.id_atraccion
) t
WHERE t.capacidad_total > 0
ORDER BY porcentaje_ocupacion DESC;
