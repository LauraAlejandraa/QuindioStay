package co.edu.uniquindio.quindiostay.dao;

import co.edu.uniquindio.quindiostay.modelo.TipoEntrada;
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
 * DAO de la tabla Tipo_entrada: es la única clase que sabe que existe esta tabla.
 * Siempre usa PreparedStatement (los ? se llenan con setString, setInt...),
 * nunca concatena texto del usuario dentro del SQL.
 */
@Repository
public class TipoEntradaDAO {

    private static final String COLUMNAS = "id_tipo_entrada, nombre, porcentaje_descuento";

    private final ConexionBD conexionBD;

    public TipoEntradaDAO(ConexionBD conexionBD) {
        this.conexionBD = conexionBD;
    }

    /** INSERT. Recibe la conexión para poder usarse dentro de una transacción. */
    public int crear(Connection con, TipoEntrada x) throws SQLException {
        String sql = "INSERT INTO Tipo_entrada (nombre, porcentaje_descuento) VALUES (?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, x.getNombre());
            ps.setBigDecimal(2, x.getPorcentajeDescuento());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    x.setIdTipoEntrada(rs.getInt(1));   // el id lo genera MySQL (AUTO_INCREMENT)
                }
            }
        }
        return x.getIdTipoEntrada();
    }

    public int crear(TipoEntrada x) throws SQLException {
        try (Connection con = conexionBD.getConexion()) {
            return crear(con, x);
        }
    }

    public TipoEntrada buscarPorId(int id) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM Tipo_entrada WHERE id_tipo_entrada = ?";
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public List<TipoEntrada> listarTodos() throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM Tipo_entrada ORDER BY id_tipo_entrada";
        List<TipoEntrada> lista = new ArrayList<>();
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
    public void actualizar(Connection con, TipoEntrada x) throws SQLException {
        String sql = "UPDATE Tipo_entrada SET nombre = ?, porcentaje_descuento = ? WHERE id_tipo_entrada = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, x.getNombre());
            ps.setBigDecimal(2, x.getPorcentajeDescuento());
            ps.setInt(3, x.getIdTipoEntrada());
            ps.executeUpdate();
        }
    }

    public void actualizar(TipoEntrada x) throws SQLException {
        try (Connection con = conexionBD.getConexion()) {
            actualizar(con, x);
        }
    }

    /** Borra la fila. Si otra tabla depende de ella, MySQL responde con el error 1451 (RESTRICT). */
    public void eliminar(int id) throws SQLException {
        String sql = "DELETE FROM Tipo_entrada WHERE id_tipo_entrada = ?";
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /** Pares id -> texto para las listas desplegables y para mostrar nombres en las tablas. */
    public Map<Integer, String> listarOpciones() throws SQLException {
        String sql = "SELECT id_tipo_entrada, nombre FROM Tipo_entrada ORDER BY nombre";
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

    /** Convierte la fila actual del ResultSet en un objeto TipoEntrada. */
    TipoEntrada mapear(ResultSet rs) throws SQLException {
        TipoEntrada x = new TipoEntrada();
        x.setIdTipoEntrada(rs.getInt("id_tipo_entrada"));
        x.setNombre(rs.getString("nombre"));
        x.setPorcentajeDescuento(rs.getBigDecimal("porcentaje_descuento"));
        return x;
    }
}
