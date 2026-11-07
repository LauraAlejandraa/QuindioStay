package co.edu.uniquindio.quindiostay.servicio;

import co.edu.uniquindio.quindiostay.dao.FuncionDAO;
import co.edu.uniquindio.quindiostay.modelo.Funcion;

import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Lógica de negocio de Funcion. El controlador nunca llama al DAO
 * directamente: pasa por aquí, donde van las reglas que un CHECK no puede revisar.
 * Si MySQL rechaza algo (FK, CHECK, UNIQUE) se traduce a un mensaje entendible.
 */
@Service
public class FuncionServicio {

    private final FuncionDAO dao;


    public FuncionServicio(FuncionDAO dao) {
        this.dao = dao;
    }

    public List<Funcion> listarTodos() {
        try {
            return dao.listarTodos();
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    public Funcion buscarPorId(int id) {
        try {
            Funcion x = dao.buscarPorId(id);
            if (x == null) {
                throw new ReglaNegocioException("No existe función con id " + id + ".");
            }
            return x;
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    /** Crea si no tiene id, o actualiza si ya lo tiene. */
    public void guardar(Funcion x) {
        try {
            validar(x);
            if (x.getIdFuncion() == null) {
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
            // Una función con boletas vendidas no se cancela desde aquí: primero se cancelan sus reservas.
            if (dao.contarBoletasVendidas(id) > 0) {
                throw new ReglaNegocioException("La función tiene boletas vendidas: cancele primero esas reservas para devolver el dinero.");
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

    public List<Funcion> listarPorPrestador(int idPrestador) {
        try {
            return dao.listarPorPrestador(idPrestador);
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    /** Reglas que miran otras tablas u otras filas (no caben en un CHECK). */
    private void validar(Funcion x) throws SQLException {
    }
}
