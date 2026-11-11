-- =====================================================================
-- QuindíoStay · Verificación del checklist de la Parte A
-- Cada consulta comprueba un punto del checklist (las secciones son las
-- del enunciado del proyecto). Al lado de cada una dice qué debe salir.
-- Cómo usarlo: cargar la base (00 -> 01 -> 02 -> 03) y ejecutar las
-- consultas una por una con Ctrl + Enter. Ninguna cambia datos.
-- Las que dicen "debe dar 0" buscan VIOLACIONES de una regla.
-- =====================================================================

USE quindiostay;

-- =====================================================================
-- 1. SCRIPTS DE LA BASE: TABLAS Y RESTRICCIONES
-- =====================================================================

-- 1.1 Las tablas (3.1 y 2.9). Debe dar 20 filas.
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'quindiostay' AND table_type = 'BASE TABLE'
ORDER BY table_name;

-- 1.2 Cada tabla tiene comentario (3.3 y 6). Debe dar 0 filas
--     (si sale alguna, esa tabla no tiene COMMENT).
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'quindiostay' AND table_type = 'BASE TABLE'
  AND (table_comment IS NULL OR table_comment = '');

-- 1.3 Hay PK, FK, CHECK y UNIQUE (6). Debe salir una fila por cada tipo:
--     CHECK, FOREIGN KEY, PRIMARY KEY y UNIQUE, cada una con su cantidad.
SELECT constraint_type, COUNT(*) AS cantidad
FROM information_schema.table_constraints
WHERE table_schema = 'quindiostay'
GROUP BY constraint_type
ORDER BY constraint_type;

-- 1.4 Hay NOT NULL (6). Debe dar un número grande (columnas obligatorias).
SELECT COUNT(*) AS columnas_not_null
FROM information_schema.columns
WHERE table_schema = 'quindiostay' AND is_nullable = 'NO';

-- 1.5 Cada FK tiene su acción al borrar (3.2). Deben salir RESTRICT y
--     CASCADE con su cantidad.
SELECT delete_rule, COUNT(*) AS foraneas
FROM information_schema.referential_constraints
WHERE constraint_schema = 'quindiostay'
GROUP BY delete_rule;

-- 1.6 Contraseñas cifradas, nunca tal cual (1.2 y 1.7). Las dos deben dar 0.
SELECT COUNT(*) AS cuentas_sin_cifrar
FROM Cuenta WHERE contrasena_cifrada NOT LIKE '$2a$%';
SELECT COUNT(*) AS usuarios_sin_cifrar
FROM Usuario_sistema WHERE contrasena_cifrada NOT LIKE '$2a$%';

-- 1.7 Que las restricciones funcionan: ejecutar el archivo
--     pruebas_restricciones.sql. Cada prueba que "debe fallar" falla con
--     el error de su regla (3819 CHECK, 1062 repetido, 1452 padre no
--     existe, 1451 RESTRICT).

-- =====================================================================
-- 2. DATOS DE EJEMPLO (sección 4)
-- =====================================================================

-- 2.1 Mínimos: está al final del 02 (conteo con UNION ALL).
--     Comparar: Municipio 12, Tipo_prestador >= 4, Prestador >= 20,
--     Cliente >= 150, Reserva >= 500, Pago >= 500, Plan >= 40,
--     Notificacion >= 800, Usuario_sistema >= 5.

-- 2.2 Prestadores repartidos de forma DESIGUAL entre municipios.
--     Deben salir cantidades distintas (Armenia 6 ... cinco municipios con 0).
SELECT m.nombre AS municipio, COUNT(p.id_prestador) AS prestadores
FROM Municipio m
LEFT JOIN Prestador p ON p.id_municipio = m.id_municipio
GROUP BY m.nombre
ORDER BY prestadores DESC;

-- 2.3 Clientes: ciudades variadas y algunos extranjeros.
--     Debe dar 36 ciudades, de ellas 142 clientes de fuera del Quindío,
--     y 20 extranjeros.
SELECT COUNT(DISTINCT ciudad_origen) AS ciudades FROM Cliente;
SELECT COUNT(*) AS clientes_de_fuera_del_quindio
FROM Cliente
WHERE ciudad_origen NOT IN ('Armenia', 'Buenavista', 'Calarcá', 'Circasia', 'Córdoba',
                            'Filandia', 'Génova', 'La Tebaida', 'Montenegro', 'Pijao',
                            'Quimbaya', 'Salento');
SELECT COUNT(*) AS extranjeros
FROM Cliente
WHERE tipo_documento IN ('pasaporte', 'cédula de extranjería');

-- 2.4 La mayoría con cuenta y perfil. Debe dar 150 clientes y 126 con cuenta.
SELECT COUNT(*) AS clientes,
       (SELECT COUNT(*) FROM Cuenta) AS con_cuenta,
       (SELECT COUNT(*) FROM Perfil_cliente) AS con_perfil
FROM Cliente;

-- 2.5 Reservas repartidas en 2025-2026. Deben salir solo 2025 y 2026.
SELECT YEAR(fecha_servicio) AS anio, COUNT(*) AS reservas
FROM Reserva GROUP BY anio;

-- 2.6 Más reservas en temporada alta. Diciembre, julio y enero deben
--     tener muchas más reservas que febrero o septiembre.
SELECT YEAR(fecha_servicio) AS anio, MONTH(fecha_servicio) AS mes, COUNT(*) AS reservas
FROM Reserva
GROUP BY anio, mes
ORDER BY anio, mes;

-- 2.7 Pagos: reservas con más de un pago (anticipo y saldo) y pagos
--     fallidos. Debe dar 128 reservas con varios pagos y 52 fallidos.
SELECT COUNT(*) AS reservas_con_varios_pagos
FROM (SELECT id_reserva
      FROM Pago
      WHERE estado IN ('exitoso', 'reembolsado')
      GROUP BY id_reserva
      HAVING COUNT(*) > 1) t;
SELECT COUNT(*) AS pagos_fallidos FROM Pago WHERE estado = 'fallido';

-- 2.8 Planes: varios por prestador, algunos inactivos o vencidos.
--     Debe dar 5 inactivos y 14 vencidos (fecha_fin antes del 5/11/2026).
SELECT id_prestador, COUNT(*) AS planes FROM Plan GROUP BY id_prestador;
SELECT SUM(estado = 'inactivo') AS inactivos,
       SUM(fecha_fin < '2026-11-05') AS vencidos
FROM Plan;

-- 2.9 Notificaciones de todos los tipos y canales, y algunas fallidas.
--     Deben salir los 5 tipos con los 3 canales (15 combinaciones).
SELECT tipo, canal, COUNT(*) AS notificaciones
FROM Notificacion
GROUP BY tipo, canal
ORDER BY tipo, canal;
SELECT COUNT(*) AS fallidas FROM Notificacion WHERE estado = 'fallida';

-- 2.10 Usuarios: al menos un administrador y un encargado.
--      Debe dar administrador 1, encargado 2, operador 2.
SELECT r.nombre AS rol, COUNT(*) AS usuarios
FROM Usuario_sistema u
INNER JOIN Rol r ON u.id_rol = r.id_rol
GROUP BY r.nombre;

-- 2.11 Teléfonos de 10 dígitos que empiezan por 3. Las dos deben dar 0.
SELECT COUNT(*) AS telefonos_mal_cliente
FROM Cliente WHERE telefono NOT REGEXP '^3[0-9]{9}$';
SELECT COUNT(*) AS telefonos_mal_prestador
FROM Prestador WHERE telefono NOT REGEXP '^3[0-9]{9}$';

-- 2.12 Nombres verosímiles (nada como "fghtree"): revisar a ojo.
SELECT primer_nombre, segundo_nombre, primer_apellido, segundo_apellido, ciudad_origen
FROM Cliente ORDER BY RAND() LIMIT 20;

-- =====================================================================
-- 3. REGLAS DEL NEGOCIO (secciones 1 y 2.9). TODAS DEBEN DAR 0.
-- =====================================================================

-- 3.1 Solo se reserva un plan activo y vigente en la fecha del servicio (1.4).
SELECT COUNT(*) AS plan_no_vigente
FROM Reserva r
INNER JOIN Plan pl ON r.id_plan = pl.id_plan
WHERE pl.estado <> 'activo'
   OR r.fecha_servicio NOT BETWEEN pl.fecha_inicio AND pl.fecha_fin;

-- 3.2 Reserva pagada = pagos exitosos cubren el valor total (1.5).
--     Completadas y confirmadas sin pagar del todo:
SELECT COUNT(*) AS pagadas_sin_cubrir
FROM Reserva r
WHERE r.estado IN ('completada', 'confirmada')
  AND (SELECT COALESCE(SUM(p.monto), 0)
       FROM Pago p
       WHERE p.id_reserva = r.id_reserva AND p.estado = 'exitoso') < r.valor_total;

-- 3.3 Pagos en línea con referencia de la transacción (1.5).
SELECT COUNT(*) AS en_linea_sin_referencia
FROM Pago
WHERE metodo IN ('tarjeta de crédito', 'tarjeta débito', 'PSE')
  AND referencia_transaccion IS NULL;

-- 3.4 Solo hay reembolsos en reservas canceladas (1.5).
SELECT COUNT(*) AS reembolso_sin_cancelar
FROM Pago p
INNER JOIN Reserva r ON p.id_reserva = r.id_reserva
WHERE p.estado = 'reembolsado' AND r.estado <> 'cancelada';

-- 3.5 Una reserva cancelada no tiene boletas activas (sus boletas se
--     anulan y liberan el aforo).
SELECT COUNT(*) AS cancelada_con_boleta_activa
FROM Boleta b
INNER JOIN Reserva r ON b.id_reserva = r.id_reserva
WHERE r.estado = 'cancelada' AND b.estado <> 'anulada';

-- 3.6 Recordatorios según el perfil: la fecha (servicio menos la
--     anticipación) y el canal preferido (1.6).
SELECT COUNT(*) AS recordatorio_fecha_mal
FROM Notificacion n
INNER JOIN Reserva r        ON n.id_reserva = r.id_reserva
INNER JOIN Perfil_cliente p ON r.id_cliente = p.id_cliente
WHERE n.tipo = 'recordatorio de reserva'
  AND DATE(n.fecha_programada) <> DATE_SUB(r.fecha_servicio, INTERVAL p.anticipacion_recordatorio DAY);
SELECT COUNT(*) AS recordatorio_canal_mal
FROM Notificacion n
INNER JOIN Reserva r        ON n.id_reserva = r.id_reserva
INNER JOIN Perfil_cliente p ON r.id_cliente = p.id_cliente
WHERE n.tipo = 'recordatorio de reserva'
  AND n.canal <> p.canal_preferido;

-- 3.7 Promociones solo a clientes que las aceptaron (1.6).
SELECT COUNT(*) AS promocion_sin_aceptar
FROM Notificacion n
INNER JOIN Perfil_cliente p ON n.id_cliente = p.id_cliente
WHERE n.tipo = 'promoción' AND p.acepta_promociones <> 'sí';

-- 3.8 Ninguna notificación enviada antes de su fecha programada (1.6).
SELECT COUNT(*) AS enviada_antes
FROM Notificacion
WHERE fecha_real_envio < fecha_programada;

-- 3.9 Todo encargado de prestador tiene su prestador (1.7).
SELECT COUNT(*) AS encargado_sin_prestador
FROM Usuario_sistema u
INNER JOIN Rol r ON u.id_rol = r.id_rol
WHERE r.nombre = 'encargado de prestador' AND u.id_prestador IS NULL;

-- 3.10 No se venden más boletas que el aforo de la zona (2.9).
SELECT z.id_zona, z.aforo, COUNT(b.id_boleta) AS vendidas
FROM Zona z
INNER JOIN Boleta b ON b.id_zona = z.id_zona AND b.estado <> 'anulada'
GROUP BY z.id_zona, z.aforo
HAVING COUNT(b.id_boleta) > z.aforo;

-- 3.11 Una entrada de niño no se vende sin una de adulto en la misma
--      compra (2.9).
SELECT id_reserva
FROM Boleta
GROUP BY id_reserva
HAVING SUM(id_tipo_entrada = 2) > 0 AND SUM(id_tipo_entrada = 1) = 0;

-- 3.12 Las boletas usadas son de reservas completadas (una boleta solo se
--      usa cuando la visita ya pasó).
SELECT COUNT(*) AS usada_sin_completar
FROM Boleta b
INNER JOIN Reserva r ON b.id_reserva = r.id_reserva
WHERE b.estado = 'usada' AND r.estado <> 'completada';

-- 3.13 Decisión de diseño (2.9): combos con entradas usadas en días
--      diferentes. Debe dar MÁS de 0 (aquí sí queremos que existan: 25).
SELECT COUNT(*) AS combos_en_dias_distintos
FROM (SELECT b.id_reserva
      FROM Boleta b
      INNER JOIN Zona z    ON b.id_zona = z.id_zona
      INNER JOIN Funcion f ON z.id_funcion = f.id_funcion
      WHERE b.id_combo IS NOT NULL
      GROUP BY b.id_reserva
      HAVING COUNT(DISTINCT f.fecha) > 1) t;

-- =====================================================================
-- 4. CONSULTAS DE LOS REPORTES (sección 5.2)
-- =====================================================================
-- 4.1 Ejecutar los 10 reportes de 04_reportes.sql: ninguno da error.
-- 4.2 Revisar su nivel: reportes 1, 2 y 3 de una sola tabla; 4, 5, 6 y 7
--     multitabla; 8, 9 y 10 con subconsultas.
-- 4.3 Lo que el negocio quiere ver (2.9): aforo vendido por función
--     (reportes 5 y 10), ingresos por atracción (9) y por tipo de entrada
--     (6), y combos más vendidos (8).
-- 4.4 Consultas puntuales (5.2): los datos de un cliente, las reservas de
--     un prestador y los pagos de una reserva.
SELECT * FROM Cliente WHERE id_cliente = 1;
SELECT * FROM vista_reserva_detalle WHERE id_prestador = 1;
SELECT * FROM Pago WHERE id_reserva = 1;
