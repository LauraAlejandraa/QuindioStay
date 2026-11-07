package co.edu.uniquindio.quindiostay.servicio;

import co.edu.uniquindio.quindiostay.dao.VistaDAO;
import co.edu.uniquindio.quindiostay.modelo.Reporte;
import co.edu.uniquindio.quindiostay.pdf.PdfUtil;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static co.edu.uniquindio.quindiostay.pdf.PdfUtil.*;

/**
 * Genera los PDF: los 10 reportes y las 5 plantillas de documentos (RF16).
 * Los datos salen de las vistas de la Parte A (vista_reserva_detalle,
 * vista_estado_pago_reserva, vista_boleta_detalle).
 */
@Service
public class PdfServicio {

    private final VistaDAO dao;

    public PdfServicio(VistaDAO dao) {
        this.dao = dao;
    }

    // ======================= Reportes =======================
    public byte[] reporte(Reporte r, List<Map<String, Object>> filas, Map<String, String> filtros) {
        return PdfUtil.crear(true, (doc, writer) -> {
            doc.add(new Paragraph("Reporte " + r.getNumero() + ". " + r.getTitulo() + " · nivel " + r.getNivel(), SUBTITULO));
            doc.add(new Paragraph(r.getPregunta(), NORMAL));
            StringBuilder f = new StringBuilder("Generado el " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            filtros.forEach((k, v) -> { if (v != null && !v.isBlank()) f.append(" · ").append(k).append(": ").append(v); });
            doc.add(new Paragraph(f + " · " + filas.size() + " filas", PEQUENA));
            doc.add(espacio());
            if (filas.isEmpty()) {
                doc.add(new Paragraph("Sin resultados para los filtros escogidos.", NORMAL));
                return;
            }
            List<String> cols = columnasVisibles(filas);
            List<String> cab = new ArrayList<>();
            cols.forEach(c -> cab.add(encabezado(c)));
            List<List<String>> datos = new ArrayList<>();
            for (Map<String, Object> fila : filas) {
                List<String> l = new ArrayList<>();
                for (String c : cols) {
                    Object v = fila.get(c);
                    l.add(esDinero(c) ? pesos(v) : texto(v));
                }
                datos.add(l);
            }
            if (r.isGrafico()) {
                graficoBarras(doc, writer, filas, r.getColumnaEtiqueta(), r.getColumnaValor(),
                        "Gráfico: " + encabezado(r.getColumnaValor()) + " por " + encabezado(r.getColumnaEtiqueta().replace("_etiqueta", "")));
                doc.add(espacio());
            }
            doc.add(tabla(cab, datos, null));
        });
    }

    public static List<String> columnasVisibles(List<Map<String, Object>> filas) {
        List<String> cols = new ArrayList<>();
        for (String c : filas.get(0).keySet()) {
            if (!c.endsWith("_etiqueta")) cols.add(c);
        }
        return cols;
    }

    public static boolean esDinero(String col) {
        return col.equals("ingresos") || col.equals("valor_total") || col.equals("precio_especial");
    }

    // ======================= Plantillas =======================
    /** 1. Comprobante de reserva. */
    public byte[] comprobanteReserva(int idReserva) {
        Map<String, Object> r = reserva(idReserva);
        return PdfUtil.crear(false, (doc, w) -> {
            doc.add(new Paragraph("Comprobante de reserva N.º " + idReserva, SUBTITULO));
            doc.add(espacio());
            doc.add(datos(new String[][]{
                    {"Estado", texto(r.get("estado_reserva"))},
                    {"Cliente", texto(r.get("cliente"))},
                    {"Documento", r.get("tipo_documento") + " " + r.get("numero_documento")},
                    {"Correo / teléfono", r.get("email") + " · " + r.get("telefono")},
                    {"Prestador", r.get("prestador") + " (NIT " + r.get("nit_prestador") + ", RNT " + r.get("rnt_prestador") + ")"},
                    {"Municipio", texto(r.get("municipio_prestador"))},
                    {"Plan", r.get("plan") + " · " + r.get("tipo_plan")},
                    {"Fecha de la reserva", texto(r.get("fecha_creacion"))},
                    {"Fecha del servicio", texto(r.get("fecha_servicio"))},
                    {"Canal", texto(r.get("canal"))},
                    {"Valor total", pesos(r.get("valor_total"))},
                    {"Pagado / saldo", pesos(r.get("total_pagado")) + " / " + pesos(r.get("saldo"))}}));
            doc.add(espacio());
            doc.add(new Paragraph("Boletas de la reserva", SUBTITULO));
            doc.add(tablaBoletas(boletas(idReserva)));
            doc.add(espacio());
            doc.add(new Paragraph("Condiciones: cancelando con más de 5 días de anticipación se reembolsa el 80 % de lo pagado; "
                    + "con 5 días o menos no hay reembolso. Cada boleta solo puede usarse el día de su función.", PEQUENA));
        });
    }

    /** 2. Comprobante de pago. */
    public byte[] comprobantePago(int idPago) {
        try {
            List<Map<String, Object>> p = dao.consultar("SELECT * FROM Pago WHERE id_pago = ?", idPago);
            if (p.isEmpty()) throw new ReglaNegocioException("No existe el pago " + idPago + ".");
            Map<String, Object> pago = p.get(0);
            int idReserva = ((Number) pago.get("id_reserva")).intValue();
            Map<String, Object> r = reserva(idReserva);
            return PdfUtil.crear(false, (doc, w) -> {
                doc.add(new Paragraph("Comprobante de pago N.º " + idPago, SUBTITULO));
                doc.add(espacio());
                doc.add(datos(new String[][]{
                        {"Fecha del pago", texto(pago.get("fecha"))},
                        {"Monto", pesos(pago.get("monto"))},
                        {"Método", texto(pago.get("metodo"))},
                        {"Estado del pago", texto(pago.get("estado"))},
                        {"Referencia de transacción", pago.get("referencia_transaccion") == null ? "(sin referencia: pago presencial)" : texto(pago.get("referencia_transaccion"))},
                        {"Reserva", "N.º " + idReserva + " · servicio el " + texto(r.get("fecha_servicio"))},
                        {"Cliente", r.get("cliente") + " · " + r.get("tipo_documento") + " " + r.get("numero_documento")},
                        {"Prestador", texto(r.get("prestador"))},
                        {"Valor total de la reserva", pesos(r.get("valor_total"))},
                        {"Total pagado a la fecha", pesos(r.get("total_pagado"))},
                        {"Saldo pendiente", pesos(r.get("saldo"))}}));
                doc.add(espacio());
                doc.add(new Paragraph("Pasarela de pago simulada para fines académicos.", PEQUENA));
            });
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    /** 3. Factura. Las boletas de combo se cobran por unidad al precio especial. */
    public byte[] factura(int idReserva) {
        Map<String, Object> r = reserva(idReserva);
        List<Map<String, Object>> bol = boletas(idReserva).stream()
                .filter(b -> !"anulada".equals(b.get("estado_boleta"))).toList();
        return PdfUtil.crear(false, (doc, w) -> {
            doc.add(new Paragraph("Factura de venta N.º QS-" + String.format("%06d", idReserva), SUBTITULO));
            doc.add(new Paragraph("Emitida el " + texto(java.time.LocalDate.now()) + " · Prestador: " + r.get("prestador")
                    + " · NIT " + r.get("nit_prestador"), PEQUENA));
            doc.add(espacio());
            doc.add(datos(new String[][]{{"Cliente", texto(r.get("cliente"))},
                    {"Documento", r.get("tipo_documento") + " " + r.get("numero_documento")},
                    {"Correo", texto(r.get("email"))}}));
            doc.add(espacio());
            // Detalle: boletas sueltas una por línea; combos agrupados por unidades
            List<List<String>> filas = new ArrayList<>();
            Map<String, Integer> combos = new LinkedHashMap<>();
            BigDecimal total = BigDecimal.ZERO;
            for (Map<String, Object> b : bol) {
                if (b.get("id_combo") == null) {
                    BigDecimal v = (BigDecimal) b.get("valor_boleta");
                    filas.add(List.of("Boleta " + b.get("codigo") + " · " + b.get("atraccion") + " (" + b.get("zona") + ", " + b.get("tipo_entrada") + ")",
                            "1", pesos(v), pesos(v)));
                    total = total.add(v);
                } else {
                    combos.merge(String.valueOf(b.get("combo")), 1, Integer::sum);
                }
            }
            for (Map.Entry<String, Integer> c : combos.entrySet()) {
                int atracciones = bol.stream().filter(b -> c.getKey().equals(String.valueOf(b.get("combo")))).map(b -> b.get("id_atraccion")).collect(java.util.stream.Collectors.toSet()).size();
                int unidades = c.getValue() / Math.max(1, atracciones);
                try {
                    BigDecimal precio = (BigDecimal) dao.consultar("SELECT precio_especial FROM Combo WHERE nombre = ?", c.getKey()).get(0).get("precio_especial");
                    filas.add(List.of("Combo " + c.getKey() + " (" + atracciones + " atracciones)", String.valueOf(unidades), pesos(precio), pesos(precio.multiply(BigDecimal.valueOf(unidades)))));
                    total = total.add(precio.multiply(BigDecimal.valueOf(unidades)));
                } catch (SQLException e) {
                    throw new ReglaNegocioException(MensajesError.traducir(e));
                }
            }
            doc.add(tabla(List.of("Descripción", "Cant.", "Valor unitario", "Subtotal"), filas, new float[]{58, 8, 17, 17}, 1, 2, 3));
            doc.add(espacio());
            doc.add(datos(new String[][]{{"Total de la factura", pesos(total)},
                    {"Valor total de la reserva", pesos(r.get("valor_total"))},
                    {"Pagado", pesos(r.get("total_pagado"))}, {"Saldo", pesos(r.get("saldo"))},
                    {"¿Pagada?", texto(r.get("pagada"))}}));
            doc.add(espacio());
            doc.add(new Paragraph("Los precios incluyen los descuentos del tipo de entrada. Documento académico: no tiene validez tributaria.", PEQUENA));
        });
    }

    /** 4. Boletas: una por página con su código. */
    public byte[] boletas(int idReserva, boolean unaPorPagina) {
        Map<String, Object> r = reserva(idReserva);
        List<Map<String, Object>> bol = boletas(idReserva).stream().filter(b -> !"anulada".equals(b.get("estado_boleta"))).toList();
        if (bol.isEmpty()) throw new ReglaNegocioException("La reserva no tiene boletas vigentes.");
        return PdfUtil.crear(false, (doc, w) -> {
            doc.add(new Paragraph("Boletas de la reserva N.º " + idReserva + " · " + r.get("cliente"), SUBTITULO));
            for (Map<String, Object> b : bol) {
                doc.add(espacio());
                PdfPTable t = new PdfPTable(1);
                t.setWidthPercentage(100);
                com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell();
                cell.setPadding(10);
                cell.setBorderColor(VERDE);
                cell.setBorderWidth(2);
                cell.addElement(new Paragraph(texto(b.get("atraccion")), new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 14, com.lowagie.text.Font.BOLD, VERDE)));
                cell.addElement(new Paragraph("Fecha: " + texto(b.get("fecha_funcion")) + " · " + texto(b.get("hora_apertura")) + " a " + texto(b.get("hora_cierre")), NORMAL));
                cell.addElement(new Paragraph("Zona: " + b.get("zona") + " · Entrada: " + b.get("tipo_entrada")
                        + (b.get("combo") != null ? " · Combo: " + b.get("combo") : ""), NORMAL));
                cell.addElement(new Paragraph("Código: " + b.get("codigo"), new com.lowagie.text.Font(com.lowagie.text.Font.COURIER, 18, com.lowagie.text.Font.BOLD)));
                cell.addElement(new Paragraph("Válida una sola vez, solo el día de la función y con la reserva pagada. Estado: " + b.get("estado_boleta"), PEQUENA));
                t.addCell(cell);
                doc.add(t);
            }
        });
    }

    /** 5. Autorización de tratamiento de datos personales (Ley 1581 de 2012). */
    public byte[] autorizacionDatos(int idCliente) {
        try {
            List<Map<String, Object>> c = dao.consultar("SELECT * FROM Cliente WHERE id_cliente = ?", idCliente);
            if (c.isEmpty()) throw new ReglaNegocioException("No existe el cliente " + idCliente + ".");
            Map<String, Object> cl = c.get(0);
            String nombre = String.join(" ", Arrays.asList(texto(cl.get("primer_nombre")), texto(cl.get("segundo_nombre")),
                    texto(cl.get("primer_apellido")), texto(cl.get("segundo_apellido")))).replaceAll("\\s+", " ").trim();
            return PdfUtil.crear(false, (doc, w) -> {
                doc.add(new Paragraph("Autorización para el tratamiento de datos personales", SUBTITULO));
                doc.add(espacio());
                Paragraph p = new Paragraph("Yo, " + nombre + ", identificado(a) con " + cl.get("tipo_documento") + " N.º "
                        + cl.get("numero_documento") + ", mayor de edad, autorizo de manera previa, expresa e informada a QuindíoStay para "
                        + "recolectar, almacenar y usar mis datos personales (nombre, documento, correo, teléfono, fecha de nacimiento y ciudad de origen) "
                        + "con estas finalidades: gestionar mis reservas, boletas y pagos; enviarme recordatorios y notificaciones por el canal que escoja; "
                        + "y, solo si lo acepté en mi perfil, enviarme promociones.\n\n"
                        + "Conozco que, conforme a la Ley 1581 de 2012, tengo derecho a conocer, actualizar, rectificar y suprimir mis datos y a revocar esta "
                        + "autorización escribiendo al correo del prestador. Mi contraseña se almacena cifrada y nunca es visible para el personal.", NORMAL);
                p.setLeading(15);
                doc.add(p);
                doc.add(espacio()); doc.add(espacio());
                doc.add(datos(new String[][]{{"Correo registrado", texto(cl.get("email"))}, {"Fecha", texto(java.time.LocalDate.now())}}));
                doc.add(espacio()); doc.add(espacio()); doc.add(espacio());
                doc.add(new Paragraph("______________________________\nFirma del titular\n" + nombre, NORMAL));
            });
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    // ------------------- auxiliares -------------------
    private Map<String, Object> reserva(int id) {
        try {
            Map<String, Object> r = dao.reservaDetalle(id);
            if (r == null) throw new ReglaNegocioException("No existe la reserva " + id + ".");
            r.putAll(dao.estadoPago(id));
            return r;
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    private List<Map<String, Object>> boletas(int id) {
        try {
            return dao.boletasDeReserva(id);
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    private PdfPTable tablaBoletas(List<Map<String, Object>> bol) {
        List<List<String>> filas = new ArrayList<>();
        for (Map<String, Object> b : bol) {
            filas.add(List.of(texto(b.get("codigo")), texto(b.get("atraccion")), texto(b.get("fecha_funcion")),
                    texto(b.get("zona")), texto(b.get("tipo_entrada")), texto(b.get("estado_boleta"))));
        }
        return tabla(List.of("Código", "Atracción", "Función", "Zona", "Entrada", "Estado"), filas, new float[]{14, 30, 14, 14, 14, 14});
    }
}
