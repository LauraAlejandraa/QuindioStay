package co.edu.uniquindio.quindiostay.dao;

import co.edu.uniquindio.quindiostay.modelo.Boleta;
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
 * DAO de la tabla Boleta: es la única clase que sabe que existe esta tabla.
 * Siempre usa PreparedStatement (los ? se llenan con setString, setInt...),
 * nunca concatena texto del usuario dentro del SQL.
 */
@Repository
public class BoletaDAO {

    private static final String COLUMNAS = "id_boleta, codigo, estado, id_reserva, id_zona, id_tipo_entrada, id_combo";

    private final ConexionBD conexionBD;

    public BoletaDAO(ConexionBD conexionBD) {
        this.conexionBD = conexionBD;
    }

    /** INSERT. Recibe la conexión para poder usarse dentro de una transacción. */
    public int crear(Connection con, Boleta x) throws SQLException {
        String sql = "INSERT INTO Boleta (codigo, estado, id_reserva, id_zona, id_tipo_entrada, id_combo) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, x.getCodigo());
            ps.setString(2, x.getEstado());
            ps.setObject(3, x.getIdReserva(), Types.INTEGER);
            ps.setObject(4, x.getIdZona(), Types.INTEGER);
            ps.setObject(5, x.getIdTipoEntrada(), Types.INTEGER);
            ps.setObject(6, x.getIdCombo(), Types.INTEGER);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    x.setIdBoleta(rs.getInt(1));   // el id lo genera MySQL (AUTO_INCREMENT)
                }
            }
        }
        return x.getIdBoleta();
    }

    public int crear(Boleta x) throws SQLException {
        try (Connection con = conexionBD.getConexion()) {
            return crear(con, x);
        }
    }

    public Boleta buscarPorId(int id) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM Boleta WHERE id_boleta = ?";
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public List<Boleta> listarTodos() throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM Boleta ORDER BY id_boleta DESC";
        List<Boleta> lista = new ArrayList<>();
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
    public void actualizar(Connection con, Boleta x) throws SQLException {
        String sql = "UPDATE Boleta SET codigo = ?, estado = ?, id_reserva = ?, id_zona = ?, id_tipo_entrada = ?, id_combo = ? WHERE id_boleta = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, x.getCodigo());
            ps.setString(2, x.getEstado());
            ps.setObject(3, x.getIdReserva(), Types.INTEGER);
            ps.setObject(4, x.getIdZona(), Types.INTEGER);
            ps.setObject(5, x.getIdTipoEntrada(), Types.INTEGER);
            ps.setObject(6, x.getIdCombo(), Types.INTEGER);
            ps.setInt(7, x.getIdBoleta());
            ps.executeUpdate();
        }
    }

    public void actualizar(Boleta x) throws SQLException {
        try (Connection con = conexionBD.getConexion()) {
            actualizar(con, x);
        }
    }

    /** "Eliminar" según la Tabla 39 de la Entrega 2: no se borra la fila, solo cambia estado a 'anulada'. */
    public void eliminar(int id) throws SQLException {
        String sql = "UPDATE Boleta SET estado = 'anulada' WHERE id_boleta = ?";
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /** Borrado físico (solo lo usa el servicio cuando no hay historial). */
    public void borrar(int id) throws SQLException {
        String sql = "DELETE FROM Boleta WHERE id_boleta = ?";
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /** Pares id -> texto para las listas desplegables y para mostrar nombres en las tablas. */
    public Map<Integer, String> listarOpciones() throws SQLException {
        String sql = "SELECT id_boleta, codigo FROM Boleta ORDER BY codigo";
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

    public void cambiarCodigo(Connection con, int idBoleta, String codigo) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE Boleta SET codigo = ? WHERE id_boleta = ?")) {
            ps.setString(1, codigo);
            ps.setInt(2, idBoleta);
            ps.executeUpdate();
        }
    }

    public void cambiarEstado(Connection con, int idBoleta, String estado) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE Boleta SET estado = ? WHERE id_boleta = ?")) {
            ps.setString(1, estado);
            ps.setInt(2, idBoleta);
            ps.executeUpdate();
        }
    }

    /** Al cancelar la reserva, sus boletas vendidas pasan a 'anulada' y liberan el aforo. */
    public int anularDeReserva(Connection con, int idReserva) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE Boleta SET estado = 'anulada' WHERE id_reserva = ? AND estado = 'vendida'")) {
            ps.setInt(1, idReserva);
            return ps.executeUpdate();
        }
    }

    /** Cuántas boletas no anuladas de un tipo (por nombre) tiene la reserva. */
    public int contarPorTipo(Connection con, int idReserva, String tipo) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT COUNT(*) FROM Boleta b INNER JOIN Tipo_entrada t ON t.id_tipo_entrada = b.id_tipo_entrada "
              + "WHERE b.id_reserva = ? AND t.nombre = ? AND b.estado <> 'anulada'")) {
            ps.setInt(1, idReserva);
            ps.setString(2, tipo);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public int contarPorEstado(Connection con, int idReserva, String estado) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT COUNT(*) FROM Boleta WHERE id_reserva = ? AND estado = ?")) {
            ps.setInt(1, idReserva);
            ps.setString(2, estado);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** Convierte la fila actual del ResultSet en un objeto Boleta. */
    Boleta mapear(ResultSet rs) throws SQLException {
        Boleta x = new Boleta();
        x.setIdBoleta(rs.getInt("id_boleta"));
        x.setCodigo(rs.getString("codigo"));
        x.setEstado(rs.getString("estado"));
        x.setIdReserva(rs.getObject("id_reserva", Integer.class));
        x.setIdZona(rs.getObject("id_zona", Integer.class));
        x.setIdTipoEntrada(rs.getObject("id_tipo_entrada", Integer.class));
        x.setIdCombo(rs.getObject("id_combo", Integer.class));
        return x;
    }
}
