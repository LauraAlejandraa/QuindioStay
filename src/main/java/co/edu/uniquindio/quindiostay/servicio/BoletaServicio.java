package co.edu.uniquindio.quindiostay.servicio;

import co.edu.uniquindio.quindiostay.dao.BoletaDAO;
import co.edu.uniquindio.quindiostay.dao.ConexionBD;
import co.edu.uniquindio.quindiostay.dao.ReservaDAO;
import co.edu.uniquindio.quindiostay.dao.VistaDAO;
import co.edu.uniquindio.quindiostay.modelo.Boleta;
import co.edu.uniquindio.quindiostay.modelo.Reserva;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Boletas. Se venden desde VentaServicio. Aquí van los cambios de estado:
 *  - vendida -> usada: solo el día de su función (antes del cierre), si la reserva está
 *    pagada por completo (pagos exitosos = total) y está confirmada (RF09). Al usar la
 *    última boleta pendiente, la reserva pasa sola a completada. Una reserva
 *    'confirmada' sin pago completo NO deja entrar: manda lo pagado, no la etiqueta.
 *  - vendida -> anulada: libera el cupo de la zona.
 *  - una boleta usada o anulada NO vuelve a vendida (ni cambia más).
 */
@Service
public class BoletaServicio {

    private final BoletaDAO dao;
    private final VistaDAO vistaDAO;
    private final ConexionBD conexionBD;
    private final VentaServicio ventaServicio;
    private final ReservaDAO reservaDAO;
    private final NotificacionServicio notificaciones;
    private final EstadoPagoReserva estadoPago;

    public BoletaServicio(BoletaDAO dao, VistaDAO vistaDAO, ConexionBD conexionBD, VentaServicio ventaServicio,
                          ReservaDAO reservaDAO, NotificacionServicio notificaciones, EstadoPagoReserva estadoPago) {
        this.estadoPago = estadoPago;
        this.dao = dao;
        this.vistaDAO = vistaDAO;
        this.conexionBD = conexionBD;
        this.ventaServicio = ventaServicio;
        this.reservaDAO = reservaDAO;
        this.notificaciones = notificaciones;
    }

    public List<Boleta> listarTodos() {
        try {
            return dao.listarTodos();
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    public Boleta buscarPorId(int id) {
        try {
            Boleta x = dao.buscarPorId(id);
            if (x == null) {
                throw new ReglaNegocioException("No existe la boleta " + id + ".");
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

    /**
     * Nueva: se agrega una boleta suelta a una reserva existente (con todas las reglas de venta).
     * Existente: solo cambia el estado, respetando las transiciones permitidas.
     */
    public void guardar(Boleta x) {
        if (x.getIdBoleta() == null) {
            if (x.getIdCombo() != null) {
                throw new ReglaNegocioException("Las boletas de combo se venden desde «Vender combo» (una por cada atracción).");
            }
            ventaServicio.agregarBoleta(x.getIdReserva(), x.getIdZona(), x.getIdTipoEntrada());
            return;
        }
        Boleta actual = buscarPorId(x.getIdBoleta());
        if (!actual.getIdZona().equals(x.getIdZona()) || !actual.getIdTipoEntrada().equals(x.getIdTipoEntrada())
                || !actual.getIdReserva().equals(x.getIdReserva()) || !actual.getCodigo().equals(x.getCodigo())) {
            throw new ReglaNegocioException("De una boleta vendida solo se cambia el estado (es la prueba de la venta).");
        }
        if (actual.getEstado().equals(x.getEstado())) {
            return;
        }
        if ("usada".equals(x.getEstado())) {
            validarEnTaquilla(actual.getCodigo());
        } else {
            cambiarEstado(actual, x.getEstado());
        }
    }

    /** Eliminar = anular (Tabla 39). */
    public void eliminar(int id) {
        cambiarEstado(buscarPorId(id), "anulada");
    }

    private void cambiarEstado(Boleta b, String nuevo) {
        if (!"vendida".equals(b.getEstado())) {
            throw new ReglaNegocioException("La boleta está " + b.getEstado() + ": una boleta usada o anulada no vuelve a cambiar de estado.");
        }
        try (Connection con = conexionBD.getConexion()) {
            con.setAutoCommit(false);
            try {
                if ("anulada".equals(nuevo)) {
                    validarNoDejaNinosSolos(con, b);
                }
                dao.cambiarEstado(con, b.getIdBoleta(), nuevo);
                if ("anulada".equals(nuevo)) {
                    if (b.getIdCombo() == null) {
                        List<Map<String, Object>> filas = vistaDAO.consultar(con,
                                "SELECT valor_boleta FROM vista_boleta_detalle WHERE id_boleta = ?", b.getIdBoleta());
                        if (!filas.isEmpty() && filas.get(0).get("valor_boleta") != null) {
                            BigDecimal valor = (BigDecimal) filas.get(0).get("valor_boleta");
                            reservaDAO.sumarValor(con, b.getIdReserva(), valor.negate());
                        }
                    }
                    Reserva r = reservaDAO.buscarPorId(con, b.getIdReserva());
                    if (r != null) {
                        notificaciones.boletaAnulada(con, r, b.getCodigo());
                    }
                    // Con el valor rebajado, lo ya pagado puede cubrir el total: se confirma sola.
                    estadoPago.sincronizar(con, b.getIdReserva());
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

    /**
     * Regla del niño con adulto: no se anula la última boleta de adulto o de
     * adulto mayor (los dos cuentan como acompañante) de una reserva que todavía
     * tiene boletas de niño vigentes (quedarían niños solos).
     */
    private void validarNoDejaNinosSolos(Connection con, Boleta b) throws SQLException {
        List<Map<String, Object>> filas = vistaDAO.consultar(con,
                "SELECT nombre FROM Tipo_entrada WHERE id_tipo_entrada = ?", b.getIdTipoEntrada());
        if (filas.isEmpty() || !VentaServicio.esAcompanante((String) filas.get(0).get("nombre"))) {
            return;
        }
        int adultos = dao.contarAcompanantes(con, b.getIdReserva());
        int ninos = dao.contarPorTipo(con, b.getIdReserva(), "niño");
        if (adultos <= 1 && ninos > 0) {
            throw new ReglaNegocioException("No se puede anular la única boleta de adulto o adulto mayor de la reserva #" + b.getIdReserva()
                    + ": quedarían " + ninos + " boleta(s) de niño sin un adulto. Anule primero las de niño o cancele la reserva.");
        }
    }

    /**
     * Taquilla (RF09): valida la boleta por su código y la marca como usada.
     * Devuelve los datos de la boleta para mostrarlos al operador.
     */
    public Map<String, Object> validarEnTaquilla(String codigo) {
        try {
            Map<String, Object> b = vistaDAO.boletaPorCodigo(codigo == null ? "" : codigo.trim().toUpperCase());
            if (b == null) {
                throw new ReglaNegocioException("No existe una boleta con el código " + codigo + ".");
            }
            String estado = (String) b.get("estado_boleta");
            if ("usada".equals(estado)) {
                throw new ReglaNegocioException("La boleta " + b.get("codigo") + " YA FUE USADA: no se permite el reingreso.");
            }
            if ("anulada".equals(estado)) {
                throw new ReglaNegocioException("La boleta " + b.get("codigo") + " está ANULADA (su reserva se canceló).");
            }
            if ("cancelada".equals(b.get("estado_funcion"))) {
                throw new ReglaNegocioException("La función de esta boleta fue CANCELADA: no se puede ingresar.");
            }
            LocalDate fechaFuncion = ((java.sql.Date) b.get("fecha_funcion")).toLocalDate();
            if (!fechaFuncion.equals(Reloj.hoy())) {
                throw new ReglaNegocioException("La boleta es para el " + fechaFuncion
                        + ". Solo se puede usar el día de su función (hoy es " + Reloj.hoy() + ").");
            }
            Object cierre = b.get("hora_cierre");
            if (cierre instanceof java.sql.Time t && Reloj.yaTermino(fechaFuncion, t.toLocalTime())) {
                throw new ReglaNegocioException("La función de esta boleta ya terminó (cerró a las " + t.toLocalTime()
                        + "): no se puede ingresar.");
            }
            Map<String, Object> pago = vistaDAO.estadoPago((Integer) b.get("id_reserva"));
            if (!"sí".equals(pago.get("pagada"))) {
                throw new ReglaNegocioException("La reserva #" + b.get("id_reserva") + " no está pagada (saldo $"
                        + String.format("%,.0f", pago.get("saldo")).replace(',', '.') + "). No puede ingresar.");
            }
            // Doble seguro: además de pagada, la reserva debe estar confirmada. Una completada ya no
            // tiene boletas por usar (se usaron o sus funciones terminaron); pendiente o cancelada, nunca.
            String estadoReserva = (String) pago.get("estado_reserva");
            if (!"confirmada".equals(estadoReserva)) {
                throw new ReglaNegocioException("La reserva #" + b.get("id_reserva") + " está " + estadoReserva
                        + ": solo se validan boletas de reservas confirmadas y pagadas. No puede ingresar.");
            }
            try (Connection con = conexionBD.getConexion()) {
                con.setAutoCommit(false);
                try {
                    dao.cambiarEstado(con, (Integer) b.get("id_boleta"), "usada");
                    // Si era la última boleta por usar, el servicio ya se prestó: la reserva queda completada.
                    b.put("reservaCompletada", estadoPago.completarSiTermino(con, (Integer) b.get("id_reserva")));
                    con.commit();
                } catch (SQLException | RuntimeException e) {
                    con.rollback();
                    throw e;
                }
            }
            b.put("estado_boleta", "usada");
            return b;
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }
}
