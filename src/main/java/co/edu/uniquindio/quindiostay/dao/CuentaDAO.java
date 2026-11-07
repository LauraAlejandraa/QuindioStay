package co.edu.uniquindio.quindiostay.dao;

import co.edu.uniquindio.quindiostay.modelo.Cuenta;
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
 * DAO de la tabla Cuenta: es la única clase que sabe que existe esta tabla.
 * Siempre usa PreparedStatement (los ? se llenan con setString, setInt...),
 * nunca concatena texto del usuario dentro del SQL.
 */
@Repository
public class CuentaDAO {

    private static final String COLUMNAS = "id_cuenta, contrasena_cifrada, id_cliente";

    private final ConexionBD conexionBD;

    public CuentaDAO(ConexionBD conexionBD) {
        this.conexionBD = conexionBD;
    }

    /** INSERT. Recibe la conexión para poder usarse dentro de una transacción. */
    public int crear(Connection con, Cuenta x) throws SQLException {
        String sql = "INSERT INTO Cuenta (contrasena_cifrada, id_cliente) VALUES (?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, x.getContrasenaCifrada());
            ps.setObject(2, x.getIdCliente(), Types.INTEGER);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    x.setIdCuenta(rs.getInt(1));   // el id lo genera MySQL (AUTO_INCREMENT)
                }
            }
        }
        return x.getIdCuenta();
    }

    public int crear(Cuenta x) throws SQLException {
        try (Connection con = conexionBD.getConexion()) {
            return crear(con, x);
        }
    }

    public Cuenta buscarPorId(int id) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM Cuenta WHERE id_cuenta = ?";
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public List<Cuenta> listarTodos() throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM Cuenta ORDER BY id_cuenta";
        List<Cuenta> lista = new ArrayList<>();
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
    public void actualizar(Connection con, Cuenta x) throws SQLException {
        String sql = "UPDATE Cuenta SET contrasena_cifrada = ?, id_cliente = ? WHERE id_cuenta = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, x.getContrasenaCifrada());
            ps.setObject(2, x.getIdCliente(), Types.INTEGER);
            ps.setInt(3, x.getIdCuenta());
            ps.executeUpdate();
        }
    }

    public void actualizar(Cuenta x) throws SQLException {
        try (Connection con = conexionBD.getConexion()) {
            actualizar(con, x);
        }
    }

    /** Borra la fila. Si otra tabla depende de ella, MySQL responde con el error 1451 (RESTRICT). */
    public void eliminar(int id) throws SQLException {
        String sql = "DELETE FROM Cuenta WHERE id_cuenta = ?";
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /** Pares id -> texto para las listas desplegables y para mostrar nombres en las tablas. */
    public Map<Integer, String> listarOpciones() throws SQLException {
        String sql = "SELECT id_cuenta, CONCAT('Cuenta ',id_cuenta) FROM Cuenta ORDER BY id_cuenta";
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

    public Cuenta buscarPorCliente(int idCliente) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM Cuenta WHERE id_cliente = ?";
        try (Connection con = conexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    /** Convierte la fila actual del ResultSet en un objeto Cuenta. */
    Cuenta mapear(ResultSet rs) throws SQLException {
        Cuenta x = new Cuenta();
        x.setIdCuenta(rs.getInt("id_cuenta"));
        x.setContrasenaCifrada(rs.getString("contrasena_cifrada"));
        x.setIdCliente(rs.getObject("id_cliente", Integer.class));
        return x;
    }
}
