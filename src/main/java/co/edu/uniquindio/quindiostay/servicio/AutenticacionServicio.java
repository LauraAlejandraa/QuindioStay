package co.edu.uniquindio.quindiostay.servicio;

import co.edu.uniquindio.quindiostay.dao.*;
import co.edu.uniquindio.quindiostay.modelo.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.SQLException;

/**
 * Login (RF01).
 *  - El cliente entra con su correo y la contraseña de su Cuenta.
 *  - El usuario interno entra con su nombre de usuario.
 * La contraseña se compara con BCrypt: matches(escrita, hashGuardado).
 * No se comparan textos porque el hash tiene sal aleatoria (cada hash es distinto).
 */
@Service
public class AutenticacionServicio {

    private static final String DATOS_INCORRECTOS = "Usuario o contraseña incorrectos.";

    private final ClienteDAO clienteDAO;
    private final CuentaDAO cuentaDAO;
    private final UsuarioSistemaDAO usuarioDAO;
    private final RolDAO rolDAO;
    private final PrestadorDAO prestadorDAO;
    private final BCryptPasswordEncoder cifrador = new BCryptPasswordEncoder();

    public AutenticacionServicio(ClienteDAO clienteDAO, CuentaDAO cuentaDAO, UsuarioSistemaDAO usuarioDAO,
                                 RolDAO rolDAO, PrestadorDAO prestadorDAO) {
        this.clienteDAO = clienteDAO;
        this.cuentaDAO = cuentaDAO;
        this.usuarioDAO = usuarioDAO;
        this.rolDAO = rolDAO;
        this.prestadorDAO = prestadorDAO;
    }

    public SesionUsuario iniciarSesion(String usuario, String contrasena) {
        if (usuario == null || usuario.isBlank() || contrasena == null || contrasena.isBlank()) {
            throw new ReglaNegocioException("Escriba el usuario (o correo) y la contraseña.");
        }
        try {
            // Si tiene @ es un correo: es un cliente. Si no, es un usuario interno.
            return usuario.contains("@") ? loginCliente(usuario.trim(), contrasena)
                                         : loginInterno(usuario.trim(), contrasena);
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    private SesionUsuario loginCliente(String email, String contrasena) throws SQLException {
        Cliente cliente = clienteDAO.buscarPorEmail(email);
        if (cliente == null) {
            throw new ReglaNegocioException(DATOS_INCORRECTOS);
        }
        Cuenta cuenta = cuentaDAO.buscarPorCliente(cliente.getIdCliente());
        if (cuenta == null) {
            throw new ReglaNegocioException("Este cliente no tiene cuenta en línea. Puede crearla en «Registrarse».");
        }
        if (!cifrador.matches(contrasena, cuenta.getContrasenaCifrada())) {
            throw new ReglaNegocioException(DATOS_INCORRECTOS);
        }
        SesionUsuario s = new SesionUsuario();
        s.setRol(SesionUsuario.CLIENTE);
        s.setIdCliente(cliente.getIdCliente());
        s.setNombreMostrar(cliente.getPrimerNombre() + " " + cliente.getPrimerApellido());
        return s;
    }

    private SesionUsuario loginInterno(String nombreUsuario, String contrasena) throws SQLException {
        UsuarioSistema u = usuarioDAO.buscarPorNombreUsuario(nombreUsuario);
        if (u == null || !cifrador.matches(contrasena, u.getContrasenaCifrada())) {
            throw new ReglaNegocioException(DATOS_INCORRECTOS);
        }
        // Un usuario bloqueado no entra (Tabla 39: el usuario no se borra, se bloquea).
        if ("bloqueado".equals(u.getEstado())) {
            throw new ReglaNegocioException("El usuario está bloqueado. Comuníquese con el administrador.");
        }
        SesionUsuario s = new SesionUsuario();
        s.setRol(rolDAO.buscarPorId(u.getIdRol()).getNombre());
        s.setIdUsuario(u.getIdUsuario());
        s.setNombreMostrar(u.getNombreUsuario());
        if (u.getIdPrestador() != null) {
            s.setIdPrestador(u.getIdPrestador());
            s.setNombrePrestador(prestadorDAO.buscarPorId(u.getIdPrestador()).getNombreComercial());
        }
        return s;
    }
}
