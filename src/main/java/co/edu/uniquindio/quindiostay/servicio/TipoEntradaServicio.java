package co.edu.uniquindio.quindiostay.servicio;

import co.edu.uniquindio.quindiostay.dao.TipoEntradaDAO;
import co.edu.uniquindio.quindiostay.modelo.TipoEntrada;

import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Lógica de negocio de Tipo_entrada. El controlador nunca llama al DAO
 * directamente: pasa por aquí, donde van las reglas que un CHECK no puede revisar.
 * Si MySQL rechaza algo (FK, CHECK, UNIQUE) se traduce a un mensaje entendible.
 */
@Service
public class TipoEntradaServicio {

    private final TipoEntradaDAO dao;


    public TipoEntradaServicio(TipoEntradaDAO dao) {
        this.dao = dao;
    }

    public List<TipoEntrada> listarTodos() {
        try {
            return dao.listarTodos();
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    public TipoEntrada buscarPorId(int id) {
        try {
            TipoEntrada x = dao.buscarPorId(id);
            if (x == null) {
                throw new ReglaNegocioException("No existe tipo de entrada con id " + id + ".");
            }
            return x;
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    /** Crea si no tiene id, o actualiza si ya lo tiene. */
    public void guardar(TipoEntrada x) {
        try {
            validar(x);
            if (x.getIdTipoEntrada() == null) {
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
    private void validar(TipoEntrada x) throws SQLException {
    }
}
