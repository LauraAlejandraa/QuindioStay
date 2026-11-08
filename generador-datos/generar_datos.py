# =====================================================================
# QuindíoStay · Bases de Datos I · Grupo 02D · 2026-2
# Generador de datos de ejemplo
#
# Qué hace: escribe el archivo ../02_carga_datos.sql con todos los
# INSERT de la base, respetando las reglas del negocio.
#
# Cómo se usa (desde la carpeta bd\generador, en el Símbolo del sistema):
#     python generar_datos.py
#
# Necesita la librería bcrypt:  pip install bcrypt
# =====================================================================

import random
from datetime import date, datetime, timedelta
from pathlib import Path

import bcrypt

# Semilla fija: cada vez que se ejecute el programa salen los mismos
# datos (los mismos clientes, reservas, etc.). Así las dos tenemos la
# misma base y los reportes dan siempre los mismos números.
random.seed(2026)

# El archivo de salida queda en la carpeta bd (una carpeta arriba).
ARCHIVO_SALIDA = Path(__file__).resolve().parent.parent / "02_carga_datos.sql"

# Aquí se van guardando las líneas del script SQL.
lineas = []


# ---------------------------------------------------------------------
# FUNCIONES DE AYUDA
# ---------------------------------------------------------------------

def valor_sql(valor):
    """Convierte un valor de Python en texto SQL.
    None -> NULL (sin comillas), números tal cual (sin comillas), textos
    entre comillas simples y fechas como DATE 'AAAA-MM-DD' (Clase 14).
    Una comilla dentro de un texto se escribe doble (D'Ana -> D''Ana),
    como vimos en la Clase 11."""
    if valor is None:
        return "NULL"
    if isinstance(valor, (int, float)):
        return str(valor)
    if isinstance(valor, datetime):
        # Fecha con hora: TIMESTAMP 'AAAA-MM-DD HH:MM:SS'
        return "TIMESTAMP '" + valor.strftime("%Y-%m-%d %H:%M:%S") + "'"
    if isinstance(valor, date):
        # Fecha: DATE 'AAAA-MM-DD', como se escribe en la Clase 14
        return "DATE '" + valor.strftime("%Y-%m-%d") + "'"
    texto = str(valor).replace("'", "''")
    return "'" + texto + "'"


def insertar(tabla, columnas, filas, por_bloque=100):
    """Escribe los INSERT de una tabla. Agrupa hasta 100 filas por
    INSERT para que el script sea más corto y corra más rápido."""
    lineas.append(f"-- {tabla}: {len(filas)} filas")
    for inicio in range(0, len(filas), por_bloque):
        bloque = filas[inicio:inicio + por_bloque]
        lineas.append(f"INSERT INTO {tabla} ({', '.join(columnas)}) VALUES")
        valores = ["  (" + ", ".join(valor_sql(v) for v in fila) + ")" for fila in bloque]
        lineas.append(",\n".join(valores) + ";")
    lineas.append("")


def redondear(valor, a=100):
    """Redondea a la centena (o al múltiplo que se indique) igual que el
    ROUND de MySQL: la mitad siempre sube (97.750 -> 97.800).
    No se usa el redondeo de Python porque lleva la mitad al número par
    (a veces baja), y entonces los valores no coincidirían con las
    consultas en MySQL."""
    return int(valor / a + 0.5) * a


def titulo(texto):
    """Escribe un comentario de sección en el script SQL."""
    lineas.append("-- " + "-" * 69)
    lineas.append("-- " + texto)
    lineas.append("-- " + "-" * 69)


# Las contraseñas se cifran con BCrypt. Cada llamada usa una sal
# aleatoria nueva, así que la misma contraseña da un hash distinto cada
# vez (por eso los hashes cambian cada vez que se corre el generador,
# aunque el resto de datos sea igual). Se usa el prefijo 2a, el que
# entiende BCryptPasswordEncoder de Java.
def cifrar(contrasena):
    sal = bcrypt.gensalt(rounds=10, prefix=b"2a")
    return bcrypt.hashpw(contrasena.encode("utf-8"), sal).decode("utf-8")


# =====================================================================
# PARTE 1 · CATÁLOGOS, PRESTADORES, AFILIACIONES Y USUARIOS
# Los id se escriben a mano (1, 2, 3...) para poder usarlos en las
# foráneas. Como el script 00 deja la base vacía, AUTO_INCREMENT acepta
# esos valores y después sigue desde el último.
# =====================================================================

# Municipios del Quindío con su código DIVIPOLA.
# Fuente: DIVIPOLA - Códigos municipios, Portal de Datos Abiertos de
# Colombia (datos.gov.co), conjunto publicado a partir de la DIVIPOLA
# del DANE.
MUNICIPIOS = [
    (1, "Armenia", "63001"),
    (2, "Buenavista", "63111"),
    (3, "Calarcá", "63130"),
    (4, "Circasia", "63190"),
    (5, "Córdoba", "63212"),
    (6, "Filandia", "63272"),
    (7, "Génova", "63302"),
    (8, "La Tebaida", "63401"),
    (9, "Montenegro", "63470"),
    (10, "Pijao", "63548"),
    (11, "Quimbaya", "63594"),
    (12, "Salento", "63690"),
]

TIPOS_PRESTADOR = [
    (1, "atracción"),
    (2, "agencia de viajes"),
    (3, "alojamiento"),
    (4, "transporte"),
    (5, "gastronomía"),
]

ROLES = [
    (1, "administrador de la plataforma"),
    (2, "encargado de prestador"),
    (3, "operador"),
]

# (id, nombre, porcentaje de descuento)
TIPOS_ENTRADA = [
    (1, "adulto", 0),
    (2, "niño", 40),
    (3, "adulto mayor", 30),
    (4, "estudiante", 20),
]

# Prestadores: (id, nombre, id_municipio, id_tipo, estado).
# Repartidos de forma DESIGUAL: Armenia 6, Montenegro 3, Quimbaya 3,
# Salento 3, Filandia 2, Calarcá 2, La Tebaida 1. Buenavista, Circasia,
# Córdoba, Génova y Pijao no tienen prestadores (el enunciado pide
# asimetría). Los nombres son inventados pero verosímiles.
PRESTADORES = [
    # Operadores de atracciones (tipo 1)
    (1, "Parque Temático del Café S.A.S.", 9, 1, "activo"),
    (2, "Granja Agropecuaria Quindiana S.A.", 11, 1, "activo"),
    (3, "Mariposario y Jardín Botánico de Calarcá", 3, 1, "activo"),
    (4, "Fundación Museo Quimbaya", 1, 1, "activo"),
    (5, "Ecoturismo Valle de las Palmas", 12, 1, "activo"),
    (6, "Acuaparque Tebaida Sol S.A.S.", 8, 1, "activo"),
    (7, "Corporación Festivales del Quindío", 1, 1, "activo"),
    # Agencias de viajes (tipo 2): venden entradas de varias atracciones
    (8, "Viajes Café y Montaña", 1, 2, "activo"),
    (9, "Agencia Ruta Paisa Tours", 1, 2, "activo"),
    (10, "Quimbaya Travel", 11, 2, "suspendido"),
    (11, "Destino Eje Cafetero", 6, 2, "activo"),
    # Alojamientos (tipo 3)
    (12, "Hotel Mirador de Salento", 12, 3, "activo"),
    (13, "Hacienda Cafetera La Esperanza", 9, 3, "activo"),
    (14, "Hostal Colores de Filandia", 6, 3, "retirado"),
    (15, "Hotel Centro Armenia", 1, 3, "activo"),
    # Transporte (tipo 4)
    (16, "Transportes Willys del Quindío", 12, 4, "activo"),
    (17, "Expreso Turístico Cafetero", 1, 4, "activo"),
    (18, "Trans Montenegro Tours", 9, 4, "activo"),
    # Gastronomía (tipo 5)
    (19, "Restaurante Sabor Quindiano", 3, 5, "activo"),
    (20, "Fonda La Arriera", 11, 5, "activo"),
]

# Atracciones: (id, nombre, descripción, ubicación, popularidad).
# La popularidad es un peso para el sorteo de reservas: el Parque
# Temático del Café vende muchísimo más que el recorrido de la finca.
# No se guarda en la base: solo sirve para que los datos sean asimétricos.
ATRACCIONES = [
    (1, "Parque Temático del Café", "Parque de atracciones mecánicas y cultura cafetera",
     "Montenegro, vía Pueblo Tapao", 30),
    (2, "Granja Agropecuaria Quindiana", "Parque temático de la vida en el campo y espectáculos con animales",
     "Quimbaya, vereda Kilómetro 7", 22),
    (3, "Mariposario y Jardín Botánico", "Jardín botánico con mariposario y senderos de bosque",
     "Calarcá, kilómetro 3 vía al Valle", 12),
    (4, "Museo de la Cultura Quimbaya", "Museo arqueológico de orfebrería y cerámica Quimbaya",
     "Armenia, avenida Bolívar", 5),
    (5, "Sendero Valle de las Palmas", "Caminata guiada entre palmas de cera",
     "Salento, vereda Cocora", 10),
    (6, "Acuaparque Tebaida Sol", "Parque acuático con piscinas y toboganes",
     "La Tebaida, vía al aeropuerto", 8),
    (7, "Festival de la Cosecha", "Festival anual con música, desfiles y muestras gastronómicas",
     "Armenia, Parque de la Vida", 4),
    (8, "Mirador Cafetero de Filandia", "Mirador con vista a los valles del Quindío y Risaralda",
     "Filandia, salida a Quimbaya", 6),
    (9, "Recorrido Finca Cafetera", "Recorrido por el proceso del café, de la semilla a la taza",
     "Montenegro, vereda La Esperanza", 2),
    (10, "Parque de Aventura Arriera", "Canopy, puentes colgantes y tirolesa",
     "Quimbaya, vía a Montenegro", 7),
]

# Qué prestadores venden cada atracción: (id_prestador, id_atraccion, fecha).
# Cada atracción tiene su operador, y algunas agencias venden varias.
# Las atracciones 8 y 10 quedan solo con agencias.
AFILIACIONES = [
    (1, 1, date(2024, 11, 2)), (2, 2, date(2024, 11, 2)), (3, 3, date(2024, 11, 15)),
    (4, 4, date(2024, 12, 1)), (5, 5, date(2024, 12, 1)), (6, 6, date(2025, 1, 10)),
    (7, 7, date(2025, 2, 20)), (1, 9, date(2025, 3, 5)),
    (8, 1, date(2024, 12, 10)), (8, 2, date(2024, 12, 10)), (8, 5, date(2025, 1, 20)),
    (8, 8, date(2025, 1, 20)), (8, 10, date(2025, 6, 1)),
    (9, 1, date(2025, 2, 1)), (9, 3, date(2025, 2, 1)), (9, 10, date(2025, 4, 12)),
    (10, 2, date(2025, 3, 1)), (10, 10, date(2025, 3, 1)),
    (11, 8, date(2025, 5, 15)), (11, 5, date(2025, 5, 15)),
]

# Usuarios internos de prueba: (id, usuario, contraseña, estado, rol, prestador).
# Las contraseñas originales se anotan en el manual para poder entrar.
USUARIOS = [
    (1, "admin", "Admin2026*", "activo", 1, None),
    (2, "encargado.parquecafe", "Encargado2026*", "activo", 2, 1),
    (3, "encargado.granja", "Encargado2026*", "activo", 2, 2),
    (4, "operador.taquilla", "Operador2026*", "activo", 3, None),
    (5, "operador.antiguo", "Operador2026*", "bloqueado", 3, None),
]


def telefono_celular():
    """Celular colombiano: 10 dígitos y empieza por 3."""
    prefijo = random.choice(["300", "301", "304", "310", "311", "312", "313",
                             "314", "315", "316", "317", "318", "320", "321", "350"])
    return prefijo + "".join(random.choice("0123456789") for _ in range(7))


def generar_parte_1():
    titulo("PARTE 1 · Catálogos, prestadores, afiliaciones y usuarios")
    insertar("Municipio", ["id_municipio", "nombre", "codigo_DANE"], MUNICIPIOS)
    insertar("Tipo_prestador", ["id_tipo_prestador", "nombre"], TIPOS_PRESTADOR)
    insertar("Rol", ["id_rol", "nombre"], ROLES)
    insertar("Tipo_entrada", ["id_tipo_entrada", "nombre", "porcentaje_descuento"], TIPOS_ENTRADA)

    filas = []
    for id_p, nombre, id_mun, id_tipo, estado in PRESTADORES:
        nit = f"9{random.randint(10000000, 99999999)}-{random.randint(0, 9)}"
        rnt = str(random.randint(10000, 99999))
        calle = random.choice(["Calle", "Carrera", "Avenida", "Kilómetro"])
        direccion = f"{calle} {random.randint(1, 40)} # {random.randint(1, 30)}-{random.randint(10, 99)}"
        if calle == "Kilómetro":
            direccion = f"Kilómetro {random.randint(1, 12)} vía principal"
        # Correo a partir del nombre, sin espacios, puntos ni tildes.
        correo_base = nombre.lower().split(" s.a")[0].replace(" ", "").replace(".", "")
        correo_base = (correo_base.replace("á", "a").replace("é", "e").replace("í", "i")
                       .replace("ó", "o").replace("ú", "u").replace("ñ", "n"))
        correo = f"contacto@{correo_base}.com.co"
        filas.append((id_p, nombre, nit, rnt, direccion, telefono_celular(), correo,
                      estado, id_mun, id_tipo))
    insertar("Prestador",
             ["id_prestador", "nombre_comercial", "NIT_documento", "RNT", "direccion",
              "telefono", "correo", "estado", "id_municipio", "id_tipo_prestador"], filas)

    insertar("Atraccion", ["id_atraccion", "nombre", "descripcion", "ubicacion"],
             [(a[0], a[1], a[2], a[3]) for a in ATRACCIONES])
    insertar("Prestador_atraccion", ["id_prestador", "id_atraccion", "fecha_afiliacion"],
             AFILIACIONES)

    insertar("Usuario_sistema",
             ["id_usuario", "nombre_usuario", "contrasena_cifrada", "estado", "id_rol",
              "id_prestador"],
             [(u[0], u[1], cifrar(u[2]), u[3], u[4], u[5]) for u in USUARIOS])


# =====================================================================
# PARTE 2 · CLIENTES, CUENTAS Y PERFILES
# 150 clientes: la mayoría colombianos de ciudades variadas (no solo del
# Quindío) y unos 20 extranjeros, como pide el enunciado. Unos 125 tienen
# cuenta y perfil; el resto se registró en el mostrador y no tiene cuenta.
# =====================================================================

NOMBRES_F = ["María", "Laura", "Valentina", "Daniela", "Camila", "Sofía", "Natalia",
             "Paula", "Andrea", "Carolina", "Juliana", "Diana", "Alejandra", "Luisa",
             "Manuela", "Isabella", "Gabriela", "Tatiana", "Lina", "Mónica", "Ana",
             "Claudia", "Sandra", "Paola", "Catalina", "Angélica", "Marcela", "Lucía"]
NOMBRES_M = ["Juan", "Andrés", "Carlos", "Santiago", "Sebastián", "Felipe", "Daniel",
             "Alejandro", "David", "Jorge", "Luis", "Diego", "Camilo", "Esteban",
             "Mateo", "Nicolás", "Julián", "Óscar", "Ricardo", "Fernando", "Miguel",
             "Javier", "Hernán", "Mauricio", "Jhon", "Cristian", "Samuel", "Tomás"]
APELLIDOS = ["García", "Rodríguez", "Martínez", "López", "González", "Hernández",
             "Pérez", "Sánchez", "Ramírez", "Torres", "Flórez", "Rivera", "Gómez",
             "Díaz", "Restrepo", "Ospina", "Cardona", "Giraldo", "Valencia", "Arango",
             "Osorio", "Marín", "Castaño", "Quintero", "Londoño", "Jaramillo",
             "Zapata", "Salazar", "Muñoz", "Rojas", "Vargas", "Morales", "Ortiz",
             "Henao", "Bedoya", "Mejía", "Agudelo", "Correa", "Duque", "Montoya"]

# Ciudades colombianas de origen con un peso (más turistas de las
# ciudades grandes y cercanas al Quindío).
CIUDADES_CO = [("Bogotá", 26), ("Medellín", 18), ("Cali", 16), ("Pereira", 12),
               ("Armenia", 8), ("Manizales", 7), ("Ibagué", 5), ("Bucaramanga", 4),
               ("Barranquilla", 4), ("Cartagena", 3), ("Calarcá", 3), ("Neiva", 3),
               ("Tuluá", 2), ("Cartago", 2), ("Villavicencio", 2), ("Popayán", 2)]

# Extranjeros: (nombre, apellido, ciudad, habla inglés)
EXTRANJEROS = [
    ("John", "Smith", "Toronto", True), ("Emily", "Johnson", "Nueva York", True),
    ("Michael", "Brown", "Miami", True), ("Sarah", "Wilson", "Londres", True),
    ("David", "Miller", "Chicago", True), ("Jessica", "Taylor", "Los Ángeles", True),
    ("Lukas", "Schmidt", "Berlín", True), ("Anna", "Müller", "Múnich", True),
    ("Pierre", "Dubois", "París", True), ("Camille", "Martin", "Lyon", True),
    ("Diego", "Fernández", "Madrid", False), ("Lucía", "Navarro", "Barcelona", False),
    ("José", "Ramírez", "Ciudad de México", False), ("Fernanda", "Silva", "São Paulo", False),
    ("Martín", "Gómez", "Buenos Aires", False), ("Valeria", "Paredes", "Lima", False),
    ("Andrés", "Mora", "Quito", False), ("Francisca", "Soto", "Santiago de Chile", False),
    ("Carlos", "Pérez", "Caracas", False), ("Thomas", "Evans", "Sídney", True),
]

DOMINIOS = ["gmail.com", "gmail.com", "gmail.com", "hotmail.com", "outlook.com", "yahoo.com"]

# Información de cada cliente que necesitan las partes siguientes
# (reservas y notificaciones). No se guarda en la base tal cual.
CLIENTES = []          # lista de diccionarios
_documentos = set()
_correos = set()


def sin_tildes(texto):
    for con, sin in zip("áéíóúüñÁÉÍÓÚÜÑ", "aeiouunAEIOUUN"):
        texto = texto.replace(con, sin)
    return texto


def correo_unico(nombre, apellido):
    base = sin_tildes(f"{nombre}.{apellido}".lower())
    correo = f"{base}@{random.choice(DOMINIOS)}"
    numero = 1
    while correo in _correos:
        numero += 1
        correo = f"{base}{numero}@{random.choice(DOMINIOS)}"
    _correos.add(correo)
    return correo


def documento_unico(tipo):
    while True:
        if tipo == "cédula de ciudadanía":
            numero = str(random.choice([10, 11, 14, 16, 24, 30, 41, 52, 66, 79, 80, 93, 1094]))
            numero += "".join(random.choice("0123456789") for _ in range(10 - len(numero)))
        elif tipo == "tarjeta de identidad":
            numero = "1" + str(random.randint(100000000, 199999999))
        elif tipo == "cédula de extranjería":
            numero = str(random.randint(100000, 9999999))
        else:  # pasaporte: dos letras y siete números
            numero = (random.choice("ABCDEFGHJKLMNPRSTUVXYZ") + random.choice("ABCDEFGHJKLMNPRSTUVXYZ")
                      + str(random.randint(1000000, 9999999)))
        if (tipo, numero) not in _documentos:
            _documentos.add((tipo, numero))
            return numero


def fecha_aleatoria(desde, hasta):
    return desde + timedelta(days=random.randint(0, (hasta - desde).days))


def generar_parte_2():
    titulo("PARTE 2 · Clientes, cuentas y perfiles")

    filas_cliente, filas_cuenta, filas_perfil = [], [], []
    id_cuenta = 0

    for id_cliente in range(1, 151):
        # Los clientes 131 a 150 son extranjeros.
        if id_cliente > 130:
            nombre, apellido1, ciudad, ingles = EXTRANJEROS[id_cliente - 131]
            apellido2 = None
            segundo_nombre = None
            tipo_doc = "cédula de extranjería" if id_cliente in (135, 141, 149) else "pasaporte"
            nacimiento = fecha_aleatoria(date(1960, 1, 1), date(2002, 12, 31))
        else:
            mujer = random.random() < 0.55
            lista = NOMBRES_F if mujer else NOMBRES_M
            nombre = random.choice(lista)
            segundo_nombre = random.choice(lista) if random.random() < 0.4 else None
            if segundo_nombre == nombre:
                segundo_nombre = None
            apellido1, apellido2 = random.sample(APELLIDOS, 2)
            ciudad = random.choices([c for c, _ in CIUDADES_CO], [p for _, p in CIUDADES_CO])[0]
            ingles = False
            # Unos pocos son menores de edad con tarjeta de identidad.
            if id_cliente in (17, 64, 112):
                tipo_doc = "tarjeta de identidad"
                nacimiento = fecha_aleatoria(date(2009, 1, 1), date(2011, 6, 30))
            else:
                tipo_doc = "cédula de ciudadanía"
                nacimiento = fecha_aleatoria(date(1955, 1, 1), date(2006, 12, 31))

        documento = documento_unico(tipo_doc)
        correo = correo_unico(nombre, apellido1)
        filas_cliente.append((id_cliente, nombre, segundo_nombre, apellido1, apellido2,
                              tipo_doc, documento, correo, telefono_celular(),
                              nacimiento, ciudad))

        # Cerca del 83 % tiene cuenta (y perfil). Los menores no tienen.
        tiene_cuenta = tipo_doc != "tarjeta de identidad" and random.random() < 0.85
        info = {"id": id_cliente, "cuenta": tiene_cuenta, "promociones": False,
                "canal": None, "anticipacion": None, "idioma": "español",
                "extranjero": id_cliente > 130}
        if tiene_cuenta:
            id_cuenta += 1
            filas_cuenta.append((id_cuenta, cifrar("Cliente2026*"), id_cliente))
            idioma = "inglés" if ingles else "español"
            canal = random.choices(["correo", "SMS", "WhatsApp"], [35, 15, 50])[0]
            anticipacion = random.choices([1, 3, 7], [30, 50, 20])[0]
            acepta = "sí" if random.random() < 0.55 else "no"
            filas_perfil.append((id_cuenta, idioma, canal, anticipacion, acepta, id_cliente))
            info.update(promociones=(acepta == "sí"), canal=canal, anticipacion=anticipacion,
                        idioma=idioma)
        CLIENTES.append(info)

    insertar("Cliente",
             ["id_cliente", "primer_nombre", "segundo_nombre", "primer_apellido",
              "segundo_apellido", "tipo_documento", "numero_documento", "email",
              "telefono", "fecha_nacimiento", "ciudad_origen"], filas_cliente)
    insertar("Cuenta", ["id_cuenta", "contrasena_cifrada", "id_cliente"], filas_cuenta)
    insertar("Perfil_cliente",
             ["id_perfil", "idioma", "canal_preferido", "anticipacion_recordatorio",
              "acepta_promociones", "id_cliente"], filas_perfil)


# =====================================================================
# PARTE 3 · PLANES, COMBOS, FUNCIONES Y ZONAS
# =====================================================================

# Fecha de "hoy" para el generador. Las funciones y reservas anteriores
# a esta fecha ya pasaron (realizadas, completadas); las posteriores
# están programadas o pendientes. Es fija para que los datos no cambien.
HOY = date(2026, 11, 5)
INICIO = date(2025, 1, 1)
FIN = date(2026, 12, 31)


def temporada(d):
    """Temporada turística de una fecha: 'alta', 'media' o 'baja'.
    Alta: Semana Santa, vacaciones de mitad de año (15 jun - 31 jul) y
    temporada de fin de año: TODO diciembre y la primera quincena de
    enero (vacaciones, novenas y festividades). Media: fines de semana y
    la semana de receso de octubre. Baja: el resto.
    Semana Santa: del Domingo de Ramos al Domingo de Resurrección
    (Pascua: 20 abr 2025 y 5 abr 2026)."""
    semana_santa = [(date(2025, 4, 13), date(2025, 4, 20)),
                    (date(2026, 3, 29), date(2026, 4, 5))]
    for desde, hasta in semana_santa:
        if desde <= d <= hasta:
            return "alta"
    if (d.month == 6 and d.day >= 15) or d.month == 7:
        return "alta"
    if d.month == 12 or (d.month == 1 and d.day <= 15):
        return "alta"
    if d.weekday() >= 5 or (d.month == 10 and 6 <= d.day <= 12):
        return "media"
    return "baja"


def peso_fecha(d):
    """Qué tan probable es que haya turistas ese día. Así hay muchas más
    funciones y reservas en temporada alta (sobre todo de mediados de
    diciembre a mediados de enero), y febrero y septiembre son meses
    flojos (datos asimétricos)."""
    peso = {"alta": 6, "media": 2.5, "baja": 1}[temporada(d)]
    # Pico del año: del 15 de diciembre al 15 de enero hay todavía más
    # turistas que en el resto de la temporada alta.
    if (d.month == 12 and d.day >= 15) or (d.month == 1 and d.day <= 15):
        peso *= 1.5
    if d.month in (2, 9):
        peso *= 0.5
    return peso


# ---------------------------------------------------------------------
# PLANES (40)
# Para vender boletas, el plan debe ser de un prestador afiliado a la
# atracción y estar activo y vigente en la fecha del servicio. Por eso
# cada operador y cada agencia tiene un plan para 2025 y otro para 2026.
# Los demás prestadores (hoteles, transporte, restaurantes) también
# tienen planes, aunque nuestro módulo solo vende boletas.
# ---------------------------------------------------------------------
PLANES = []                # (id, nombre, descripcion, tipo, precio, inicio, fin, estado, id_prestador)
PLANES_BOLETERIA = {}      # (id_prestador, año) -> id_plan que sirve para vender boletas


def agregar_plan(nombre, descripcion, tipo, precio, inicio, fin, estado, id_prestador):
    id_plan = len(PLANES) + 1
    PLANES.append((id_plan, nombre, descripcion, tipo, precio, inicio, fin, estado, id_prestador))
    return id_plan


def generar_planes():
    # Operadores de atracciones (1 a 7) y agencias (8 a 11): plan 2025 y plan 2026.
    nombres_boleteria = {
        1: ("Entrada Parque del Café", "básico", 72000),
        2: ("Entrada Granja Quindiana", "básico", 65000),
        3: ("Entrada Mariposario", "básico", 30000),
        4: ("Entrada Museo Quimbaya", "básico", 15000),
        5: ("Caminata Valle de las Palmas", "estándar", 40000),
        6: ("Día de Sol Acuaparque", "estándar", 45000),
        7: ("Boletería Festival de la Cosecha", "estándar", 35000),
        8: ("Pasaporte Café y Montaña", "premium", 120000),
        9: ("Tour Ruta Paisa", "premium", 110000),
        10: ("Plan Quimbaya Aventura", "estándar", 90000),
        11: ("Pasaporte Eje Cafetero", "premium", 115000),
    }
    for id_p, (nombre, tipo, precio) in nombres_boleteria.items():
        for anio in (2025, 2026):
            precio_anio = precio if anio == 2025 else redondear(precio * 1.08, 1000)
            # La agencia 10 está suspendida: su plan 2026 quedó inactivo.
            estado = "inactivo" if (id_p == 10 and anio == 2026) else "activo"
            id_plan = agregar_plan(f"{nombre} {anio}",
                                   "Incluye las entradas compradas en la reserva y el acceso a las zonas escogidas",
                                   tipo, precio_anio, date(anio, 1, 1), date(anio, 12, 31),
                                   estado, id_p)
            if estado == "activo":
                PLANES_BOLETERIA[(id_p, anio)] = id_plan

    # Planes extra de los operadores y agencias: algunos vencidos o inactivos.
    agregar_plan("Promoción Halloween 2025", "Entrada con descuento la semana de Halloween",
                 "básico", 50000, date(2025, 10, 25), date(2025, 11, 2), "inactivo", 1)
    agregar_plan("Pasadía Granja todo incluido", "Entrada, almuerzo típico y transporte desde Armenia",
                 "todo incluido", 150000, date(2026, 1, 1), date(2026, 12, 31), "inactivo", 2)
    agregar_plan("Noche de Museos 2025", "Visita nocturna guiada al museo",
                 "estándar", 20000, date(2025, 5, 15), date(2025, 5, 18), "inactivo", 4)
    agregar_plan("Temporada de vacaciones 2025", "Entrada con almuerzo en vacaciones de mitad de año",
                 "estándar", 95000, date(2025, 6, 15), date(2025, 7, 31), "activo", 6)
    agregar_plan("Tour fotográfico Valle de las Palmas", "Caminata al amanecer con guía fotógrafo",
                 "premium", 85000, date(2026, 1, 1), date(2026, 12, 31), "activo", 5)

    # Hoteles, transporte y restaurantes (prestadores 12 a 20).
    otros = [
        (12, "Solo alojamiento", "Noche en habitación doble sin alimentación", "básico", 180000),
        (12, "Alojamiento con desayuno", "Noche en habitación doble con desayuno típico", "estándar", 210000),
        (13, "Noche en hacienda con desayuno", "Habitación en casa de hacienda cafetera y desayuno", "estándar", 260000),
        (13, "Experiencia cafetera todo incluido", "Alojamiento, alimentación y recorrido por el cafetal", "todo incluido", 420000),
        (14, "Habitación compartida", "Cama en habitación compartida del hostal", "básico", 60000),
        (15, "Habitación ejecutiva", "Noche en habitación ejecutiva con desayuno", "premium", 230000),
        (15, "Habitación estándar", "Noche en habitación sencilla", "básico", 150000),
        (16, "Viaje en Willys a Cocora", "Transporte ida y regreso Salento - Valle de Cocora", "básico", 12000),
        (16, "Willys privado por el día", "Campero Willys con conductor durante 8 horas", "premium", 350000),
        (17, "Traslado aeropuerto - hotel", "Traslado desde el aeropuerto El Edén a hoteles de Armenia", "estándar", 60000),
        (18, "Ruta de los parques", "Bus turístico Armenia - Montenegro - Quimbaya", "estándar", 45000),
        (19, "Almuerzo típico quindiano", "Bandeja paisa o sancocho con bebida de café", "básico", 38000),
        (20, "Cena con música en vivo", "Cena típica y presentación de música colombiana", "premium", 75000),
    ]
    for id_p, nombre, descripcion, tipo, precio in otros:
        # El hostal 14 se retiró: su plan quedó inactivo.
        estado = "inactivo" if id_p == 14 else "activo"
        agregar_plan(nombre, descripcion, tipo, precio, date(2026, 1, 1), date(2026, 12, 31),
                     estado, id_p)

    insertar("Plan", ["id_plan", "nombre", "descripcion", "tipo", "precio", "fecha_inicio",
                      "fecha_fin", "estado", "id_prestador"], PLANES)


# ---------------------------------------------------------------------
# COMBOS
# Cada combo solo lo puede vender un prestador afiliado a TODAS sus
# atracciones (regla de la Entrega 1). Por eso cada combo dice qué
# prestadores lo venden. El combo 6 está inactivo y nunca se vendió.
# ---------------------------------------------------------------------
# (id, nombre, descripción, precio especial, estado, atracciones, prestadores que lo venden)
COMBOS = [
    (1, "Ruta del Café", "Parque Temático del Café y Recorrido Finca Cafetera",
     68000, "activo", [1, 9], [1]),
    (2, "Aventura Quindiana", "Granja Agropecuaria Quindiana y Parque de Aventura Arriera",
     85000, "activo", [2, 10], [8, 10]),
    (3, "Café y Mariposas", "Parque Temático del Café y Mariposario y Jardín Botánico",
     80000, "activo", [1, 3], [9]),
    (4, "Paisajes Cafeteros", "Sendero Valle de las Palmas y Mirador Cafetero de Filandia",
     48000, "activo", [5, 8], [8, 11]),
    (5, "Gran Tour del Quindío", "Parque del Café, Granja Quindiana, Valle de las Palmas y Mirador de Filandia",
     160000, "activo", [1, 2, 5, 8], [8]),
    (6, "Festival y Museo", "Festival de la Cosecha y Museo de la Cultura Quimbaya",
     40000, "inactivo", [7, 4], []),
]


def generar_combos():
    insertar("Combo", ["id_combo", "nombre", "descripcion", "precio_especial", "estado"],
             [c[:5] for c in COMBOS])
    filas = []
    for combo in COMBOS:
        for id_atr in combo[5]:
            filas.append((len(filas) + 1, combo[0], id_atr))
    insertar("Combo_atraccion", ["id_combo_atraccion", "id_combo", "id_atraccion"], filas)


# ---------------------------------------------------------------------
# FUNCIONES Y ZONAS
# Cada atracción abre en unas fechas (funciones), sorteadas con el peso
# de la temporada. Cada función se divide en zonas con aforo y precio.
# Los aforos son pequeños porque representan los cupos que se venden por
# QuindíoStay: así los reportes de ocupación muestran diferencias.
# ---------------------------------------------------------------------
# Horario y zonas de cada atracción: (apertura, cierre, popularidad, zonas)
# La popularidad es un peso: el Parque del Café (28) recibe muchas más
# funciones que la finca cafetera (8).
# zonas = lista de (nombre, aforo mínimo, aforo máximo, precio base)
CONFIG_ATRACCION = {
    1: ("09:00", "18:00", 28, [("General", 25, 35, 72000), ("Preferencial", 10, 14, 95000), ("VIP", 5, 8, 130000)]),
    2: ("09:00", "17:30", 22, [("General", 25, 35, 65000), ("Preferencial", 10, 14, 85000), ("VIP", 5, 8, 115000)]),
    3: ("08:30", "16:30", 15, [("General", 20, 30, 30000), ("Preferencial", 8, 12, 42000)]),
    4: ("09:00", "17:00", 10, [("General", 20, 30, 15000)]),
    5: ("07:00", "15:00", 15, [("General", 15, 25, 40000), ("Preferencial", 6, 10, 60000)]),
    6: ("10:00", "17:00", 12, [("General", 25, 35, 45000), ("Preferencial", 10, 14, 60000), ("VIP", 4, 6, 90000)]),
    7: ("14:00", "23:00", 6, [("General", 40, 60, 35000), ("Preferencial", 15, 20, 55000), ("VIP", 6, 10, 90000)]),
    8: ("08:00", "19:00", 12, [("General", 20, 30, 18000)]),
    9: ("08:00", "12:00", 8, [("General", 10, 15, 50000)]),
    10: ("08:30", "17:00", 12, [("General", 15, 25, 55000), ("Preferencial", 6, 10, 75000)]),
}

# El Festival de la Cosecha solo se hace unos días de octubre cada año.
FECHAS_FESTIVAL = [date(2025, 10, 10), date(2025, 10, 11), date(2025, 10, 12),
                   date(2026, 10, 9), date(2026, 10, 10), date(2026, 10, 11)]

FUNCIONES = {}   # id_atraccion -> lista de funciones (diccionarios)


# Total de funciones que se reparten entre las atracciones (sin contar
# las del Festival, que tiene sus fechas fijas).
TOTAL_FUNCIONES = 180


def repartir_funciones():
    """Reparte las funciones de todas las atracciones en dos pasos:
    1. Cuántas funciones tiene cada MES, según el peso de sus días
       (temporada). Diciembre y enero reciben muchas; febrero y
       septiembre pocas, pero nunca cero, porque los parques abren todo
       el año.
    2. A qué ATRACCIÓN le toca cada función de ese mes, según su
       popularidad, y en qué día del mes (otra vez según el peso).
    Devuelve un diccionario: id_atraccion -> lista de fechas."""
    todas = [INICIO + timedelta(days=i) for i in range((FIN - INICIO).days + 1)]
    meses = {}
    for d in todas:
        meses.setdefault((d.year, d.month), []).append(d)
    peso_mes = {m: sum(peso_fecha(d) for d in dias) for m, dias in meses.items()}
    total = sum(peso_mes.values())

    # Reparto proporcional por mes: primero la parte entera y después los
    # sobrantes a los meses con mayor residuo (método del mayor residuo).
    exacto = {m: TOTAL_FUNCIONES * p / total for m, p in peso_mes.items()}
    cupos = {m: int(x) for m, x in exacto.items()}
    faltan = TOTAL_FUNCIONES - sum(cupos.values())
    for m in sorted(exacto, key=lambda m: exacto[m] - cupos[m], reverse=True)[:faltan]:
        cupos[m] += 1

    atracciones = [a for a in CONFIG_ATRACCION if a != 7]
    popularidad = [CONFIG_ATRACCION[a][2] for a in atracciones]
    fechas = {a: set() for a in atracciones}
    for m, cupo in cupos.items():
        dias = meses[m]
        pesos = [peso_fecha(d) for d in dias]
        asignadas = 0
        while asignadas < cupo:
            id_atr = random.choices(atracciones, popularidad)[0]
            dia = random.choices(dias, pesos)[0]
            if dia not in fechas[id_atr]:      # una función por atracción y día
                fechas[id_atr].add(dia)
                asignadas += 1
    resultado = {a: sorted(f) for a, f in fechas.items()}
    resultado[7] = FECHAS_FESTIVAL
    return resultado


def generar_funciones():
    filas_funcion, filas_zona = [], []
    fechas_por_atraccion = repartir_funciones()
    for id_atr, (apertura, cierre, popularidad, zonas) in CONFIG_ATRACCION.items():
        fechas = fechas_por_atraccion[id_atr]

        FUNCIONES[id_atr] = []
        for fecha in fechas:
            id_funcion = len(filas_funcion) + 1
            if fecha >= HOY:
                estado = "programada"
            else:
                # Unas pocas funciones pasadas se cancelaron (por lluvia, por ejemplo).
                estado = "cancelada" if random.random() < 0.05 else "realizada"
            filas_funcion.append((id_funcion, fecha, apertura, cierre, estado, id_atr))

            funcion = {"id": id_funcion, "fecha": fecha, "estado": estado, "zonas": []}
            # En temporada alta las zonas cuestan 15 % más.
            recargo = 1.15 if temporada(fecha) == "alta" else 1.0
            for nombre, aforo_min, aforo_max, precio in zonas:
                id_zona = len(filas_zona) + 1
                aforo = random.randint(aforo_min, aforo_max)
                precio_zona = redondear(precio * recargo)
                filas_zona.append((id_zona, nombre, aforo, precio_zona, id_funcion))
                funcion["zonas"].append({"id": id_zona, "nombre": nombre, "aforo": aforo,
                                         "precio": precio_zona, "vendidas": 0})
            FUNCIONES[id_atr].append(funcion)

    insertar("Funcion", ["id_funcion", "fecha", "hora_apertura", "hora_cierre", "estado",
                         "id_atraccion"], filas_funcion)
    insertar("Zona", ["id_zona", "nombre", "aforo", "precio", "id_funcion"], filas_zona)


def generar_parte_3():
    titulo("PARTE 3 · Planes, combos, funciones y zonas")
    generar_planes()
    generar_combos()
    generar_funciones()


# =====================================================================
# PARTE 4 · RESERVAS, BOLETAS Y PAGOS
# Aquí se aplican casi todas las reglas del negocio:
#   - el plan es de un prestador afiliado a la atracción y está activo y
#     vigente en la fecha del servicio;
#   - ninguna zona vende más boletas que su aforo;
#   - una boleta de niño siempre va con una de adulto en la misma reserva;
#   - cada unidad de combo tiene una boleta por cada atracción del combo
#     y se cobra el precio especial (no el precio de cada zona);
#   - las reservas pasadas quedan completadas o canceladas y las futuras
#     pendientes o confirmadas;
#   - en las reservas pagadas, los pagos exitosos suman el valor total;
#   - al cancelar con más de 5 días de anticipación se reembolsa el 80 %.
# =====================================================================

TOTAL_RESERVAS = 500
RESERVAS = []        # información de cada reserva para la parte 5
_referencias = set()


def referencia_unica(metodo):
    """Referencia simulada de la pasarela de pagos (no se repite)."""
    prefijo = "PSE" if metodo == "PSE" else "TRX"
    while True:
        ref = f"{prefijo}-{random.randint(1000000000, 9999999999)}"
        if ref not in _referencias:
            _referencias.add(ref)
            return ref


def prestadores_que_venden(atracciones, fecha_creacion, fecha_servicio):
    """Prestadores afiliados a TODAS las atracciones (desde antes de crear
    la reserva) que tienen un plan activo para el año del servicio."""
    candidatos = []
    for id_p, *_ in PRESTADORES:
        afiliado = all(any(a[0] == id_p and a[1] == id_atr and a[2] <= fecha_creacion
                           for a in AFILIACIONES) for id_atr in atracciones)
        if afiliado and (id_p, fecha_servicio.year) in PLANES_BOLETERIA:
            candidatos.append(id_p)
    return candidatos


def disponibles(zona):
    return zona["aforo"] - zona["vendidas"]


def escoger_funcion(id_atr=None, desde=None, hasta=None):
    """Escoge una función que no esté cancelada y que tenga cupo, con más
    probabilidad en las atracciones populares.
    No se vuelve a usar el peso de la temporada: las funciones YA están
    concentradas en temporada alta (repartir_funciones). Si se usara otra
    vez, la temporada contaría doble y los meses flojos quedarían vacíos."""
    opciones, pesos = [], []
    for atr, funciones in FUNCIONES.items():
        if id_atr is not None and atr != id_atr:
            continue
        for f in funciones:
            if f["estado"] == "cancelada":
                continue
            if desde and f["fecha"] < desde:
                continue
            if hasta and f["fecha"] > hasta:
                continue
            if all(disponibles(z) <= 0 for z in f["zonas"]):
                continue
            opciones.append((atr, f))
            pesos.append(CONFIG_ATRACCION[atr][2])
    if not opciones:
        return None
    return random.choices(opciones, pesos)[0]


def escoger_zona(funcion, cantidad):
    """Zona con cupo para 'cantidad' boletas. General es la más vendida."""
    preferencia = {"General": 65, "Preferencial": 25, "VIP": 10}
    zonas = [z for z in funcion["zonas"] if disponibles(z) >= cantidad]
    if not zonas:
        return None
    return random.choices(zonas, [preferencia[z["nombre"]] for z in zonas])[0]


def tipos_del_grupo(personas):
    """Tipo de entrada de cada persona del grupo. Regla: si hay un niño,
    en la misma reserva tiene que haber al menos un adulto."""
    tipos = random.choices([1, 2, 3, 4], [55, 22, 12, 11], k=personas)
    if 2 in tipos and 1 not in tipos:
        tipos[0] = 1          # el primero pasa a ser el adulto responsable
    return tipos


def escoger_cliente(canal):
    """Las reservas por web o aplicación necesitan cuenta. Los menores de
    edad no hacen reservas. Unos clientes reservan mucho más que otros."""
    opciones = [c for c in CLIENTES if c["id"] not in (17, 64, 112)
                and (c["cuenta"] or canal not in ("web", "aplicación móvil"))]
    pesos = [c["frecuencia"] for c in opciones]
    return random.choices(opciones, pesos)[0]


def escoger_canal(id_prestador):
    if id_prestador >= 8:     # agencias de viajes
        return random.choices(["agencia intermediaria", "web", "teléfono"], [70, 20, 10])[0]
    return random.choices(["web", "aplicación móvil", "teléfono", "mostrador"], [45, 25, 10, 20])[0]


def escoger_metodo(canal):
    if canal in ("web", "aplicación móvil"):
        return random.choices(["tarjeta de crédito", "tarjeta débito", "PSE"], [45, 20, 35])[0]
    if canal == "mostrador":
        return random.choices(["efectivo", "tarjeta débito", "tarjeta de crédito"], [55, 30, 15])[0]
    if canal == "teléfono":
        return random.choices(["PSE", "transferencia"], [60, 40])[0]
    return random.choices(["transferencia", "PSE"], [70, 30])[0]


def fecha_de_creacion(fecha_servicio):
    """Cuántos días antes se reservó: en temporada alta se reserva con
    más anticipación. Una reserva nunca se crea después de HOY."""
    if temporada(fecha_servicio) == "alta":
        anticipacion = random.choices([0, 3, 10, 25, 45], [5, 15, 30, 30, 20])[0]
    else:
        anticipacion = random.choices([0, 2, 5, 12, 25], [15, 30, 30, 15, 10])[0]
    anticipacion += random.randint(0, 3)
    creacion = fecha_servicio - timedelta(days=anticipacion)
    if creacion > HOY:
        # Una reserva no puede crearse en el futuro. Si la cuenta da una
        # fecha posterior a HOY, se reparte en los 45 días anteriores
        # (si se pusiera siempre HOY, se amontonarían todas en un día).
        creacion = HOY - timedelta(days=random.randint(0, 45))
    return creacion


def generar_parte_4():
    titulo("PARTE 4 · Reservas, boletas y pagos")

    # Unos clientes son muy frecuentes y otros reservan una sola vez o
    # nunca (para que los reportes por cliente muestren diferencias).
    for c in CLIENTES:
        c["frecuencia"] = random.choices([0.3, 1, 2, 4], [25, 45, 22, 8])[0]

    filas_reserva, filas_boleta, filas_pago = [], [], []

    while len(filas_reserva) < TOTAL_RESERVAS:
        es_combo = random.random() < 0.15
        boletas = []        # (zona, tipo_entrada, id_combo)

        if es_combo:
            combo = random.choices([c for c in COMBOS if c[4] == "activo"],
                                   [5, 3, 3, 4, 2])[0]
            id_combo, precio_combo, atracciones = combo[0], combo[3], combo[5]
            unidades = random.choices([1, 2, 3, 4], [40, 35, 15, 10])[0]
            # La primera atracción fija la fecha; las demás se visitan
            # el mismo día o en los 15 días siguientes (días diferentes).
            elegido = escoger_funcion(atracciones[0])
            if elegido is None:
                continue
            funciones_combo = [elegido[1]]
            for id_atr in atracciones[1:]:
                f = escoger_funcion(id_atr, elegido[1]["fecha"],
                                    elegido[1]["fecha"] + timedelta(days=15))
                if f is None:
                    break
                funciones_combo.append(f[1])
            if len(funciones_combo) < len(atracciones):
                continue
            zonas_combo = [escoger_zona(f, unidades) for f in funciones_combo]
            if None in zonas_combo:
                continue
            fecha_servicio = funciones_combo[0]["fecha"]
            fecha_creacion = fecha_de_creacion(fecha_servicio)
            vendedores = prestadores_que_venden(atracciones, fecha_creacion, fecha_servicio)
            if not vendedores:
                continue
            id_prestador = random.choice(vendedores)
            for _ in range(unidades):
                tipo = random.choices([1, 3, 4], [70, 15, 15])[0]
                for zona in zonas_combo:
                    boletas.append((zona, tipo, id_combo))
            valor_total = precio_combo * unidades
        else:
            elegido = escoger_funcion()
            if elegido is None:
                continue
            id_atr, funcion = elegido
            personas = random.choices([1, 2, 3, 4, 5, 6], [12, 35, 20, 20, 8, 5])[0]
            zona = escoger_zona(funcion, personas)
            if zona is None:
                continue
            fecha_servicio = funcion["fecha"]
            fecha_creacion = fecha_de_creacion(fecha_servicio)
            vendedores = prestadores_que_venden([id_atr], fecha_creacion, fecha_servicio)
            if not vendedores:
                continue
            # El operador de la atracción vende más que las agencias.
            pesos = [4 if v <= 7 else 1 for v in vendedores]
            id_prestador = random.choices(vendedores, pesos)[0]
            valor_total = 0
            for tipo in tipos_del_grupo(personas):
                descuento = TIPOS_ENTRADA[tipo - 1][2]
                valor_total += redondear(zona["precio"] * (100 - descuento) / 100)
                boletas.append((zona, tipo, None))

        id_reserva = len(filas_reserva) + 1
        canal = escoger_canal(id_prestador)
        cliente = escoger_cliente(canal)
        id_plan = PLANES_BOLETERIA[(id_prestador, fecha_servicio.year)]

        # Estado de la reserva según si el servicio ya pasó.
        if fecha_servicio < HOY:
            estado = "cancelada" if random.random() < 0.09 else "completada"
        else:
            estado = random.choices(["confirmada", "pendiente", "cancelada"], [55, 38, 7])[0]

        fecha_cancelacion = None
        if estado == "cancelada":
            dias = (fecha_servicio - fecha_creacion).days
            fecha_cancelacion = fecha_creacion + timedelta(days=random.randint(0, max(dias, 0)))
            fecha_cancelacion = min(fecha_cancelacion, HOY)

        filas_reserva.append((id_reserva, fecha_creacion, fecha_servicio, estado,
                              valor_total, canal, cliente["id"], id_plan))

        # Boletas: código único QS-000001, QS-000002...
        for zona, tipo, id_combo in boletas:
            id_boleta = len(filas_boleta) + 1
            if estado == "cancelada":
                estado_boleta = "anulada"        # libera el cupo de la zona
            elif estado == "completada":
                # Casi todos asistieron; unos pocos no usaron su boleta.
                estado_boleta = "usada" if random.random() < 0.95 else "vendida"
                zona["vendidas"] += 1
            else:
                estado_boleta = "vendida"
                zona["vendidas"] += 1
            filas_boleta.append((id_boleta, f"QS-{id_boleta:06d}", estado_boleta,
                                 id_reserva, zona["id"], tipo, id_combo))

        # ---------------- Pagos ----------------
        metodo = escoger_metodo(canal)
        pagos = []          # (fecha, monto, metodo, estado)

        def pago_exitoso(fecha, monto, metodo_pago):
            # A veces el primer intento con tarjeta o PSE falla y se reintenta.
            if metodo_pago in ("tarjeta de crédito", "tarjeta débito", "PSE") and random.random() < 0.12:
                pagos.append((fecha, monto, metodo_pago, "fallido"))
            pagos.append((fecha, monto, metodo_pago, "exitoso"))

        pagada = estado in ("completada", "confirmada") or (
            estado == "cancelada" and random.random() < 0.8)
        if pagada:
            if random.random() < 0.3 and valor_total >= 60000:
                # Anticipo del 50 % al reservar y saldo al llegar.
                anticipo = redondear(valor_total * 0.5)
                pago_exitoso(fecha_creacion, anticipo, metodo)
                fecha_saldo = fecha_servicio if fecha_servicio <= HOY else fecha_creacion
                if estado == "cancelada":
                    fecha_saldo = min(fecha_cancelacion, fecha_saldo)
                metodo_saldo = random.choice(["efectivo", "tarjeta débito"]) if fecha_saldo == fecha_servicio else metodo
                pago_exitoso(max(fecha_saldo, fecha_creacion), valor_total - anticipo, metodo_saldo)
            else:
                pago_exitoso(fecha_creacion, valor_total, metodo)
        elif estado == "pendiente":
            caso = random.choices(["sin pago", "anticipo", "en proceso"], [45, 35, 20])[0]
            if caso == "anticipo" and valor_total >= 20000:
                pago_exitoso(fecha_creacion, redondear(valor_total * 0.5), metodo)
            elif caso == "en proceso":
                pagos.append((fecha_creacion, valor_total, "PSE", "pendiente"))

        # Regla de cancelación: con más de 5 días de anticipación se
        # reembolsa el 80 %; los pagos exitosos quedan como 'reembolsado'.
        reembolso = 0
        if estado == "cancelada":
            if (fecha_servicio - fecha_cancelacion).days > 5:
                pagado = sum(p[1] for p in pagos if p[3] == "exitoso")
                reembolso = redondear(pagado * 0.8)
                pagos = [(f, m, me, "reembolsado" if e == "exitoso" else e)
                         for f, m, me, e in pagos]

        for fecha, monto, metodo_pago, estado_pago in pagos:
            ref = referencia_unica(metodo_pago) if metodo_pago in (
                "tarjeta de crédito", "tarjeta débito", "PSE") else None
            filas_pago.append((len(filas_pago) + 1, fecha, monto, metodo_pago,
                               estado_pago, ref, id_reserva))

        RESERVAS.append({"id": id_reserva, "cliente": cliente["id"], "estado": estado,
                         "creacion": fecha_creacion, "servicio": fecha_servicio,
                         "cancelacion": fecha_cancelacion, "reembolso": reembolso,
                         "pagos": [(f, e) for f, _, _, e in pagos]})

    insertar("Reserva", ["id_reserva", "fecha_creacion", "fecha_servicio", "estado",
                         "valor_total", "canal", "id_cliente", "id_plan"], filas_reserva)
    insertar("Boleta", ["id_boleta", "codigo", "estado", "id_reserva", "id_zona",
                        "id_tipo_entrada", "id_combo"], filas_boleta)
    insertar("Pago", ["id_pago", "fecha", "monto", "metodo", "estado",
                      "referencia_transaccion", "id_reserva"], filas_pago)


# =====================================================================
# PARTE 5 · NOTIFICACIONES
# Reglas (enunciado y Entrega 2):
#   - una notificación de una reserva lleva id_reserva (y id_cliente en
#     NULL); una promoción lleva id_cliente (e id_reserva en NULL);
#   - los recordatorios se programan con la anticipación (1, 3 o 7 días)
#     y el canal que el cliente escogió en su perfil;
#   - las promociones solo van a clientes que las aceptaron;
#   - una notificación no se envía antes de su fecha programada;
#   - lo programado para después de "ahora" sigue pendiente.
# =====================================================================

# "Ahora" para las notificaciones: el 5 de noviembre de 2026 a las 8 a. m.
AHORA = datetime(2026, 11, 5, 8, 0)

MENSAJES = {
    "confirmación de reserva": (
        "Hola, tu reserva #{r} para el {f} quedó registrada en QuindíoStay.",
        "Hi, your QuindíoStay booking #{r} for {f} has been registered."),
    "confirmación de pago": (
        "Recibimos tu pago de la reserva #{r}. ¡Gracias por elegir QuindíoStay!",
        "We received your payment for booking #{r}. Thank you for choosing QuindíoStay!"),
    "recordatorio de reserva": (
        "Recordatorio: tu visita de la reserva #{r} es el {f}. Lleva tus boletas QS.",
        "Reminder: your visit for booking #{r} is on {f}. Bring your QS tickets."),
    "cambio de estado": (
        "Tu reserva #{r} cambió a estado: {e}.",
        "Your booking #{r} status changed to: {e}."),
}

PROMOCIONES = [
    (datetime(2025, 3, 20, 10, 0), "Semana Santa en el Quindío: 15 % de descuento en el combo Ruta del Café.",
     "Holy Week in Quindío: 15% off the Coffee Route combo."),
    (datetime(2025, 6, 5, 10, 0), "Vacaciones de mitad de año: niños con 40 % de descuento en todos los parques.",
     "Mid-year holidays: kids get 40% off in every park."),
    (datetime(2025, 11, 25, 10, 0), "Diciembre en el Quindío: reserva tu Gran Tour antes del 10 de diciembre.",
     "December in Quindío: book your Grand Tour before December 10."),
    (datetime(2026, 3, 12, 10, 0), "Semana Santa 2026: pasaporte Café y Montaña con precio especial.",
     "Holy Week 2026: Coffee and Mountain pass at a special price."),
    (datetime(2026, 6, 4, 10, 0), "Vacaciones 2026: combo Aventura Quindiana para toda la familia.",
     "2026 holidays: Quindío Adventure combo for the whole family."),
    (datetime(2026, 11, 20, 10, 0), "Temporada de fin de año: nuevas funciones del Parque Temático del Café.",
     "Year-end season: new shows at the Coffee Theme Park."),
]


def resultado_envio(programada, canal):
    """Estado y fecha real de envío de una notificación.
    Si todavía no llega su fecha: pendiente y sin fecha real.
    Si ya llegó: enviada, leída o fallida (SMS y WhatsApp fallan más)."""
    if programada > AHORA:
        return "pendiente", None
    prob_falla = 0.03 if canal == "correo" else 0.08
    if random.random() < prob_falla:
        return "fallida", None
    real = programada + timedelta(minutes=random.randint(0, 20))
    real = min(real, AHORA)
    estado = "leída" if random.random() < 0.45 else "enviada"
    return estado, real


def generar_parte_5():
    titulo("PARTE 5 · Notificaciones")
    clientes = {c["id"]: c for c in CLIENTES}
    filas = []

    def agregar(tipo, programada, cliente, id_reserva=None, estado_reserva=None,
                fecha_servicio=None, textos_promocion=None):
        # Canal e idioma del perfil; quien no tiene perfil recibe SMS en español.
        canal = cliente["canal"] or "SMS"
        idioma = 1 if cliente["idioma"] == "inglés" else 0
        if tipo == "promoción":
            mensaje = textos_promocion[idioma]
        else:
            mensaje = MENSAJES[tipo][idioma].format(
                r=id_reserva, f=fecha_servicio.strftime("%d/%m/%Y") if fecha_servicio else "",
                e=estado_reserva)
        estado, real = resultado_envio(programada, canal)
        filas.append((len(filas) + 1, tipo, canal, mensaje, programada, real, estado,
                      cliente["id"] if tipo == "promoción" else None,
                      id_reserva if tipo != "promoción" else None))

    for r in RESERVAS:
        cliente = clientes[r["cliente"]]
        hora = datetime.combine(r["creacion"], datetime.min.time()) + timedelta(
            hours=random.randint(7, 20), minutes=random.randint(0, 59))

        # 1. Confirmación de la reserva, el mismo día en que se creó.
        agregar("confirmación de reserva", hora, cliente, r["id"], fecha_servicio=r["servicio"])

        # 2. Confirmación de cada pago exitoso o reembolsado (fue exitoso).
        for fecha_pago, estado_pago in r["pagos"]:
            if estado_pago in ("exitoso", "reembolsado"):
                momento = datetime.combine(fecha_pago, datetime.min.time()) + timedelta(
                    hours=random.randint(8, 21), minutes=random.randint(0, 59))
                agregar("confirmación de pago", momento, cliente, r["id"])

        # 3. Recordatorio según el perfil: X días antes, a las 8 a. m.
        #    Solo si el cliente tiene perfil, la reserva no se canceló y la
        #    fecha del recordatorio no es anterior a la creación.
        if cliente["anticipacion"] and r["estado"] != "cancelada":
            dia = r["servicio"] - timedelta(days=cliente["anticipacion"])
            if dia > r["creacion"]:
                agregar("recordatorio de reserva", datetime.combine(dia, datetime.min.time())
                        + timedelta(hours=8), cliente, r["id"], fecha_servicio=r["servicio"])

        # 4. Cambio de estado: cancelaciones y reservas completadas.
        if r["estado"] == "cancelada":
            momento = datetime.combine(r["cancelacion"], datetime.min.time()) + timedelta(
                hours=random.randint(9, 19))
            agregar("cambio de estado", momento, cliente, r["id"], estado_reserva="cancelada")
        elif r["estado"] == "completada" and random.random() < 0.3:
            momento = datetime.combine(r["servicio"], datetime.min.time()) + timedelta(hours=20)
            agregar("cambio de estado", momento, cliente, r["id"], estado_reserva="completada")

    # 5. Promociones: solo a clientes con acepta_promociones = 'sí'.
    aceptan = [c for c in CLIENTES if c["promociones"]]
    for programada, texto_es, texto_en in PROMOCIONES:
        # Cada promoción le llega al 60 % de los clientes que las aceptan.
        for cliente in random.sample(aceptan, k=int(len(aceptan) * 0.6)):
            agregar("promoción", programada, cliente, textos_promocion=(texto_es, texto_en))

    insertar("Notificacion", ["id_notificacion", "tipo", "canal", "mensaje", "fecha_programada",
                              "fecha_real_envio", "estado", "id_cliente", "id_reserva"], filas)


# =====================================================================
# PROGRAMA PRINCIPAL
# =====================================================================

def main():
    lineas.append("-- " + "=" * 69)
    lineas.append("-- QuindíoStay · Script 02: carga de datos de ejemplo")
    lineas.append("-- ARCHIVO GENERADO por bd/generador/generar_datos.py: no editar a mano.")
    lineas.append("-- Ejecutar DESPUÉS de 00_limpieza.sql y 01_creacion_tablas.sql.")
    lineas.append("-- " + "=" * 69)
    lineas.append("")
    lineas.append("USE quindiostay;")
    lineas.append("")
    lineas.append("-- Orden de carga: primero los padres y después las hijas (Clase 14).")
    lineas.append("-- Cada INSERT lleva la lista de columnas. MySQL permite varias filas")
    lineas.append("-- en un mismo INSERT (VALUES (...), (...)), así el script es más corto.")
    lineas.append("-- Workbench trabaja con autocommit; igual se confirma con COMMIT al final.")
    lineas.append("")

    generar_parte_1()
    generar_parte_2()
    generar_parte_3()
    generar_parte_4()
    generar_parte_5()

    # Al final se confirma todo con COMMIT (Clase 14: el carrito de compras)
    # y se cuentan las filas de cada tabla para comprobar la carga.
    titulo("CONFIRMAR Y VERIFICAR LA CARGA")
    lineas.append("COMMIT;")
    lineas.append("")
    lineas.append("-- Conteo de filas por tabla (mínimos del enunciado, sección 4)")
    tablas = ["Municipio", "Tipo_prestador", "Rol", "Tipo_entrada", "Prestador",
              "Atraccion", "Prestador_atraccion", "Usuario_sistema", "Cliente", "Cuenta",
              "Perfil_cliente", "Plan", "Combo", "Combo_atraccion", "Funcion", "Zona",
              "Reserva", "Boleta", "Pago", "Notificacion"]
    consultas = [f"SELECT '{t}' AS tabla, COUNT(*) AS filas FROM {t}" for t in tablas]
    lineas.append("\nUNION ALL\n".join(consultas) + ";")

    ARCHIVO_SALIDA.write_text("\n".join(lineas) + "\n", encoding="utf-8")
    print(f"Listo: se escribió {ARCHIVO_SALIDA}")


if __name__ == "__main__":
    main()
