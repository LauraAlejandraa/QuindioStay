package co.edu.uniquindio.quindiostay.servicio;

import co.edu.uniquindio.quindiostay.dao.ZonaDAO;
import co.edu.uniquindio.quindiostay.modelo.Zona;

import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Lógica de negocio de Zona. El controlador nunca llama al DAO
 * directamente: pasa por aquí, donde van las reglas que un CHECK no puede revisar.
 * Si MySQL rechaza algo (FK, CHECK, UNIQUE) se traduce a un mensaje entendible.
 */
@Service
public class ZonaServicio {

    private final ZonaDAO dao;


    public ZonaServicio(ZonaDAO dao) {
        this.dao = dao;
    }

    public List<Zona> listarTodos() {
        try {
            return dao.listarTodos();
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    public Zona buscarPorId(int id) {
        try {
            Zona x = dao.buscarPorId(id);
            if (x == null) {
                throw new ReglaNegocioException("No existe zona con id " + id + ".");
            }
            return x;
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    /** Crea si no tiene id, o actualiza si ya lo tiene. */
    public void guardar(Zona x) {
        try {
            validar(x);
            if (x.getIdZona() == null) {
                dao.crear(x);
            } else {
                dao.actualizar(x);
            }
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    /** Eliminar: borra la fila (Tabla 39 de la Entrega 2). */
    public void eliminar(int id) {
        try {
            int boletas = dao.contarBoletas(id);
            if (boletas > 0) {
                throw new ReglaNegocioException("No se puede eliminar la zona porque tiene " + boletas
                        + (boletas == 1 ? " boleta registrada" : " boletas registradas")
                        + " (vendidas, usadas o anuladas): son el historial de ventas. Si ya no la quiere vender, "
                        + "puede cancelar la función o dejar la zona como está.");
            }
            dao.eliminar(id);
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

    public List<Zona> listarPorPrestador(int idPrestador) {
        try {
            return dao.listarPorPrestador(idPrestador);
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    /** Reglas que miran otras tablas u otras filas (no caben en un CHECK). */
    private void validar(Zona x) throws SQLException {
        if (x.getNombre() == null || x.getNombre().isBlank()) {
            throw new ReglaNegocioException("El nombre de la zona no puede quedar vacío.");
        }
        validarOrdenPrecios(x);
    }

    /** Orden de las zonas conocidas, de la más barata a la más cara. Otros nombres no entran en la regla. */
    private static final List<String> ORDEN = List.of("general", "preferencial", "vip");

    /**
     * En una misma función: General < Preferencial < VIP (precio estrictamente mayor).
     * Solo compara zonas con esos nombres (sin importar mayúsculas); una zona con otro nombre
     * (por ejemplo «Palco») no tiene orden fijo y su precio es libre.
     */
    private void validarOrdenPrecios(Zona x) throws SQLException {
        int nivel = ORDEN.indexOf(x.getNombre().trim().toLowerCase());
        if (nivel < 0 || x.getPrecio() == null || x.getIdFuncion() == null) {
            return;
        }
        for (Zona otra : dao.listarTodos()) {
            if (!x.getIdFuncion().equals(otra.getIdFuncion()) || otra.getIdZona().equals(x.getIdZona())) {
                continue;
            }
            int nivelOtra = ORDEN.indexOf(otra.getNombre().trim().toLowerCase());
            if (nivelOtra < 0 || nivelOtra == nivel) {
                continue;
            }
            boolean mal = nivel > nivelOtra ? x.getPrecio().compareTo(otra.getPrecio()) <= 0
                                            : x.getPrecio().compareTo(otra.getPrecio()) >= 0;
            if (mal) {
                throw new ReglaNegocioException("El precio de la zona " + x.getNombre().trim() + " ($" + pesos(x.getPrecio())
                        + ") debe ser " + (nivel > nivelOtra ? "mayor" : "menor") + " que el de la zona " + otra.getNombre()
                        + " ($" + pesos(otra.getPrecio()) + ") de la misma función. Orden de precios: General < Preferencial < VIP.");
            }
        }
    }

    private static String pesos(java.math.BigDecimal v) {
        return String.format("%,.0f", v).replace(',', '.');
    }
}
