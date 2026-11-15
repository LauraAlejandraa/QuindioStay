package co.edu.uniquindio.quindiostay.servicio;

import co.edu.uniquindio.quindiostay.dao.*;
import co.edu.uniquindio.quindiostay.modelo.Reserva;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reservas (núcleo común). Las reservas se CREAN desde la venta de boletas
 * (VentaServicio). Aquí van los cambios de estado y la cancelación (RF15):
 *   pendiente  -> confirmada (solo si está PAGADA por completo) | cancelada
 *   confirmada -> completada | cancelada | pendiente (solo si dejó de estar pagada)
 *   completada y cancelada son finales.
 * Confirmada = pagada: normalmente la reserva se confirma SOLA al pagarse
 * (EstadoPagoReserva). A mano solo se confirma si los pagos exitosos ya cubren
 * el total (por ejemplo, después de anular una boleta).
 * Significado de cada estado:
 *   pendiente  = creada, falta confirmarla (normalmente falta pagarla).
 *   confirmada = pagada por completo; el servicio todavía no se ha prestado.
 *   completada = el servicio YA SE PRESTÓ: todas sus boletas se usaron en taquilla
 *                o sus funciones ya terminaron. Pasa SOLA (EstadoPagoReserva).
 *   cancelada  = anulada; sus boletas quedan anuladas y se aplica el reembolso.
 * Una reserva solo pasa a completada si estaba confirmada, pagada y no le queda ninguna
 * boleta por usar en una función que no ha terminado. Como completada es final
 * (no se cancela después), marcarla antes de tiempo dejaría una reserva que no se
 * puede cancelar ni validar en taquilla; por eso la regla se revisa aquí, en el
 * servicio, y vale igual para el botón del detalle y para el formulario de editar.
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
    private final EstadoPagoReserva estadoPago;

    public ReservaServicio(ReservaDAO dao, BoletaDAO boletaDAO, PagoDAO pagoDAO, VistaDAO vistaDAO,
                           ConexionBD conexionBD, NotificacionServicio notificaciones, EstadoPagoReserva estadoPago) {
        this.estadoPago = estadoPago;
        this.dao = dao;
        this.boletaDAO = boletaDAO;
        this.pagoDAO = pagoDAO;
        this.vistaDAO = vistaDAO;
        this.conexionBD = conexionBD;
        this.notificaciones = notificaciones;
    }

    public List<Reserva> listarTodos() {
        try {
            estadoPago.corregirConfirmadasSinPago();   // confirmada sin pago completo -> pendiente
            estadoPago.revisarCompletadas();           // completada solo si el servicio ya se prestó
            return dao.listarTodos();
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    public List<Reserva> listarPorPrestador(int idPrestador) {
        try {
            estadoPago.corregirConfirmadasSinPago();   // confirmada sin pago completo -> pendiente
            estadoPago.revisarCompletadas();           // completada solo si el servicio ya se prestó
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
            List<Map<String, Object>> boletas = vistaDAO.boletasDeReserva(id);
            d.put("boletas", boletas);
            d.put("boletasVigentes", boletas.stream().filter(b -> !"anulada".equals(b.get("estado_boleta"))).count());
            d.putAll(ResumenPago.calcular(vistaDAO, id, (String) d.get("estado_reserva"),
                    (BigDecimal) d.get("total_pagado"), (BigDecimal) d.get("saldo")));
            d.put("pagos", vistaDAO.pagosDeReserva(id));
            notificaciones.enviarVencidas();   // envío simulado: las que ya llegaron a su hora quedan 'enviada'
            d.put("notificaciones", vistaDAO.notificacionesDeReserva(id));
            d.put("reembolsoSiCancela", reembolsoSiCancela(id));
            Reserva r = dao.buscarPorId(id);
            d.put("puedeCompletar", motivoNoCompletar(r) == null);
            d.put("motivoNoCompletar", motivoNoCompletar(r));
            d.put("motivoNoConfirmar", "pendiente".equals(r.getEstado()) ? estadoPago.motivoNoConfirmar(id) : null);
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
        if ("completada".equals(nuevo)) {
            String motivo = motivoNoCompletar(r);
            if (motivo != null) {
                throw new ReglaNegocioException(motivo);
            }
        }
        boolean permitido = ("pendiente".equals(actual) && "confirmada".equals(nuevo))
                || ("confirmada".equals(actual) && "completada".equals(nuevo))
                || ("confirmada".equals(actual) && "pendiente".equals(nuevo));
        if (!permitido) {
            throw new ReglaNegocioException("No se puede pasar una reserva de '" + actual + "' a '" + nuevo + "'.");
        }
        if ("confirmada".equals(nuevo)) {
            String motivo = estadoPago.motivoNoConfirmar(id);
            if (motivo != null) {
                throw new ReglaNegocioException(motivo);
            }
        }
        if ("pendiente".equals(nuevo) && estadoPago.estaPagada(id)) {
            throw new ReglaNegocioException("La reserva #" + id + " ya está pagada por completo: debe seguir confirmada. "
                    + "Solo vuelve a pendiente si deja de estar pagada (por ejemplo, si se reembolsa un pago).");
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
                notificaciones.cambioEstado(con, r, "cancelada", reembolso, pagado);
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

    /**
     * Por qué la reserva NO puede pasar a completada todavía (null si sí puede).
     * Completada = el servicio ya se prestó: confirmada, pagada y sin boletas por usar
     * para funciones que no han terminado (ver EstadoPagoReserva). Normalmente pasa sola.
     */
    public String motivoNoCompletar(Reserva r) {
        return estadoPago.motivoNoCompletar(r.getIdReserva(), r.getEstado());
    }

    /**
     * Estados que se pueden elegir en el formulario de editar: el actual y los
     * cambios permitidos desde él (confirmada solo si está pagada por completo;
     * completada solo si está pagada y ya llegó la fecha del servicio).
     */
    public List<String> estadosPosibles(Reserva r) {
        List<String> lista = new ArrayList<>();
        lista.add(r.getEstado());
        if ("pendiente".equals(r.getEstado())) {
            if (estadoPago.motivoNoConfirmar(r.getIdReserva()) == null) {
                lista.add("confirmada");
            }
            lista.add("cancelada");
        } else if ("confirmada".equals(r.getEstado())) {
            if (!estadoPago.estaPagada(r.getIdReserva())) {
                lista.add("pendiente");   // corrección: quedó confirmada sin pago completo
            }
            if (motivoNoCompletar(r) == null) {
                lista.add("completada");
            }
            lista.add("cancelada");
        }
        return lista;
    }

    /** Por qué una reserva pendiente no se puede confirmar todavía (null si sí se puede o si no está pendiente). */
    public String motivoNoConfirmar(Reserva r) {
        return "pendiente".equals(r.getEstado()) ? estadoPago.motivoNoConfirmar(r.getIdReserva()) : null;
    }

    /** Lo pagado con éxito (sin contar reembolsos) en este momento. */
    public BigDecimal pagadoExitoso(int id) {
        try {
            return (BigDecimal) vistaDAO.estadoPago(id).get("total_pagado");
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    /**
     * Texto del mensaje en pantalla al cancelar. pagadoAntes es lo pagado con éxito
     * ANTES de cancelar: si era $0 no se habla de reembolso, porque no había nada que devolver.
     */
    public static String textoReembolso(BigDecimal pagadoAntes, BigDecimal reembolso, boolean paraCliente) {
        if (reembolso.signum() > 0) {
            String valor = String.format("%,.0f", reembolso).replace(',', '.');
            return paraCliente ? "Se le reembolsará el 80 % de lo pagado: $" + valor + "."
                               : "Reembolso del 80 %: $" + valor + ".";
        }
        if (pagadoAntes == null || pagadoAntes.signum() <= 0) {
            return "No había pagos que reembolsar.";
        }
        return paraCliente ? "No hay reembolso: faltaban 5 días o menos para el servicio."
                           : "Sin reembolso (faltaban 5 días o menos para el servicio).";
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
        return ChronoUnit.DAYS.between(Reloj.hoy(), r.getFechaServicio());
    }
}
