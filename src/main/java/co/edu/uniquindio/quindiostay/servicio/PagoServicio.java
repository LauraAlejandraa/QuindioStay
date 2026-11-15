package co.edu.uniquindio.quindiostay.servicio;

import co.edu.uniquindio.quindiostay.dao.*;
import co.edu.uniquindio.quindiostay.modelo.Pago;
import co.edu.uniquindio.quindiostay.modelo.Reserva;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Pagos (RF04): una reserva se paga de una vez o en varios abonos.
 * La pasarela en línea es SIMULADA: para tarjeta y PSE se inventa una referencia única.
 * Una reserva está pagada cuando la suma de sus pagos EXITOSOS cubre el valor total
 * (vista_estado_pago_reserva); en ese momento pasa a 'confirmada'.
 * Un pago no se borra: "eliminar" es reembolsarlo (Tabla 39).
 */
@Service
public class PagoServicio {

    /** Métodos permitidos (ck_pago_metodo). Tarjetas y PSE exigen referencia (ck_pago_referencia). */
    private static final List<String> METODOS = List.of("tarjeta de crédito", "tarjeta débito", "PSE", "transferencia", "efectivo");

    private final PagoDAO dao;
    private final ReservaDAO reservaDAO;
    private final VistaDAO vistaDAO;
    private final ConexionBD conexionBD;
    private final NotificacionServicio notificaciones;
    private final EstadoPagoReserva estadoPago;

    public PagoServicio(PagoDAO dao, ReservaDAO reservaDAO, VistaDAO vistaDAO, ConexionBD conexionBD,
                        NotificacionServicio notificaciones, EstadoPagoReserva estadoPago) {
        this.estadoPago = estadoPago;
        this.dao = dao;
        this.reservaDAO = reservaDAO;
        this.vistaDAO = vistaDAO;
        this.conexionBD = conexionBD;
        this.notificaciones = notificaciones;
    }

    public List<Pago> listarTodos() {
        try {
            return dao.listarTodos();
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    public List<Pago> listarPorReserva(int idReserva) {
        try {
            return dao.listarPorReserva(idReserva);
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    public Pago buscarPorId(int id) {
        try {
            Pago x = dao.buscarPorId(id);
            if (x == null) {
                throw new ReglaNegocioException("No existe el pago " + id + ".");
            }
            return x;
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    /**
     * Guardar desde el CRUD. Si es nuevo, se registra con las reglas de pago.
     * Si ya existe, solo puede cambiar de estado: pendiente -> exitoso/fallido,
     * exitoso -> reembolsado. Monto, método, fecha y referencia son registro contable.
     */
    public void guardar(Pago x) {
        if (x.getIdPago() == null) {
            registrar(x.getIdReserva(), x.getMonto(), x.getMetodo(), x.getEstado(), x.getReferenciaTransaccion());
            return;
        }
        Pago actual = buscarPorId(x.getIdPago());
        if (actual.getEstado().equals(x.getEstado())) {
            return;
        }
        boolean permitido = ("pendiente".equals(actual.getEstado()) && ("exitoso".equals(x.getEstado()) || "fallido".equals(x.getEstado())))
                || ("exitoso".equals(actual.getEstado()) && "reembolsado".equals(x.getEstado()));
        if (!permitido) {
            throw new ReglaNegocioException("Un pago no puede pasar de '" + actual.getEstado() + "' a '" + x.getEstado()
                    + "'. Solo se cambia el estado; los demás datos son registro contable.");
        }
        try (Connection con = conexionBD.getConexion()) {
            con.setAutoCommit(false);
            try {
                if ("exitoso".equals(x.getEstado())) {
                    Reserva r = reservaDAO.buscarPorId(con, actual.getIdReserva());   // FOR UPDATE: un pago a la vez
                    if (r == null || "cancelada".equals(r.getEstado())) {
                        throw new ReglaNegocioException("La reserva está cancelada: este pago no se puede marcar como exitoso.");
                    }
                    BigDecimal saldo = (BigDecimal) vistaDAO.estadoPago(con, actual.getIdReserva()).get("saldo");
                    if (actual.getMonto().compareTo(saldo) > 0) {
                        throw new ReglaNegocioException("El monto de este pago ($" + String.format("%,.0f", actual.getMonto()).replace(',', '.')
                                + ") supera el saldo pendiente de la reserva ($" + String.format("%,.0f", saldo).replace(',', '.') + "). No se puede marcar como exitoso.");
                    }
                }
                dao.cambiarEstado(con, actual.getIdPago(), x.getEstado());
                if ("exitoso".equals(x.getEstado())) {
                    despuesDePagoExitoso(con, actual.getIdReserva(), actual.getMonto());
                } else if ("reembolsado".equals(x.getEstado())) {
                    // Si era una reserva confirmada, deja de estar pagada: vuelve a pendiente.
                    estadoPago.sincronizar(con, actual.getIdReserva());
                }
                con.commit();
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    /** Eliminar = reembolsar (solo un pago exitoso). */
    public void eliminar(int id) {
        Pago p = buscarPorId(id);
        if (!"exitoso".equals(p.getEstado())) {
            throw new ReglaNegocioException("Solo se reembolsa un pago exitoso (este está " + p.getEstado() + ").");
        }
        p.setEstado("reembolsado");
        guardar(p);
    }

    /**
     * Registra un pago o abono. 'estado' puede ser exitoso, fallido o pendiente
     * (la pasarela simulada permite probar un pago rechazado).
     * Devuelve el id del pago.
     */
    public int registrar(Integer idReserva, BigDecimal monto, String metodo, String estado, String referencia) {
        if (idReserva == null) {
            throw new ReglaNegocioException("Escoja la reserva que se paga.");
        }
        if (monto == null || monto.signum() <= 0) {
            throw new ReglaNegocioException("El monto debe ser mayor que 0.");
        }
        if (estado == null || "reembolsado".equals(estado)) {
            throw new ReglaNegocioException("Un pago nuevo queda exitoso, pendiente o fallido; el reembolso solo sale de una cancelación.");
        }
        if (metodo == null || !METODOS.contains(metodo)) {
            throw new ReglaNegocioException("Escoja un método de pago válido: tarjeta de crédito, tarjeta débito, PSE, transferencia o efectivo.");
        }
        try (Connection con = conexionBD.getConexion()) {
            con.setAutoCommit(false);
            try {
                Reserva r = reservaDAO.buscarPorId(con, idReserva);   // FOR UPDATE: un abono a la vez
                if (r == null) {
                    throw new ReglaNegocioException("La reserva no existe.");
                }
                if ("cancelada".equals(r.getEstado())) {
                    throw new ReglaNegocioException("La reserva está cancelada: no recibe pagos.");
                }
                if (!"pendiente".equals(r.getEstado()) && !"confirmada".equals(r.getEstado())) {
                    throw new ReglaNegocioException("La reserva está " + r.getEstado() + ": no recibe pagos.");
                }
                if (r.getFechaServicio() != null && r.getFechaServicio().isBefore(Reloj.hoy())) {
                    throw new ReglaNegocioException("La fecha de visita de la reserva ("
                            + r.getFechaServicio().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            + ") ya pasó: no recibe pagos nuevos.");
                }
                if (todasLasFuncionesTerminaron(con, idReserva)) {
                    throw new ReglaNegocioException("Las funciones de esta reserva ya terminaron (hoy, hora de cierre "
                            + "cumplida): no recibe pagos nuevos.");
                }
                BigDecimal saldo = (BigDecimal) vistaDAO.estadoPago(con, idReserva).get("saldo");
                // Aplica también a pagos pendientes o fallidos: ningún pago puede ser mayor que lo que se debe.
                if (monto.compareTo(saldo) > 0) {
                    throw new ReglaNegocioException("El monto ($" + String.format("%,.0f", monto).replace(',', '.')
                            + ") supera el saldo pendiente de la reserva ($"
                            + String.format("%,.0f", saldo).replace(',', '.') + ").");
                }
                Pago p = new Pago();
                p.setFecha(Reloj.hoy());
                p.setMonto(monto);
                p.setMetodo(metodo);
                p.setEstado(estado);
                p.setReferenciaTransaccion(referencia == null || referencia.isBlank()
                        ? generarReferencia(metodo) : referencia.trim());
                p.setIdReserva(idReserva);
                int id = dao.crear(con, p);
                if ("exitoso".equals(estado)) {
                    despuesDePagoExitoso(con, idReserva, monto);
                }
                con.commit();
                return id;
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    /**
     * true si la reserva tiene boletas vigentes y TODAS son de funciones que ya
     * terminaron (fecha pasada, o de hoy con la hora de cierre ya cumplida).
     */
    private boolean todasLasFuncionesTerminaron(Connection con, int idReserva) throws SQLException {
        List<Map<String, Object>> filas = vistaDAO.consultar(con,
                "SELECT f.fecha, f.hora_cierre FROM Boleta b "
              + "INNER JOIN Zona z ON z.id_zona = b.id_zona INNER JOIN Funcion f ON f.id_funcion = z.id_funcion "
              + "WHERE b.id_reserva = ? AND b.estado <> 'anulada'", idReserva);
        if (filas.isEmpty()) {
            return false;
        }
        for (Map<String, Object> f : filas) {
            LocalDate fecha = ((java.sql.Date) f.get("fecha")).toLocalDate();
            java.time.LocalTime cierre = ((java.sql.Time) f.get("hora_cierre")).toLocalTime();
            if (!Reloj.yaTermino(fecha, cierre)) {
                return false;
            }
        }
        return true;
    }

    /** Notifica el pago y, si ya quedó pagada, confirma la reserva y programa el recordatorio. */
    private void despuesDePagoExitoso(Connection con, int idReserva, BigDecimal monto) throws SQLException {
        Reserva r = reservaDAO.buscarPorId(con, idReserva);
        notificaciones.confirmacionPago(con, r, monto);
        estadoPago.sincronizar(con, idReserva);   // pagada por completo -> confirmada
    }

    /** Pasarela simulada: referencia única para tarjeta y PSE (ej. PSE-20261107-483920). */
    private String generarReferencia(String metodo) {
        String prefijo;
        switch (metodo) {
            case "tarjeta de crédito": prefijo = "TC"; break;
            case "tarjeta débito": prefijo = "TD"; break;
            case "PSE": prefijo = "PSE"; break;
            case "transferencia": prefijo = "TRF"; break;
            default: return null;   // efectivo: sin referencia
        }
        return prefijo + "-" + Reloj.hoy().format(DateTimeFormatter.BASIC_ISO_DATE) + "-"
                + ThreadLocalRandom.current().nextInt(100000, 1000000);
    }
}
