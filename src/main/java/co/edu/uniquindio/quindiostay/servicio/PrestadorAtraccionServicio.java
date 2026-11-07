package co.edu.uniquindio.quindiostay.servicio;

import co.edu.uniquindio.quindiostay.dao.PrestadorAtraccionDAO;
import co.edu.uniquindio.quindiostay.modelo.PrestadorAtraccion;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;

/** Afiliación de prestadores a atracciones (RF06). */
@Service
public class PrestadorAtraccionServicio {

    private final PrestadorAtraccionDAO dao;

    public PrestadorAtraccionServicio(PrestadorAtraccionDAO dao) {
        this.dao = dao;
    }

    public List<PrestadorAtraccion> listarTodos() {
        try {
            return dao.listarTodos();
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    public PrestadorAtraccion buscarPorId(int idPrestador, int idAtraccion) {
        try {
            PrestadorAtraccion x = dao.buscarPorId(idPrestador, idAtraccion);
            if (x == null) {
                throw new ReglaNegocioException("No existe esa afiliación.");
            }
            return x;
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    public void guardar(PrestadorAtraccion x) {
        try {
            if (x.isExistente()) {
                dao.actualizar(x);
            } else {
                dao.crear(x);   // si la pareja ya existe, MySQL responde 1062 (PK compuesta)
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                throw new ReglaNegocioException("Ese prestador ya está afiliado a esa atracción (la PK compuesta no se repite).");
            }
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    public void eliminar(int idPrestador, int idAtraccion) {
        try {
            if (dao.contarVentas(idPrestador, idAtraccion) > 0) {
                throw new ReglaNegocioException("El prestador ya vendió boletas de esa atracción: la afiliación es historial y no se elimina.");
            }
            dao.eliminar(idPrestador, idAtraccion);
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }
}
