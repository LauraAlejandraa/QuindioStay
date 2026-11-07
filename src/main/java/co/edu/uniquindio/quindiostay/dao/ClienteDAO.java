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
