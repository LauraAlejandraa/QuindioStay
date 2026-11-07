package co.edu.uniquindio.quindiostay.dao;

import co.edu.uniquindio.quindiostay.modelo.Notificacion;
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
 * DAO de la tabla Notificacion: es la única clase que sabe que existe esta tabla.
 * Siempre usa PreparedStatement (los ? se llenan con setString, setInt...),
 * nunca concatena texto del usuario dentro del SQL.
 */
@Repository
public class NotificacionDAO {

    private static final String COLUMNAS = "id_notificacion, tipo, canal, mensaje, fecha_programada, fecha_real_envio, estado, id_cliente, id_reserva";

    private final ConexionBD conexionBD;

    public NotificacionDAO(ConexionBD conexionBD) {
        this.conexionBD = conexionBD;
    }

    /** INSERT. Recibe la conexión para poder usarse dentro de una transacción. */
    public int crear(Connection con, Notificacion x) throws SQLException {
        String sql = "INSERT INTO Notificacion (tipo, canal, mensaje, fecha_programada, fecha_real_envio, estado, id_cliente, id_reserva) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, x.getTipo());
            ps.setString(2, x.getCanal());
            ps.setString(3, x.getMensaje());
            ps.setObject(4, x.getFechaProgramada());
            ps.setObject(5, x.getFechaRealEnvio());
            ps.setString(6, x.getEstado());
            ps.setObject(7, x.getIdCliente(), Types.INTEGER);
            ps.setObject(8, x.getIdReserva(), Types.INTEGER);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    x.setIdNotificacion(rs.getInt(1));   // el id lo genera MySQL (AUTO_INCREMENT)
                }
            }
        }
        return x.getIdNotificacion();
    }

    public int crear(Notificacion x) throws SQLException {
        try (Connection con = conexionBD.getConexion()) {
            return crear(con, x);
        }
    }

    public Notificacion buscarPorId(int id) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM Notificacion WHERE id_notificacion = ?";
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public List<Notificacion> listarTodos() throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM Notificacion ORDER BY fecha_programada DESC";
        List<Notificacion> lista = new ArrayList<>();
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
    public void actualizar(Connection con, Notificacion x) throws SQLException {
        String sql = "UPDATE Notificacion SET tipo = ?, canal = ?, mensaje = ?, fecha_programada = ?, fecha_real_envio = ?, estado = ?, id_cliente = ?, id_reserva = ? WHERE id_notificacion = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, x.getTipo());
            ps.setString(2, x.getCanal());
            ps.setString(3, x.getMensaje());
            ps.setObject(4, x.getFechaProgramada());
            ps.setObject(5, x.getFechaRealEnvio());
            ps.setString(6, x.getEstado());
            ps.setObject(7, x.getIdCliente(), Types.INTEGER);
            ps.setObject(8, x.getIdReserva(), Types.INTEGER);
            ps.setInt(9, x.getIdNotificacion());
            ps.executeUpdate();
        }
    }

    public void actualizar(Notificacion x) throws SQLException {
        try (Connection con = conexionBD.getConexion()) {
            actualizar(con, x);
        }
    }

    /** Borra la fila. Si otra tabla depende de ella, MySQL responde con el error 1451 (RESTRICT). */
    public void eliminar(int id) throws SQLException {
        String sql = "DELETE FROM Notificacion WHERE id_notificacion = ?";
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /** Pares id -> texto para las listas desplegables y para mostrar nombres en las tablas. */
    public Map<Integer, String> listarOpciones() throws SQLException {
        String sql = "SELECT id_notificacion, CONCAT(id_notificacion) FROM Notificacion ORDER BY id_notificacion";
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

    /** Convierte la fila actual del ResultSet en un objeto Notificacion. */
    Notificacion mapear(ResultSet rs) throws SQLException {
        Notificacion x = new Notificacion();
        x.setIdNotificacion(rs.getInt("id_notificacion"));
        x.setTipo(rs.getString("tipo"));
        x.setCanal(rs.getString("canal"));
        x.setMensaje(rs.getString("mensaje"));
        x.setFechaProgramada(rs.getObject("fecha_programada", LocalDateTime.class));
        x.setFechaRealEnvio(rs.getObject("fecha_real_envio", LocalDateTime.class));
        x.setEstado(rs.getString("estado"));
        x.setIdCliente(rs.getObject("id_cliente", Integer.class));
        x.setIdReserva(rs.getObject("id_reserva", Integer.class));
        return x;
    }
}
