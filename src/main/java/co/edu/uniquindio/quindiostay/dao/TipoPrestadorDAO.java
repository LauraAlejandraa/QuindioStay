package co.edu.uniquindio.quindiostay.dao;

import co.edu.uniquindio.quindiostay.modelo.TipoPrestador;
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
 * DAO de la tabla Tipo_prestador: es la única clase que sabe que existe esta tabla.
 * Siempre usa PreparedStatement (los ? se llenan con setString, setInt...),
 * nunca concatena texto del usuario dentro del SQL.
 */
@Repository
public class TipoPrestadorDAO {

    private static final String COLUMNAS = "id_tipo_prestador, nombre";

    private final ConexionBD conexionBD;

    public TipoPrestadorDAO(ConexionBD conexionBD) {
        this.conexionBD = conexionBD;
    }

    /** INSERT. Recibe la conexión para poder usarse dentro de una transacción. */
    public int crear(Connection con, TipoPrestador x) throws SQLException {
        String sql = "INSERT INTO Tipo_prestador (nombre) VALUES (?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, x.getNombre());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    x.setIdTipoPrestador(rs.getInt(1));   // el id lo genera MySQL (AUTO_INCREMENT)
                }
            }
        }
        return x.getIdTipoPrestador();
    }

    public int crear(TipoPrestador x) throws SQLException {
        try (Connection con = conexionBD.getConexion()) {
            return crear(con, x);
        }
    }

    public TipoPrestador buscarPorId(int id) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM Tipo_prestador WHERE id_tipo_prestador = ?";
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public List<TipoPrestador> listarTodos() throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM Tipo_prestador ORDER BY nombre";
        List<TipoPrestador> lista = new ArrayList<>();
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
    public void actualizar(Connection con, TipoPrestador x) throws SQLException {
        String sql = "UPDATE Tipo_prestador SET nombre = ? WHERE id_tipo_prestador = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, x.getNombre());
            ps.setInt(2, x.getIdTipoPrestador());
            ps.executeUpdate();
        }
    }

    public void actualizar(TipoPrestador x) throws SQLException {
        try (Connection con = conexionBD.getConexion()) {
            actualizar(con, x);
        }
    }

    /** Borra la fila. Si otra tabla depende de ella, MySQL responde con el error 1451 (RESTRICT). */
    public void eliminar(int id) throws SQLException {
        String sql = "DELETE FROM Tipo_prestador WHERE id_tipo_prestador = ?";
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /** Pares id -> texto para las listas desplegables y para mostrar nombres en las tablas. */
    public Map<Integer, String> listarOpciones() throws SQLException {
        String sql = "SELECT id_tipo_prestador, nombre FROM Tipo_prestador ORDER BY nombre";
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

    /** Convierte la fila actual del ResultSet en un objeto TipoPrestador. */
    TipoPrestador mapear(ResultSet rs) throws SQLException {
        TipoPrestador x = new TipoPrestador();
        x.setIdTipoPrestador(rs.getInt("id_tipo_prestador"));
        x.setNombre(rs.getString("nombre"));
        return x;
    }
}
