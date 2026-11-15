package co.edu.uniquindio.quindiostay.dao;

import co.edu.uniquindio.quindiostay.modelo.Cliente;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO de la tabla Cliente: es la única clase que sabe que existe esta tabla.
 * Siempre usa PreparedStatement (los ? se llenan con setString, setInt...),
 * nunca concatena texto del usuario dentro del SQL.
 */
@Repository
public class ClienteDAO {

    private static final String COLUMNAS = "id_cliente, primer_nombre, segundo_nombre, primer_apellido, segundo_apellido, tipo_documento, numero_documento, email, telefono, fecha_nacimiento, ciudad_origen";

    private final ConexionBD conexionBD;

    public ClienteDAO(ConexionBD conexionBD) {
        this.conexionBD = conexionBD;
    }

    /** INSERT. Recibe la conexión para poder usarse dentro de una transacción. */
    public int crear(Connection con, Cliente x) throws SQLException {
        String sql = "INSERT INTO Cliente (primer_nombre, segundo_nombre, primer_apellido, segundo_apellido, tipo_documento, numero_documento, email, telefono, fecha_nacimiento, ciudad_origen) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, x.getPrimerNombre());
            ps.setString(2, JdbcUtil.vacioANull(x.getSegundoNombre()));
            ps.setString(3, x.getPrimerApellido());
            ps.setString(4, JdbcUtil.vacioANull(x.getSegundoApellido()));
            ps.setString(5, x.getTipoDocumento());
            ps.setString(6, x.getNumeroDocumento());
            ps.setString(7, x.getEmail());
            ps.setString(8, x.getTelefono());
            ps.setObject(9, x.getFechaNacimiento());
            ps.setString(10, x.getCiudadOrigen());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    x.setIdCliente(rs.getInt(1));   // el id lo genera MySQL (AUTO_INCREMENT)
                }
            }
        }
        return x.getIdCliente();
    }

    public int crear(Cliente x) throws SQLException {
        try (Connection con = conexionBD.getConexion()) {
            return crear(con, x);
        }
    }

    public Cliente buscarPorId(int id) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM Cliente WHERE id_cliente = ?";
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public List<Cliente> listarTodos() throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM Cliente ORDER BY primer_apellido, primer_nombre";
        List<Cliente> lista = new ArrayList<>();
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    /** UPDATE de todas las columnas, menos la clave primaria. */
    public void actualizar(Connection con, Cliente x) throws SQLException {
        String sql = "UPDATE Cliente SET primer_nombre = ?, segundo_nombre = ?, primer_apellido = ?, segundo_apellido = ?, tipo_documento = ?, numero_documento = ?, email = ?, telefono = ?, fecha_nacimiento = ?, ciudad_origen = ? WHERE id_cliente = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, x.getPrimerNombre());
            ps.setString(2, JdbcUtil.vacioANull(x.getSegundoNombre()));
            ps.setString(3, x.getPrimerApellido());
            ps.setString(4, JdbcUtil.vacioANull(x.getSegundoApellido()));
            ps.setString(5, x.getTipoDocumento());
            ps.setString(6, x.getNumeroDocumento());
            ps.setString(7, x.getEmail());
            ps.setString(8, x.getTelefono());
            ps.setObject(9, x.getFechaNacimiento());
            ps.setString(10, x.getCiudadOrigen());
            ps.setInt(11, x.getIdCliente());
            ps.executeUpdate();
        }
    }

    public void actualizar(Cliente x) throws SQLException {
        try (Connection con = conexionBD.getConexion()) {
            actualizar(con, x);
        }
    }

    /** Borra la fila. Si otra tabla depende de ella, MySQL responde con el error 1451 (RESTRICT). */
    public void eliminar(int id) throws SQLException {
        String sql = "DELETE FROM Cliente WHERE id_cliente = ?";
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /** Pares id -> texto para las listas desplegables y para mostrar nombres en las tablas. */
    public Map<Integer, String> listarOpciones() throws SQLException {
        String sql = "SELECT id_cliente, CONCAT(primer_nombre,' ',primer_apellido,' · ',numero_documento) FROM Cliente ORDER BY id_cliente";
        Map<Integer, String> opciones = new LinkedHashMap<>();
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                opciones.put(rs.getInt(1), rs.getString(2));
            }
        }
        return opciones;
    }

    /** Login del cliente y registro: el correo es el usuario de la cuenta (es UNIQUE). */
    public Cliente buscarPorEmail(String email) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM Cliente WHERE email = ?";
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    /**
     * ¿Otro cliente ya usa ese número de documento? Se compara SOLO el número,
     * sin importar el tipo: una persona conserva su número al pasar de tarjeta
     * de identidad a cédula. idExcluir es el propio cliente al editar (o null).
     */
    public boolean existeNumeroDocumento(String numero, Integer idExcluir) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Cliente WHERE numero_documento = ? AND id_cliente <> ?";
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, numero);
            ps.setInt(2, idExcluir == null ? -1 : idExcluir);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    /** ¿Tiene reservas? (Tabla 39: el cliente solo se borra si no tiene reservas). */
    public int contarReservas(int idCliente) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Reserva WHERE id_cliente = ?";
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /**
     * Historial del cliente para decidir si se puede eliminar él o su cuenta:
     * reservas por estado (pendiente, confirmada, completada, cancelada), número de
     * reservas vivas (pendiente/confirmada) con sus ids, total pagado (exitoso o
     * reembolsado) y notificaciones (de promoción o de sus reservas).
     */
    public Map<String, Object> historial(int idCliente) throws SQLException {
        Map<String, Object> h = new LinkedHashMap<>();
        try (Connection con = conexionBD.getConexion()) {
            Map<String, Integer> porEstado = new LinkedHashMap<>();
            for (String e : List.of("pendiente", "confirmada", "completada", "cancelada")) {
                porEstado.put(e, 0);
            }
            List<String> vivas = new ArrayList<>();
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT id_reserva, estado FROM Reserva WHERE id_cliente = ? ORDER BY id_reserva")) {
                ps.setInt(1, idCliente);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String e = rs.getString("estado");
                        porEstado.merge(e, 1, Integer::sum);
                        if ("pendiente".equals(e) || "confirmada".equals(e)) {
                            vivas.add("#" + rs.getInt("id_reserva") + " " + e);
                        }
                    }
                }
            }
            h.put("porEstado", porEstado);
            h.put("reservas", porEstado.values().stream().mapToInt(Integer::intValue).sum());
            h.put("vivas", vivas);
            h.put("pagado", escalar(con, "SELECT COALESCE(SUM(p.monto), 0) FROM Pago p INNER JOIN Reserva r ON r.id_reserva = p.id_reserva "
                    + "WHERE r.id_cliente = ? AND p.estado IN ('exitoso', 'reembolsado')", idCliente));
            h.put("notificaciones", ((Number) escalar(con, "SELECT COUNT(*) FROM Notificacion n LEFT JOIN Reserva r ON r.id_reserva = n.id_reserva "
                    + "WHERE n.id_cliente = ? OR r.id_cliente = ?", idCliente, idCliente)).intValue());
        }
        return h;
    }

    private Object escalar(Connection con, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getObject(1);
            }
        }
    }

    /** Convierte la fila actual del ResultSet en un objeto Cliente. */
    Cliente mapear(ResultSet rs) throws SQLException {
        Cliente x = new Cliente();
        x.setIdCliente(rs.getInt("id_cliente"));
        x.setPrimerNombre(rs.getString("primer_nombre"));
        x.setSegundoNombre(rs.getString("segundo_nombre"));
        x.setPrimerApellido(rs.getString("primer_apellido"));
        x.setSegundoApellido(rs.getString("segundo_apellido"));
        x.setTipoDocumento(rs.getString("tipo_documento"));
        x.setNumeroDocumento(rs.getString("numero_documento"));
        x.setEmail(rs.getString("email"));
        x.setTelefono(rs.getString("telefono"));
        x.setFechaNacimiento(rs.getObject("fecha_nacimiento", LocalDate.class));
        x.setCiudadOrigen(rs.getString("ciudad_origen"));
        return x;
    }
}
