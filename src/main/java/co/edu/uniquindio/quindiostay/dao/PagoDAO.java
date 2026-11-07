package co.edu.uniquindio.quindiostay.dao;

import co.edu.uniquindio.quindiostay.modelo.Pago;
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
 * DAO de la tabla Pago: es la única clase que sabe que existe esta tabla.
 * Siempre usa PreparedStatement (los ? se llenan con setString, setInt...),
 * nunca concatena texto del usuario dentro del SQL.
 */
@Repository
public class PagoDAO {

    private static final String COLUMNAS = "id_pago, fecha, monto, metodo, estado, referencia_transaccion, id_reserva";

    private final ConexionBD conexionBD;

    public PagoDAO(ConexionBD conexionBD) {
        this.conexionBD = conexionBD;
    }

    /** INSERT. Recibe la conexión para poder usarse dentro de una transacción. */
    public int crear(Connection con, Pago x) throws SQLException {
        String sql = "INSERT INTO Pago (fecha, monto, metodo, estado, referencia_transaccion, id_reserva) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, x.getFecha());
            ps.setBigDecimal(2, x.getMonto());
            ps.setString(3, x.getMetodo());
            ps.setString(4, x.getEstado());
            ps.setString(5, JdbcUtil.vacioANull(x.getReferenciaTransaccion()));
            ps.setObject(6, x.getIdReserva(), Types.INTEGER);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    x.setIdPago(rs.getInt(1));   // el id lo genera MySQL (AUTO_INCREMENT)
                }
            }
        }
        return x.getIdPago();
    }

    public int crear(Pago x) throws SQLException {
        try (Connection con = conexionBD.getConexion()) {
            return crear(con, x);
        }
    }

    public Pago buscarPorId(int id) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM Pago WHERE id_pago = ?";
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public List<Pago> listarTodos() throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM Pago ORDER BY id_pago DESC";
        List<Pago> lista = new ArrayList<>();
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
    public void actualizar(Connection con, Pago x) throws SQLException {
        String sql = "UPDATE Pago SET fecha = ?, monto = ?, metodo = ?, estado = ?, referencia_transaccion = ?, id_reserva = ? WHERE id_pago = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, x.getFecha());
            ps.setBigDecimal(2, x.getMonto());
            ps.setString(3, x.getMetodo());
            ps.setString(4, x.getEstado());
            ps.setString(5, JdbcUtil.vacioANull(x.getReferenciaTransaccion()));
            ps.setObject(6, x.getIdReserva(), Types.INTEGER);
            ps.setInt(7, x.getIdPago());
            ps.executeUpdate();
        }
    }

    public void actualizar(Pago x) throws SQLException {
        try (Connection con = conexionBD.getConexion()) {
            actualizar(con, x);
        }
    }

    /** "Eliminar" según la Tabla 39 de la Entrega 2: no se borra la fila, solo cambia estado a 'reembolsado'. */
    public void eliminar(int id) throws SQLException {
        String sql = "UPDATE Pago SET estado = 'reembolsado' WHERE id_pago = ?";
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /** Borrado físico (solo lo usa el servicio cuando no hay historial). */
    public void borrar(int id) throws SQLException {
        String sql = "DELETE FROM Pago WHERE id_pago = ?";
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /** Pares id -> texto para las listas desplegables y para mostrar nombres en las tablas. */
    public Map<Integer, String> listarOpciones() throws SQLException {
        String sql = "SELECT id_pago, CONCAT('Pago ', id_pago) FROM Pago ORDER BY id_pago";
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

    public void cambiarEstado(Connection con, int idPago, String estado) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE Pago SET estado = ? WHERE id_pago = ?")) {
            ps.setString(1, estado);
            ps.setInt(2, idPago);
            ps.executeUpdate();
        }
    }

    /** Reembolso al cancelar: los pagos exitosos de la reserva pasan a 'reembolsado'. */
    public int reembolsarDeReserva(Connection con, int idReserva) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE Pago SET estado = 'reembolsado' WHERE id_reserva = ? AND estado = 'exitoso'")) {
            ps.setInt(1, idReserva);
            return ps.executeUpdate();
        }
    }

    public List<Pago> listarPorReserva(int idReserva) throws SQLException {
        List<Pago> lista = new ArrayList<>();
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement("SELECT " + COLUMNAS + " FROM Pago WHERE id_reserva = ? ORDER BY id_pago")) {
            ps.setInt(1, idReserva);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    /** Convierte la fila actual del ResultSet en un objeto Pago. */
    Pago mapear(ResultSet rs) throws SQLException {
        Pago x = new Pago();
        x.setIdPago(rs.getInt("id_pago"));
        x.setFecha(rs.getObject("fecha", LocalDate.class));
        x.setMonto(rs.getBigDecimal("monto"));
        x.setMetodo(rs.getString("metodo"));
        x.setEstado(rs.getString("estado"));
        x.setReferenciaTransaccion(rs.getString("referencia_transaccion"));
        x.setIdReserva(rs.getObject("id_reserva", Integer.class));
        return x;
    }
}
