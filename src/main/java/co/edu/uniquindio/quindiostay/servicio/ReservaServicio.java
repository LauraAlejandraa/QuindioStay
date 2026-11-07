package co.edu.uniquindio.quindiostay.servicio;

import co.edu.uniquindio.quindiostay.dao.*;
import co.edu.uniquindio.quindiostay.modelo.Reserva;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * Reservas (núcleo común). Las reservas se CREAN desde la venta de boletas
 * (VentaServicio). Aquí van los cambios de estado y la cancelación (RF15):
 *   pendiente  -> confirmada | cancelada
 *   confirmada -> completada | cancelada
 *   completada y cancelada son finales.
 * Una reserva solo pasa a completada si estaba confirmada.
 * "Eliminar" una reserva es cancelarla: queda como historial (Tabla 39).
 */
@Service
public class ReservaServicio {

    /** Porcentaje que se devuelve al cancelar con más de 5 días de anticipación. */
    public static final BigDecimal PORCENTAJE_REEMBOLSO = new BigDecimal("0.80");
    public static final int DIAS_MINIMOS_REEMBOLSO = 5;

    private final ReservaDAO dao;
    private final BoletaDAO boletaDAO;
    private final PagoDAO pagoDAO;
    private final VistaDAO vistaDAO;
    private final ConexionBD conexionBD;
    private final NotificacionServicio notificaciones;

    public ReservaServicio(ReservaDAO dao, BoletaDAO boletaDAO, PagoDAO pagoDAO, VistaDAO vistaDAO,
                           ConexionBD conexionBD, NotificacionServicio notificaciones) {
        this.dao = dao;
        this.boletaDAO = boletaDAO;
        this.pagoDAO = pagoDAO;
        this.vistaDAO = vistaDAO;
        this.conexionBD = conexionBD;
        this.notificaciones = notificaciones;
    }

    public List<Reserva> listarTodos() {
        try {
            return dao.listarTodos();
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    public List<Reserva> listarPorPrestador(int idPrestador) {
        try {
            return dao.listarPorPrestador(idPrestador);
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    public Reserva buscarPorId(int id) {
        try {
            Reserva x = dao.buscarPorId(id);
            if (x == null) {
                throw new ReglaNegocioException("No existe la reserva #" + id + ".");
            }
            return x;
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    public Map<Integer, String> listarOpciones() {
        try {
            return dao.listarOpciones();
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    /** Datos completos para la pantalla de detalle y las plantillas (vistas de la Parte A). */
    public Map<String, Object> detalle(int id) {
        try {
            Map<String, Object> d = vistaDAO.reservaDetalle(id);
            if (d == null) {
                throw new ReglaNegocioException("No existe la reserva #" + id + ".");
            }
            d.putAll(vistaDAO.estadoPago(id));
            d.put("boletas", vistaDAO.boletasDeReserva(id));
            d.put("pagos", vistaDAO.pagosDeReserva(id));
            d.put("notificaciones", vistaDAO.notificacionesDeReserva(id));
            d.put("reembolsoSiCancela", reembolsoSiCancela(id));
            return d;
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    /**
     * Guardar desde el formulario del CRUD: solo se pueden cambiar el canal y el estado.
     * La fecha, el cliente, el plan y el valor quedan congelados desde la venta.
     */
    public void guardar(Reserva x) {
        if (x.getIdReserva() == null) {
            throw new ReglaNegocioException("Las reservas nuevas se crean desde «Vender boletas», porque llevan sus boletas.");
        }
        Reserva actual = buscarPorId(x.getIdReserva());
        try {
            if (!actual.getCanal().equals(x.getCanal())) {
                actual.setCanal(x.getCanal());
                dao.actualizar(actual);
            }
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
        if (!actual.getEstado().equals(x.getEstado())) {
            cambiarEstado(x.getIdReserva(), x.getEstado());
        }
    }

    /** Eliminar = cancelar (Tabla 39). */
    public void eliminar(int id) {
        cancelar(id);
    }

    public void cambiarEstado(int id, String nuevo) {
        Reserva r = buscarPorId(id);
        String actual = r.getEstado();
        if ("cancelada".equals(nuevo)) {
            cancelar(id);
            return;
        }
        boolean permitido = ("pendiente".equals(actual) && "confirmada".equals(nuevo))
                || ("confirmada".equals(actual) && "completada".equals(nuevo));
        if (!permitido) {
            if ("completada".equals(nuevo)) {
                throw new ReglaNegocioException("Una reserva solo pasa a completada si estaba confirmada (está " + actual + ").");
            }
            throw new ReglaNegocioException("No se puede pasar una reserva de '" + actual + "' a '" + nuevo + "'.");
        }
        try (Connection con = conexionBD.getConexion()) {
            con.setAutoCommit(false);
            try {
                dao.cambiarEstado(con, id, nuevo);
                r.setEstado(nuevo);
                notificaciones.cambioEstado(con, r, nuevo);
                if ("confirmada".equals(nuevo)) {
                    notificaciones.recordatorio(con, r);   // al confirmar se programa el recordatorio
                }
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    /**
     * Cancelación (RF15): la reserva pasa a 'cancelada', sus boletas a 'anulada'
     * (se libera el aforo) y, si faltan MÁS de 5 días para el servicio, se reembolsa
     * el 80 % de lo pagado y los pagos exitosos quedan 'reembolsado'.
     * Devuelve el valor reembolsado (0 si no hay reembolso).
     */
    public BigDecimal cancelar(int id) {
        Reserva r = buscarPorId(id);
        if ("cancelada".equals(r.getEstado()) || "completada".equals(r.getEstado())) {
            throw new ReglaNegocioException("La reserva ya está " + r.getEstado() + ": no se puede cancelar.");
        }
        try (Connection con = conexionBD.getConexion()) {
            con.setAutoCommit(false);
            try {
                if (boletaDAO.contarPorEstado(con, id, "usada") > 0) {
                    throw new ReglaNegocioException("La reserva tiene boletas usadas: el servicio ya se prestó y no se puede cancelar.");
                }
                BigDecimal pagado = (BigDecimal) vistaDAO.estadoPago(con, id).get("total_pagado");
                BigDecimal reembolso = BigDecimal.ZERO;
                if (diasHastaServicio(r) > DIAS_MINIMOS_REEMBOLSO && pagado.signum() > 0) {
                    reembolso = pagado.multiply(PORCENTAJE_REEMBOLSO).setScale(2, RoundingMode.HALF_UP);
                    pagoDAO.reembolsarDeReserva(con, id);
                }
                dao.cambiarEstado(con, id, "cancelada");
                boletaDAO.anularDeReserva(con, id);
                r.setEstado("cancelada");
                notificaciones.cambioEstado(con, r, "cancelada");
                con.commit();
                return reembolso;
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    /** Lo que se devolvería si se cancela hoy (para mostrarlo antes de confirmar). */
    public BigDecimal reembolsoSiCancela(int id) throws SQLException {
        Reserva r = dao.buscarPorId(id);
        BigDecimal pagado = (BigDecimal) vistaDAO.estadoPago(id).get("total_pagado");
        if (diasHastaServicio(r) > DIAS_MINIMOS_REEMBOLSO) {
            return pagado.multiply(PORCENTAJE_REEMBOLSO).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    private long diasHastaServicio(Reserva r) {
        return ChronoUnit.DAYS.between(LocalDate.now(), r.getFechaServicio());
    }
}
