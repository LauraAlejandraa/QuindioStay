package co.edu.uniquindio.quindiostay.dao;

import co.edu.uniquindio.quindiostay.modelo.Reserva;
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
 * DAO de la tabla Reserva: es la única clase que sabe que existe esta tabla.
 * Siempre usa PreparedStatement (los ? se llenan con setString, setInt...),
 * nunca concatena texto del usuario dentro del SQL.
 */
@Repository
public class ReservaDAO {

    private static final String COLUMNAS = "id_reserva, fecha_creacion, fecha_servicio, estado, valor_total, canal, id_cliente, id_plan";

    private final ConexionBD conexionBD;

    public ReservaDAO(ConexionBD conexionBD) {
        this.conexionBD = conexionBD;
    }

    /** INSERT. Recibe la conexión para poder usarse dentro de una transacción. */
    public int crear(Connection con, Reserva x) throws SQLException {
        String sql = "INSERT INTO Reserva (fecha_creacion, fecha_servicio, estado, valor_total, canal, id_cliente, id_plan) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, x.getFechaCreacion());
            ps.setObject(2, x.getFechaServicio());
            ps.setString(3, x.getEstado());
            ps.setBigDecimal(4, x.getValorTotal());
            ps.setString(5, x.getCanal());
            ps.setObject(6, x.getIdCliente(), Types.INTEGER);
            ps.setObject(7, x.getIdPlan(), Types.INTEGER);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    x.setIdReserva(rs.getInt(1));   // el id lo genera MySQL (AUTO_INCREMENT)
                }
            }
        }
        return x.getIdReserva();
    }

    public int crear(Reserva x) throws SQLException {
        try (Connection con = conexionBD.getConexion()) {
            return crear(con, x);
        }
    }

    public Reserva buscarPorId(int id) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM Reserva WHERE id_reserva = ?";
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public List<Reserva> listarTodos() throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM Reserva ORDER BY id_reserva DESC";
        List<Reserva> lista = new ArrayList<>();
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
    public void actualizar(Connection con, Reserva x) throws SQLException {
        String sql = "UPDATE Reserva SET fecha_creacion = ?, fecha_servicio = ?, estado = ?, valor_total = ?, canal = ?, id_cliente = ?, id_plan = ? WHERE id_reserva = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, x.getFechaCreacion());
            ps.setObject(2, x.getFechaServicio());
            ps.setString(3, x.getEstado());
            ps.setBigDecimal(4, x.getValorTotal());
            ps.setString(5, x.getCanal());
            ps.setObject(6, x.getIdCliente(), Types.INTEGER);
            ps.setObject(7, x.getIdPlan(), Types.INTEGER);
            ps.setInt(8, x.getIdReserva());
            ps.executeUpdate();
        }
    }

    public void actualizar(Reserva x) throws SQLException {
        try (Connection con = conexionBD.getConexion()) {
            actualizar(con, x);
        }
    }

    /** "Eliminar" según la Tabla 39 de la Entrega 2: no se borra la fila, solo cambia estado a 'cancelada'. */
    public void eliminar(int id) throws SQLException {
        String sql = "UPDATE Reserva SET estado = 'cancelada' WHERE id_reserva = ?";
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /** Borrado físico (solo lo usa el servicio cuando no hay historial). */
    public void borrar(int id) throws SQLException {
        String sql = "DELETE FROM Reserva WHERE id_reserva = ?";
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /** Pares id -> texto para las listas desplegables y para mostrar nombres en las tablas. */
    public Map<Integer, String> listarOpciones() throws SQLException {
        String sql = "SELECT id_reserva, CONCAT('#', id_reserva, ' · ', DATE_FORMAT(fecha_servicio, '%d/%m/%Y'), ' · ', estado) FROM Reserva ORDER BY id_reserva";
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

    public Reserva buscarPorId(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT " + COLUMNAS + " FROM Reserva WHERE id_reserva = ? FOR UPDATE")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public void cambiarEstado(Connection con, int idReserva, String estado) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE Reserva SET estado = ? WHERE id_reserva = ?")) {
            ps.setString(1, estado);
            ps.setInt(2, idReserva);
            ps.executeUpdate();
        }
    }

    /** Suma el valor de una boleta agregada después a una reserva existente. */
    public void sumarValor(Connection con, int idReserva, BigDecimal valor) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE Reserva SET valor_total = valor_total + ? WHERE id_reserva = ?")) {
            ps.setBigDecimal(1, valor);
            ps.setInt(2, idReserva);
            ps.executeUpdate();
        }
    }

    /** Reservas de los planes de un prestador (el prestador se conoce por el plan: 3FN). */
    public List<Reserva> listarPorPrestador(int idPrestador) throws SQLException {
        String sql = "SELECT r.id_reserva, r.fecha_creacion, r.fecha_servicio, r.estado, r.valor_total, r.canal, "
                + "r.id_cliente, r.id_plan FROM Reserva r INNER JOIN Plan pl ON pl.id_plan = r.id_plan "
                + "WHERE pl.id_prestador = ? ORDER BY r.id_reserva DESC";
        List<Reserva> lista = new ArrayList<>();
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

    /** Convierte la fila actual del ResultSet en un objeto Reserva. */
    Reserva mapear(ResultSet rs) throws SQLException {
        Reserva x = new Reserva();
        x.setIdReserva(rs.getInt("id_reserva"));
        x.setFechaCreacion(rs.getObject("fecha_creacion", LocalDate.class));
        x.setFechaServicio(rs.getObject("fecha_servicio", LocalDate.class));
        x.setEstado(rs.getString("estado"));
        x.setValorTotal(rs.getBigDecimal("valor_total"));
        x.setCanal(rs.getString("canal"));
        x.setIdCliente(rs.getObject("id_cliente", Integer.class));
        x.setIdPlan(rs.getObject("id_plan", Integer.class));
        return x;
    }
}
