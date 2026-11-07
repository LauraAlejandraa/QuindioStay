package co.edu.uniquindio.quindiostay.dao;

import co.edu.uniquindio.quindiostay.modelo.Zona;
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
 * DAO de la tabla Zona: es la única clase que sabe que existe esta tabla.
 * Siempre usa PreparedStatement (los ? se llenan con setString, setInt...),
 * nunca concatena texto del usuario dentro del SQL.
 */
@Repository
public class ZonaDAO {

    private static final String COLUMNAS = "id_zona, nombre, aforo, precio, id_funcion";

    private final ConexionBD conexionBD;

    public ZonaDAO(ConexionBD conexionBD) {
        this.conexionBD = conexionBD;
    }

    /** INSERT. Recibe la conexión para poder usarse dentro de una transacción. */
    public int crear(Connection con, Zona x) throws SQLException {
        String sql = "INSERT INTO Zona (nombre, aforo, precio, id_funcion) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, x.getNombre());
            ps.setObject(2, x.getAforo(), Types.INTEGER);
            ps.setBigDecimal(3, x.getPrecio());
            ps.setObject(4, x.getIdFuncion(), Types.INTEGER);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    x.setIdZona(rs.getInt(1));   // el id lo genera MySQL (AUTO_INCREMENT)
                }
            }
        }
        return x.getIdZona();
    }

    public int crear(Zona x) throws SQLException {
        try (Connection con = conexionBD.getConexion()) {
            return crear(con, x);
        }
    }

    public Zona buscarPorId(int id) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM Zona WHERE id_zona = ?";
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public List<Zona> listarTodos() throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM Zona ORDER BY id_funcion DESC, precio";
        List<Zona> lista = new ArrayList<>();
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
    public void actualizar(Connection con, Zona x) throws SQLException {
        String sql = "UPDATE Zona SET nombre = ?, aforo = ?, precio = ?, id_funcion = ? WHERE id_zona = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, x.getNombre());
            ps.setObject(2, x.getAforo(), Types.INTEGER);
            ps.setBigDecimal(3, x.getPrecio());
            ps.setObject(4, x.getIdFuncion(), Types.INTEGER);
            ps.setInt(5, x.getIdZona());
            ps.executeUpdate();
        }
    }

    public void actualizar(Zona x) throws SQLException {
        try (Connection con = conexionBD.getConexion()) {
            actualizar(con, x);
        }
    }

    /** Borra la fila. Si otra tabla depende de ella, MySQL responde con el error 1451 (RESTRICT). */
    public void eliminar(int id) throws SQLException {
        String sql = "DELETE FROM Zona WHERE id_zona = ?";
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /** Pares id -> texto para las listas desplegables y para mostrar nombres en las tablas. */
    public Map<Integer, String> listarOpciones() throws SQLException {
        String sql = "SELECT z.id_zona, CONCAT(a.nombre, ' · ', DATE_FORMAT(f.fecha, '%d/%m/%Y'), ' · ', z.nombre) FROM Zona z INNER JOIN Funcion f ON f.id_funcion = z.id_funcion INNER JOIN Atraccion a ON a.id_atraccion = f.id_atraccion ORDER BY f.fecha DESC, a.nombre, z.nombre";
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

    /**
     * Cupos que le quedan a la zona, dentro de una transacción.
     * FOR UPDATE bloquea la fila de la zona hasta el COMMIT: si dos taquillas
     * venden la misma zona al tiempo, la segunda espera y cuenta bien el aforo.
     * Solo ocupan aforo las boletas vendidas o usadas (no las anuladas).
     */
    public int cuposDisponibles(Connection con, int idZona) throws SQLException {
        int aforo;
        try (PreparedStatement ps = con.prepareStatement("SELECT aforo FROM Zona WHERE id_zona = ? FOR UPDATE")) {
            ps.setInt(1, idZona);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("La zona no existe", "45000", 1452);
                }
                aforo = rs.getInt(1);
            }
        }
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT COUNT(*) FROM Boleta WHERE id_zona = ? AND estado <> 'anulada'")) {
            ps.setInt(1, idZona);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return aforo - rs.getInt(1);
            }
        }
    }

    /** Zonas de las funciones de las atracciones afiliadas a un prestador (encargado). */
    public List<Zona> listarPorPrestador(int idPrestador) throws SQLException {
        String sql = "SELECT z.id_zona, z.nombre, z.aforo, z.precio, z.id_funcion FROM Zona z "
                + "INNER JOIN Funcion f ON f.id_funcion = z.id_funcion "
                + "INNER JOIN Prestador_atraccion pa ON pa.id_atraccion = f.id_atraccion "
                + "WHERE pa.id_prestador = ? ORDER BY z.id_funcion DESC, z.precio";
        List<Zona> lista = new ArrayList<>();
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

    /** Convierte la fila actual del ResultSet en un objeto Zona. */
    Zona mapear(ResultSet rs) throws SQLException {
        Zona x = new Zona();
        x.setIdZona(rs.getInt("id_zona"));
        x.setNombre(rs.getString("nombre"));
        x.setAforo(rs.getObject("aforo", Integer.class));
        x.setPrecio(rs.getBigDecimal("precio"));
        x.setIdFuncion(rs.getObject("id_funcion", Integer.class));
        return x;
    }
}
