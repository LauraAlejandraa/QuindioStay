package co.edu.uniquindio.quindiostay.dao;

import co.edu.uniquindio.quindiostay.modelo.Atraccion;
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
 * DAO de la tabla Atraccion: es la única clase que sabe que existe esta tabla.
 * Siempre usa PreparedStatement (los ? se llenan con setString, setInt...),
 * nunca concatena texto del usuario dentro del SQL.
 */
@Repository
public class AtraccionDAO {

    private static final String COLUMNAS = "id_atraccion, nombre, descripcion, ubicacion";

    private final ConexionBD conexionBD;

    public AtraccionDAO(ConexionBD conexionBD) {
        this.conexionBD = conexionBD;
    }

    /** INSERT. Recibe la conexión para poder usarse dentro de una transacción. */
    public int crear(Connection con, Atraccion x) throws SQLException {
        String sql = "INSERT INTO Atraccion (nombre, descripcion, ubicacion) VALUES (?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, x.getNombre());
            ps.setString(2, x.getDescripcion());
            ps.setString(3, x.getUbicacion());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    x.setIdAtraccion(rs.getInt(1));   // el id lo genera MySQL (AUTO_INCREMENT)
                }
            }
        }
        return x.getIdAtraccion();
    }

    public int crear(Atraccion x) throws SQLException {
        try (Connection con = conexionBD.getConexion()) {
            return crear(con, x);
        }
    }

    public Atraccion buscarPorId(int id) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM Atraccion WHERE id_atraccion = ?";
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public List<Atraccion> listarTodos() throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM Atraccion ORDER BY nombre";
        List<Atraccion> lista = new ArrayList<>();
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
    public void actualizar(Connection con, Atraccion x) throws SQLException {
        String sql = "UPDATE Atraccion SET nombre = ?, descripcion = ?, ubicacion = ? WHERE id_atraccion = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, x.getNombre());
            ps.setString(2, x.getDescripcion());
            ps.setString(3, x.getUbicacion());
            ps.setInt(4, x.getIdAtraccion());
            ps.executeUpdate();
        }
    }

    public void actualizar(Atraccion x) throws SQLException {
        try (Connection con = conexionBD.getConexion()) {
            actualizar(con, x);
        }
    }

    /** Borra la fila. Si otra tabla depende de ella, MySQL responde con el error 1451 (RESTRICT). */
    public void eliminar(int id) throws SQLException {
        String sql = "DELETE FROM Atraccion WHERE id_atraccion = ?";
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /** Pares id -> texto para las listas desplegables y para mostrar nombres en las tablas. */
    public Map<Integer, String> listarOpciones() throws SQLException {
        String sql = "SELECT id_atraccion, nombre FROM Atraccion ORDER BY nombre";
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

    /** Atracciones afiliadas a un prestador (lo que ve el encargado). */
    public List<Atraccion> listarPorPrestador(int idPrestador) throws SQLException {
        String sql = "SELECT a.id_atraccion, a.nombre, a.descripcion, a.ubicacion FROM Atraccion a "
                + "INNER JOIN Prestador_atraccion pa ON pa.id_atraccion = a.id_atraccion "
                + "WHERE pa.id_prestador = ? ORDER BY a.nombre";
        List<Atraccion> lista = new ArrayList<>();
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idPrestador);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    /** Buscar por nombre (Tabla 39): LIKE con el texto como parámetro, nunca concatenado. */
    public List<Atraccion> buscarPorNombre(String texto) throws SQLException {
        List<Atraccion> lista = new ArrayList<>();
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement("SELECT " + COLUMNAS + " FROM Atraccion WHERE nombre LIKE ? ORDER BY nombre")) {
            ps.setString(1, "%" + texto + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    /** Convierte la fila actual del ResultSet en un objeto Atraccion. */
    Atraccion mapear(ResultSet rs) throws SQLException {
        Atraccion x = new Atraccion();
        x.setIdAtraccion(rs.getInt("id_atraccion"));
        x.setNombre(rs.getString("nombre"));
        x.setDescripcion(rs.getString("descripcion"));
        x.setUbicacion(rs.getString("ubicacion"));
        return x;
    }
}
