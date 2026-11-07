package co.edu.uniquindio.quindiostay.dao;

import co.edu.uniquindio.quindiostay.modelo.PrestadorAtraccion;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO de Prestador_atraccion. Como la PK es compuesta, buscar, actualizar y
 * eliminar reciben los DOS valores (id_prestador, id_atraccion).
 */
@Repository
public class PrestadorAtraccionDAO {

    private final ConexionBD conexionBD;

    public PrestadorAtraccionDAO(ConexionBD conexionBD) {
        this.conexionBD = conexionBD;
    }

    public void crear(Connection con, PrestadorAtraccion x) throws SQLException {
        String sql = "INSERT INTO Prestador_atraccion (id_prestador, id_atraccion, fecha_afiliacion) VALUES (?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, x.getIdPrestador());
            ps.setInt(2, x.getIdAtraccion());
            ps.setObject(3, x.getFechaAfiliacion());
            ps.executeUpdate();
        }
    }

    public void crear(PrestadorAtraccion x) throws SQLException {
        try (Connection con = conexionBD.getConexion()) {
            crear(con, x);
        }
    }

    public PrestadorAtraccion buscarPorId(int idPrestador, int idAtraccion) throws SQLException {
        String sql = "SELECT id_prestador, id_atraccion, fecha_afiliacion FROM Prestador_atraccion "
                   + "WHERE id_prestador = ? AND id_atraccion = ?";
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idPrestador);
            ps.setInt(2, idAtraccion);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public List<PrestadorAtraccion> listarTodos() throws SQLException {
        String sql = "SELECT id_prestador, id_atraccion, fecha_afiliacion FROM Prestador_atraccion "
                   + "ORDER BY id_atraccion, id_prestador";
        List<PrestadorAtraccion> lista = new ArrayList<>();
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    /** Solo cambia la fecha: la pareja (prestador, atracción) es la clave y no se modifica. */
    public void actualizar(PrestadorAtraccion x) throws SQLException {
        String sql = "UPDATE Prestador_atraccion SET fecha_afiliacion = ? WHERE id_prestador = ? AND id_atraccion = ?";
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, x.getFechaAfiliacion());
            ps.setInt(2, x.getIdPrestador());
            ps.setInt(3, x.getIdAtraccion());
            ps.executeUpdate();
        }
    }

    public void eliminar(int idPrestador, int idAtraccion) throws SQLException {
        String sql = "DELETE FROM Prestador_atraccion WHERE id_prestador = ? AND id_atraccion = ?";
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idPrestador);
            ps.setInt(2, idAtraccion);
            ps.executeUpdate();
        }
    }

    /** ¿El prestador está afiliado a la atracción? (regla del plan de la reserva). */
    public boolean estaAfiliado(int idPrestador, int idAtraccion) throws SQLException {
        return buscarPorId(idPrestador, idAtraccion) != null;
    }

    /** ¿El prestador vendió boletas de esta atracción? Si sí, la afiliación es historial. */
    public int contarVentas(int idPrestador, int idAtraccion) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Boleta b "
                   + "INNER JOIN Reserva r ON r.id_reserva = b.id_reserva "
                   + "INNER JOIN Plan pl ON pl.id_plan = r.id_plan "
                   + "INNER JOIN Zona z ON z.id_zona = b.id_zona "
                   + "INNER JOIN Funcion f ON f.id_funcion = z.id_funcion "
                   + "WHERE pl.id_prestador = ? AND f.id_atraccion = ?";
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idPrestador);
            ps.setInt(2, idAtraccion);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private PrestadorAtraccion mapear(ResultSet rs) throws SQLException {
        PrestadorAtraccion x = new PrestadorAtraccion();
        x.setIdPrestador(rs.getInt("id_prestador"));
        x.setIdAtraccion(rs.getInt("id_atraccion"));
        x.setFechaAfiliacion(rs.getObject("fecha_afiliacion", LocalDate.class));
        x.setExistente(true);
        return x;
    }
}
