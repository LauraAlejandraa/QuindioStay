package co.edu.uniquindio.quindiostay.dao;

import co.edu.uniquindio.quindiostay.modelo.PerfilCliente;
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
 * DAO de la tabla Perfil_cliente: es la única clase que sabe que existe esta tabla.
 * Siempre usa PreparedStatement (los ? se llenan con setString, setInt...),
 * nunca concatena texto del usuario dentro del SQL.
 */
@Repository
public class PerfilClienteDAO {

    private static final String COLUMNAS = "id_perfil, idioma, canal_preferido, anticipacion_recordatorio, acepta_promociones, id_cliente";

    private final ConexionBD conexionBD;

    public PerfilClienteDAO(ConexionBD conexionBD) {
        this.conexionBD = conexionBD;
    }

    /** INSERT. Recibe la conexión para poder usarse dentro de una transacción. */
    public int crear(Connection con, PerfilCliente x) throws SQLException {
        String sql = "INSERT INTO Perfil_cliente (idioma, canal_preferido, anticipacion_recordatorio, acepta_promociones, id_cliente) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, x.getIdioma());
            ps.setString(2, x.getCanalPreferido());
            ps.setObject(3, x.getAnticipacionRecordatorio(), Types.INTEGER);
            ps.setString(4, x.getAceptaPromociones());
            ps.setObject(5, x.getIdCliente(), Types.INTEGER);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    x.setIdPerfil(rs.getInt(1));   // el id lo genera MySQL (AUTO_INCREMENT)
                }
            }
        }
        return x.getIdPerfil();
    }

    public int crear(PerfilCliente x) throws SQLException {
        try (Connection con = conexionBD.getConexion()) {
            return crear(con, x);
        }
    }

    public PerfilCliente buscarPorId(int id) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM Perfil_cliente WHERE id_perfil = ?";
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public List<PerfilCliente> listarTodos() throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM Perfil_cliente ORDER BY id_perfil";
        List<PerfilCliente> lista = new ArrayList<>();
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
    public void actualizar(Connection con, PerfilCliente x) throws SQLException {
        String sql = "UPDATE Perfil_cliente SET idioma = ?, canal_preferido = ?, anticipacion_recordatorio = ?, acepta_promociones = ?, id_cliente = ? WHERE id_perfil = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, x.getIdioma());
            ps.setString(2, x.getCanalPreferido());
            ps.setObject(3, x.getAnticipacionRecordatorio(), Types.INTEGER);
            ps.setString(4, x.getAceptaPromociones());
            ps.setObject(5, x.getIdCliente(), Types.INTEGER);
            ps.setInt(6, x.getIdPerfil());
            ps.executeUpdate();
        }
    }

    public void actualizar(PerfilCliente x) throws SQLException {
        try (Connection con = conexionBD.getConexion()) {
            actualizar(con, x);
        }
    }

    /** Borra la fila. Si otra tabla depende de ella, MySQL responde con el error 1451 (RESTRICT). */
    public void eliminar(int id) throws SQLException {
        String sql = "DELETE FROM Perfil_cliente WHERE id_perfil = ?";
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /** Pares id -> texto para las listas desplegables y para mostrar nombres en las tablas. */
    public Map<Integer, String> listarOpciones() throws SQLException {
        String sql = "SELECT id_perfil, CONCAT('Perfil ',id_perfil) FROM Perfil_cliente ORDER BY id_perfil";
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

    public PerfilCliente buscarPorCliente(int idCliente) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM Perfil_cliente WHERE id_cliente = ?";
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    /** Convierte la fila actual del ResultSet en un objeto PerfilCliente. */
    PerfilCliente mapear(ResultSet rs) throws SQLException {
        PerfilCliente x = new PerfilCliente();
        x.setIdPerfil(rs.getInt("id_perfil"));
        x.setIdioma(rs.getString("idioma"));
        x.setCanalPreferido(rs.getString("canal_preferido"));
        x.setAnticipacionRecordatorio(rs.getObject("anticipacion_recordatorio", Integer.class));
        x.setAceptaPromociones(rs.getString("acepta_promociones"));
        x.setIdCliente(rs.getObject("id_cliente", Integer.class));
        return x;
    }
}
