package co.edu.uniquindio.quindiostay.servicio;

import co.edu.uniquindio.quindiostay.dao.ComboAtraccionDAO;
import co.edu.uniquindio.quindiostay.dao.ComboDAO;
import co.edu.uniquindio.quindiostay.modelo.ComboAtraccion;

import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Lógica de negocio de Combo_atraccion. El controlador nunca llama al DAO
 * directamente: pasa por aquí, donde van las reglas que un CHECK no puede revisar.
 * Si MySQL rechaza algo (FK, CHECK, UNIQUE) se traduce a un mensaje entendible.
 */
@Service
public class ComboAtraccionServicio {

    private final ComboAtraccionDAO dao;
    private final ComboDAO comboDAO;


    public ComboAtraccionServicio(ComboAtraccionDAO dao, ComboDAO comboDAO) {
        this.dao = dao;
        this.comboDAO = comboDAO;
    }

    public List<ComboAtraccion> listarTodos() {
        try {
            return dao.listarTodos();
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    public ComboAtraccion buscarPorId(int id) {
        try {
            ComboAtraccion x = dao.buscarPorId(id);
            if (x == null) {
                throw new ReglaNegocioException("No existe atracción del combo con id " + id + ".");
            }
            return x;
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    /** Crea si no tiene id, o actualiza si ya lo tiene. */
    public void guardar(ComboAtraccion x) {
        try {
            validar(x);
            if (x.getIdComboAtraccion() == null) {
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
            ComboAtraccion x = dao.buscarPorId(id);
            validar(x);
            if ("activo".equals(comboDAO.buscarPorId(x.getIdCombo()).getEstado())
                    && comboDAO.atraccionesDelCombo(x.getIdCombo()).size() <= 2) {
                throw new ReglaNegocioException("El combo está activo y quedaría con menos de dos atracciones. Inactívelo primero.");
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

    /** Reglas que miran otras tablas u otras filas (no caben en un CHECK). */
    private void validar(ComboAtraccion x) throws SQLException {
        // Si el combo ya se vendió, su composición no cambia: las unidades vendidas se
        // calculan dividiendo sus boletas por su número de atracciones.
        if (comboDAO.contarBoletas(x.getIdCombo()) > 0) {
            throw new ReglaNegocioException("Ese combo ya tiene boletas vendidas: su composición no se puede cambiar.");
        }
    }
}
