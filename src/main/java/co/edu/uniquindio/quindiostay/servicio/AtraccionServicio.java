package co.edu.uniquindio.quindiostay.servicio;

import co.edu.uniquindio.quindiostay.dao.AtraccionDAO;
import co.edu.uniquindio.quindiostay.dao.ConexionBD;
import co.edu.uniquindio.quindiostay.dao.PrestadorAtraccionDAO;
import co.edu.uniquindio.quindiostay.modelo.PrestadorAtraccion;
import java.sql.Connection;
import java.time.LocalDate;
import co.edu.uniquindio.quindiostay.modelo.Atraccion;

import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Lógica de negocio de Atraccion. El controlador nunca llama al DAO
 * directamente: pasa por aquí, donde van las reglas que un CHECK no puede revisar.
 * Si MySQL rechaza algo (FK, CHECK, UNIQUE) se traduce a un mensaje entendible.
 */
@Service
public class AtraccionServicio {

    private final AtraccionDAO dao;
    private final PrestadorAtraccionDAO afiliacionDAO;
    private final ConexionBD conexionBD;


    public AtraccionServicio(AtraccionDAO dao, PrestadorAtraccionDAO afiliacionDAO, ConexionBD conexionBD) {
        this.dao = dao;
        this.afiliacionDAO = afiliacionDAO;
        this.conexionBD = conexionBD;
    }

    public List<Atraccion> listarTodos() {
        try {
            return dao.listarTodos();
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    public Atraccion buscarPorId(int id) {
        try {
            Atraccion x = dao.buscarPorId(id);
            if (x == null) {
                throw new ReglaNegocioException("No existe atracción con id " + id + ".");
            }
            return x;
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    /** Crea si no tiene id, o actualiza si ya lo tiene. */
    public void guardar(Atraccion x) {
        try {
            validar(x);
            if (x.getIdAtraccion() == null) {
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

    /** Lo que ve el encargado: solo las atracciones afiliadas a su prestador. */
    public List<Atraccion> listarPorPrestador(int idPrestador) {
        try {
            return dao.listarPorPrestador(idPrestador);
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    public List<Atraccion> buscarPorNombre(String texto) {
        try {
            return dao.buscarPorNombre(texto);
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    public boolean perteneceAPrestador(int idAtraccion, int idPrestador) {
        try {
            return afiliacionDAO.estaAfiliado(idPrestador, idAtraccion);
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    /**
     * El encargado crea una atracción: se guarda la atracción y su afiliación a su
     * prestador en una sola transacción (toda atracción tiene al menos un prestador).
     */
    public void crearParaPrestador(Atraccion x, int idPrestador) {
        try (Connection con = conexionBD.getConexion()) {
            con.setAutoCommit(false);
            try {
                int id = dao.crear(con, x);
                PrestadorAtraccion pa = new PrestadorAtraccion();
                pa.setIdPrestador(idPrestador);
                pa.setIdAtraccion(id);
                pa.setFechaAfiliacion(LocalDate.now());
                afiliacionDAO.crear(con, pa);
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    /** Reglas que miran otras tablas u otras filas (no caben en un CHECK). */
    private void validar(Atraccion x) throws SQLException {
    }
}
