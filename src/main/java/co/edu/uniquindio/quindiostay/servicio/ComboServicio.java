package co.edu.uniquindio.quindiostay.servicio;

import co.edu.uniquindio.quindiostay.dao.ComboDAO;
import co.edu.uniquindio.quindiostay.modelo.Combo;

import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Lógica de negocio de Combo. El controlador nunca llama al DAO
 * directamente: pasa por aquí, donde van las reglas que un CHECK no puede revisar.
 * Si MySQL rechaza algo (FK, CHECK, UNIQUE) se traduce a un mensaje entendible.
 */
@Service
public class ComboServicio {

    private final ComboDAO dao;


    public ComboServicio(ComboDAO dao) {
        this.dao = dao;
    }

    public List<Combo> listarTodos() {
        try {
            return dao.listarTodos();
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    public Combo buscarPorId(int id) {
        try {
            Combo x = dao.buscarPorId(id);
            if (x == null) {
                throw new ReglaNegocioException("No existe combo con id " + id + ".");
            }
            return x;
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    /** Crea si no tiene id, o actualiza si ya lo tiene. */
    public void guardar(Combo x) {
        try {
            validar(x);
            if (x.getIdCombo() == null) {
                dao.crear(x);
            } else {
                dao.actualizar(x);
            }
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    /** Eliminar: cambia su estado (Tabla 39 de la Entrega 2). */
    public void eliminar(int id) {
        try {
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
    private void validar(Combo x) throws SQLException {
        // Un combo agrupa MÁS de una atracción (regla de la Entrega 1): no se activa con menos de dos.
        if ("activo".equals(x.getEstado())) {
            int n = x.getIdCombo() == null ? 0 : dao.atraccionesDelCombo(x.getIdCombo()).size();
            if (n < 2) {
                throw new ReglaNegocioException("Un combo activo debe tener al menos dos atracciones. "
                        + "Guárdelo como inactivo, agregue sus atracciones en «Atracciones de los combos» y luego actívelo.");
            }
        }
    }

    public List<Integer> atraccionesDelCombo(int idCombo) {
        try {
            return dao.atraccionesDelCombo(idCombo);
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }
}
