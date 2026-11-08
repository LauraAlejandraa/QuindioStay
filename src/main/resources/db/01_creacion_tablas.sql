-- =====================================================================
-- QuindíoStay · Bases de Datos I · Grupo 02D · 2026-2
-- Script 01: creación de tablas con sus restricciones
-- Motor: MySQL 8.0.46
-- Ejecutar DESPUÉS de 00_limpieza.sql, conectada como root.
-- Orden: primero las tablas PADRE (no apuntan a nadie) y después las
-- HIJAS, porque una clave foránea no puede apuntar a una tabla que
-- todavía no existe (Clase 10).
-- Nombres de restricciones: pk_ primaria, uq_ única, ck_ CHECK, fk_ foránea.
-- =====================================================================

USE quindiostay;

-- ---------------------------------------------------------------------
-- BLOQUE 1 · Tablas PADRE del núcleo (catálogos)
-- ---------------------------------------------------------------------

-- Municipio: los 12 municipios del Quindío con los que trabaja la
-- plataforma. Ni el nombre ni el código DANE se repiten.
-- El código DANE es CHAR(5) porque siempre tiene 5 caracteres y no se
-- hacen cuentas con él (Clase 8).
CREATE TABLE Municipio (
    id_municipio  INT          NOT NULL AUTO_INCREMENT,
    nombre        VARCHAR(60)  NOT NULL,
    codigo_DANE   CHAR(5)      NOT NULL,
    CONSTRAINT pk_municipio        PRIMARY KEY (id_municipio),
    CONSTRAINT uq_municipio_nombre UNIQUE (nombre),
    CONSTRAINT uq_municipio_dane   UNIQUE (codigo_DANE),
    CONSTRAINT ck_municipio_dane   CHECK (codigo_DANE REGEXP '^[0-9]{5}$')
) COMMENT = 'Municipios del Quindío donde operan los prestadores';

-- Tipo_prestador: clasificación de los prestadores (alojamiento,
-- agencia de viajes, atracción...). El nombre no se repite.
-- No lleva CHECK con lista cerrada porque el enunciado permite
-- "otros que ustedes puedan sustentar".
CREATE TABLE Tipo_prestador (
    id_tipo_prestador  INT          NOT NULL AUTO_INCREMENT,
    nombre             VARCHAR(40)  NOT NULL,
    CONSTRAINT pk_tipo_prestador        PRIMARY KEY (id_tipo_prestador),
    CONSTRAINT uq_tipo_prestador_nombre UNIQUE (nombre)
) COMMENT = 'Tipos de prestador turístico';

-- Rol: los tres roles de los usuarios internos. Es una lista cerrada
-- del enunciado, por eso lleva CHECK con IN.
CREATE TABLE Rol (
    id_rol  INT          NOT NULL AUTO_INCREMENT,
    nombre  VARCHAR(40)  NOT NULL,
    CONSTRAINT pk_rol        PRIMARY KEY (id_rol),
    CONSTRAINT uq_rol_nombre UNIQUE (nombre),
    CONSTRAINT ck_rol_nombre CHECK (nombre IN ('administrador de la plataforma',
                                               'encargado de prestador',
                                               'operador'))
) COMMENT = 'Roles de los usuarios internos del sistema';

-- ---------------------------------------------------------------------
-- BLOQUE 2 · Otras tablas PADRE: Cliente y los catálogos del módulo de
-- boletería (Atraccion, Tipo_entrada, Combo). Tampoco apuntan a nadie.
-- ---------------------------------------------------------------------

-- Cliente: personas que hacen reservas. Puede existir sin cuenta (si se
-- registró en el mostrador). El par tipo + número de documento no se
-- repite, y el correo tampoco.
-- segundo_nombre y segundo_apellido son opcionales (muchos clientes,
-- sobre todo extranjeros, no tienen).
-- La regla "fecha_nacimiento no posterior a hoy" NO va en un CHECK:
-- MySQL no permite CURDATE() en un CHECK. La valida la aplicación.
CREATE TABLE Cliente (
    id_cliente        INT           NOT NULL AUTO_INCREMENT,
    primer_nombre     VARCHAR(40)   NOT NULL,
    segundo_nombre    VARCHAR(40),
    primer_apellido   VARCHAR(40)   NOT NULL,
    segundo_apellido  VARCHAR(40),
    tipo_documento    VARCHAR(25)   NOT NULL,
    numero_documento  VARCHAR(20)   NOT NULL,
    email             VARCHAR(100)  NOT NULL,
    telefono          VARCHAR(15)   NOT NULL,
    fecha_nacimiento  DATE          NOT NULL,
    ciudad_origen     VARCHAR(60)   NOT NULL,
    CONSTRAINT pk_cliente           PRIMARY KEY (id_cliente),
    CONSTRAINT uq_cliente_documento UNIQUE (tipo_documento, numero_documento),
    CONSTRAINT uq_cliente_email     UNIQUE (email),
    CONSTRAINT ck_cliente_tipo_doc  CHECK (tipo_documento IN ('cédula de ciudadanía',
                                                              'cédula de extranjería',
                                                              'pasaporte',
                                                              'tarjeta de identidad')),
    CONSTRAINT ck_cliente_email     CHECK (email LIKE '%_@_%._%'),
    CONSTRAINT ck_cliente_telefono  CHECK (telefono REGEXP '^[0-9]{7,15}$')
) COMMENT = 'Clientes de la plataforma, con o sin cuenta';

-- Atraccion: parques, museos, jardines o festivales que venden entradas.
-- Se relaciona con Prestador por medio de Prestador_atraccion (N:M).
CREATE TABLE Atraccion (
    id_atraccion  INT           NOT NULL AUTO_INCREMENT,
    nombre        VARCHAR(100)  NOT NULL,
    descripcion   VARCHAR(255)  NOT NULL,
    ubicacion     VARCHAR(150)  NOT NULL,
    CONSTRAINT pk_atraccion PRIMARY KEY (id_atraccion)
) COMMENT = 'Atracciones turísticas que venden boletas';

-- Tipo_entrada: adulto, niño, adulto mayor o estudiante, cada uno con
-- su porcentaje de descuento sobre el precio de la zona.
CREATE TABLE Tipo_entrada (
    id_tipo_entrada       INT           NOT NULL AUTO_INCREMENT,
    nombre                VARCHAR(20)   NOT NULL,
    porcentaje_descuento  DECIMAL(5,2)  NOT NULL,
    CONSTRAINT pk_tipo_entrada        PRIMARY KEY (id_tipo_entrada),
    CONSTRAINT uq_tipo_entrada_nombre UNIQUE (nombre),
    CONSTRAINT ck_tipo_entrada_nombre CHECK (nombre IN ('adulto', 'niño',
                                                        'adulto mayor', 'estudiante')),
    CONSTRAINT ck_tipo_entrada_desc   CHECK (porcentaje_descuento BETWEEN 0 AND 100)
) COMMENT = 'Tipos de entrada con su porcentaje de descuento';

-- Combo: oferta que agrupa entradas a varias atracciones con un precio
-- especial. Un combo vendido no se borra: se inactiva.
CREATE TABLE Combo (
    id_combo         INT            NOT NULL AUTO_INCREMENT,
    nombre           VARCHAR(100)   NOT NULL,
    descripcion      VARCHAR(255)   NOT NULL,
    precio_especial  DECIMAL(12,2)  NOT NULL,
    estado           VARCHAR(10)    NOT NULL DEFAULT 'activo',
    CONSTRAINT pk_combo        PRIMARY KEY (id_combo),
    CONSTRAINT ck_combo_precio CHECK (precio_especial > 0),
    CONSTRAINT ck_combo_estado CHECK (estado IN ('activo', 'inactivo'))
) COMMENT = 'Combos de entradas a varias atracciones';

-- ---------------------------------------------------------------------
-- BLOQUE 3 · Primeras tablas HIJAS: sus padres ya existen.
-- Acción al borrar el padre (Entrega 1):
--   RESTRICT -> no deja borrar el padre si tiene hijas (protege historial)
--   CASCADE  -> al borrar el padre se borran sus hijas (no existen sin él)
-- ---------------------------------------------------------------------

-- Prestador: empresa o persona que ofrece un servicio turístico.
-- Pertenece a un municipio y a un tipo. El NIT o documento no se repite.
-- Un prestador retirado no se borra: cambia su estado a 'retirado'.
CREATE TABLE Prestador (
    id_prestador       INT           NOT NULL AUTO_INCREMENT,
    nombre_comercial   VARCHAR(100)  NOT NULL,
    NIT_documento      VARCHAR(20)   NOT NULL,
    RNT                VARCHAR(20)   NOT NULL,
    direccion          VARCHAR(150)  NOT NULL,
    telefono           VARCHAR(15)   NOT NULL,
    correo             VARCHAR(100)  NOT NULL,
    estado             VARCHAR(12)   NOT NULL DEFAULT 'activo',
    id_municipio       INT           NOT NULL,
    id_tipo_prestador  INT           NOT NULL,
    CONSTRAINT pk_prestador          PRIMARY KEY (id_prestador),
    CONSTRAINT uq_prestador_nit      UNIQUE (NIT_documento),
    CONSTRAINT ck_prestador_telefono CHECK (telefono REGEXP '^[0-9]{7,15}$'),
    CONSTRAINT ck_prestador_correo   CHECK (correo LIKE '%_@_%._%'),
    CONSTRAINT ck_prestador_estado   CHECK (estado IN ('activo', 'suspendido', 'retirado')),
    -- RESTRICT: no se borra un municipio que tenga prestadores
    CONSTRAINT fk_prestador_municipio
        FOREIGN KEY (id_municipio) REFERENCES Municipio (id_municipio)
        ON DELETE RESTRICT,
    -- RESTRICT: no se borra un tipo de prestador que esté en uso
    CONSTRAINT fk_prestador_tipo
        FOREIGN KEY (id_tipo_prestador) REFERENCES Tipo_prestador (id_tipo_prestador)
        ON DELETE RESTRICT
) COMMENT = 'Prestadores de servicios turísticos';

-- Cuenta: acceso en línea del cliente (relación 1:1 con Cliente).
-- El usuario es el email del cliente, por eso no se repite aquí.
-- La contraseña se guarda cifrada (hash BCrypt), nunca tal cual.
CREATE TABLE Cuenta (
    id_cuenta           INT           NOT NULL AUTO_INCREMENT,
    contrasena_cifrada  VARCHAR(255)  NOT NULL,
    id_cliente          INT           NOT NULL,
    CONSTRAINT pk_cuenta         PRIMARY KEY (id_cuenta),
    -- UNIQUE: un cliente tiene como máximo una cuenta (1:1)
    CONSTRAINT uq_cuenta_cliente UNIQUE (id_cliente),
    -- CASCADE: la cuenta no existe sin su cliente
    CONSTRAINT fk_cuenta_cliente
        FOREIGN KEY (id_cliente) REFERENCES Cliente (id_cliente)
        ON DELETE CASCADE
) COMMENT = 'Cuentas de acceso en línea de los clientes';

-- Perfil_cliente: preferencias del cliente (relación 1:1 con Cliente).
-- De aquí salen el canal y la anticipación de los recordatorios, y si
-- se le pueden enviar promociones.
CREATE TABLE Perfil_cliente (
    id_perfil                  INT          NOT NULL AUTO_INCREMENT,
    idioma                     VARCHAR(10)  NOT NULL,
    canal_preferido            VARCHAR(10)  NOT NULL,
    anticipacion_recordatorio  INT          NOT NULL,
    acepta_promociones         VARCHAR(2)   NOT NULL,
    id_cliente                 INT          NOT NULL,
    CONSTRAINT pk_perfil_cliente     PRIMARY KEY (id_perfil),
    CONSTRAINT uq_perfil_cliente     UNIQUE (id_cliente),
    CONSTRAINT ck_perfil_idioma      CHECK (idioma IN ('español', 'inglés')),
    CONSTRAINT ck_perfil_canal       CHECK (canal_preferido IN ('correo', 'SMS', 'WhatsApp')),
    CONSTRAINT ck_perfil_anticipacion CHECK (anticipacion_recordatorio IN (1, 3, 7)),
    CONSTRAINT ck_perfil_promociones CHECK (acepta_promociones IN ('sí', 'no')),
    -- CASCADE: el perfil no existe sin su cliente
    CONSTRAINT fk_perfil_cliente
        FOREIGN KEY (id_cliente) REFERENCES Cliente (id_cliente)
        ON DELETE CASCADE
) COMMENT = 'Preferencias de notificación de cada cliente';

-- Funcion: cada fecha y horario en que abre una atracción.
-- La hora de cierre es posterior a la de apertura.
-- Una función no se borra: se cancela (estado 'cancelada').
CREATE TABLE Funcion (
    id_funcion     INT          NOT NULL AUTO_INCREMENT,
    fecha          DATE         NOT NULL,
    hora_apertura  TIME         NOT NULL,
    hora_cierre    TIME         NOT NULL,
    estado         VARCHAR(12)  NOT NULL DEFAULT 'programada',
    id_atraccion   INT          NOT NULL,
    CONSTRAINT pk_funcion        PRIMARY KEY (id_funcion),
    CONSTRAINT ck_funcion_horas  CHECK (hora_cierre > hora_apertura),
    CONSTRAINT ck_funcion_estado CHECK (estado IN ('programada', 'cancelada', 'realizada')),
    -- RESTRICT: se conserva la historia de funciones de cada atracción
    CONSTRAINT fk_funcion_atraccion
        FOREIGN KEY (id_atraccion) REFERENCES Atraccion (id_atraccion)
        ON DELETE RESTRICT
) COMMENT = 'Funciones (fecha y horario) de cada atracción';

-- Combo_atraccion: rompe la relación N:M entre Combo y Atraccion.
-- Dice qué atracciones incluye cada combo; una atracción no se repite
-- dentro del mismo combo.
CREATE TABLE Combo_atraccion (
    id_combo_atraccion  INT  NOT NULL AUTO_INCREMENT,
    id_combo            INT  NOT NULL,
    id_atraccion        INT  NOT NULL,
    CONSTRAINT pk_combo_atraccion PRIMARY KEY (id_combo_atraccion),
    CONSTRAINT uq_combo_atraccion UNIQUE (id_combo, id_atraccion),
    -- CASCADE: si se borra un combo (que nunca se vendió), se borra su
    -- composición. Si ya se vendió, Boleta no deja borrarlo (RESTRICT).
    CONSTRAINT fk_combo_atraccion_combo
        FOREIGN KEY (id_combo) REFERENCES Combo (id_combo)
        ON DELETE CASCADE,
    -- RESTRICT: no se borra una atracción que forma parte de un combo
    CONSTRAINT fk_combo_atraccion_atraccion
        FOREIGN KEY (id_atraccion) REFERENCES Atraccion (id_atraccion)
        ON DELETE RESTRICT
) COMMENT = 'Atracciones que forman parte de cada combo';

-- ---------------------------------------------------------------------
-- BLOQUE 4 · Hijas de segundo nivel: apuntan a tablas del bloque 3
-- (Prestador, Funcion) y a padres de los bloques 1 y 2.
-- ---------------------------------------------------------------------

-- Usuario_sistema: usuarios internos que inician sesión con usuario y
-- contraseña (cifrada con BCrypt). El nombre de usuario no se repite.
-- id_prestador es opcional: solo lo tiene el 'encargado de prestador'.
-- La regla "si el rol es encargado, id_prestador es obligatorio" mira
-- otra tabla (Rol), por eso la valida la aplicación y no un CHECK.
-- Un usuario no se borra: se bloquea.
CREATE TABLE Usuario_sistema (
    id_usuario          INT           NOT NULL AUTO_INCREMENT,
    nombre_usuario      VARCHAR(40)   NOT NULL,
    contrasena_cifrada  VARCHAR(255)  NOT NULL,
    estado              VARCHAR(10)   NOT NULL DEFAULT 'activo',
    id_rol              INT           NOT NULL,
    id_prestador        INT,
    CONSTRAINT pk_usuario_sistema PRIMARY KEY (id_usuario),
    CONSTRAINT uq_usuario_nombre  UNIQUE (nombre_usuario),
    CONSTRAINT ck_usuario_estado  CHECK (estado IN ('activo', 'bloqueado')),
    -- RESTRICT: no se borra un rol que tenga usuarios
    CONSTRAINT fk_usuario_rol
        FOREIGN KEY (id_rol) REFERENCES Rol (id_rol)
        ON DELETE RESTRICT,
    -- RESTRICT: no se borra un prestador que tenga encargados
    CONSTRAINT fk_usuario_prestador
        FOREIGN KEY (id_prestador) REFERENCES Prestador (id_prestador)
        ON DELETE RESTRICT
) COMMENT = 'Usuarios internos del sistema (administrador, encargado, operador)';

-- Plan: opciones que ofrece cada prestador (ej. "entrada individual",
-- "pasaporte del día"). Dos planes del mismo prestador no se llaman
-- igual. Un plan vendido no se borra: se inactiva.
CREATE TABLE Plan (
    id_plan       INT            NOT NULL AUTO_INCREMENT,
    nombre        VARCHAR(80)    NOT NULL,
    descripcion   VARCHAR(255)   NOT NULL,
    tipo          VARCHAR(15)    NOT NULL,
    precio        DECIMAL(12,2)  NOT NULL,
    fecha_inicio  DATE           NOT NULL,
    fecha_fin     DATE           NOT NULL,
    estado        VARCHAR(10)    NOT NULL DEFAULT 'activo',
    id_prestador  INT            NOT NULL,
    CONSTRAINT pk_plan                  PRIMARY KEY (id_plan),
    CONSTRAINT uq_plan_prestador_nombre UNIQUE (id_prestador, nombre),
    CONSTRAINT ck_plan_tipo   CHECK (tipo IN ('básico', 'estándar', 'premium', 'todo incluido')),
    CONSTRAINT ck_plan_precio CHECK (precio > 0),
    CONSTRAINT ck_plan_fechas CHECK (fecha_fin >= fecha_inicio),
    CONSTRAINT ck_plan_estado CHECK (estado IN ('activo', 'inactivo')),
    -- RESTRICT: protege el historial comercial del prestador
    CONSTRAINT fk_plan_prestador
        FOREIGN KEY (id_prestador) REFERENCES Prestador (id_prestador)
        ON DELETE RESTRICT
) COMMENT = 'Planes que ofrece cada prestador, con precio y vigencia';

-- Prestador_atraccion: rompe la relación N:M entre Prestador y
-- Atraccion (qué prestadores venden entradas de qué atracciones).
-- Su clave primaria es COMPUESTA: la pareja no se repite.
CREATE TABLE Prestador_atraccion (
    id_prestador      INT   NOT NULL,
    id_atraccion      INT   NOT NULL,
    fecha_afiliacion  DATE  NOT NULL,
    CONSTRAINT pk_prestador_atraccion PRIMARY KEY (id_prestador, id_atraccion),
    -- RESTRICT: los prestadores no se borran (pasan a 'retirado')
    CONSTRAINT fk_prest_atr_prestador
        FOREIGN KEY (id_prestador) REFERENCES Prestador (id_prestador)
        ON DELETE RESTRICT,
    -- RESTRICT: protege las atracciones del catálogo
    CONSTRAINT fk_prest_atr_atraccion
        FOREIGN KEY (id_atraccion) REFERENCES Atraccion (id_atraccion)
        ON DELETE RESTRICT
) COMMENT = 'Afiliación de prestadores a atracciones (N:M)';

-- Zona: localidades de una función (general, preferencial, VIP), cada
-- una con su aforo y su precio. Dentro de una función, el nombre de
-- zona no se repite.
CREATE TABLE Zona (
    id_zona     INT            NOT NULL AUTO_INCREMENT,
    nombre      VARCHAR(30)    NOT NULL,
    aforo       INT            NOT NULL,
    precio      DECIMAL(12,2)  NOT NULL,
    id_funcion  INT            NOT NULL,
    CONSTRAINT pk_zona                PRIMARY KEY (id_zona),
    CONSTRAINT uq_zona_funcion_nombre UNIQUE (id_funcion, nombre),
    CONSTRAINT ck_zona_aforo  CHECK (aforo > 0),
    CONSTRAINT ck_zona_precio CHECK (precio >= 0),
    -- CASCADE: la zona no existe sin su función. Si la zona ya tiene
    -- boletas, Boleta no deja borrarla (RESTRICT), así que en la
    -- práctica solo se borran zonas de funciones sin ventas.
    CONSTRAINT fk_zona_funcion
        FOREIGN KEY (id_funcion) REFERENCES Funcion (id_funcion)
        ON DELETE CASCADE
) COMMENT = 'Zonas de cada función, con aforo y precio propios';

-- ---------------------------------------------------------------------
-- BLOQUE 5 · Últimas HIJAS: Reserva y las que dependen de ella.
-- ---------------------------------------------------------------------

-- Reserva: compromiso de un cliente con un prestador para una fecha.
-- El prestador se conoce por el plan (no se guarda aquí: 3FN).
-- Una reserva cancelada no se borra: queda como historial.
-- Reglas que miran otras tablas y valida la aplicación: el plan debe
-- estar activo y vigente en la fecha del servicio, y una reserva solo
-- pasa a 'completada' si antes estuvo 'confirmada'.
CREATE TABLE Reserva (
    id_reserva      INT            NOT NULL AUTO_INCREMENT,
    fecha_creacion  DATE           NOT NULL DEFAULT (CURRENT_DATE),
    fecha_servicio  DATE           NOT NULL,
    estado          VARCHAR(12)    NOT NULL DEFAULT 'pendiente',
    valor_total     DECIMAL(12,2)  NOT NULL,
    canal           VARCHAR(25)    NOT NULL,
    id_cliente      INT            NOT NULL,
    id_plan         INT            NOT NULL,
    CONSTRAINT pk_reserva        PRIMARY KEY (id_reserva),
    CONSTRAINT ck_reserva_fechas CHECK (fecha_servicio >= fecha_creacion),
    CONSTRAINT ck_reserva_estado CHECK (estado IN ('pendiente', 'confirmada',
                                                   'cancelada', 'completada')),
    CONSTRAINT ck_reserva_valor  CHECK (valor_total >= 0),
    CONSTRAINT ck_reserva_canal  CHECK (canal IN ('web', 'aplicación móvil', 'teléfono',
                                                  'mostrador', 'agencia intermediaria')),
    -- RESTRICT: protege el historial contable del cliente
    CONSTRAINT fk_reserva_cliente
        FOREIGN KEY (id_cliente) REFERENCES Cliente (id_cliente)
        ON DELETE RESTRICT,
    -- RESTRICT: mantiene el vínculo con el plan contratado
    CONSTRAINT fk_reserva_plan
        FOREIGN KEY (id_plan) REFERENCES Plan (id_plan)
        ON DELETE RESTRICT
) COMMENT = 'Reservas de los clientes (núcleo común)';

-- Pago: una reserva puede pagarse de una vez o en varios abonos.
-- Los pagos en línea (tarjetas y PSE) llevan la referencia de la
-- pasarela, que no se repite. Un pago es un registro contable: no se
-- borra (Clase 10).
CREATE TABLE Pago (
    id_pago                 INT            NOT NULL AUTO_INCREMENT,
    fecha                   DATE           NOT NULL,
    monto                   DECIMAL(12,2)  NOT NULL,
    metodo                  VARCHAR(20)    NOT NULL,
    estado                  VARCHAR(12)    NOT NULL,
    referencia_transaccion  VARCHAR(50),
    id_reserva              INT            NOT NULL,
    CONSTRAINT pk_pago            PRIMARY KEY (id_pago),
    CONSTRAINT uq_pago_referencia UNIQUE (referencia_transaccion),
    CONSTRAINT ck_pago_monto      CHECK (monto > 0),
    CONSTRAINT ck_pago_metodo     CHECK (metodo IN ('tarjeta de crédito', 'tarjeta débito',
                                                    'PSE', 'transferencia', 'efectivo')),
    CONSTRAINT ck_pago_estado     CHECK (estado IN ('exitoso', 'fallido',
                                                    'pendiente', 'reembolsado')),
    -- Pago en línea => la referencia es obligatoria
    CONSTRAINT ck_pago_referencia CHECK (metodo NOT IN ('tarjeta de crédito', 'tarjeta débito', 'PSE')
                                         OR referencia_transaccion IS NOT NULL),
    -- RESTRICT: registro financiero intocable
    CONSTRAINT fk_pago_reserva
        FOREIGN KEY (id_reserva) REFERENCES Reserva (id_reserva)
        ON DELETE RESTRICT
) COMMENT = 'Pagos y abonos de cada reserva';

-- Notificacion (ajustada en la Entrega 2 para cumplir 3FN):
--   - ligada a una reserva  -> lleva id_reserva, id_cliente en NULL
--     (el cliente se obtiene por la reserva)
--   - promoción             -> lleva id_cliente, id_reserva en NULL
-- Por eso exactamente una de las dos foráneas es no nula (CHECK).
-- Las dos foráneas son RESTRICT: MySQL no permite CASCADE ni SET NULL
-- en columnas que están en un CHECK, y además es historial.
-- Regla que mira otra tabla y valida la aplicación: las promociones
-- solo van a clientes con acepta_promociones = 'sí'.
CREATE TABLE Notificacion (
    id_notificacion   INT           NOT NULL AUTO_INCREMENT,
    tipo              VARCHAR(25)   NOT NULL,
    canal             VARCHAR(10)   NOT NULL,
    mensaje           VARCHAR(500)  NOT NULL,
    fecha_programada  DATETIME      NOT NULL,
    fecha_real_envio  DATETIME,
    estado            VARCHAR(10)   NOT NULL DEFAULT 'pendiente',
    id_cliente        INT,
    id_reserva        INT,
    CONSTRAINT pk_notificacion PRIMARY KEY (id_notificacion),
    CONSTRAINT ck_notif_tipo   CHECK (tipo IN ('recordatorio de reserva', 'confirmación de reserva',
                                               'confirmación de pago', 'cambio de estado',
                                               'promoción')),
    CONSTRAINT ck_notif_canal  CHECK (canal IN ('correo', 'SMS', 'WhatsApp')),
    CONSTRAINT ck_notif_estado CHECK (estado IN ('pendiente', 'enviada', 'fallida', 'leída')),
    -- Exactamente una de las dos foráneas es no nula
    CONSTRAINT ck_notif_destino CHECK ((id_reserva IS NOT NULL AND id_cliente IS NULL)
                                    OR (id_reserva IS NULL AND id_cliente IS NOT NULL)),
    -- La promoción va al cliente; los demás tipos van ligados a una reserva
    CONSTRAINT ck_notif_tipo_destino CHECK ((tipo = 'promoción' AND id_cliente IS NOT NULL)
                                         OR (tipo <> 'promoción' AND id_reserva IS NOT NULL)),
    -- No se puede enviar antes de la fecha programada
    CONSTRAINT ck_notif_fechas CHECK (fecha_real_envio IS NULL
                                      OR fecha_real_envio >= fecha_programada),
    -- Mientras está pendiente, todavía no tiene fecha real de envío
    CONSTRAINT ck_notif_pendiente CHECK (estado <> 'pendiente' OR fecha_real_envio IS NULL),
    CONSTRAINT fk_notif_cliente
        FOREIGN KEY (id_cliente) REFERENCES Cliente (id_cliente)
        ON DELETE RESTRICT,
    CONSTRAINT fk_notif_reserva
        FOREIGN KEY (id_reserva) REFERENCES Reserva (id_reserva)
        ON DELETE RESTRICT
) COMMENT = 'Notificaciones a los clientes: recordatorios, confirmaciones y promociones';

-- Boleta: cada entrada vendida. Tiene código único y pertenece a una
-- reserva, una zona (y por la zona, a una función) y un tipo de entrada.
-- id_combo es NULL si la boleta se vendió suelta.
-- Una boleta no se borra: se anula. Reglas que miran otras filas y
-- valida la aplicación: no superar el aforo de la zona, una entrada de
-- niño solo con una de adulto en la misma reserva, y una boleta usada
-- o anulada no vuelve a 'vendida'.
CREATE TABLE Boleta (
    id_boleta        INT          NOT NULL AUTO_INCREMENT,
    codigo           VARCHAR(30)  NOT NULL,
    estado           VARCHAR(10)  NOT NULL DEFAULT 'vendida',
    id_reserva       INT          NOT NULL,
    id_zona          INT          NOT NULL,
    id_tipo_entrada  INT          NOT NULL,
    id_combo         INT,
    CONSTRAINT pk_boleta        PRIMARY KEY (id_boleta),
    CONSTRAINT uq_boleta_codigo UNIQUE (codigo),
    CONSTRAINT ck_boleta_estado CHECK (estado IN ('vendida', 'usada', 'anulada')),
    -- RESTRICT en todas: la boleta es la prueba de la venta
    CONSTRAINT fk_boleta_reserva
        FOREIGN KEY (id_reserva) REFERENCES Reserva (id_reserva)
        ON DELETE RESTRICT,
    CONSTRAINT fk_boleta_zona
        FOREIGN KEY (id_zona) REFERENCES Zona (id_zona)
        ON DELETE RESTRICT,
    CONSTRAINT fk_boleta_tipo_entrada
        FOREIGN KEY (id_tipo_entrada) REFERENCES Tipo_entrada (id_tipo_entrada)
        ON DELETE RESTRICT,
    CONSTRAINT fk_boleta_combo
        FOREIGN KEY (id_combo) REFERENCES Combo (id_combo)
        ON DELETE RESTRICT
) COMMENT = 'Boletas vendidas, sueltas o como parte de un combo';

-- ---------------------------------------------------------------------
-- BLOQUE 6 · Índices adicionales (Tabla 40 de la Entrega 2).
-- MySQL ya crea índices para las PK, los UNIQUE y las foráneas; estos
-- son para columnas que los reportes ordenan o buscan seguido.
-- ---------------------------------------------------------------------
CREATE INDEX ix_reserva_fecha_creacion ON Reserva (fecha_creacion);
CREATE INDEX ix_funcion_fecha          ON Funcion (fecha);
CREATE INDEX ix_notif_fecha_programada ON Notificacion (fecha_programada);

-- ---------------------------------------------------------------------
-- BLOQUE 7 · Verificar lo creado (como el BLOQUE 3 de la Clase 10).
-- En Oracle se consultan user_tables y user_constraints; en MySQL la
-- información equivalente está en information_schema.
-- Solo sirven para MIRAR lo creado: no cambian nada.
-- ---------------------------------------------------------------------

-- Las tablas creadas (deben ser 20)
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'quindiostay'
  AND table_type = 'BASE TABLE'
ORDER BY table_name;

-- Las restricciones de una tabla (PRIMARY KEY, UNIQUE, CHECK, FOREIGN KEY).
-- En Windows MySQL guarda los nombres de tabla en minúscula; por eso LOWER().
SELECT constraint_name, constraint_type
FROM information_schema.table_constraints
WHERE table_schema = 'quindiostay'
  AND LOWER(table_name) = 'reserva'
ORDER BY constraint_type, constraint_name;

-- Qué hace cada clave foránea al borrar el padre (RESTRICT o CASCADE)
SELECT table_name, constraint_name, referenced_table_name, delete_rule
FROM information_schema.referential_constraints
WHERE constraint_schema = 'quindiostay'
ORDER BY table_name;

-- Fin del script 01: 20 tablas.
