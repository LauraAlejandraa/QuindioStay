package co.edu.uniquindio.quindiostay.dao;

import co.edu.uniquindio.quindiostay.modelo.Funcion;
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
 * DAO de la tabla Funcion: es la única clase que sabe que existe esta tabla.
 * Siempre usa PreparedStatement (los ? se llenan con setString, setInt...),
 * nunca concatena texto del usuario dentro del SQL.
 */
@Repository
public class FuncionDAO {

    private static final String COLUMNAS = "id_funcion, fecha, hora_apertura, hora_cierre, estado, id_atraccion";

    private final ConexionBD conexionBD;

    public FuncionDAO(ConexionBD conexionBD) {
        this.conexionBD = conexionBD;
    }

    /** INSERT. Recibe la conexión para poder usarse dentro de una transacción. */
    public int crear(Connection con, Funcion x) throws SQLException {
        String sql = "INSERT INTO Funcion (fecha, hora_apertura, hora_cierre, estado, id_atraccion) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, x.getFecha());
            ps.setObject(2, x.getHoraApertura());
            ps.setObject(3, x.getHoraCierre());
            ps.setString(4, x.getEstado());
            ps.setObject(5, x.getIdAtraccion(), Types.INTEGER);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    x.setIdFuncion(rs.getInt(1));   // el id lo genera MySQL (AUTO_INCREMENT)
                }
            }
        }
        return x.getIdFuncion();
    }

    public int crear(Funcion x) throws SQLException {
        try (Connection con = conexionBD.getConexion()) {
            return crear(con, x);
        }
    }

    public Funcion buscarPorId(int id) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM Funcion WHERE id_funcion = ?";
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public List<Funcion> listarTodos() throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM Funcion ORDER BY fecha DESC, hora_apertura";
        List<Funcion> lista = new ArrayList<>();
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
    public void actualizar(Connection con, Funcion x) throws SQLException {
        String sql = "UPDATE Funcion SET fecha = ?, hora_apertura = ?, hora_cierre = ?, estado = ?, id_atraccion = ? WHERE id_funcion = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, x.getFecha());
            ps.setObject(2, x.getHoraApertura());
            ps.setObject(3, x.getHoraCierre());
            ps.setString(4, x.getEstado());
            ps.setObject(5, x.getIdAtraccion(), Types.INTEGER);
            ps.setInt(6, x.getIdFuncion());
            ps.executeUpdate();
        }
    }

    public void actualizar(Funcion x) throws SQLException {
        try (Connection con = conexionBD.getConexion()) {
            actualizar(con, x);
        }
    }

    /** "Eliminar" según la Tabla 39 de la Entrega 2: no se borra la fila, solo cambia estado a 'cancelada'. */
    public void eliminar(int id) throws SQLException {
        String sql = "UPDATE Funcion SET estado = 'cancelada' WHERE id_funcion = ?";
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /** Borrado físico (solo lo usa el servicio cuando no hay historial). */
    public void borrar(int id) throws SQLException {
        String sql = "DELETE FROM Funcion WHERE id_funcion = ?";
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /** Pares id -> texto para las listas desplegables y para mostrar nombres en las tablas. */
    public Map<Integer, String> listarOpciones() throws SQLException {
        String sql = "SELECT f.id_funcion, CONCAT(a.nombre, ' · ', DATE_FORMAT(f.fecha, '%d/%m/%Y'), ' ', TIME_FORMAT(f.hora_apertura, '%H:%i')) FROM Funcion f INNER JOIN Atraccion a ON a.id_atraccion = f.id_atraccion ORDER BY f.fecha DESC, a.nombre";
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

    public List<Funcion> listarPorPrestador(int idPrestador) throws SQLException {
        String sql = "SELECT f.id_funcion, f.fecha, f.hora_apertura, f.hora_cierre, f.estado, f.id_atraccion FROM Funcion f "
                + "INNER JOIN Prestador_atraccion pa ON pa.id_atraccion = f.id_atraccion "
                + "WHERE pa.id_prestador = ? ORDER BY f.fecha DESC, f.hora_apertura";
        List<Funcion> lista = new ArrayList<>();
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

    public int contarBoletasVendidas(int idFuncion) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Boleta b INNER JOIN Zona z ON z.id_zona = b.id_zona "
                + "WHERE z.id_funcion = ? AND b.estado <> 'anulada'";
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idFuncion);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** Convierte la fila actual del ResultSet en un objeto Funcion. */
    Funcion mapear(ResultSet rs) throws SQLException {
        Funcion x = new Funcion();
        x.setIdFuncion(rs.getInt("id_funcion"));
        x.setFecha(rs.getObject("fecha", LocalDate.class));
        x.setHoraApertura(rs.getObject("hora_apertura", LocalTime.class));
        x.setHoraCierre(rs.getObject("hora_cierre", LocalTime.class));
        x.setEstado(rs.getString("estado"));
        x.setIdAtraccion(rs.getObject("id_atraccion", Integer.class));
        return x;
    }
}
