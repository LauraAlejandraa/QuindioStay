package co.edu.uniquindio.quindiostay.servicio;

import co.edu.uniquindio.quindiostay.dao.ClienteDAO;
import co.edu.uniquindio.quindiostay.dao.ConexionBD;
import co.edu.uniquindio.quindiostay.dao.CuentaDAO;
import co.edu.uniquindio.quindiostay.dao.PerfilClienteDAO;
import co.edu.uniquindio.quindiostay.modelo.Cliente;
import co.edu.uniquindio.quindiostay.modelo.Cuenta;
import co.edu.uniquindio.quindiostay.modelo.PerfilCliente;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.time.Period;

/**
 * Lógica de negocio de Cliente, incluido el registro de un cliente nuevo
 * (Cliente + Cuenta + Perfil_cliente en UNA sola transacción).
 */
@Service
public class ClienteServicio {

    private final ClienteDAO dao;
    private final CuentaDAO cuentaDAO;
    private final PerfilClienteDAO perfilDAO;
    private final ConexionBD conexionBD;
    private final BCryptPasswordEncoder cifrador = new BCryptPasswordEncoder();

    public ClienteServicio(ClienteDAO dao, CuentaDAO cuentaDAO, PerfilClienteDAO perfilDAO, ConexionBD conexionBD) {
        this.dao = dao;
        this.cuentaDAO = cuentaDAO;
        this.perfilDAO = perfilDAO;
        this.conexionBD = conexionBD;
    }

    public List<Cliente> listarTodos() {
        try {
            return dao.listarTodos();
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    public Cliente buscarPorId(int id) {
        try {
            Cliente x = dao.buscarPorId(id);
            if (x == null) {
                throw new ReglaNegocioException("No existe cliente con id " + id + ".");
            }
            return x;
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    public void guardar(Cliente x) {
        validar(x);
        try {
            if (x.getIdCliente() == null) {
                dao.crear(x);
            } else {
                dao.actualizar(x);
            }
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    /**
     * Registro de un cliente nuevo desde la pantalla «Registrarse».
     * Las tres tablas se guardan juntas: si una falla (por ejemplo, el correo
     * ya existe), se hace ROLLBACK y no queda nada a medias.
     */
    public void registrar(Cliente cliente, String contrasena, PerfilCliente perfil) {
        validar(cliente);
        if (contrasena == null || contrasena.length() < 8) {
            throw new ReglaNegocioException("La contraseña debe tener al menos 8 caracteres.");
        }
        try (Connection con = conexionBD.getConexion()) {
            con.setAutoCommit(false);           // empieza la transacción
            try {
                int idCliente = dao.crear(con, cliente);

                Cuenta cuenta = new Cuenta();
                cuenta.setIdCliente(idCliente);
                cuenta.setContrasenaCifrada(cifrador.encode(contrasena));   // BCrypt
                cuentaDAO.crear(con, cuenta);

                perfil.setIdCliente(idCliente);
                perfilDAO.crear(con, perfil);

                con.commit();                    // las tres filas quedan guardadas
            } catch (SQLException e) {
                con.rollback();                  // no queda ninguna
                throw e;
            }
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    /** Tabla 39: un cliente solo se borra si no tiene reservas (su cuenta y perfil se van por CASCADE). */
    public void eliminar(int id) {
        try {
            if (dao.contarReservas(id) > 0) {
                throw new ReglaNegocioException("El cliente tiene reservas: es historial y no se puede eliminar.");
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


    /**
     * La fecha de nacimiento no puede ser posterior a hoy, el documento debe tener un
     * formato válido según su tipo, y la cédula de ciudadanía exige ser mayor de edad.
     * Ninguna va en un CHECK: dependen de la fecha de HOY (no determinista en MySQL) o
     * cruzan con otra columna de la misma fila (tipo_documento).
     */
    private void validar(Cliente x) {
        if (x.getFechaNacimiento() != null && x.getFechaNacimiento().isAfter(LocalDate.now())) {
            throw new ReglaNegocioException("La fecha de nacimiento no puede ser posterior a hoy.");
        }
        validarDocumento(x);
        if (x.getFechaNacimiento() != null && "cédula de ciudadanía".equals(x.getTipoDocumento())) {
            int edad = Period.between(x.getFechaNacimiento(), LocalDate.now()).getYears();
            if (edad < 18) {
                throw new ReglaNegocioException("Para registrarse con cédula de ciudadanía hay que ser mayor de edad (18 años). Use tarjeta de identidad si es menor.");
            }
        }
    }

    /**
     * El pasaporte sí puede combinar letras y números (así son los pasaportes reales),
     * por eso solo se exige "solo dígitos" para los documentos colombianos numéricos.
     */
    private void validarDocumento(Cliente x) {
        String tipo = x.getTipoDocumento();
        String numero = x.getNumeroDocumento();
        if (numero == null || tipo == null) {
            return;
        }
        boolean esNumerico = tipo.equals("cédula de ciudadanía") || tipo.equals("cédula de extranjería")
                || tipo.equals("tarjeta de identidad");
        if (esNumerico) {
            if (!numero.matches("[0-9]{6,15}")) {
                throw new ReglaNegocioException("El número de documento debe tener solo dígitos, entre 6 y 15.");
            }
        } else if (numero.length() < 6) {
            throw new ReglaNegocioException("El número de documento debe tener al menos 6 caracteres.");
        }
    }
}
