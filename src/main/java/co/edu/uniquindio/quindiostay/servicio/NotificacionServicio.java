package co.edu.uniquindio.quindiostay.servicio;

import co.edu.uniquindio.quindiostay.dao.*;
import co.edu.uniquindio.quindiostay.modelo.Notificacion;
import co.edu.uniquindio.quindiostay.modelo.PerfilCliente;
import co.edu.uniquindio.quindiostay.modelo.Reserva;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Notificaciones (RF05). El envío real se SIMULA: «enviar» solo cambia el estado.
 * Envío automático: cada minuto (y antes de mostrar notificaciones) las que están
 * 'pendiente' y cuya fecha programada ya llegó pasan solas a 'enviada' con la fecha
 * real de envío. Las programadas a futuro (ej. el recordatorio de las 8:00) siguen
 * 'pendiente' («por enviar») hasta su hora. Las 'leída' y 'fallida' no se tocan.
 * El admin puede seguir usando el botón «Enviar» (ej. para reintentar una fallida).
 * Reglas que la base no puede revisar sola:
 *  - Las promociones solo van a clientes con acepta_promociones = 'sí' (mira Perfil_cliente).
 *  - El recordatorio se programa con la anticipación y el canal del perfil del cliente.
 * Las notificaciones no se borran: son historial (Tabla 39).
 */
@Service
public class NotificacionServicio {

    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final NotificacionDAO dao;
    private final PerfilClienteDAO perfilDAO;
    private final VistaDAO vistaDAO;
    private final ReservaDAO reservaDAO;
    private final ConexionBD conexionBD;

    /** Estados en los que una notificación todavía se puede editar (aún no le llegó al cliente). */
    public static final List<String> EDITABLES = List.of("pendiente", "fallida");

    /** Qué significa cada estado (se muestra en el formulario). */
    public static final Map<String, String> AYUDA_ESTADOS = Map.of(
            "pendiente", "Por enviar: la aplicación la envía sola cuando llega la fecha programada.",
            "fallida", "El envío falló: corrija canal o mensaje y vuelva a dejarla pendiente, o use «Enviar».",
            "enviada", "Ya le llegó al cliente: es historial y no se modifica.",
            "leída", "El cliente ya la leyó: es historial y no se modifica.");

    public NotificacionServicio(NotificacionDAO dao, PerfilClienteDAO perfilDAO, VistaDAO vistaDAO,
                                ReservaDAO reservaDAO, ConexionBD conexionBD) {
        this.dao = dao;
        this.perfilDAO = perfilDAO;
        this.vistaDAO = vistaDAO;
        this.reservaDAO = reservaDAO;
        this.conexionBD = conexionBD;
    }

    /** Una notificación se edita solo mientras no le ha llegado al cliente (pendiente o fallida). */
    public static boolean esEditable(Notificacion n) {
        return n.getIdNotificacion() == null || EDITABLES.contains(n.getEstado());
    }

    /**
     * Estados que se pueden escoger en el formulario:
     *  - nueva: solo 'pendiente' (la aplicación la pasa a 'enviada'; el cliente la marca 'leída').
     *  - pendiente: sigue 'pendiente' (para corregir mensaje, canal o fecha antes de que salga).
     *  - fallida: sigue 'fallida' o vuelve a 'pendiente' para reprogramarla.
     *  - enviada / leída: finales, no se editan.
     */
    public static List<String> estadosPosibles(Notificacion actual) {
        if (actual == null || actual.getIdNotificacion() == null) {
            return List.of("pendiente");
        }
        return switch (actual.getEstado()) {
            case "pendiente" -> List.of("pendiente");
            case "fallida" -> List.of("fallida", "pendiente");
            default -> List.of(actual.getEstado());
        };
    }

    /** Cuántos clientes reciben la promoción masiva, por canal preferido (ej. WhatsApp → 44). */
    public Map<String, Long> destinatariosPromocionPorCanal() {
        try {
            Map<String, Long> m = new java.util.LinkedHashMap<>();
            for (Map<String, Object> f : vistaDAO.consultar(
                    "SELECT canal_preferido, COUNT(*) AS n FROM Perfil_cliente WHERE acepta_promociones = 'sí' "
                    + "GROUP BY canal_preferido ORDER BY canal_preferido")) {
                m.put((String) f.get("canal_preferido"), ((Number) f.get("n")).longValue());
            }
            return m;
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    /** Clientes que aceptan promociones, con su canal preferido (para el formulario). */
    public Map<Integer, String> opcionesClientePromocion() {
        try {
            Map<Integer, String> m = new java.util.LinkedHashMap<>();
            for (Map<String, Object> f : vistaDAO.consultar(
                    "SELECT c.id_cliente, CONCAT(c.primer_nombre,' ',c.primer_apellido,' · ',c.numero_documento,"
                    + "' · prefiere ', p.canal_preferido) AS etiqueta FROM Cliente c "
                    + "INNER JOIN Perfil_cliente p ON p.id_cliente = c.id_cliente "
                    + "WHERE p.acepta_promociones = 'sí' ORDER BY c.primer_nombre, c.primer_apellido")) {
                m.put(((Number) f.get("id_cliente")).intValue(), (String) f.get("etiqueta"));
            }
            return m;
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    /** Reservas con el nombre de su cliente y el canal que prefiere (para el formulario). */
    public Map<Integer, String> opcionesReserva() {
        try {
            Map<Integer, String> m = new java.util.LinkedHashMap<>();
            for (Map<String, Object> f : vistaDAO.consultar(
                    "SELECT r.id_reserva, CONCAT('#', r.id_reserva, ' · ', c.primer_nombre, ' ', c.primer_apellido, ' · ', "
                    + "DATE_FORMAT(r.fecha_servicio, '%d/%m/%Y'), ' · ', r.estado, ' · prefiere ', "
                    + "COALESCE(p.canal_preferido, 'correo')) AS etiqueta FROM Reserva r "
                    + "INNER JOIN Cliente c ON c.id_cliente = r.id_cliente "
                    + "LEFT JOIN Perfil_cliente p ON p.id_cliente = r.id_cliente ORDER BY r.id_reserva DESC")) {
                m.put(((Number) f.get("id_reserva")).intValue(), (String) f.get("etiqueta"));
            }
            return m;
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    public List<Notificacion> listarTodos() {
        enviarVencidas();
        try {
            return dao.listarTodos();
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    public Notificacion buscarPorId(int id) {
        try {
            Notificacion x = dao.buscarPorId(id);
            if (x == null) {
                throw new ReglaNegocioException("No existe la notificación " + id + ".");
            }
            return x;
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    public List<Map<String, Object>> listarPorCliente(int idCliente) {
        enviarVencidas();
        try {
            return vistaDAO.notificacionesDeCliente(idCliente);
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    public void guardar(Notificacion x) {
        try {
            Notificacion actual = x.getIdNotificacion() == null ? null : dao.buscarPorId(x.getIdNotificacion());
            if (x.getIdNotificacion() != null && actual == null) {
                throw new ReglaNegocioException("No existe la notificación " + x.getIdNotificacion() + ".");
            }
            validar(x, actual);
            if (x.getIdNotificacion() == null) {
                dao.crear(x);
            } else {
                dao.actualizar(x);
            }
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    public void eliminar(int id) {
        throw new ReglaNegocioException("Las notificaciones son historial: no se eliminan.");
    }

    /**
     * Envío simulado automático: pasa a 'enviada' (con la fecha real de envío)
     * toda notificación 'pendiente' cuya fecha programada ya llegó.
     * Se llama cada minuto y también antes de listar notificaciones, para que la
     * pantalla siempre muestre el estado al día. Devuelve cuántas se enviaron.
     */
    public int enviarVencidas() {
        try {
            return dao.enviarVencidas(ahora());
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    /** Tarea programada: cada minuto «envía» las notificaciones que ya llegaron a su hora. */
    @Scheduled(fixedDelay = 60_000, initialDelay = 10_000)
    public void enviarProgramadasAutomaticamente() {
        try {
            enviarVencidas();
        } catch (RuntimeException e) {
            // Si la base no está disponible en este minuto, se reintenta en el siguiente.
            System.err.println("Envío automático de notificaciones: " + e.getMessage());
        }
    }

    /** Envío manual (botón «Enviar» del admin): pasa a 'enviada' con la fecha real de hoy. */
    public void marcarEnviada(int id) {
        Notificacion n = buscarPorId(id);
        if (!"pendiente".equals(n.getEstado()) && !"fallida".equals(n.getEstado())) {
            throw new ReglaNegocioException("Solo se envían notificaciones pendientes o fallidas.");
        }
        LocalDateTime ahora = Reloj.ahora().withSecond(0);
        if (ahora.isBefore(n.getFechaProgramada())) {
            throw new ReglaNegocioException("Todavía no llega la fecha programada ("
                    + n.getFechaProgramada().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + ").");
        }
        if ("promoción".equals(n.getTipo())) {
            try {
                PerfilCliente p = perfilDAO.buscarPorCliente(n.getIdCliente());
                if (p == null || !"sí".equals(p.getAceptaPromociones())) {
                    throw new ReglaNegocioException("Ese cliente ya no acepta promociones: no se le envía.");
                }
            } catch (SQLException e) {
                throw new ReglaNegocioException(MensajesError.traducir(e));
            }
        }
        n.setEstado("enviada");
        n.setFechaRealEnvio(ahora);
        try {
            dao.actualizar(n);   // el envío no pasa por validar(): 'enviada' no se escoge en el formulario
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    /**
     * El cliente marca como leída una notificación de su historial (RF05.1).
     * Solo las suyas: de una reserva suya o una promoción dirigida a él.
     * Se puede desde 'pendiente' (el envío es simulado: si ya llegó la fecha
     * programada, se da por entregada en este momento) o desde 'enviada'.
     * No se puede si todavía no llega la fecha programada, si el envío falló
     * o si ya estaba leída.
     */
    public void marcarLeida(int id, int idCliente) {
        boolean esSuya = listarPorCliente(idCliente).stream()
                .anyMatch(f -> ((Number) f.get("id_notificacion")).intValue() == id);
        if (!esSuya) {
            throw new ReglaNegocioException("Esa notificación no es suya.");
        }
        Notificacion n = buscarPorId(id);
        if ("leída".equals(n.getEstado())) {
            throw new ReglaNegocioException("Esa notificación ya estaba marcada como leída.");
        }
        if ("fallida".equals(n.getEstado())) {
            throw new ReglaNegocioException("El envío de esa notificación falló: no se puede marcar como leída.");
        }
        LocalDateTime ahora = Reloj.ahora().withSecond(0);
        if ("pendiente".equals(n.getEstado())) {
            if (ahora.isBefore(n.getFechaProgramada())) {
                throw new ReglaNegocioException("Esa notificación todavía no se ha enviado (está programada para el "
                        + n.getFechaProgramada().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + ").");
            }
            n.setFechaRealEnvio(ahora);
        }
        n.setEstado("leída");
        try {
            // Sin pasar por validar(): una promoción ya enviada se puede leer aunque
            // después el cliente haya dejado de aceptar promociones.
            dao.actualizar(n);
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    /**
     * Envía una promoción a TODOS los clientes que aceptaron promociones,
     * por el canal de su perfil. Devuelve cuántas se programaron.
     */
    public int enviarPromocion(String mensaje, LocalDateTime fechaProgramada) {
        if (mensaje == null || mensaje.isBlank()) {
            throw new ReglaNegocioException("Escriba el mensaje de la promoción.");
        }
        try (Connection con = conexionBD.getConexion()) {
            List<Map<String, Object>> destinos = vistaDAO.consultar(con,
                    "SELECT id_cliente, canal_preferido FROM Perfil_cliente WHERE acepta_promociones = 'sí'");
            con.setAutoCommit(false);
            try {
                for (Map<String, Object> d : destinos) {
                    Notificacion n = new Notificacion();
                    n.setTipo("promoción");
                    n.setCanal((String) d.get("canal_preferido"));
                    n.setMensaje(mensaje.trim());
                    n.setFechaProgramada(fechaProgramada);
                    n.setEstado("pendiente");
                    n.setIdCliente(((Number) d.get("id_cliente")).intValue());
                    dao.crear(con, n);
                }
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
            return destinos.size();
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    // -----------------------------------------------------------------
    // Notificaciones automáticas (las llaman Venta, Pago y Reserva dentro
    // de su misma transacción, por eso reciben la conexión).
    // -----------------------------------------------------------------

    public void confirmacionReserva(Connection con, Reserva r) throws SQLException {
        boolean ingles = esIngles(r.getIdCliente());
        crearDeReserva(con, r, "confirmación de reserva", ahora(),
                ingles ? "Hi! Your booking #" + r.getIdReserva() + " for " + r.getFechaServicio().format(DMY) + " was registered in QuindíoStay."
                       : "Hola, tu reserva #" + r.getIdReserva() + " para el " + r.getFechaServicio().format(DMY) + " quedó registrada en QuindíoStay.");
    }

    public void confirmacionPago(Connection con, Reserva r, java.math.BigDecimal monto) throws SQLException {
        boolean ingles = esIngles(r.getIdCliente());
        String valor = String.format("%,.0f", monto).replace(',', '.');
        crearDeReserva(con, r, "confirmación de pago", ahora(),
                ingles ? "We received your payment of $" + valor + " for booking #" + r.getIdReserva() + ". Thank you for choosing QuindíoStay!"
                       : "Recibimos tu pago de $" + valor + " de la reserva #" + r.getIdReserva() + ". ¡Gracias por elegir QuindíoStay!");
    }

    public void cambioEstado(Connection con, Reserva r, String nuevoEstado) throws SQLException {
        cambioEstado(con, r, nuevoEstado, null);
    }

    public void cambioEstado(Connection con, Reserva r, String nuevoEstado, java.math.BigDecimal reembolso) throws SQLException {
        cambioEstado(con, r, nuevoEstado, reembolso, null);
    }

    /**
     * Igual que cambioEstado, pero con un dato adicional en el mensaje de cancelación:
     * si hubo reembolso dice cuánto; si había pagos pero no hubo reembolso (5 días o menos)
     * lo explica; si no había pagos no dice nada de reembolsos.
     */
    public void cambioEstado(Connection con, Reserva r, String nuevoEstado, java.math.BigDecimal reembolso,
                             java.math.BigDecimal pagado) throws SQLException {
        boolean ingles = esIngles(r.getIdCliente());
        String extra = "";
        if (reembolso != null && reembolso.signum() > 0) {
            String valor = String.format("%,.0f", reembolso).replace(',', '.');
            extra = ingles ? " We refunded $" + valor + "."
                    : " Te reembolsamos $" + valor + ".";
        } else if (pagado != null && pagado.signum() > 0) {
            extra = ingles ? " No refund: it was cancelled 5 days or less before the service."
                    : " Sin reembolso: se canceló con 5 días o menos de anticipación.";
        }
        crearDeReserva(con, r, "cambio de estado", ahora(),
                (ingles ? "Your booking #" + r.getIdReserva() + " changed to status: " + nuevoEstado + "."
                        : "Tu reserva #" + r.getIdReserva() + " cambió a estado: " + nuevoEstado + ".") + extra);
    }

    /**
     * Al confirmar la reserva se genera el recordatorio según el perfil:
     * se programa 'anticipacion_recordatorio' días antes del servicio, a las 8:00,
     * por el canal preferido. Si el cliente no tiene perfil: 1 día antes, por correo.
     */
    public void recordatorio(Connection con, Reserva r) throws SQLException {
        PerfilCliente p = perfilDAO.buscarPorCliente(r.getIdCliente());
        int dias = p != null ? p.getAnticipacionRecordatorio() : 1;
        LocalDateTime fecha = r.getFechaServicio().minusDays(dias).atTime(LocalTime.of(8, 0));
        if (fecha.isBefore(ahora())) {
            fecha = ahora();   // si ya pasó ese momento, sale de una vez
        }
        boolean ingles = p != null && "inglés".equals(p.getIdioma());
        crearDeReserva(con, r, "recordatorio de reserva", fecha,
                ingles ? "Reminder: your visit for booking #" + r.getIdReserva() + " is on " + r.getFechaServicio().format(DMY) + ". Bring your QS tickets."
                       : "Recordatorio: tu visita de la reserva #" + r.getIdReserva() + " es el " + r.getFechaServicio().format(DMY) + ". Lleva tus boletas QS.");
    }

    /** Se anuló UNA boleta suelta (no toda la reserva), por ejemplo desde Boletas → Eliminar. */
    public void boletaAnulada(Connection con, Reserva r, String codigoBoleta) throws SQLException {
        boolean ingles = esIngles(r.getIdCliente());
        crearDeReserva(con, r, "cambio de estado", ahora(),
                ingles ? "Your ticket " + codigoBoleta + " from booking #" + r.getIdReserva() + " was cancelled."
                        : "Tu boleta " + codigoBoleta + " de la reserva #" + r.getIdReserva() + " fue anulada.");
    }

    /**
     * La reserva estaba confirmada pero ya no está pagada por completo (se reembolsó un pago,
     * se agregó una boleta o quedó confirmada a mano sin pago): vuelve a 'pendiente'.
     */
    public void volvioAPendiente(Connection con, Reserva r, java.math.BigDecimal saldo) throws SQLException {
        boolean ingles = esIngles(r.getIdCliente());
        String valor = String.format("%,.0f", saldo).replace(',', '.');
        crearDeReserva(con, r, "cambio de estado", ahora(),
                ingles ? "Your booking #" + r.getIdReserva() + " changed to status: pendiente. It is not fully paid yet: $" + valor + " still due."
                       : "Tu reserva #" + r.getIdReserva() + " cambió a estado: pendiente. Aún no está pagada por completo: falta pagar $" + valor + ".");
    }

    private void crearDeReserva(Connection con, Reserva r, String tipo, LocalDateTime fecha, String mensaje) throws SQLException {
        PerfilCliente p = perfilDAO.buscarPorCliente(r.getIdCliente());
        Notificacion n = new Notificacion();
        n.setTipo(tipo);
        n.setCanal(p != null ? p.getCanalPreferido() : "correo");
        n.setMensaje(mensaje);
        n.setFechaProgramada(fecha);
        n.setEstado("pendiente");
        n.setIdReserva(r.getIdReserva());   // ligada a la reserva: id_cliente queda NULL (3FN)
        n.setIdCliente(null);
        dao.crear(con, n);
    }

    private boolean esIngles(int idCliente) throws SQLException {
        PerfilCliente p = perfilDAO.buscarPorCliente(idCliente);
        return p != null && "inglés".equals(p.getIdioma());
    }

    private LocalDateTime ahora() {
        return Reloj.ahora().withSecond(0);
    }

    /**
     * Reglas que miran otra tabla (no caben en un CHECK):
     *  1. Solo se editan las pendientes o fallidas; enviada y leída son historial.
     *  2. Una nueva nace 'pendiente'; la fecha real de envío la pone la aplicación al enviar.
     *  3. Promoción: va a UN cliente (sin reserva) que aceptó promociones.
     *     Los demás tipos: van a UNA reserva (sin cliente; el cliente sale de la reserva).
     *     Si se llenan los dos, o el que no corresponde, se rechaza (antes se borraba en silencio).
     *  4. El canal es el que el cliente escogió en su perfil (correo si no tiene perfil).
     *  5. A una reserva cancelada solo se le manda «cambio de estado».
     * Las reglas 4 y 5 se revisan al crear o cuando cambia el tipo, el canal o el destino,
     * para no bloquear una notificación vieja si el cliente cambió después su preferencia.
     */
    private void validar(Notificacion x, Notificacion actual) throws SQLException {
        if (actual != null && !esEditable(actual)) {
            throw new ReglaNegocioException("La notificación #" + actual.getIdNotificacion() + " ya está «" + actual.getEstado()
                    + "»: es historial y no se puede modificar. Solo se editan las pendientes o fallidas.");
        }
        if (x.getEstado() == null || !estadosPosibles(actual).contains(x.getEstado())) {
            if (actual == null) {
                throw new ReglaNegocioException("Una notificación nueva se crea como «pendiente». La aplicación la pasa a «enviada» "
                        + "cuando llega su fecha programada (no escriba la fecha real de envío) y el cliente la marca como «leída».");
            }
            throw new ReglaNegocioException("Una notificación «" + actual.getEstado() + "» solo puede quedar en: "
                    + String.join(", ", estadosPosibles(actual)) + ". El envío lo hace la aplicación (o el botón «Enviar»).");
        }
        if (x.getFechaProgramada() == null) {
            throw new ReglaNegocioException("Escriba la fecha programada.");
        }
        // La fecha real de envío no se escribe a mano: una pendiente o fallida no tiene.
        if (x.getFechaRealEnvio() != null) {
            throw new ReglaNegocioException("La fecha real de envío la pone la aplicación cuando la envía: déjela vacía.");
        }

        boolean promo = "promoción".equals(x.getTipo());
        if (promo) {
            if (x.getIdReserva() != null) {
                throw new ReglaNegocioException("Una promoción va dirigida a un cliente, no a una reserva: deje «Reserva» en (ninguno). "
                        + "Para escribirle sobre una reserva use otro tipo (por ejemplo «cambio de estado»).");
            }
            if (x.getIdCliente() == null) {
                throw new ReglaNegocioException("Una promoción debe ir dirigida a un cliente.");
            }
        } else {
            if (x.getIdCliente() != null) {
                throw new ReglaNegocioException("Una notificación de tipo «" + x.getTipo() + "» se liga a una reserva y el cliente "
                        + "es el de esa reserva: deje «Cliente» en (ninguno). El campo Cliente es solo para promociones.");
            }
            if (x.getIdReserva() == null) {
                throw new ReglaNegocioException("Este tipo de notificación debe estar ligada a una reserva.");
            }
        }

        int idCliente;
        Reserva r = null;
        if (promo) {
            idCliente = x.getIdCliente();
            PerfilCliente p = perfilDAO.buscarPorCliente(idCliente);
            if (p == null || !"sí".equals(p.getAceptaPromociones())) {
                throw new ReglaNegocioException("Ese cliente no aceptó recibir promociones (acepta_promociones = 'no' o sin perfil).");
            }
        } else {
            r = reservaDAO.buscarPorId(x.getIdReserva());
            if (r == null) {
                throw new ReglaNegocioException("No existe la reserva #" + x.getIdReserva() + ".");
            }
            idCliente = r.getIdCliente();
        }

        boolean cambioDestino = actual == null
                || !java.util.Objects.equals(actual.getTipo(), x.getTipo())
                || !java.util.Objects.equals(actual.getCanal(), x.getCanal())
                || !java.util.Objects.equals(actual.getIdCliente(), x.getIdCliente())
                || !java.util.Objects.equals(actual.getIdReserva(), x.getIdReserva());
        if (cambioDestino) {
            PerfilCliente p = perfilDAO.buscarPorCliente(idCliente);
            String canal = p != null ? p.getCanalPreferido() : "correo";
            if (!canal.equals(x.getCanal())) {
                throw new ReglaNegocioException("Ese cliente " + (p != null ? "prefiere recibir sus notificaciones por " + canal
                        : "no tiene perfil, así que se le escribe por correo") + ": escoja el canal «" + canal + "».");
            }
            if (r != null && "cancelada".equals(r.getEstado()) && !"cambio de estado".equals(x.getTipo())) {
                throw new ReglaNegocioException("La reserva #" + r.getIdReserva() + " está cancelada: solo se le puede enviar "
                        + "una notificación de «cambio de estado» (no recordatorios ni confirmaciones).");
            }
        }
    }
}
