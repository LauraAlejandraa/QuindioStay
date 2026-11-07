package co.edu.uniquindio.quindiostay.dao;

import co.edu.uniquindio.quindiostay.modelo.Prestador;
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
 * DAO de la tabla Prestador: es la única clase que sabe que existe esta tabla.
 * Siempre usa PreparedStatement (los ? se llenan con setString, setInt...),
 * nunca concatena texto del usuario dentro del SQL.
 */
@Repository
public class PrestadorDAO {

    private static final String COLUMNAS = "id_prestador, nombre_comercial, NIT_documento, RNT, direccion, telefono, correo, estado, id_municipio, id_tipo_prestador";

    private final ConexionBD conexionBD;

    public PrestadorDAO(ConexionBD conexionBD) {
        this.conexionBD = conexionBD;
    }

    /** INSERT. Recibe la conexión para poder usarse dentro de una transacción. */
    public int crear(Connection con, Prestador x) throws SQLException {
        String sql = "INSERT INTO Prestador (nombre_comercial, NIT_documento, RNT, direccion, telefono, correo, estado, id_municipio, id_tipo_prestador) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, x.getNombreComercial());
            ps.setString(2, x.getNitDocumento());
            ps.setString(3, x.getRnt());
            ps.setString(4, x.getDireccion());
            ps.setString(5, x.getTelefono());
            ps.setString(6, x.getCorreo());
            ps.setString(7, x.getEstado());
            ps.setObject(8, x.getIdMunicipio(), Types.INTEGER);
            ps.setObject(9, x.getIdTipoPrestador(), Types.INTEGER);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    x.setIdPrestador(rs.getInt(1));   // el id lo genera MySQL (AUTO_INCREMENT)
                }
            }
        }
        return x.getIdPrestador();
    }

    public int crear(Prestador x) throws SQLException {
        try (Connection con = conexionBD.getConexion()) {
            return crear(con, x);
        }
    }

    public Prestador buscarPorId(int id) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM Prestador WHERE id_prestador = ?";
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public List<Prestador> listarTodos() throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM Prestador ORDER BY nombre_comercial";
        List<Prestador> lista = new ArrayList<>();
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
    public void actualizar(Connection con, Prestador x) throws SQLException {
        String sql = "UPDATE Prestador SET nombre_comercial = ?, NIT_documento = ?, RNT = ?, direccion = ?, telefono = ?, correo = ?, estado = ?, id_municipio = ?, id_tipo_prestador = ? WHERE id_prestador = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, x.getNombreComercial());
            ps.setString(2, x.getNitDocumento());
            ps.setString(3, x.getRnt());
            ps.setString(4, x.getDireccion());
            ps.setString(5, x.getTelefono());
            ps.setString(6, x.getCorreo());
            ps.setString(7, x.getEstado());
            ps.setObject(8, x.getIdMunicipio(), Types.INTEGER);
            ps.setObject(9, x.getIdTipoPrestador(), Types.INTEGER);
            ps.setInt(10, x.getIdPrestador());
            ps.executeUpdate();
        }
    }

    public void actualizar(Prestador x) throws SQLException {
        try (Connection con = conexionBD.getConexion()) {
            actualizar(con, x);
        }
    }

    /** "Eliminar" según la Tabla 39 de la Entrega 2: no se borra la fila, solo cambia estado a 'retirado'. */
    public void eliminar(int id) throws SQLException {
        String sql = "UPDATE Prestador SET estado = 'retirado' WHERE id_prestador = ?";
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /** Borrado físico (solo lo usa el servicio cuando no hay historial). */
    public void borrar(int id) throws SQLException {
        String sql = "DELETE FROM Prestador WHERE id_prestador = ?";
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /** Pares id -> texto para las listas desplegables y para mostrar nombres en las tablas. */
    public Map<Integer, String> listarOpciones() throws SQLException {
        String sql = "SELECT id_prestador, nombre_comercial FROM Prestador ORDER BY nombre_comercial";
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

    /** Convierte la fila actual del ResultSet en un objeto Prestador. */
    Prestador mapear(ResultSet rs) throws SQLException {
        Prestador x = new Prestador();
        x.setIdPrestador(rs.getInt("id_prestador"));
        x.setNombreComercial(rs.getString("nombre_comercial"));
        x.setNitDocumento(rs.getString("NIT_documento"));
        x.setRnt(rs.getString("RNT"));
        x.setDireccion(rs.getString("direccion"));
        x.setTelefono(rs.getString("telefono"));
        x.setCorreo(rs.getString("correo"));
        x.setEstado(rs.getString("estado"));
        x.setIdMunicipio(rs.getObject("id_municipio", Integer.class));
        x.setIdTipoPrestador(rs.getObject("id_tipo_prestador", Integer.class));
        return x;
    }
}
