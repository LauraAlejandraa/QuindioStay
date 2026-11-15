package co.edu.uniquindio.quindiostay.servicio;

import co.edu.uniquindio.quindiostay.dao.RolDAO;
import co.edu.uniquindio.quindiostay.modelo.Rol;
import co.edu.uniquindio.quindiostay.modelo.SesionUsuario;

import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Lógica de negocio de Rol. El controlador nunca llama al DAO
 * directamente: pasa por aquí, donde van las reglas que un CHECK no puede revisar.
 * Si MySQL rechaza algo (FK, CHECK, UNIQUE) se traduce a un mensaje entendible.
 */
@Service
public class RolServicio {

    private final RolDAO dao;


    public RolServicio(RolDAO dao) {
        this.dao = dao;
    }

    public List<Rol> listarTodos() {
        try {
            return dao.listarTodos();
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    public Rol buscarPorId(int id) {
        try {
            Rol x = dao.buscarPorId(id);
            if (x == null) {
                throw new ReglaNegocioException("No existe rol con id " + id + ".");
            }
            return x;
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    /**
     * Los roles son una lista cerrada del enunciado (CHECK ck_rol_nombre) y su nombre es el que
     * decide el menú y los permisos (ver SesionUsuario). Por eso solo hay 3 y no se editan.
     */
    public static final List<String> NOMBRES = List.of(
            SesionUsuario.ADMIN, SesionUsuario.ENCARGADO, SesionUsuario.OPERADOR);

    /** Roles de la lista cerrada que todavía no están creados (lo que ofrece «Nuevo»). */
    public List<String> nombresDisponibles() {
        List<String> existentes = listarTodos().stream().map(Rol::getNombre).toList();
        return NOMBRES.stream().filter(n -> !existentes.contains(n)).toList();
    }

    /** Usuarios por rol, para la columna «Usuarios» de la lista. */
    public Map<Integer, Integer> contarUsos() {
        try {
            return dao.contarUsuariosPorRol();
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    /** Solo crea: un rol existente no se edita (cambiarle el nombre cambiaría los permisos de sus usuarios). */
    public void guardar(Rol x) {
        if (x.getIdRol() != null) {
            throw new ReglaNegocioException(MENSAJE_NO_EDITABLE);
        }
        String nombre = x.getNombre() == null ? "" : x.getNombre().trim();
        if (nombre.isEmpty()) {
            throw new ReglaNegocioException("Escoja el rol que va a crear.");
        }
        if (!NOMBRES.contains(nombre)) {
            throw new ReglaNegocioException("La aplicación solo maneja 3 roles fijos: "
                    + String.join(", ", NOMBRES) + ".");
        }
        if (!nombresDisponibles().contains(nombre)) {
            throw new ReglaNegocioException("El rol «" + nombre + "» ya existe. "
                    + (nombresDisponibles().isEmpty() ? "Los 3 roles de la aplicación ya están creados, no hay ninguno nuevo para registrar."
                                                     : "Solo falta crear: " + String.join(", ", nombresDisponibles()) + "."));
        }
        x.setNombre(nombre);
        try {
            dao.crear(x);
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    public static final String MENSAJE_NO_EDITABLE = "Los roles no se editan: el nombre del rol es el que decide "
            + "el menú y los permisos de sus usuarios. Para cambiar lo que puede hacer una persona, cámbiele el rol en Usuarios del sistema.";

    /** Eliminar: solo un rol que ningún usuario tenga (FK fk_usuario_rol con RESTRICT). */
    public void eliminar(int id) {
        try {
            Rol x = buscarPorId(id);
            int usuarios = dao.contarUsuarios(id);
            if (usuarios > 0) {
                throw new ReglaNegocioException("No se puede eliminar el rol «" + x.getNombre() + "» porque "
                        + (usuarios == 1 ? "1 usuario del sistema lo tiene" : usuarios + " usuarios del sistema lo tienen")
                        + " asignado. Primero cámbieles el rol o elimine esos usuarios.");
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
