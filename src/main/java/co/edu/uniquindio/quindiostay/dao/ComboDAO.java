package co.edu.uniquindio.quindiostay.dao;

import co.edu.uniquindio.quindiostay.modelo.Combo;
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
 * DAO de la tabla Combo: es la única clase que sabe que existe esta tabla.
 * Siempre usa PreparedStatement (los ? se llenan con setString, setInt...),
 * nunca concatena texto del usuario dentro del SQL.
 */
@Repository
public class ComboDAO {

    private static final String COLUMNAS = "id_combo, nombre, descripcion, precio_especial, estado";

    private final ConexionBD conexionBD;

    public ComboDAO(ConexionBD conexionBD) {
        this.conexionBD = conexionBD;
    }

    /** INSERT. Recibe la conexión para poder usarse dentro de una transacción. */
    public int crear(Connection con, Combo x) throws SQLException {
        String sql = "INSERT INTO Combo (nombre, descripcion, precio_especial, estado) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, x.getNombre());
            ps.setString(2, x.getDescripcion());
            ps.setBigDecimal(3, x.getPrecioEspecial());
            ps.setString(4, x.getEstado());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    x.setIdCombo(rs.getInt(1));   // el id lo genera MySQL (AUTO_INCREMENT)
                }
            }
        }
        return x.getIdCombo();
    }

    public int crear(Combo x) throws SQLException {
        try (Connection con = conexionBD.getConexion()) {
            return crear(con, x);
        }
    }

    public Combo buscarPorId(int id) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM Combo WHERE id_combo = ?";
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public List<Combo> listarTodos() throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM Combo ORDER BY nombre";
        List<Combo> lista = new ArrayList<>();
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
    public void actualizar(Connection con, Combo x) throws SQLException {
        String sql = "UPDATE Combo SET nombre = ?, descripcion = ?, precio_especial = ?, estado = ? WHERE id_combo = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, x.getNombre());
            ps.setString(2, x.getDescripcion());
            ps.setBigDecimal(3, x.getPrecioEspecial());
            ps.setString(4, x.getEstado());
            ps.setInt(5, x.getIdCombo());
            ps.executeUpdate();
        }
    }

    public void actualizar(Combo x) throws SQLException {
        try (Connection con = conexionBD.getConexion()) {
            actualizar(con, x);
        }
    }

    /** "Eliminar" según la Tabla 39 de la Entrega 2: no se borra la fila, solo cambia estado a 'inactivo'. */
    public void eliminar(int id) throws SQLException {
        String sql = "UPDATE Combo SET estado = 'inactivo' WHERE id_combo = ?";
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /** Borrado físico (solo lo usa el servicio cuando no hay historial). */
    public void borrar(int id) throws SQLException {
        String sql = "DELETE FROM Combo WHERE id_combo = ?";
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /** Pares id -> texto para las listas desplegables y para mostrar nombres en las tablas. */
    public Map<Integer, String> listarOpciones() throws SQLException {
        String sql = "SELECT id_combo, nombre FROM Combo ORDER BY nombre";
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

    public int contarBoletas(int idCombo) throws SQLException {
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM Boleta WHERE id_combo = ?")) {
            ps.setInt(1, idCombo);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** Ids de las atracciones que forman el combo (Combo_atraccion). */
    public List<Integer> atraccionesDelCombo(int idCombo) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement("SELECT id_atraccion FROM Combo_atraccion WHERE id_combo = ? ORDER BY id_atraccion")) {
            ps.setInt(1, idCombo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt(1));
                }
            }
        }
        return ids;
    }

    /** Convierte la fila actual del ResultSet en un objeto Combo. */
    Combo mapear(ResultSet rs) throws SQLException {
        Combo x = new Combo();
        x.setIdCombo(rs.getInt("id_combo"));
        x.setNombre(rs.getString("nombre"));
        x.setDescripcion(rs.getString("descripcion"));
        x.setPrecioEspecial(rs.getBigDecimal("precio_especial"));
        x.setEstado(rs.getString("estado"));
        return x;
    }
}
