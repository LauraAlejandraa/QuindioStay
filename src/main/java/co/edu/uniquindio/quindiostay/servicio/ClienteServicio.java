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

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Map;

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

    /**
     * Eliminar un cliente (pestaña Clientes). Regla (Tabla 39: el historial no se borra):
     *   - SÍ se elimina si no tiene reservas (de ningún estado) ni notificaciones:
     *     por ejemplo, alguien que se registró y nunca compró. Su cuenta y su perfil
     *     se van con él (ON DELETE CASCADE).
     *   - NO se elimina si tiene reservas (pendientes, confirmadas, completadas o
     *     canceladas), y con ellas sus boletas, combos comprados y pagos; ni si tiene
     *     notificaciones. Es historial contable.
     * Si lo que se quiere es que ya no entre al sistema, se elimina su CUENTA
     * (Núcleo → Cuentas de clientes), con su propia regla (CuentaServicio.eliminar).
     */
    public void eliminar(int id) {
        Cliente c = buscarPorId(id);
        try {
            String motivo = motivoNoEliminar(c, dao.historial(id));
            if (motivo != null) {
                throw new ReglaNegocioException(motivo);
            }
            dao.eliminar(id);
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    @SuppressWarnings("unchecked")
    static String motivoNoEliminar(Cliente c, Map<String, Object> h) {
        String nombre = nombre(c);
        int reservas = (Integer) h.get("reservas");
        int notificaciones = (Integer) h.get("notificaciones");
        if (reservas > 0) {
            Map<String, Integer> porEstado = (Map<String, Integer>) h.get("porEstado");
            StringBuilder detalle = new StringBuilder();
            porEstado.forEach((estado, n) -> {
                if (n > 0) {
                    detalle.append(detalle.length() == 0 ? "" : ", ").append(n).append(" ").append(estado)
                           .append(n == 1 ? "" : "s");
                }
            });
            BigDecimal pagado = (BigDecimal) h.get("pagado");
            return "No se puede eliminar a " + nombre + ": tiene " + reservas + (reservas == 1 ? " reserva" : " reservas")
                    + " (" + detalle + ")"
                    + (pagado.signum() > 0 ? " y pagos por $" + String.format("%,.0f", pagado).replace(',', '.') : "")
                    + ". Las reservas, sus boletas y combos y los pagos son historial y no se borran. "
                    + "Si no debe volver a entrar, elimine su cuenta en «Cuentas de clientes» "
                    + "(sus datos y su historial se conservan).";
        }
        if (notificaciones > 0) {
            return "No se puede eliminar a " + nombre + ": tiene " + notificaciones
                    + (notificaciones == 1 ? " notificación" : " notificaciones") + " (historial; las notificaciones no se borran). "
                    + "Si no debe volver a entrar, elimine su cuenta en «Cuentas de clientes».";
        }
        return null;
    }

    static String nombre(Cliente c) {
        return c.getPrimerNombre() + " " + c.getPrimerApellido() + " (doc. " + c.getNumeroDocumento() + ")";
    }

    public Map<Integer, String> listarOpciones() {
        try {
            return dao.listarOpciones();
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    /** Nadie menor de 7 años puede ser cliente, con ningún documento. */
    public static final int EDAD_MINIMA_CLIENTE = 7;

    /**
     * La fecha de nacimiento no puede ser posterior a hoy, el documento debe tener un
     * formato válido según su tipo, y la edad debe corresponder al tipo de documento.
     * La base de datos respalda el formato (ck_cliente_numero_doc) y la edad el día del
     * registro (ck_cliente_edad_doc, con fecha_registro porque MySQL no permite CURDATE()
     * en un CHECK); aquí se revisan con la fecha de HOY para dar un mensaje claro.
     */
    private void validar(Cliente x) {
        if (x.getFechaNacimiento() != null && x.getFechaNacimiento().isAfter(LocalDate.now())) {
            throw new ReglaNegocioException("La fecha de nacimiento no puede ser posterior a hoy.");
        }
        validarNombre("El primer nombre", x.getPrimerNombre(), true);
        validarNombre("El segundo nombre", x.getSegundoNombre(), false);
        validarNombre("El primer apellido", x.getPrimerApellido(), true);
        validarNombre("El segundo apellido", x.getSegundoApellido(), false);
        validarDocumento(x);
        if (x.getFechaNacimiento() != null && "cédula de ciudadanía".equals(x.getTipoDocumento())) {
            int edad = Period.between(x.getFechaNacimiento(), LocalDate.now()).getYears();
            if (edad < 18) {
                throw new ReglaNegocioException("Para registrarse con cédula de ciudadanía hay que ser mayor de edad (18 años). Use tarjeta de identidad si es menor.");
            }
        }
        if (x.getFechaNacimiento() != null && "tarjeta de identidad".equals(x.getTipoDocumento())) {
            int edad = Period.between(x.getFechaNacimiento(), LocalDate.now()).getYears();
            if (edad < 7 || edad > 17) {
                throw new ReglaNegocioException("La tarjeta de identidad es solo para edades entre 7 y 17 años. Use cédula de ciudadanía si ya es mayor de edad.");
            }
        }
        // Extranjeros: la misma edad mínima que la tarjeta de identidad (7 años). Un niño
        // menor de 7 no es cliente: va como boleta de niño en la reserva de un adulto.
        if (x.getFechaNacimiento() != null
                && ("cédula de extranjería".equals(x.getTipoDocumento()) || "pasaporte".equals(x.getTipoDocumento()))) {
            int edad = Period.between(x.getFechaNacimiento(), LocalDate.now()).getYears();
            if (edad < EDAD_MINIMA_CLIENTE) {
                throw new ReglaNegocioException("Para registrarse con " + x.getTipoDocumento() + " hay que tener al menos "
                        + EDAD_MINIMA_CLIENTE + " años. Un niño menor de " + EDAD_MINIMA_CLIENTE
                        + " años no se registra: entra con una boleta de niño en la reserva de un adulto.");
            }
        }
    }

    /** Solo letras (con tildes y ñ) y espacios; el segundo nombre/apellido puede ir vacío. */
    private void validarNombre(String etiqueta, String valor, boolean obligatorio) {
        if (valor == null || valor.isBlank()) {
            if (obligatorio) {
                throw new ReglaNegocioException(etiqueta + " es obligatorio.");
            }
            return;
        }
        if (!valor.matches("[\\p{L} ]+")) {
            throw new ReglaNegocioException(etiqueta + " solo puede tener letras (sin números ni caracteres especiales).");
        }
    }

    /**
     * El pasaporte sí puede combinar letras MAYÚSCULAS y números (así son los
     * pasaportes reales); los demás documentos son solo dígitos. Son las mismas
     * reglas que ahora exige el CHECK ck_cliente_numero_doc en la base de datos
     * (Parte A): se repiten aquí para que el mensaje de error sea inmediato.
     */
    private void validarDocumento(Cliente x) {
        String tipo = x.getTipoDocumento();
        String numero = x.getNumeroDocumento();
        if (numero == null || tipo == null) {
            return;
        }
        if ("pasaporte".equals(tipo)) {
            if (!numero.matches("[A-Z0-9]{5,15}")) {
                throw new ReglaNegocioException("El número de pasaporte debe tener solo letras mayúsculas y dígitos, entre 5 y 15 caracteres.");
            }
        } else if (!numero.matches("[0-9]{6,15}")) {
            throw new ReglaNegocioException("El número de documento debe tener solo dígitos, entre 6 y 15.");
        }
        // El número es único sin importar el tipo (TI -> CC conserva el número).
        // La BD lo respalda con UNIQUE (numero_documento); se revisa aquí para dar el mensaje completo.
        try {
            if (dao.existeNumeroDocumento(numero, x.getIdCliente())) {
                throw new ReglaNegocioException("Ya existe un cliente con el número de documento " + numero
                        + " (aunque sea con otro tipo de documento). El número de documento no se puede repetir.");
            }
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }
}