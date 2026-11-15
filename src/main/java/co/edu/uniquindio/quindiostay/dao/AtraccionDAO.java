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

    /** DELETE dentro de una transacción (la usa el servicio después de quitar las afiliaciones). */
    public void eliminar(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM Atraccion WHERE id_atraccion = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /** Cuántas funciones tiene la atracción (de cualquier estado). */
    public int contarFunciones(int id) throws SQLException {
        return contar("SELECT COUNT(*) FROM Funcion WHERE id_atraccion = ?", id);
    }

    /** Cuántas boletas (de cualquier estado) se emitieron para funciones de la atracción. */
    public int contarBoletas(int id) throws SQLException {
        return contar("SELECT COUNT(*) FROM Boleta b INNER JOIN Zona z ON z.id_zona = b.id_zona "
                + "INNER JOIN Funcion f ON f.id_funcion = z.id_funcion WHERE f.id_atraccion = ?", id);
    }

    /** Nombres de los combos de los que forma parte la atracción. */
    public List<String> combosDe(int id) throws SQLException {
        return nombres("SELECT c.nombre FROM Combo_atraccion ca INNER JOIN Combo c ON c.id_combo = ca.id_combo "
                + "WHERE ca.id_atraccion = ? ORDER BY c.nombre", id);
    }

    /** id -> nombre de los prestadores afiliados a la atracción. */
    public Map<Integer, String> prestadoresAfiliados(int id) throws SQLException {
        String sql = "SELECT p.id_prestador, p.nombre_comercial FROM Prestador_atraccion pa "
                + "INNER JOIN Prestador p ON p.id_prestador = pa.id_prestador WHERE pa.id_atraccion = ? ORDER BY p.nombre_comercial";
        Map<Integer, String> mapa = new LinkedHashMap<>();
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    mapa.put(rs.getInt(1), rs.getString(2));
                }
            }
        }
        return mapa;
    }

    private int contar(String sql, int id) throws SQLException {
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private List<String> nombres(String sql, int id) throws SQLException {
        List<String> lista = new ArrayList<>();
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(rs.getString(1));
                }
            }
        }
        return lista;
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

    /**
     * Buscar por nombre (Tabla 39): el texto se parte en palabras y CADA palabra debe
     * coincidir con el inicio de alguna palabra del nombre ("par caf" encuentra
     * "Parque del Café", pero "afe" no). Los valores van como parámetros, nunca concatenados.
     */
    public List<Atraccion> buscarPorNombre(String texto) throws SQLException {
        List<Atraccion> lista = new ArrayList<>();
        String[] palabras = texto == null ? new String[0] : texto.trim().split("\\s+");
        StringBuilder sql = new StringBuilder("SELECT " + COLUMNAS + " FROM Atraccion WHERE 1=1");
        List<String> valores = new ArrayList<>();
        for (String p : palabras) {
            if (p.isEmpty()) continue;
            // Se escapan los comodines para que "%" o "_" escritos se busquen tal cual.
            String limpia = p.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
            sql.append(" AND (nombre LIKE ? OR nombre LIKE ?)");
            valores.add(limpia + "%");        // al inicio del nombre
            valores.add("% " + limpia + "%"); // al inicio de otra palabra del nombre
        }
        sql.append(" ORDER BY nombre");
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < valores.size(); i++) {
                ps.setString(i + 1, valores.get(i));
            }
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
