package co.edu.uniquindio.quindiostay.servicio;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Traduce los errores que devuelve MySQL a mensajes entendibles para la pantalla.
 * Se usa el código de error de MySQL y el nombre de la restricción
 * (por eso la Parte A le puso nombre a cada una: pk_, uq_, ck_, fk_).
 *
 * Códigos de MySQL que se manejan:
 *   1062 -> se repite un valor UNIQUE o la PK
 *   1451 -> no se puede borrar el padre porque tiene hijas (ON DELETE RESTRICT)
 *   1452 -> la foránea apunta a un padre que no existe
 *   3819 -> se violó un CHECK
 *   1048 -> se dejó vacía una columna NOT NULL
 *   1406 -> el texto es más largo de lo permitido
 */
public final class MensajesError {

    private static final Map<String, String> RESTRICCIONES = new HashMap<>();

    static {
        // UNIQUE
        RESTRICCIONES.put("uq_municipio_nombre", "Ya existe un municipio con ese nombre.");
        RESTRICCIONES.put("uq_municipio_dane", "Ya existe un municipio con ese código DANE.");
        RESTRICCIONES.put("uq_tipo_prestador_nombre", "Ya existe un tipo de prestador con ese nombre.");
        RESTRICCIONES.put("uq_rol_nombre", "Ese rol ya existe.");
        RESTRICCIONES.put("uq_cliente_documento", "Ya existe un cliente con ese tipo y número de documento.");
        RESTRICCIONES.put("uq_cliente_email", "Ya existe un cliente con ese correo electrónico.");
        RESTRICCIONES.put("uq_tipo_entrada_nombre", "Ese tipo de entrada ya existe.");
        RESTRICCIONES.put("uq_prestador_nit", "Ya existe un prestador con ese NIT o documento.");
        RESTRICCIONES.put("uq_cuenta_cliente", "Ese cliente ya tiene una cuenta (relación 1:1).");
        RESTRICCIONES.put("uq_perfil_cliente", "Ese cliente ya tiene un perfil (relación 1:1).");
        RESTRICCIONES.put("uq_combo_atraccion", "Esa atracción ya forma parte del combo.");
        RESTRICCIONES.put("uq_usuario_nombre", "Ya existe un usuario con ese nombre de usuario.");
        RESTRICCIONES.put("uq_plan_prestador_nombre", "Ese prestador ya tiene un plan con ese nombre.");
        RESTRICCIONES.put("uq_zona_funcion_nombre", "Esa función ya tiene una zona con ese nombre.");
        RESTRICCIONES.put("uq_pago_referencia", "Ya existe un pago con esa referencia de transacción.");
        RESTRICCIONES.put("uq_boleta_codigo", "Ya existe una boleta con ese código.");
        RESTRICCIONES.put("PRIMARY", "Ya existe un registro con esa clave primaria.");
        // CHECK
        RESTRICCIONES.put("ck_municipio_dane", "El código DANE debe tener exactamente 5 dígitos.");
        RESTRICCIONES.put("ck_rol_nombre", "El rol debe ser administrador de la plataforma, encargado de prestador u operador.");
        RESTRICCIONES.put("ck_cliente_tipo_doc", "El tipo de documento no es válido.");
        RESTRICCIONES.put("ck_cliente_email", "El correo electrónico no es válido (debe tener @ y punto).");
        RESTRICCIONES.put("ck_cliente_telefono", "El teléfono solo puede tener dígitos (entre 7 y 15).");
        RESTRICCIONES.put("ck_tipo_entrada_nombre", "El tipo de entrada debe ser adulto, niño, adulto mayor o estudiante.");
        RESTRICCIONES.put("ck_tipo_entrada_desc", "El porcentaje de descuento debe estar entre 0 y 100.");
        RESTRICCIONES.put("ck_combo_precio", "El precio especial del combo debe ser mayor que 0.");
        RESTRICCIONES.put("ck_combo_estado", "El estado del combo debe ser activo o inactivo.");
        RESTRICCIONES.put("ck_prestador_telefono", "El teléfono del prestador solo puede tener dígitos (entre 7 y 15).");
        RESTRICCIONES.put("ck_prestador_correo", "El correo del prestador no es válido.");
        RESTRICCIONES.put("ck_prestador_estado", "El estado del prestador debe ser activo, suspendido o retirado.");
        RESTRICCIONES.put("ck_perfil_idioma", "El idioma debe ser español o inglés.");
        RESTRICCIONES.put("ck_perfil_canal", "El canal debe ser correo, SMS o WhatsApp.");
        RESTRICCIONES.put("ck_perfil_anticipacion", "La anticipación del recordatorio debe ser 1, 3 o 7 días.");
        RESTRICCIONES.put("ck_perfil_promociones", "Acepta promociones debe ser sí o no.");
        RESTRICCIONES.put("ck_funcion_horas", "La hora de cierre debe ser posterior a la hora de apertura.");
        RESTRICCIONES.put("ck_funcion_estado", "El estado de la función debe ser programada, cancelada o realizada.");
        RESTRICCIONES.put("ck_usuario_estado", "El estado del usuario debe ser activo o bloqueado.");
        RESTRICCIONES.put("ck_plan_tipo", "El tipo de plan debe ser básico, estándar, premium o todo incluido.");
        RESTRICCIONES.put("ck_plan_precio", "El precio del plan debe ser mayor que 0.");
        RESTRICCIONES.put("ck_plan_fechas", "La fecha de fin del plan no puede ser anterior a la fecha de inicio.");
        RESTRICCIONES.put("ck_plan_estado", "El estado del plan debe ser activo o inactivo.");
        RESTRICCIONES.put("ck_zona_aforo", "El aforo de la zona debe ser mayor que 0.");
        RESTRICCIONES.put("ck_zona_precio", "El precio de la zona no puede ser negativo.");
        RESTRICCIONES.put("ck_reserva_fechas", "La fecha del servicio no puede ser anterior a la fecha de creación de la reserva.");
        RESTRICCIONES.put("ck_reserva_estado", "El estado de la reserva no es válido.");
        RESTRICCIONES.put("ck_reserva_valor", "El valor total de la reserva no puede ser negativo.");
        RESTRICCIONES.put("ck_reserva_canal", "El canal de la reserva no es válido.");
        RESTRICCIONES.put("ck_pago_monto", "El monto del pago debe ser mayor que 0.");
        RESTRICCIONES.put("ck_pago_metodo", "El método de pago no es válido.");
        RESTRICCIONES.put("ck_pago_estado", "El estado del pago no es válido.");
        RESTRICCIONES.put("ck_pago_referencia", "Los pagos con tarjeta o PSE deben tener referencia de transacción.");
        RESTRICCIONES.put("ck_notif_tipo", "El tipo de notificación no es válido.");
        RESTRICCIONES.put("ck_notif_canal", "El canal de la notificación debe ser correo, SMS o WhatsApp.");
        RESTRICCIONES.put("ck_notif_estado", "El estado de la notificación no es válido.");
        RESTRICCIONES.put("ck_notif_destino", "Una notificación va a una reserva o a un cliente, pero no a los dos ni a ninguno.");
        RESTRICCIONES.put("ck_notif_tipo_destino", "Una promoción va dirigida a un cliente; los demás tipos van ligados a una reserva.");
        RESTRICCIONES.put("ck_notif_fechas", "La fecha real de envío no puede ser anterior a la fecha programada.");
        RESTRICCIONES.put("ck_notif_pendiente", "Una notificación pendiente todavía no puede tener fecha real de envío.");
        RESTRICCIONES.put("ck_boleta_estado", "El estado de la boleta debe ser vendida, usada o anulada.");
        // FOREIGN KEY (al borrar el padre, 1451; o al apuntar a un padre que no existe, 1452)
        RESTRICCIONES.put("fk_prestador_municipio", "el municipio (tiene prestadores asociados)");
        RESTRICCIONES.put("fk_prestador_tipo", "el tipo de prestador (hay prestadores de ese tipo)");
        RESTRICCIONES.put("fk_funcion_atraccion", "la atracción (tiene funciones registradas)");
        RESTRICCIONES.put("fk_combo_atraccion_atraccion", "la atracción (forma parte de un combo)");
        RESTRICCIONES.put("fk_usuario_rol", "el rol (tiene usuarios)");
        RESTRICCIONES.put("fk_usuario_prestador", "el prestador (tiene usuarios encargados)");
        RESTRICCIONES.put("fk_plan_prestador", "el prestador (tiene planes)");
        RESTRICCIONES.put("fk_prest_atr_prestador", "el prestador (está afiliado a atracciones)");
        RESTRICCIONES.put("fk_prest_atr_atraccion", "la atracción (tiene prestadores afiliados)");
        RESTRICCIONES.put("fk_reserva_cliente", "el cliente (tiene reservas: es historial)");
        RESTRICCIONES.put("fk_reserva_plan", "el plan (tiene reservas)");
        RESTRICCIONES.put("fk_pago_reserva", "la reserva (tiene pagos)");
        RESTRICCIONES.put("fk_notif_cliente", "el cliente (tiene notificaciones)");
        RESTRICCIONES.put("fk_notif_reserva", "la reserva (tiene notificaciones)");
        RESTRICCIONES.put("fk_boleta_reserva", "la reserva (tiene boletas)");
        RESTRICCIONES.put("fk_boleta_zona", "la zona (tiene boletas vendidas)");
        RESTRICCIONES.put("fk_boleta_tipo_entrada", "el tipo de entrada (tiene boletas)");
        RESTRICCIONES.put("fk_boleta_combo", "el combo (tiene boletas vendidas)");
    }

    private MensajesError() {
    }

    public static String traducir(SQLException e) {
        String texto = e.getMessage() == null ? "" : e.getMessage();
        String restriccion = buscarRestriccion(texto);
        switch (e.getErrorCode()) {
            case 1062:
                return RESTRICCIONES.getOrDefault(restriccion, "Ese valor ya existe y no se puede repetir.");
            case 3819:
                return RESTRICCIONES.getOrDefault(restriccion, "Un dato no cumple las reglas de la base de datos.");
            case 1451:
                String padre = RESTRICCIONES.get(restriccion);
                return "No se puede eliminar " + (padre != null ? padre : "este registro porque otros datos dependen de él")
                        + ". La base de datos lo protege con ON DELETE RESTRICT.";
            case 1452:
                return "El registro relacionado que escogió no existe.";
            case 1048:
                return "Hay un campo obligatorio vacío.";
            case 1406:
                return "Un texto es más largo de lo permitido.";
            case 1292:
            case 1366:
                return "Un valor no tiene el formato correcto (fecha, número u hora).";
            case 0:
                if (texto.contains("Communications link failure") || texto.contains("Access denied")) {
                    return "No hay conexión con la base de datos. Revise que MySQL esté encendido y los datos de application.properties.";
                }
                return "Error de base de datos: " + texto;
            default:
                if (e.getErrorCode() == 1045) {
                    return "No hay conexión con la base de datos (usuario o contraseña de MySQL incorrectos).";
                }
                return "Error de base de datos (" + e.getErrorCode() + "): " + texto;
        }
    }

    /** Busca en el mensaje de MySQL el nombre de la restricción violada. */
    private static String buscarRestriccion(String texto) {
        for (String nombre : RESTRICCIONES.keySet()) {
            if (texto.contains("'" + nombre + "'") || texto.contains("`" + nombre + "`")
                    || texto.contains("." + nombre + "'")) {
                return nombre;
            }
        }
        return "";
    }
}
