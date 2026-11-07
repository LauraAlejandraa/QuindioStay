package co.edu.uniquindio.quindiostay.dao;

import co.edu.uniquindio.quindiostay.modelo.UsuarioSistema;
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
 * DAO de la tabla Usuario_sistema: es la única clase que sabe que existe esta tabla.
 * Siempre usa PreparedStatement (los ? se llenan con setString, setInt...),
 * nunca concatena texto del usuario dentro del SQL.
 */
@Repository
public class UsuarioSistemaDAO {

    private static final String COLUMNAS = "id_usuario, nombre_usuario, contrasena_cifrada, estado, id_rol, id_prestador";

    private final ConexionBD conexionBD;

    public UsuarioSistemaDAO(ConexionBD conexionBD) {
        this.conexionBD = conexionBD;
    }

    /** INSERT. Recibe la conexión para poder usarse dentro de una transacción. */
    public int crear(Connection con, UsuarioSistema x) throws SQLException {
        String sql = "INSERT INTO Usuario_sistema (nombre_usuario, contrasena_cifrada, estado, id_rol, id_prestador) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, x.getNombreUsuario());
            ps.setString(2, x.getContrasenaCifrada());
            ps.setString(3, x.getEstado());
            ps.setObject(4, x.getIdRol(), Types.INTEGER);
            ps.setObject(5, x.getIdPrestador(), Types.INTEGER);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    x.setIdUsuario(rs.getInt(1));   // el id lo genera MySQL (AUTO_INCREMENT)
                }
            }
        }
        return x.getIdUsuario();
    }

    public int crear(UsuarioSistema x) throws SQLException {
        try (Connection con = conexionBD.getConexion()) {
            return crear(con, x);
        }
    }

    public UsuarioSistema buscarPorId(int id) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM Usuario_sistema WHERE id_usuario = ?";
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public List<UsuarioSistema> listarTodos() throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM Usuario_sistema ORDER BY nombre_usuario";
        List<UsuarioSistema> lista = new ArrayList<>();
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
    public void actualizar(Connection con, UsuarioSistema x) throws SQLException {
        String sql = "UPDATE Usuario_sistema SET nombre_usuario = ?, contrasena_cifrada = ?, estado = ?, id_rol = ?, id_prestador = ? WHERE id_usuario = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, x.getNombreUsuario());
            ps.setString(2, x.getContrasenaCifrada());
            ps.setString(3, x.getEstado());
            ps.setObject(4, x.getIdRol(), Types.INTEGER);
            ps.setObject(5, x.getIdPrestador(), Types.INTEGER);
            ps.setInt(6, x.getIdUsuario());
            ps.executeUpdate();
        }
    }

    public void actualizar(UsuarioSistema x) throws SQLException {
        try (Connection con = conexionBD.getConexion()) {
            actualizar(con, x);
        }
    }

    /** "Eliminar" según la Tabla 39 de la Entrega 2: no se borra la fila, solo cambia estado a 'bloqueado'. */
    public void eliminar(int id) throws SQLException {
        String sql = "UPDATE Usuario_sistema SET estado = 'bloqueado' WHERE id_usuario = ?";
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /** Borrado físico (solo lo usa el servicio cuando no hay historial). */
    public void borrar(int id) throws SQLException {
        String sql = "DELETE FROM Usuario_sistema WHERE id_usuario = ?";
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /** Pares id -> texto para las listas desplegables y para mostrar nombres en las tablas. */
    public Map<Integer, String> listarOpciones() throws SQLException {
        String sql = "SELECT id_usuario, nombre_usuario FROM Usuario_sistema ORDER BY nombre_usuario";
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

    /** Login del usuario interno: busca por nombre_usuario (es UNIQUE). */
    public UsuarioSistema buscarPorNombreUsuario(String nombreUsuario) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM Usuario_sistema WHERE nombre_usuario = ?";
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombreUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    /** Convierte la fila actual del ResultSet en un objeto UsuarioSistema. */
    UsuarioSistema mapear(ResultSet rs) throws SQLException {
        UsuarioSistema x = new UsuarioSistema();
        x.setIdUsuario(rs.getInt("id_usuario"));
        x.setNombreUsuario(rs.getString("nombre_usuario"));
        x.setContrasenaCifrada(rs.getString("contrasena_cifrada"));
        x.setEstado(rs.getString("estado"));
        x.setIdRol(rs.getObject("id_rol", Integer.class));
        x.setIdPrestador(rs.getObject("id_prestador", Integer.class));
        return x;
    }
}
