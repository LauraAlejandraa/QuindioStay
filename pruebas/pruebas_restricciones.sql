USE quindiostay;

-- PRUEBAS DE QUE LAS RESTRICCIONES SI SIRVEN -------------------
-- BLOQUE 1----
INSERT INTO Municipio (nombre, codigo_DANE) VALUES ('Prueba', '123');
INSERT INTO Rol (nombre) VALUES ('gerente');


-- Prueba UNIQUE compuesto: el segundo INSERT debe fallar
-- BLOQUE 2 ----
INSERT INTO Cliente (primer_nombre, primer_apellido, tipo_documento, numero_documento,
                     email, telefono, fecha_nacimiento, ciudad_origen)
VALUES ('Ana', 'Gómez', 'cédula de ciudadanía', '1094000111',
        'ana.gomez@gmail.com', '3104567890', '1998-05-12', 'Armenia');

INSERT INTO Cliente (primer_nombre, primer_apellido, tipo_documento, numero_documento,
                     email, telefono, fecha_nacimiento, ciudad_origen)
VALUES ('Luis', 'Pérez', 'cédula de ciudadanía', '1094000111',
        'luis.perez@gmail.com', '3157894561', '1995-08-20', 'Pereira');
	
-- PORCENTAJE DE DESCUENTO NO ESTA ENTRE 0 Y 100
INSERT INTO Tipo_entrada (nombre, porcentaje_descuento) VALUES ('adulto', 150);

-- PRECIO ESPECIAL DEBE SER MAYOR A 0
INSERT INTO Combo (nombre, descripcion, precio_especial)
VALUES ('Ruta del café', 'Entradas a dos parques', 0);

-- Datos de apoyo (deben funcionar)
-- BLOQUE 3
INSERT INTO Municipio (nombre, codigo_DANE) VALUES ('Armenia', '63001');
INSERT INTO Tipo_prestador (nombre) VALUES ('atracción');
INSERT INTO Prestador (nombre_comercial, NIT_documento, RNT, direccion, telefono,
                       correo, id_municipio, id_tipo_prestador)
VALUES ('Parque Los Arrieros', '900123456-1', '12345', 'Vía Montenegro km 4',
        '3104445566', 'info@arrieros.com', 1, 1);
        
-- Prueba A. APUNTA A UN MUNICIPIO QUE NO EXISTE
INSERT INTO Prestador (nombre_comercial, NIT_documento, RNT, direccion, telefono,
                       correo, id_municipio, id_tipo_prestador)
VALUES ('Hotel Prueba', '900999888-2', '54321', 'Calle 10', '3001112233',
        'hotel@prueba.com', 999, 1);
        
-- Prueba B · NO SE PUEDE BORRAR UN MUNICIPIO CON PRESTADOR
DELETE FROM Municipio WHERE id_municipio = 1;

-- Prueba C · CASCADE: al borrar un cliente se borra automaticamente su cuenta
INSERT INTO Cliente (primer_nombre, primer_apellido, tipo_documento, numero_documento,
                     email, telefono, fecha_nacimiento, ciudad_origen)
VALUES ('Ana', 'Gómez', 'cédula de ciudadanía', '1094000111',
        'ana.gomez@gmail.com', '3104567890', '1998-05-12', 'Armenia');
INSERT INTO Cuenta (contrasena_cifrada, id_cliente) VALUES ('hash_de_prueba', 1);
SELECT * FROM Cuenta;
DELETE FROM Cliente WHERE id_cliente = 1;
SELECT * FROM Cuenta;

-- BLOQUE 4
INSERT INTO Municipio (nombre, codigo_DANE) VALUES ('Armenia', '63001');
INSERT INTO Tipo_prestador (nombre) VALUES ('atracción');
INSERT INTO Prestador (nombre_comercial, NIT_documento, RNT, direccion, telefono,
                       correo, id_municipio, id_tipo_prestador)
VALUES ('Parque Los Arrieros', '900123456-1', '12345', 'Vía Montenegro km 4',
        '3104445566', 'info@arrieros.com', 1, 1);
INSERT INTO Atraccion (nombre, descripcion, ubicacion)
VALUES ('Parque Los Arrieros', 'Parque temático de la cultura arriera', 'Quimbaya');

-- UN PRESTADOR NO PUEDE TENER LA MISMA ATRACCION DOS VECES
INSERT INTO Prestador_atraccion VALUES (1, 1, '2025-01-15');
INSERT INTO Prestador_atraccion VALUES (1, 1, '2026-03-10');


-- BLOQUE 5
-- DATOS DE APOYO (todos deben funcionar)
-- De qué se trata: para probar Reserva, Pago y Notificacion
-- necesitamos que existan sus padres, porque sin ellos las
-- foráneas no dejan insertar (error 1452).
INSERT INTO Municipio (nombre, codigo_DANE) VALUES ('Armenia', '63001');
INSERT INTO Tipo_prestador (nombre) VALUES ('atracción');
INSERT INTO Prestador (nombre_comercial, NIT_documento, RNT, direccion, telefono,
                       correo, id_municipio, id_tipo_prestador)
VALUES ('Parque Los Arrieros', '900123456-1', '12345', 'Vía Montenegro km 4',
        '3104445566', 'info@arrieros.com', 1, 1);
INSERT INTO Cliente (primer_nombre, primer_apellido, tipo_documento, numero_documento,
                     email, telefono, fecha_nacimiento, ciudad_origen)
VALUES ('Ana', 'Gómez', 'cédula de ciudadanía', '1094000111',
        'ana.gomez@gmail.com', '3104567890', '1998-05-12', 'Armenia');
INSERT INTO Plan (nombre, descripcion, tipo, precio, fecha_inicio, fecha_fin, id_prestador)
VALUES ('Entrada general', 'Acceso a todas las atracciones del parque', 'básico',
        45000, '2026-01-01', '2026-12-31', 1);
INSERT INTO Reserva (fecha_creacion, fecha_servicio, valor_total, canal, id_cliente, id_plan)
VALUES ('2026-11-04', '2026-11-20', 45000, 'web', 1, 1);

-- PRUEBA 1 · Debe FALLAR una reserva no puede tener la fecha del servicio ANTES de la fecha en que se creó
INSERT INTO Reserva (fecha_creacion, fecha_servicio, valor_total, canal, id_cliente, id_plan)
VALUES ('2026-11-04', '2026-10-01', 45000, 'web', 1, 1);

-- PRUEBA 2 · Aquí se paga con tarjeta SIN referencia.
INSERT INTO Pago (fecha, monto, metodo, estado, id_reserva)
VALUES ('2026-11-04', 45000, 'tarjeta de crédito', 'exitoso', 1);

-- PRUEBA 3 · Debe FUNCIONAR: el mismo pago, pero en efectivo. El efectivo no pasa por pasarela, así que puede ir sin referencia.
INSERT INTO Pago (fecha, monto, metodo, estado, id_reserva)
VALUES ('2026-11-04', 45000, 'efectivo', 'exitoso', 1);

-- PRUEBA 4 · Una notificación va O a una reserva O a un cliente, nunca a los dos. Aquí se llenan los dos.
INSERT INTO Notificacion (tipo, canal, mensaje, fecha_programada, id_cliente, id_reserva)
VALUES ('recordatorio de reserva', 'correo', 'Tu visita es en 3 días',
        '2026-11-17 08:00:00', 1, 1);

-- PRUEBA 5 · una notificación no se puede haber enviado ANTES de la fecha en que estaba programada
INSERT INTO Notificacion (tipo, canal, mensaje, fecha_programada, fecha_real_envio,
                          estado, id_reserva)
VALUES ('recordatorio de reserva', 'correo', 'Tu visita es en 3 días',
        '2026-11-17 08:00:00', '2026-11-15 08:00:00', 'enviada', 1);

-- PRUEBA 6 · RESTRICT. La reserva 1 ya tiene un pago (el de la prueba 3). Si dejáramos borrarla, se perdería el registro contable.
DELETE FROM Reserva WHERE id_reserva = 1;