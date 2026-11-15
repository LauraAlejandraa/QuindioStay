package co.edu.uniquindio.quindiostay.servicio;

import co.edu.uniquindio.quindiostay.dao.BoletaDAO;
import co.edu.uniquindio.quindiostay.dao.ConexionBD;
import co.edu.uniquindio.quindiostay.dao.ReservaDAO;
import co.edu.uniquindio.quindiostay.dao.VistaDAO;
import co.edu.uniquindio.quindiostay.modelo.Reserva;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Regla única que une el estado de la reserva con sus pagos:
 *   CONFIRMADA = PAGADA POR COMPLETO (la suma de los pagos EXITOSOS cubre el valor total,
 *   vista_estado_pago_reserva.pagada = 'sí') y con al menos una boleta vigente.
 * Por eso:
 *   - pendiente que queda pagada        -> pasa sola a confirmada (al pagar o al anular una boleta);
 *   - confirmada que deja de estar pagada -> vuelve sola a pendiente (se reembolsó un pago,
 *     se agregó una boleta o quedó confirmada a mano sin pago, como pasaba antes).
 * Y COMPLETADA = el servicio ya se prestó: confirmada (pagada) y ya no le queda ninguna
 * boleta por usar, es decir, cada boleta vigente se USÓ en taquilla o su función YA TERMINÓ
 * (el cliente no llegó). Pasa sola a completada: al validar en taquilla la última boleta,
 * o cuando termina la última función (revisión cada minuto). Una reserva con una boleta
 * para una función futura (como la #503, servicio el 21/11) NO puede estar completada.
 * Se llama dentro de la misma transacción que hizo el cambio (pago, boleta). Además, al arrancar
 * la aplicación y cada vez que se abre la lista de reservas revisa los datos que ya existen
 * (corrige reservas confirmadas sin pago, como la #505, aunque se hayan cambiado por SQL).
 */
@Service
public class EstadoPagoReserva {

    private static final Logger LOG = LoggerFactory.getLogger(EstadoPagoReserva.class);

    private final ReservaDAO reservaDAO;
    private final BoletaDAO boletaDAO;
    private final VistaDAO vistaDAO;
    private final ConexionBD conexionBD;
    private final NotificacionServicio notificaciones;

    public EstadoPagoReserva(ReservaDAO reservaDAO, BoletaDAO boletaDAO, VistaDAO vistaDAO,
                             ConexionBD conexionBD, NotificacionServicio notificaciones) {
        this.reservaDAO = reservaDAO;
        this.boletaDAO = boletaDAO;
        this.vistaDAO = vistaDAO;
        this.conexionBD = conexionBD;
        this.notificaciones = notificaciones;
    }

    /**
     * Por qué la reserva NO puede quedar confirmada (null si sí puede).
     * Mensaje para el formulario de editar y para el botón «Confirmar».
     */
    public String motivoNoConfirmar(Connection con, int idReserva) throws SQLException {
        Map<String, Object> p = vistaDAO.estadoPago(con, idReserva);
        if (vigentes(con, idReserva) == 0) {
            return "La reserva #" + idReserva + " no tiene boletas vigentes: no se puede confirmar.";
        }
        if (!"sí".equals(p.get("pagada"))) {
            return "No se puede confirmar la reserva #" + idReserva + ": no está pagada por completo "
                    + "(valor " + pesos(p.get("valor_total")) + ", pagado " + pesos(p.get("total_pagado"))
                    + ", falta " + pesos(p.get("saldo")) + "). Una reserva se confirma sola cuando sus pagos "
                    + "exitosos cubren el total: registre el pago.";
        }
        return null;
    }

    public String motivoNoConfirmar(int idReserva) {
        try (Connection con = conexionBD.getConexion()) {
            return motivoNoConfirmar(con, idReserva);
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    public boolean estaPagada(int idReserva) {
        try {
            return "sí".equals(vistaDAO.estadoPago(idReserva).get("pagada"));
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    /**
     * Ajusta el estado según lo pagado, dentro de la transacción del llamador.
     * Solo toca reservas pendientes o confirmadas (completada y cancelada son finales).
     */
    public void sincronizar(Connection con, int idReserva) throws SQLException {
        Reserva r = reservaDAO.buscarPorId(con, idReserva);
        if (r == null) {
            return;
        }
        Map<String, Object> p = vistaDAO.estadoPago(con, idReserva);
        boolean pagada = "sí".equals(p.get("pagada"));
        if ("pendiente".equals(r.getEstado()) && pagada && vigentes(con, idReserva) > 0) {
            reservaDAO.cambiarEstado(con, idReserva, "confirmada");
            r.setEstado("confirmada");
            notificaciones.cambioEstado(con, r, "confirmada");
            notificaciones.recordatorio(con, r);
        } else if ("confirmada".equals(r.getEstado()) && !pagada) {
            reservaDAO.cambiarEstado(con, idReserva, "pendiente");
            r.setEstado("pendiente");
            notificaciones.volvioAPendiente(con, r, (BigDecimal) p.get("saldo"));
        }
    }

    /**
     * Datos que ya existían: toda reserva confirmada que no está pagada por completo
     * vuelve a pendiente (por ejemplo, la #505 que se confirmó a mano sin pagos).
     * Corre al arrancar la aplicación y al listar reservas. Devuelve cuántas corrigió.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void alArrancar() {
        corregirConfirmadasSinPago();
        revisarCompletadas();
    }

    public int corregirConfirmadasSinPago() {
        int corregidas = 0;
        try (Connection con = conexionBD.getConexion()) {
            List<Map<String, Object>> filas = vistaDAO.consultar(con,
                    "SELECT id_reserva FROM vista_estado_pago_reserva WHERE estado_reserva = 'confirmada' AND pagada = 'no'");
            for (Map<String, Object> f : filas) {
                int id = ((Number) f.get("id_reserva")).intValue();
                con.setAutoCommit(false);
                try {
                    sincronizar(con, id);
                    con.commit();
                    corregidas++;
                    LOG.info("Reserva #{} estaba confirmada sin pago completo: volvió a pendiente.", id);
                } catch (SQLException e) {
                    con.rollback();
                    LOG.warn("No se pudo corregir la reserva #{}: {}", id, e.getMessage());
                }
            }
        } catch (SQLException e) {
            LOG.warn("No se pudieron revisar las reservas confirmadas sin pago: {}", e.getMessage());
        }
        return corregidas;
    }

    /** Boletas vendidas (sin usar) cuya función todavía no termina: si hay alguna, el servicio no ha terminado. */
    private static final String POR_USAR =
            "SELECT TIMESTAMP(f.fecha, f.hora_apertura) AS inicio FROM Boleta b "
          + "INNER JOIN Zona z ON z.id_zona = b.id_zona INNER JOIN Funcion f ON f.id_funcion = z.id_funcion "
          + "WHERE b.id_reserva = ? AND b.estado = 'vendida' AND TIMESTAMP(f.fecha, f.hora_cierre) > ? ORDER BY 1";

    /**
     * Por qué la reserva NO puede quedar completada (null si sí puede).
     * Completada = confirmada + pagada + ninguna boleta pendiente de usar en una función que no ha terminado.
     */
    public String motivoNoCompletar(Connection con, int idReserva, String estado) throws SQLException {
        if (!"confirmada".equals(estado)) {
            return "Una reserva solo pasa a completada si estaba confirmada (está " + estado + ").";
        }
        if (!"sí".equals(vistaDAO.estadoPago(con, idReserva).get("pagada"))) {
            return "La reserva no está pagada por completo: no se puede completar.";
        }
        if (vigentes(con, idReserva) == 0) {
            return "La reserva no tiene boletas vigentes.";
        }
        List<Map<String, Object>> porUsar = vistaDAO.consultar(con, POR_USAR, idReserva, Timestamp.valueOf(Reloj.ahora()));
        if (!porUsar.isEmpty()) {
            Object inicio = porUsar.get(0).get("inicio");
            String cuando = inicio instanceof java.time.LocalDateTime t ? t.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                    : inicio instanceof Timestamp ts ? ts.toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : String.valueOf(inicio);
            return "Todavía hay " + porUsar.size() + (porUsar.size() == 1 ? " boleta sin usar" : " boletas sin usar")
                    + " para funciones que no han terminado (la próxima: " + cuando + "). "
                    + "«Completada» significa que el servicio ya se prestó: la reserva se completa sola cuando todas "
                    + "sus boletas se usan en taquilla o cuando terminan sus funciones.";
        }
        return null;
    }

    public String motivoNoCompletar(int idReserva, String estado) {
        try (Connection con = conexionBD.getConexion()) {
            return motivoNoCompletar(con, idReserva, estado);
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    /** Confirmada que ya cumple todo -> completada (dentro de la transacción del llamador). */
    public boolean completarSiTermino(Connection con, int idReserva) throws SQLException {
        Reserva r = reservaDAO.buscarPorId(con, idReserva);
        if (r == null || motivoNoCompletar(con, idReserva, r.getEstado()) != null) {
            return false;
        }
        reservaDAO.cambiarEstado(con, idReserva, "completada");
        r.setEstado("completada");
        notificaciones.cambioEstado(con, r, "completada");
        return true;
    }

    /**
     * Revisión general (al arrancar, cada minuto y al abrir la lista de reservas):
     *   1. confirmadas que ya terminaron (todas las boletas usadas o funciones terminadas) -> completada;
     *   2. completadas que NO cumplen (no pagadas, o con boletas para funciones futuras, como la #503)
     *      -> vuelven a confirmada (si están pagadas) o a pendiente (si no).
     * Devuelve cuántas cambió.
     */
    @Scheduled(fixedRate = 60_000, initialDelay = 60_000)
    public int revisarCompletadas() {
        int cambios = 0;
        Timestamp ahora = Timestamp.valueOf(Reloj.ahora());
        try (Connection con = conexionBD.getConexion()) {
            List<Map<String, Object>> aCompletar = vistaDAO.consultar(con,
                    "SELECT v.id_reserva FROM vista_estado_pago_reserva v WHERE v.estado_reserva = 'confirmada' AND v.pagada = 'sí' "
                  + "AND EXISTS (SELECT 1 FROM Boleta b WHERE b.id_reserva = v.id_reserva AND b.estado <> 'anulada') "
                  + "AND NOT EXISTS (SELECT 1 FROM Boleta b INNER JOIN Zona z ON z.id_zona = b.id_zona "
                  + "  INNER JOIN Funcion f ON f.id_funcion = z.id_funcion "
                  + "  WHERE b.id_reserva = v.id_reserva AND b.estado = 'vendida' AND TIMESTAMP(f.fecha, f.hora_cierre) > ?)", ahora);
            for (Map<String, Object> f : aCompletar) {
                int id = ((Number) f.get("id_reserva")).intValue();
                con.setAutoCommit(false);
                try {
                    if (completarSiTermino(con, id)) {
                        cambios++;
                    }
                    con.commit();
                } catch (SQLException e) {
                    con.rollback();
                    LOG.warn("No se pudo completar la reserva #{}: {}", id, e.getMessage());
                }
            }
            List<Map<String, Object>> malCompletadas = vistaDAO.consultar(con,
                    "SELECT v.id_reserva, v.pagada FROM vista_estado_pago_reserva v WHERE v.estado_reserva = 'completada' AND (v.pagada = 'no' "
                  + "OR EXISTS (SELECT 1 FROM Boleta b INNER JOIN Zona z ON z.id_zona = b.id_zona "
                  + "  INNER JOIN Funcion f ON f.id_funcion = z.id_funcion "
                  + "  WHERE b.id_reserva = v.id_reserva AND b.estado = 'vendida' AND TIMESTAMP(f.fecha, f.hora_cierre) > ?))", ahora);
            for (Map<String, Object> f : malCompletadas) {
                int id = ((Number) f.get("id_reserva")).intValue();
                String nuevo = "sí".equals(f.get("pagada")) ? "confirmada" : "pendiente";
                con.setAutoCommit(false);
                try {
                    Reserva r = reservaDAO.buscarPorId(con, id);
                    reservaDAO.cambiarEstado(con, id, nuevo);
                    r.setEstado(nuevo);
                    notificaciones.cambioEstado(con, r, nuevo);
                    if ("confirmada".equals(nuevo)) {
                        notificaciones.recordatorio(con, r);
                    }
                    con.commit();
                    cambios++;
                    LOG.info("Reserva #{} estaba completada antes de prestarse el servicio: volvió a {}.", id, nuevo);
                } catch (SQLException e) {
                    con.rollback();
                    LOG.warn("No se pudo corregir la reserva completada #{}: {}", id, e.getMessage());
                }
            }
        } catch (SQLException e) {
            LOG.warn("No se pudieron revisar las reservas completadas: {}", e.getMessage());
        }
        return cambios;
    }

    private int vigentes(Connection con, int idReserva) throws SQLException {
        return boletaDAO.contarPorEstado(con, idReserva, "vendida") + boletaDAO.contarPorEstado(con, idReserva, "usada");
    }

    static String pesos(Object v) {
        BigDecimal b = v == null ? BigDecimal.ZERO : (BigDecimal) v;
        return "$" + String.format("%,.0f", b).replace(',', '.');
    }
}
