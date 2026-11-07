package co.edu.uniquindio.quindiostay.dao;

import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/**
 * Consultas sobre las VISTAS del script 03_vistas.sql (Parte A) y otras
 * consultas puntuales (RF14). Cada fila se devuelve como un Map
 * columna -> valor, leyendo los nombres de las columnas con getMetaData().
 */
@Repository
public class VistaDAO {

    private final ConexionBD conexionBD;

    public VistaDAO(ConexionBD conexionBD) {
        this.conexionBD = conexionBD;
    }

    /** Ejecuta un SELECT con parámetros y devuelve las filas como mapas. */
    public List<Map<String, Object>> consultar(String sql, Object... parametros) throws SQLException {
        try (Connection con = conexionBD.getConexion()) {
            return consultar(con, sql, parametros);
        }
    }

    public List<Map<String, Object>> consultar(Connection con, String sql, Object... parametros) throws SQLException {
        List<Map<String, Object>> filas = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                ps.setObject(i + 1, parametros[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                while (rs.next()) {
                    Map<String, Object> fila = new LinkedHashMap<>();
                    for (int c = 1; c <= md.getColumnCount(); c++) {
                        fila.put(md.getColumnLabel(c), rs.getObject(c));
                    }
                    filas.add(fila);
                }
            }
        }
        return filas;
    }

    private Map<String, Object> primera(List<Map<String, Object>> filas) {
        return filas.isEmpty() ? null : filas.get(0);
    }

    // ---------- vista_reserva_detalle ----------
    public Map<String, Object> reservaDetalle(int idReserva) throws SQLException {
        return primera(consultar("SELECT * FROM vista_reserva_detalle WHERE id_reserva = ?", idReserva));
    }

    public List<Map<String, Object>> reservasDePrestador(int idPrestador) throws SQLException {
        return consultar("SELECT * FROM vista_reserva_detalle WHERE id_prestador = ? ORDER BY id_reserva DESC", idPrestador);
    }

    /** Reservas de un cliente con lo pagado y el saldo (vista_estado_pago_reserva + vista_reserva_detalle). */
    public List<Map<String, Object>> reservasDeCliente(int idCliente) throws SQLException {
        return consultar("SELECT d.*, e.total_pagado, e.saldo, e.pagada "
                + "FROM vista_reserva_detalle d "
                + "INNER JOIN vista_estado_pago_reserva e ON e.id_reserva = d.id_reserva "
                + "WHERE d.id_cliente = ? ORDER BY d.fecha_servicio DESC", idCliente);
    }

    // ---------- vista_estado_pago_reserva ----------
    public Map<String, Object> estadoPago(int idReserva) throws SQLException {
        return primera(consultar("SELECT * FROM vista_estado_pago_reserva WHERE id_reserva = ?", idReserva));
    }

    public Map<String, Object> estadoPago(Connection con, int idReserva) throws SQLException {
        return primera(consultar(con, "SELECT * FROM vista_estado_pago_reserva WHERE id_reserva = ?", idReserva));
    }

    // ---------- vista_boleta_detalle ----------
    public List<Map<String, Object>> boletasDeReserva(int idReserva) throws SQLException {
        return consultar("SELECT * FROM vista_boleta_detalle WHERE id_reserva = ? ORDER BY fecha_funcion, atraccion, id_boleta", idReserva);
    }

    public Map<String, Object> boletaPorCodigo(String codigo) throws SQLException {
        return primera(consultar("SELECT * FROM vista_boleta_detalle WHERE codigo = ?", codigo));
    }

    public List<Map<String, Object>> boletasDeFuncion(int idFuncion) throws SQLException {
        return consultar("SELECT * FROM vista_boleta_detalle WHERE id_funcion = ? ORDER BY zona, codigo", idFuncion);
    }

    // ---------- vista_ocupacion_zona ----------
    public List<Map<String, Object>> zonasDeFuncion(int idFuncion) throws SQLException {
        return consultar("SELECT * FROM vista_ocupacion_zona WHERE id_funcion = ? ORDER BY precio", idFuncion);
    }

    /** Zonas de funciones programadas desde hoy de una atracción (para vender combos). */
    public List<Map<String, Object>> zonasFuturasDeAtraccion(int idAtraccion) throws SQLException {
        return consultar("SELECT o.*, f.hora_apertura FROM vista_ocupacion_zona o "
                + "INNER JOIN Funcion f ON f.id_funcion = o.id_funcion "
                + "WHERE o.id_atraccion = ? AND o.estado_funcion = 'programada' AND o.fecha_funcion >= CURDATE() "
                + "AND o.cupos_disponibles > 0 ORDER BY o.fecha_funcion, o.precio", idAtraccion);
    }

    // ---------- vista_planes_para_vender ----------
    /** Planes activos, de prestadores activos, afiliados a la atracción y VIGENTES en la fecha. */
    public List<Map<String, Object>> planesParaVender(int idAtraccion, LocalDate fecha) throws SQLException {
        return consultar("SELECT * FROM vista_planes_para_vender "
                + "WHERE id_atraccion = ? AND ? BETWEEN fecha_inicio AND fecha_fin ORDER BY prestador, precio",
                idAtraccion, fecha);
    }

    /** Para un combo, el prestador debe estar afiliado a TODAS las atracciones del combo. */
    public List<Map<String, Object>> planesParaCombo(int idCombo, LocalDate fecha) throws SQLException {
        return consultar("SELECT v.id_plan, v.plan, v.tipo_plan, v.precio, v.id_prestador, v.prestador "
                + "FROM vista_planes_para_vender v "
                + "INNER JOIN Combo_atraccion ca ON ca.id_atraccion = v.id_atraccion "
                + "WHERE ca.id_combo = ? AND ? BETWEEN v.fecha_inicio AND v.fecha_fin "
                + "GROUP BY v.id_plan, v.plan, v.tipo_plan, v.precio, v.id_prestador, v.prestador "
                + "HAVING COUNT(DISTINCT v.id_atraccion) = (SELECT COUNT(*) FROM Combo_atraccion x WHERE x.id_combo = ?) "
                + "ORDER BY v.prestador, v.precio", idCombo, fecha, idCombo);
    }

    // ---------- vista_notificacion_cliente ----------
    public List<Map<String, Object>> notificacionesDeCliente(int idCliente) throws SQLException {
        return consultar("SELECT * FROM vista_notificacion_cliente WHERE id_cliente = ? ORDER BY fecha_programada DESC", idCliente);
    }

    public List<Map<String, Object>> notificacionesDeReserva(int idReserva) throws SQLException {
        return consultar("SELECT * FROM Notificacion WHERE id_reserva = ? ORDER BY fecha_programada", idReserva);
    }

    public List<Map<String, Object>> pagosDeReserva(int idReserva) throws SQLException {
        return consultar("SELECT * FROM Pago WHERE id_reserva = ? ORDER BY fecha, id_pago", idReserva);
    }

    /** Funciones que se pueden vender: programadas, desde hoy, con su atracción. */
    public List<Map<String, Object>> funcionesParaVender(Integer idAtraccion) throws SQLException {
        String sql = "SELECT f.id_funcion, f.fecha, f.hora_apertura, f.hora_cierre, a.id_atraccion, a.nombre AS atraccion, "
                + "(SELECT COALESCE(SUM(o.cupos_disponibles), 0) FROM vista_ocupacion_zona o WHERE o.id_funcion = f.id_funcion) AS cupos "
                + "FROM Funcion f INNER JOIN Atraccion a ON a.id_atraccion = f.id_atraccion "
                + "WHERE f.estado = 'programada' AND f.fecha >= CURDATE() "
                + (idAtraccion != null ? "AND a.id_atraccion = ? " : "")
                + "ORDER BY f.fecha, f.hora_apertura, a.nombre";
        return idAtraccion != null ? consultar(sql, idAtraccion) : consultar(sql);
    }

    /** Conteos para la pantalla de inicio. */
    public Map<String, Object> resumenInicio() throws SQLException {
        return primera(consultar("SELECT "
                + "(SELECT COUNT(*) FROM Reserva) AS reservas, "
                + "(SELECT COUNT(*) FROM Reserva WHERE estado = 'pendiente') AS pendientes, "
                + "(SELECT COUNT(*) FROM Boleta WHERE estado <> 'anulada') AS boletas, "
                + "(SELECT COUNT(*) FROM Cliente) AS clientes, "
                + "(SELECT COUNT(*) FROM Funcion WHERE estado = 'programada' AND fecha >= CURDATE()) AS funciones, "
                + "(SELECT COALESCE(SUM(monto), 0) FROM Pago WHERE estado = 'exitoso') AS ingresos"));
    }
}
