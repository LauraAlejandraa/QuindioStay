package co.edu.uniquindio.quindiostay.servicio;

import co.edu.uniquindio.quindiostay.dao.TipoPrestadorDAO;
import co.edu.uniquindio.quindiostay.modelo.TipoPrestador;

import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Lógica de negocio de Tipo_prestador. El controlador nunca llama al DAO
 * directamente: pasa por aquí, donde van las reglas que un CHECK no puede revisar.
 * Si MySQL rechaza algo (FK, CHECK, UNIQUE) se traduce a un mensaje entendible.
 */
@Service
public class TipoPrestadorServicio {

    private final TipoPrestadorDAO dao;


    public TipoPrestadorServicio(TipoPrestadorDAO dao) {
        this.dao = dao;
    }

    public List<TipoPrestador> listarTodos() {
        try {
            return dao.listarTodos();
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    public TipoPrestador buscarPorId(int id) {
        try {
            TipoPrestador x = dao.buscarPorId(id);
            if (x == null) {
                throw new ReglaNegocioException("No existe tipo de prestador con id " + id + ".");
            }
            return x;
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    /** Crea si no tiene id, o actualiza si ya lo tiene. */
    public void guardar(TipoPrestador x) {
        try {
            validar(x);
            if (x.getIdTipoPrestador() == null) {
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
    private void validar(TipoPrestador x) throws SQLException {
    }
}
