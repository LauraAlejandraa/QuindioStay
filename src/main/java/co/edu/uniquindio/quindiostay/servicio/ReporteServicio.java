package co.edu.uniquindio.quindiostay.servicio;

import co.edu.uniquindio.quindiostay.dao.VistaDAO;
import co.edu.uniquindio.quindiostay.modelo.Reporte;
import co.edu.uniquindio.quindiostay.modelo.Reporte.Filtro;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

/**
 * Los 10 reportes (RF13): 3 simples, 4 intermedios y 3 avanzados.
 * Son las consultas de la Entrega 2 con los ajustes acordados con la Parte A
 * (guía, paso 6): R3 cuenta por estado, R4 con filtros, R6 con ingresos y
 * R8 con unidades de combo exactas. Llevan gráfico de barras los reportes 6, 9 y 10.
 * Solo ocupan aforo las boletas que no están anuladas (vendida o usada).
 */
@Service
public class ReporteServicio {

    private final VistaDAO dao;
    private final List<Reporte> reportes = new ArrayList<>();

    public ReporteServicio(VistaDAO dao) {
        this.dao = dao;
        reportes.add(new Reporte(1, "Atracciones registradas", "Simple",
                "¿Cuáles son las atracciones turísticas registradas en QuindíoStay y qué información tiene el sistema sobre cada una?",
                "SELECT id_atraccion AS id, nombre AS atraccion, descripcion, ubicacion "
                + "FROM Atraccion ORDER BY nombre ASC", List.of(), null, null));

        reportes.add(new Reporte(2, "Funciones programadas", "Simple",
                "¿Cuáles son las funciones programadas y qué fecha y horario tiene cada una?",
                "SELECT id_funcion AS id, fecha, hora_apertura AS apertura, hora_cierre AS cierre, estado, id_atraccion "
                + "FROM Funcion WHERE estado = 'programada' ORDER BY fecha ASC, hora_apertura ASC", List.of(), null, null));

        reportes.add(new Reporte(3, "Boletas por estado", "Simple",
                "¿Cuántas boletas hay registradas según su estado (vendida, usada, anulada)?",
                "SELECT estado, COUNT(*) AS total_boletas FROM Boleta GROUP BY estado ORDER BY total_boletas DESC",
                List.of(), null, null));

        reportes.add(new Reporte(4, "Reservas realizadas por cliente", "Intermedio",
                "¿Qué reservas han realizado los clientes y cuál es el detalle de cada una, según su estado o rango de fechas?",
                "SELECT r.id_reserva AS reserva, CONCAT(c.primer_nombre, ' ', c.primer_apellido) AS cliente, "
                + "r.fecha_creacion AS fecha_reserva, r.fecha_servicio, r.estado, r.valor_total "
                + "FROM Reserva r INNER JOIN Cliente c ON c.id_cliente = r.id_cliente "
                + "WHERE (? IS NULL OR r.estado = ?) AND (? IS NULL OR r.fecha_servicio >= ?) AND (? IS NULL OR r.fecha_servicio <= ?) "
                + "ORDER BY r.fecha_creacion DESC, r.id_reserva DESC",
                List.of(new Filtro("estado", "Estado", "lista", List.of("pendiente", "confirmada", "completada", "cancelada")),
                        new Filtro("desde", "Servicio desde", "fecha", null),
                        new Filtro("hasta", "Servicio hasta", "fecha", null)), null, null));

        reportes.add(new Reporte(5, "Aforo vendido y disponible por función y zona", "Intermedio",
                "¿Cuántas boletas se han vendido y cuántos cupos quedan en cada zona de cada función?",
                "SELECT f.id_funcion AS funcion, a.nombre AS atraccion, f.fecha, z.nombre AS zona, z.aforo, "
                + "COUNT(b.id_boleta) AS boletas_vendidas, z.aforo - COUNT(b.id_boleta) AS cupos_disponibles "
                + "FROM Funcion f INNER JOIN Atraccion a ON a.id_atraccion = f.id_atraccion "
                + "INNER JOIN Zona z ON z.id_funcion = f.id_funcion "
                + "LEFT JOIN Boleta b ON b.id_zona = z.id_zona AND b.estado <> 'anulada' "
                + "GROUP BY f.id_funcion, a.nombre, f.fecha, z.id_zona, z.nombre, z.aforo "
                + "ORDER BY f.fecha DESC, f.id_funcion, z.nombre LIMIT 300", List.of(), null, null));

        reportes.add(new Reporte(6, "Ventas e ingresos por tipo de entrada", "Intermedio",
                "¿Cuántas boletas se vendieron de cada tipo de entrada y cuántos ingresos dejaron?",
                "SELECT t.nombre AS tipo_entrada, COUNT(b.id_boleta) AS boletas_vendidas, "
                + "SUM(ROUND(z.precio * (100 - t.porcentaje_descuento) / 100, -2)) AS ingresos "
                + "FROM Tipo_entrada t INNER JOIN Boleta b ON b.id_tipo_entrada = t.id_tipo_entrada "
                + "INNER JOIN Zona z ON z.id_zona = b.id_zona "
                + "WHERE b.estado <> 'anulada' AND b.id_combo IS NULL "
                + "GROUP BY t.id_tipo_entrada, t.nombre HAVING COUNT(b.id_boleta) > 0 ORDER BY boletas_vendidas DESC",
                List.of(), "tipo_entrada", "ingresos"));

        reportes.add(new Reporte(7, "Ingresos por atracción", "Intermedio",
                "¿Cuánto dinero generan las boletas sueltas de cada atracción?",
                "SELECT a.nombre AS atraccion, COUNT(b.id_boleta) AS boletas, "
                + "SUM(ROUND(z.precio * (100 - t.porcentaje_descuento) / 100, -2)) AS ingresos "
                + "FROM Atraccion a INNER JOIN Funcion f ON f.id_atraccion = a.id_atraccion "
                + "INNER JOIN Zona z ON z.id_funcion = f.id_funcion INNER JOIN Boleta b ON b.id_zona = z.id_zona "
                + "INNER JOIN Tipo_entrada t ON t.id_tipo_entrada = b.id_tipo_entrada "
                + "WHERE b.estado <> 'anulada' AND b.id_combo IS NULL "
                + "GROUP BY a.id_atraccion, a.nombre HAVING COUNT(b.id_boleta) > 0 ORDER BY ingresos DESC",
                List.of(), null, null));

        reportes.add(new Reporte(8, "Combos más vendidos", "Avanzado",
                "¿Cuáles son los combos más vendidos y cuántas unidades se han vendido de cada uno?",
                "SELECT c.id_combo AS id, c.nombre AS combo, c.precio_especial, "
                + "(SELECT COUNT(*) FROM Boleta b WHERE b.id_combo = c.id_combo AND b.estado <> 'anulada') "
                + "/ (SELECT COUNT(*) FROM Combo_atraccion ca WHERE ca.id_combo = c.id_combo) AS unidades_vendidas "
                + "FROM Combo c ORDER BY unidades_vendidas DESC", List.of(), null, null));

        reportes.add(new Reporte(9, "Atracciones con más boletas vendidas", "Avanzado",
                "¿Cuáles son las atracciones con mayor cantidad de boletas vendidas?",
                "SELECT a.id_atraccion AS id, a.nombre AS atraccion, a.ubicacion, "
                + "(SELECT COUNT(b.id_boleta) FROM Funcion f INNER JOIN Zona z ON z.id_funcion = f.id_funcion "
                + "INNER JOIN Boleta b ON b.id_zona = z.id_zona WHERE f.id_atraccion = a.id_atraccion AND b.estado <> 'anulada') AS boletas_vendidas "
                + "FROM Atraccion a ORDER BY boletas_vendidas DESC", List.of(), "atraccion", "boletas_vendidas"));

        reportes.add(new Reporte(10, "Funciones con mayor ocupación", "Avanzado",
                "¿Cuáles son las funciones con mayor porcentaje de ocupación?",
                "SELECT t.id_funcion AS funcion, a.nombre AS atraccion, t.fecha, t.capacidad, t.vendidas AS boletas_vendidas, "
                + "ROUND(t.vendidas * 100 / t.capacidad, 1) AS porcentaje_ocupacion "
                + "FROM (SELECT f.id_funcion, f.id_atraccion, f.fecha, "
                + "(SELECT SUM(z.aforo) FROM Zona z WHERE z.id_funcion = f.id_funcion) AS capacidad, "
                + "(SELECT COUNT(*) FROM Boleta b INNER JOIN Zona z2 ON z2.id_zona = b.id_zona "
                + "WHERE z2.id_funcion = f.id_funcion AND b.estado <> 'anulada') AS vendidas FROM Funcion f) t "
                + "INNER JOIN Atraccion a ON a.id_atraccion = t.id_atraccion WHERE t.capacidad > 0 "
                + "ORDER BY porcentaje_ocupacion DESC, t.fecha LIMIT 15", List.of(), "funcion_etiqueta", "porcentaje_ocupacion"));
    }

    public List<Reporte> listar() { return reportes; }

    public Reporte buscar(int n) {
        return reportes.stream().filter(r -> r.getNumero() == n).findFirst()
                .orElseThrow(() -> new ReglaNegocioException("No existe el reporte " + n + "."));
    }

    /** Ejecuta el reporte. Los filtros vacíos se pasan como NULL (la consulta los ignora). */
    public List<Map<String, Object>> ejecutar(Reporte r, Map<String, String> filtros) {
        List<Object> params = new ArrayList<>();
        for (Filtro f : r.getFiltros()) {
            String v = filtros.get(f.getNombre());
            Object valor = (v == null || v.isBlank()) ? null : ("fecha".equals(f.getTipo()) ? java.sql.Date.valueOf(LocalDate.parse(v)) : v);
            params.add(valor);
            params.add(valor);
        }
        try {
            List<Map<String, Object>> filas = dao.consultar(r.getSql(), params.toArray());
            if (r.getNumero() == 10) {   // etiqueta legible para el gráfico
                for (Map<String, Object> fila : filas) {
                    fila.put("funcion_etiqueta", "#" + fila.get("funcion") + " " + fila.get("atraccion"));
                }
            }
            return filas;
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }
}
