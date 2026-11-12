package co.edu.uniquindio.quindiostay.servicio;

import co.edu.uniquindio.quindiostay.dao.BoletaDAO;
import co.edu.uniquindio.quindiostay.dao.ConexionBD;
import co.edu.uniquindio.quindiostay.dao.ReservaDAO;
import co.edu.uniquindio.quindiostay.dao.VistaDAO;
import co.edu.uniquindio.quindiostay.modelo.Boleta;
import co.edu.uniquindio.quindiostay.modelo.Reserva;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Boletas. Se venden desde VentaServicio. Aquí van los cambios de estado:
 *  - vendida -> usada: solo el día de su función y si la reserva está pagada (RF09).
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

    public BoletaServicio(BoletaDAO dao, VistaDAO vistaDAO, ConexionBD conexionBD, VentaServicio ventaServicio,
                          ReservaDAO reservaDAO, NotificacionServicio notificaciones) {
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
            dao.cambiarEstado(con, b.getIdBoleta(), nuevo);
            if ("anulada".equals(nuevo)) {
                Reserva r = reservaDAO.buscarPorId(con, b.getIdReserva());
                if (r != null) {
                    notificaciones.boletaAnulada(con, r, b.getCodigo());
                }
            }
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
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
            LocalDate fechaFuncion = ((java.sql.Date) b.get("fecha_funcion")).toLocalDate();
            if (!fechaFuncion.equals(LocalDate.now())) {
                throw new ReglaNegocioException("La boleta es para el " + fechaFuncion
                        + ". Solo se puede usar el día de su función (hoy es " + LocalDate.now() + ").");
            }
            Map<String, Object> pago = vistaDAO.estadoPago((Integer) b.get("id_reserva"));
            if (!"sí".equals(pago.get("pagada"))) {
                throw new ReglaNegocioException("La reserva #" + b.get("id_reserva") + " no está pagada (saldo $"
                        + String.format("%,.0f", pago.get("saldo")).replace(',', '.') + "). No puede ingresar.");
            }
            try (Connection con = conexionBD.getConexion()) {
                dao.cambiarEstado(con, (Integer) b.get("id_boleta"), "usada");
            }
            b.put("estado_boleta", "usada");
            return b;
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }
}