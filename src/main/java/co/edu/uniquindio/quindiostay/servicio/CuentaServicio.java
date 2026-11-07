package co.edu.uniquindio.quindiostay.servicio;

import co.edu.uniquindio.quindiostay.dao.CuentaDAO;
import co.edu.uniquindio.quindiostay.modelo.Cuenta;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Lógica de negocio de Cuenta. El controlador nunca llama al DAO
 * directamente: pasa por aquí, donde van las reglas que un CHECK no puede revisar.
 * Si MySQL rechaza algo (FK, CHECK, UNIQUE) se traduce a un mensaje entendible.
 */
@Service
public class CuentaServicio {

    private final CuentaDAO dao;
    private final BCryptPasswordEncoder cifrador = new BCryptPasswordEncoder();

    public CuentaServicio(CuentaDAO dao) {
        this.dao = dao;
    }

    public List<Cuenta> listarTodos() {
        try {
            return dao.listarTodos();
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    public Cuenta buscarPorId(int id) {
        try {
            Cuenta x = dao.buscarPorId(id);
            if (x == null) {
                throw new ReglaNegocioException("No existe cuenta con id " + id + ".");
            }
            return x;
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    /** Crea si no tiene id, o actualiza si ya lo tiene. */
    public void guardar(Cuenta x) {
        try {
            validar(x);
        // La contraseña nunca se guarda tal cual: se cifra con BCrypt.
        if (x.getContrasenaNueva() != null && !x.getContrasenaNueva().isBlank()) {
            x.setContrasenaCifrada(cifrador.encode(x.getContrasenaNueva()));
        } else if (x.getIdCuenta() == null) {
            throw new ReglaNegocioException("Escriba una contraseña.");
        } else {
            x.setContrasenaCifrada(dao.buscarPorId(x.getIdCuenta()).getContrasenaCifrada());
        }
            if (x.getIdCuenta() == null) {
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
    private void validar(Cuenta x) throws SQLException {
    }
}
