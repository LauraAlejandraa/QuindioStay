package co.edu.uniquindio.quindiostay.servicio;

import co.edu.uniquindio.quindiostay.dao.RolDAO;
import co.edu.uniquindio.quindiostay.dao.UsuarioSistemaDAO;
import co.edu.uniquindio.quindiostay.modelo.UsuarioSistema;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Lógica de negocio de Usuario_sistema. El controlador nunca llama al DAO
 * directamente: pasa por aquí, donde van las reglas que un CHECK no puede revisar.
 * Si MySQL rechaza algo (FK, CHECK, UNIQUE) se traduce a un mensaje entendible.
 */
@Service
public class UsuarioSistemaServicio {

    private final UsuarioSistemaDAO dao;
    private final RolDAO rolDAO;
    private final BCryptPasswordEncoder cifrador = new BCryptPasswordEncoder();

    public UsuarioSistemaServicio(UsuarioSistemaDAO dao, RolDAO rolDAO) {
        this.dao = dao;
        this.rolDAO = rolDAO;
    }

    public List<UsuarioSistema> listarTodos() {
        try {
            return dao.listarTodos();
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    public UsuarioSistema buscarPorId(int id) {
        try {
            UsuarioSistema x = dao.buscarPorId(id);
            if (x == null) {
                throw new ReglaNegocioException("No existe usuario con id " + id + ".");
            }
            return x;
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    /** Crea si no tiene id, o actualiza si ya lo tiene. */
    public void guardar(UsuarioSistema x) {
        try {
            validar(x);
        // La contraseña nunca se guarda tal cual: se cifra con BCrypt.
        if (x.getContrasenaNueva() != null && !x.getContrasenaNueva().isBlank()) {
            x.setContrasenaCifrada(cifrador.encode(x.getContrasenaNueva()));
        } else if (x.getIdUsuario() == null) {
            throw new ReglaNegocioException("Escriba una contraseña.");
        } else {
            x.setContrasenaCifrada(dao.buscarPorId(x.getIdUsuario()).getContrasenaCifrada());
        }
            if (x.getIdUsuario() == null) {
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
    private void validar(UsuarioSistema x) throws SQLException {
        // Si el rol es 'encargado de prestador', el prestador es obligatorio; si no, va vacío.
        // No es un CHECK porque el nombre del rol está en OTRA tabla (Rol).
        String rol = rolDAO.buscarPorId(x.getIdRol()).getNombre();
        if ("encargado de prestador".equals(rol)) {
            if (x.getIdPrestador() == null) {
                throw new ReglaNegocioException("Un encargado de prestador debe tener un prestador asociado.");
            }
        } else {
            x.setIdPrestador(null);
        }
    }
}
