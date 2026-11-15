package co.edu.uniquindio.quindiostay.servicio;

import co.edu.uniquindio.quindiostay.dao.ClienteDAO;
import co.edu.uniquindio.quindiostay.dao.CuentaDAO;
import co.edu.uniquindio.quindiostay.modelo.Cliente;
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
    private final ClienteDAO clienteDAO;
    private final BCryptPasswordEncoder cifrador = new BCryptPasswordEncoder();

    public CuentaServicio(CuentaDAO dao, ClienteDAO clienteDAO) {
        this.dao = dao;
        this.clienteDAO = clienteDAO;
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
            if (x.getContrasenaNueva() != null && !x.getContrasenaNueva().isBlank() && x.getContrasenaNueva().length() < 8) {
                throw new ReglaNegocioException("La contraseña debe tener al menos 8 caracteres.");
            }
            if (x.getIdCuenta() != null) {
                // La cuenta es el acceso de UN cliente: no se pasa a otro cliente al editarla.
                x.setIdCliente(buscarPorId(x.getIdCuenta()).getIdCliente());
            }
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

    /**
     * Eliminar la CUENTA (el acceso: correo + contraseña), no el cliente.
     * Regla:
     *   - NO se elimina si el cliente tiene reservas vivas (pendientes o confirmadas),
     *     con sus boletas, combos y pagos (pendientes o no): sin cuenta no podría pagarlas, verlas, descargar sus
     *     boletas ni cancelarlas. Primero se completan o se cancelan.
     *   - SÍ se elimina si no tiene reservas, o si todas ya terminaron (completadas o
     *     canceladas). El cliente y todo su historial (reservas, boletas, combos, pagos,
     *     notificaciones) se conservan; solo deja de poder iniciar sesión.
     * Devuelve el mensaje de éxito para mostrar.
     */
    @SuppressWarnings("unchecked")
    public String eliminar(int id) {
        Cuenta cuenta = buscarPorId(id);
        try {
            Cliente c = clienteDAO.buscarPorId(cuenta.getIdCliente());
            Map<String, Object> h = clienteDAO.historial(cuenta.getIdCliente());
            List<String> vivas = (List<String>) h.get("vivas");
            String nombre = c.getPrimerNombre() + " " + c.getPrimerApellido();
            if (!vivas.isEmpty()) {
                throw new ReglaNegocioException("No se puede eliminar la cuenta de " + nombre + ": tiene "
                        + (vivas.size() == 1 ? "una reserva activa (" : vivas.size() + " reservas activas (")
                        + String.join(", ", vivas) + "). Sin cuenta no podría pagarlas, ver sus boletas ni cancelarlas. "
                        + "Primero deben completarse o cancelarse.");
            }
            dao.eliminar(id);
            int reservas = (Integer) h.get("reservas");
            return "Cuenta eliminada: " + nombre + " ya no puede iniciar sesión."
                    + (reservas > 0 ? " Sus datos y su historial (" + reservas + (reservas == 1 ? " reserva" : " reservas")
                            + ") se conservan." : " Sus datos de cliente se conservan.");
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
