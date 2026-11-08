-- =====================================================================
-- QuindíoStay · Bases de Datos I · Grupo 02D · 2026-2
-- Script 03: vistas
-- Motor: MySQL 8.0.46
-- Estilo de la clase (Clase 15): alias de tabla sin AS, toda columna con
-- el alias de su tabla, y en cada ON siempre FK = PK (la foránea de la
-- tabla hija = la primaria de la tabla padre).
-- Ejecutar DESPUÉS de 00, 01 y 02.
-- Una vista es una consulta guardada con nombre: no guarda datos propios,
-- cada vez que se consulta MySQL ejecuta su SELECT. Se usan para no
-- repetir los JOIN largos en la aplicación y en los reportes.
-- CREATE OR REPLACE permite correr el script varias veces sin error.
-- =====================================================================

USE quindiostay;

-- ---------------------------------------------------------------------
-- Vista 1 · vista_boleta_detalle
-- Cada boleta con todo lo que la describe: su reserva y cliente, la
-- zona, la función (fecha y horario), la atracción, el tipo de entrada,
-- el combo (si salió de uno) y su valor.
-- Recorre el camino Boleta -> Zona -> Funcion -> Atraccion: la boleta
-- no guarda la función, se obtiene por la zona (decisión de la Entrega 1).
-- valor_boleta: precio de la zona menos el descuento del tipo de entrada,
-- redondeado a la centena. Si la boleta salió de un combo queda en NULL,
-- porque el combo se cobra por unidad con su precio especial.
-- Uso: taquilla (validar por código, RF09), plantilla de boleta y
-- reportes de boletería.
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW vista_boleta_detalle AS
SELECT b.id_boleta,
       b.codigo,
       b.estado                AS estado_boleta,
       b.id_reserva,
       r.id_cliente,
       a.id_atraccion,
       a.nombre                AS atraccion,
       f.id_funcion,
       f.fecha                 AS fecha_funcion,
       f.hora_apertura,
       f.hora_cierre,
       f.estado                AS estado_funcion,
       z.id_zona,
       z.nombre                AS zona,
       z.precio                AS precio_zona,
       t.id_tipo_entrada,
       t.nombre                AS tipo_entrada,
       t.porcentaje_descuento,
       b.id_combo,
       c.nombre                AS combo,
       CASE
           WHEN b.id_combo IS NULL
           THEN ROUND(z.precio * (100 - t.porcentaje_descuento) / 100, -2)
           ELSE NULL
       END                     AS valor_boleta
FROM Boleta b
INNER JOIN Reserva r      ON b.id_reserva = r.id_reserva
INNER JOIN Zona z         ON b.id_zona = z.id_zona
INNER JOIN Funcion f      ON z.id_funcion = f.id_funcion
INNER JOIN Atraccion a    ON f.id_atraccion = a.id_atraccion
INNER JOIN Tipo_entrada t ON b.id_tipo_entrada = t.id_tipo_entrada
LEFT JOIN Combo c         ON b.id_combo = c.id_combo;

-- ---------------------------------------------------------------------
-- Vista 2 · vista_ocupacion_zona
-- Cada zona con su aforo, las boletas vendidas (las que no están
-- anuladas) y los cupos que le quedan.
-- LEFT JOIN con Boleta: así también salen las zonas que no han vendido
-- nada (con 0 vendidas). La condición del estado va en el ON y no en un
-- WHERE, para no perder esas zonas.
-- Uso: la aplicación revisa los cupos ANTES de vender (regla del aforo)
-- y el Reporte 5.
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW vista_ocupacion_zona AS
SELECT z.id_zona,
       z.nombre                         AS zona,
       z.aforo,
       z.precio,
       f.id_funcion,
       f.fecha                          AS fecha_funcion,
       f.estado                         AS estado_funcion,
       a.id_atraccion,
       a.nombre                         AS atraccion,
       COUNT(b.id_boleta)               AS boletas_vendidas,
       z.aforo - COUNT(b.id_boleta)     AS cupos_disponibles
FROM Zona z
INNER JOIN Funcion f   ON z.id_funcion = f.id_funcion
INNER JOIN Atraccion a ON f.id_atraccion = a.id_atraccion
LEFT JOIN Boleta b     ON b.id_zona = z.id_zona
                      AND b.estado <> 'anulada'
GROUP BY z.id_zona, z.nombre, z.aforo, z.precio,
         f.id_funcion, f.fecha, f.estado, a.id_atraccion, a.nombre;

-- ---------------------------------------------------------------------
-- Vista 3 · vista_estado_pago_reserva
-- Cada reserva con lo que se ha pagado y si está pagada.
-- Regla del enunciado (1.5): una reserva se considera pagada solo cuando
-- la suma de sus pagos EXITOSOS cubre su valor total. Los pagos fallidos,
-- pendientes y reembolsados no cuentan.
-- La suma se calcula con una subconsulta por cada reserva; COALESCE
-- convierte en 0 el NULL de las reservas que no tienen ningún pago.
-- Uso: comprobantes, factura, y la regla de que una boleta solo pasa a
-- 'usada' si su reserva está pagada.
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW vista_estado_pago_reserva AS
SELECT r.id_reserva,
       r.id_cliente,
       r.fecha_creacion,
       r.fecha_servicio,
       r.estado                                AS estado_reserva,
       r.valor_total,
       COALESCE((SELECT SUM(p.monto)
                 FROM Pago p
                 WHERE p.id_reserva = r.id_reserva
                   AND p.estado = 'exitoso'), 0) AS total_pagado,
       r.valor_total - COALESCE((SELECT SUM(p.monto)
                                 FROM Pago p
                                 WHERE p.id_reserva = r.id_reserva
                                   AND p.estado = 'exitoso'), 0) AS saldo,
       CASE
           WHEN COALESCE((SELECT SUM(p.monto)
                          FROM Pago p
                          WHERE p.id_reserva = r.id_reserva
                            AND p.estado = 'exitoso'), 0) >= r.valor_total
           THEN 'sí'
           ELSE 'no'
       END                                     AS pagada
FROM Reserva r;

-- ---------------------------------------------------------------------
-- Vista 4 · vista_notificacion_cliente
-- Todas las notificaciones con el cliente al que van.
-- Desde la Entrega 2 (3FN), una notificación de una reserva guarda solo
-- id_reserva (el cliente se obtiene por la reserva) y una promoción
-- guarda solo id_cliente. La vista junta los dos casos:
-- COALESCE toma n.id_cliente si existe (promoción) y, si es NULL, toma
-- el cliente de la reserva (r.id_cliente).
-- LEFT JOIN con Reserva para no perder las promociones (no tienen reserva).
-- Uso: historial de notificaciones del cliente (RF05).
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW vista_notificacion_cliente AS
SELECT n.id_notificacion,
       COALESCE(n.id_cliente, r.id_cliente) AS id_cliente,
       n.id_reserva,
       n.tipo,
       n.canal,
       n.mensaje,
       n.fecha_programada,
       n.fecha_real_envio,
       n.estado
FROM Notificacion n
LEFT JOIN Reserva r ON n.id_reserva = r.id_reserva;

-- ---------------------------------------------------------------------
-- Vista 5 · vista_reserva_detalle
-- Cada reserva con los datos de su cliente, su plan, el prestador que la
-- vende y el municipio del prestador.
-- La reserva no guarda el prestador: se obtiene por el plan (3FN,
-- Entrega 2). Por eso el camino es Reserva -> Plan -> Prestador.
-- CONCAT_WS une los nombres con un espacio y se salta los NULL (el
-- segundo nombre y el segundo apellido son opcionales).
-- Uso: plantillas (comprobante de reserva, comprobante de pago, factura),
-- consultas del RF14 (reservas de un prestador, datos de un cliente) y el
-- filtro del encargado de prestador (WHERE id_prestador = ...).
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW vista_reserva_detalle AS
SELECT r.id_reserva,
       r.fecha_creacion,
       r.fecha_servicio,
       r.estado                AS estado_reserva,
       r.valor_total,
       r.canal,
       c.id_cliente,
       CONCAT_WS(' ', c.primer_nombre, c.segundo_nombre,
                      c.primer_apellido, c.segundo_apellido) AS cliente,
       c.tipo_documento,
       c.numero_documento,
       c.email,
       c.telefono,
       pl.id_plan,
       pl.nombre               AS plan,
       pl.tipo                 AS tipo_plan,
       pr.id_prestador,
       pr.nombre_comercial     AS prestador,
       pr.NIT_documento        AS nit_prestador,
       pr.RNT                  AS rnt_prestador,
       m.nombre                AS municipio_prestador
FROM Reserva r
INNER JOIN Cliente c    ON r.id_cliente = c.id_cliente
INNER JOIN Plan pl      ON r.id_plan = pl.id_plan
INNER JOIN Prestador pr ON pl.id_prestador = pr.id_prestador
INNER JOIN Municipio m  ON pr.id_municipio = m.id_municipio;

-- ---------------------------------------------------------------------
-- Vista 6 · vista_planes_para_vender
-- Los planes con los que se pueden vender boletas de cada atracción:
-- planes activos de prestadores activos y afiliados a la atracción.
-- Regla del enunciado (1.4) y de la Entrega 1: solo se reserva un plan
-- activo y vigente en la fecha del servicio, de un prestador afiliado a
-- la atracción de las boletas.
-- La vista resuelve "activo" y "afiliado". La VIGENCIA depende de la
-- fecha del servicio, así que la aplicación la filtra al consultar:
--   SELECT * FROM vista_planes_para_vender
--   WHERE id_atraccion = ? AND ? BETWEEN fecha_inicio AND fecha_fin;
-- (el segundo ? es la fecha de la función que se va a vender)
-- Para un combo, el prestador debe aparecer en TODAS sus atracciones.
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW vista_planes_para_vender AS
SELECT pa.id_atraccion,
       a.nombre                AS atraccion,
       pr.id_prestador,
       pr.nombre_comercial     AS prestador,
       pa.fecha_afiliacion,
       pl.id_plan,
       pl.nombre               AS plan,
       pl.tipo                 AS tipo_plan,
       pl.precio,
       pl.fecha_inicio,
       pl.fecha_fin
FROM Prestador_atraccion pa
INNER JOIN Atraccion a  ON pa.id_atraccion = a.id_atraccion
INNER JOIN Prestador pr ON pa.id_prestador = pr.id_prestador
INNER JOIN Plan pl      ON pl.id_prestador = pr.id_prestador
WHERE pl.estado = 'activo'
  AND pr.estado = 'activo';
