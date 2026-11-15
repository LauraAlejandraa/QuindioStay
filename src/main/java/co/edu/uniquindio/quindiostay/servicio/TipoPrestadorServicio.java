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

    /** Prestadores por tipo, para la columna «Prestadores» de la lista. */
    public Map<Integer, Integer> contarUsos() {
        try {
            return dao.contarPrestadoresPorTipo();
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    /**
     * Crea si no tiene id, o actualiza si ya lo tiene. Es una lista abierta
     * (el enunciado permite "otros que ustedes puedan sustentar"), solo no se repite el nombre.
     */
    public void guardar(TipoPrestador x) {
        try {
            String nombre = x.getNombre() == null ? "" : x.getNombre().trim().replaceAll("\\s+", " ");
            if (nombre.isEmpty()) {
                throw new ReglaNegocioException("El nombre del tipo de prestador no puede quedar vacío.");
            }
            if (nombre.length() > 40) {
                throw new ReglaNegocioException("El nombre del tipo de prestador puede tener máximo 40 caracteres.");
            }
            for (TipoPrestador otro : dao.listarTodos()) {
                if (otro.getNombre().equalsIgnoreCase(nombre) && !otro.getIdTipoPrestador().equals(x.getIdTipoPrestador())) {
                    throw new ReglaNegocioException("Ya existe el tipo de prestador «" + otro.getNombre() + "».");
                }
            }
            x.setNombre(nombre);
            if (x.getIdTipoPrestador() == null) {
                dao.crear(x);
            } else {
                buscarPorId(x.getIdTipoPrestador());
                dao.actualizar(x);
            }
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    /** Eliminar: solo un tipo que ningún prestador tenga (FK fk_prestador_tipo con RESTRICT). */
    public void eliminar(int id) {
        try {
            TipoPrestador x = buscarPorId(id);
            int prestadores = dao.contarPrestadores(id);
            if (prestadores > 0) {
                List<String> ejemplos = dao.nombresPrestadores(id, 3);
                throw new ReglaNegocioException("No se puede eliminar el tipo de prestador «" + x.getNombre() + "» porque "
                        + (prestadores == 1 ? "1 prestador es" : prestadores + " prestadores son") + " de ese tipo ("
                        + String.join(", ", ejemplos) + (prestadores > ejemplos.size() ? "..." : "")
                        + "). Primero cámbielos de tipo en Prestadores o elimínelos.");
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
}
