package co.edu.uniquindio.quindiostay.servicio;

import co.edu.uniquindio.quindiostay.dao.*;
import co.edu.uniquindio.quindiostay.modelo.Notificacion;
import co.edu.uniquindio.quindiostay.modelo.PerfilCliente;
import co.edu.uniquindio.quindiostay.modelo.Reserva;
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
    private final ConexionBD conexionBD;

    public NotificacionServicio(NotificacionDAO dao, PerfilClienteDAO perfilDAO, VistaDAO vistaDAO, ConexionBD conexionBD) {
        this.dao = dao;
        this.perfilDAO = perfilDAO;
        this.vistaDAO = vistaDAO;
        this.conexionBD = conexionBD;
    }

    public List<Notificacion> listarTodos() {
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
        try {
            return vistaDAO.notificacionesDeCliente(idCliente);
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    public void guardar(Notificacion x) {
        try {
            validar(x);
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

    /** Simula el envío: pasa a 'enviada' con la fecha real de hoy. */
    public void marcarEnviada(int id) {
        Notificacion n = buscarPorId(id);
        if (!"pendiente".equals(n.getEstado()) && !"fallida".equals(n.getEstado())) {
            throw new ReglaNegocioException("Solo se envían notificaciones pendientes o fallidas.");
        }
        LocalDateTime ahora = LocalDateTime.now().withSecond(0).withNano(0);
        if (ahora.isBefore(n.getFechaProgramada())) {
            throw new ReglaNegocioException("Todavía no llega la fecha programada ("
                    + n.getFechaProgramada().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + ").");
        }
        n.setEstado("enviada");
        n.setFechaRealEnvio(ahora);
        guardar(n);
    }

    /** El cliente abre una notificación de su historial: pasa a 'leída'. */
    public void marcarLeida(int id, int idCliente) {
        boolean esSuya = listarPorCliente(idCliente).stream()
                .anyMatch(f -> ((Number) f.get("id_notificacion")).intValue() == id);
        if (!esSuya) {
            throw new ReglaNegocioException("Esa notificación no es suya.");
        }
        Notificacion n = buscarPorId(id);
        if ("enviada".equals(n.getEstado())) {
            n.setEstado("leída");
            guardar(n);
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

    /** Igual que cambioEstado, pero con un dato adicional en el mensaje (ej. el valor reembolsado). */
    public void cambioEstado(Connection con, Reserva r, String nuevoEstado, java.math.BigDecimal reembolso) throws SQLException {
        boolean ingles = esIngles(r.getIdCliente());
        String extra = "";
        if (reembolso != null && reembolso.signum() > 0) {
            String valor = String.format("%,.0f", reembolso).replace(',', '.');
            extra = ingles ? " We refunded $" + valor + "."
                    : " Te reembolsamos $" + valor + ".";
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
        return LocalDateTime.now().withSecond(0).withNano(0);
    }

    /** Reglas que miran otra tabla (no caben en un CHECK). */
    private void validar(Notificacion x) throws SQLException {
        if ("promoción".equals(x.getTipo())) {
            x.setIdReserva(null);
            if (x.getIdCliente() == null) {
                throw new ReglaNegocioException("Una promoción debe ir dirigida a un cliente.");
            }
            PerfilCliente p = perfilDAO.buscarPorCliente(x.getIdCliente());
            if (p == null || !"sí".equals(p.getAceptaPromociones())) {
                throw new ReglaNegocioException("Ese cliente no aceptó recibir promociones (acepta_promociones = 'no' o sin perfil).");
            }
        } else {
            x.setIdCliente(null);   // el cliente se conoce por la reserva (3FN)
            if (x.getIdReserva() == null) {
                throw new ReglaNegocioException("Este tipo de notificación debe estar ligada a una reserva.");
            }
        }
        if ("pendiente".equals(x.getEstado())) {
            x.setFechaRealEnvio(null);
        } else if (x.getFechaRealEnvio() == null) {
            throw new ReglaNegocioException("Si la notificación ya no está pendiente, escriba la fecha real de envío.");
        }
    }
}
