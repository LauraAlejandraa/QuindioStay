package co.edu.uniquindio.quindiostay.dao;

import co.edu.uniquindio.quindiostay.modelo.Plan;
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
 * DAO de la tabla Plan: es la única clase que sabe que existe esta tabla.
 * Siempre usa PreparedStatement (los ? se llenan con setString, setInt...),
 * nunca concatena texto del usuario dentro del SQL.
 */
@Repository
public class PlanDAO {

    private static final String COLUMNAS = "id_plan, nombre, descripcion, tipo, precio, fecha_inicio, fecha_fin, estado, id_prestador";

    private final ConexionBD conexionBD;

    public PlanDAO(ConexionBD conexionBD) {
        this.conexionBD = conexionBD;
    }

    /** INSERT. Recibe la conexión para poder usarse dentro de una transacción. */
    public int crear(Connection con, Plan x) throws SQLException {
        String sql = "INSERT INTO Plan (nombre, descripcion, tipo, precio, fecha_inicio, fecha_fin, estado, id_prestador) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, x.getNombre());
            ps.setString(2, x.getDescripcion());
            ps.setString(3, x.getTipo());
            ps.setBigDecimal(4, x.getPrecio());
            ps.setObject(5, x.getFechaInicio());
            ps.setObject(6, x.getFechaFin());
            ps.setString(7, x.getEstado());
            ps.setObject(8, x.getIdPrestador(), Types.INTEGER);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    x.setIdPlan(rs.getInt(1));   // el id lo genera MySQL (AUTO_INCREMENT)
                }
            }
        }
        return x.getIdPlan();
    }

    public int crear(Plan x) throws SQLException {
        try (Connection con = conexionBD.getConexion()) {
            return crear(con, x);
        }
    }

    public Plan buscarPorId(int id) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM Plan WHERE id_plan = ?";
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public List<Plan> listarTodos() throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM Plan ORDER BY id_prestador, fecha_inicio DESC";
        List<Plan> lista = new ArrayList<>();
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
    public void actualizar(Connection con, Plan x) throws SQLException {
        String sql = "UPDATE Plan SET nombre = ?, descripcion = ?, tipo = ?, precio = ?, fecha_inicio = ?, fecha_fin = ?, estado = ?, id_prestador = ? WHERE id_plan = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, x.getNombre());
            ps.setString(2, x.getDescripcion());
            ps.setString(3, x.getTipo());
            ps.setBigDecimal(4, x.getPrecio());
            ps.setObject(5, x.getFechaInicio());
            ps.setObject(6, x.getFechaFin());
            ps.setString(7, x.getEstado());
            ps.setObject(8, x.getIdPrestador(), Types.INTEGER);
            ps.setInt(9, x.getIdPlan());
            ps.executeUpdate();
        }
    }

    public void actualizar(Plan x) throws SQLException {
        try (Connection con = conexionBD.getConexion()) {
            actualizar(con, x);
        }
    }

    /** "Eliminar" según la Tabla 39 de la Entrega 2: no se borra la fila, solo cambia estado a 'inactivo'. */
    public void eliminar(int id) throws SQLException {
        String sql = "UPDATE Plan SET estado = 'inactivo' WHERE id_plan = ?";
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /** Borrado físico (solo lo usa el servicio cuando no hay historial). */
    public void borrar(int id) throws SQLException {
        String sql = "DELETE FROM Plan WHERE id_plan = ?";
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /** Pares id -> texto para las listas desplegables y para mostrar nombres en las tablas. */
    public Map<Integer, String> listarOpciones() throws SQLException {
        String sql = "SELECT pl.id_plan, CONCAT(pl.nombre, ' · ', pr.nombre_comercial) FROM Plan pl INNER JOIN Prestador pr ON pr.id_prestador = pl.id_prestador ORDER BY pr.nombre_comercial, pl.nombre";
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

    public List<Plan> listarPorPrestador(int idPrestador) throws SQLException {
        List<Plan> lista = new ArrayList<>();
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement("SELECT " + COLUMNAS + " FROM Plan WHERE id_prestador = ? ORDER BY fecha_inicio DESC")) {
            ps.setInt(1, idPrestador);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    /** Convierte la fila actual del ResultSet en un objeto Plan. */
    Plan mapear(ResultSet rs) throws SQLException {
        Plan x = new Plan();
        x.setIdPlan(rs.getInt("id_plan"));
        x.setNombre(rs.getString("nombre"));
        x.setDescripcion(rs.getString("descripcion"));
        x.setTipo(rs.getString("tipo"));
        x.setPrecio(rs.getBigDecimal("precio"));
        x.setFechaInicio(rs.getObject("fecha_inicio", LocalDate.class));
        x.setFechaFin(rs.getObject("fecha_fin", LocalDate.class));
        x.setEstado(rs.getString("estado"));
        x.setIdPrestador(rs.getObject("id_prestador", Integer.class));
        return x;
    }
}
