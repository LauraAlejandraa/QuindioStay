package co.edu.uniquindio.quindiostay.dao;

import co.edu.uniquindio.quindiostay.modelo.ComboAtraccion;
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
 * DAO de la tabla Combo_atraccion: es la única clase que sabe que existe esta tabla.
 * Siempre usa PreparedStatement (los ? se llenan con setString, setInt...),
 * nunca concatena texto del usuario dentro del SQL.
 */
@Repository
public class ComboAtraccionDAO {

    private static final String COLUMNAS = "id_combo_atraccion, id_combo, id_atraccion";

    private final ConexionBD conexionBD;

    public ComboAtraccionDAO(ConexionBD conexionBD) {
        this.conexionBD = conexionBD;
    }

    /** INSERT. Recibe la conexión para poder usarse dentro de una transacción. */
    public int crear(Connection con, ComboAtraccion x) throws SQLException {
        String sql = "INSERT INTO Combo_atraccion (id_combo, id_atraccion) VALUES (?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, x.getIdCombo(), Types.INTEGER);
            ps.setObject(2, x.getIdAtraccion(), Types.INTEGER);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    x.setIdComboAtraccion(rs.getInt(1));   // el id lo genera MySQL (AUTO_INCREMENT)
                }
            }
        }
        return x.getIdComboAtraccion();
    }

    public int crear(ComboAtraccion x) throws SQLException {
        try (Connection con = conexionBD.getConexion()) {
            return crear(con, x);
        }
    }

    public ComboAtraccion buscarPorId(int id) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM Combo_atraccion WHERE id_combo_atraccion = ?";
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public List<ComboAtraccion> listarTodos() throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM Combo_atraccion ORDER BY id_combo, id_atraccion";
        List<ComboAtraccion> lista = new ArrayList<>();
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
    public void actualizar(Connection con, ComboAtraccion x) throws SQLException {
        String sql = "UPDATE Combo_atraccion SET id_combo = ?, id_atraccion = ? WHERE id_combo_atraccion = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, x.getIdCombo(), Types.INTEGER);
            ps.setObject(2, x.getIdAtraccion(), Types.INTEGER);
            ps.setInt(3, x.getIdComboAtraccion());
            ps.executeUpdate();
        }
    }

    public void actualizar(ComboAtraccion x) throws SQLException {
        try (Connection con = conexionBD.getConexion()) {
            actualizar(con, x);
        }
    }

    /** Borra la fila. Si otra tabla depende de ella, MySQL responde con el error 1451 (RESTRICT). */
    public void eliminar(int id) throws SQLException {
        String sql = "DELETE FROM Combo_atraccion WHERE id_combo_atraccion = ?";
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /** Pares id -> texto para las listas desplegables y para mostrar nombres en las tablas. */
    public Map<Integer, String> listarOpciones() throws SQLException {
        String sql = "SELECT id_combo_atraccion, CONCAT(id_combo_atraccion) FROM Combo_atraccion ORDER BY id_combo_atraccion";
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

    /** Convierte la fila actual del ResultSet en un objeto ComboAtraccion. */
    ComboAtraccion mapear(ResultSet rs) throws SQLException {
        ComboAtraccion x = new ComboAtraccion();
        x.setIdComboAtraccion(rs.getInt("id_combo_atraccion"));
        x.setIdCombo(rs.getObject("id_combo", Integer.class));
        x.setIdAtraccion(rs.getObject("id_atraccion", Integer.class));
        return x;
    }
}
