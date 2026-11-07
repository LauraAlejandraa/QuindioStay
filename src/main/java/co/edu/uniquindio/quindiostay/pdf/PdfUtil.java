package co.edu.uniquindio.quindiostay.pdf;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Ayudas comunes para armar los PDF de reportes y plantillas con OpenPDF. */
public final class PdfUtil {

    public static final Color VERDE = new Color(0x1f, 0x6f, 0x43);
    public static final Color VERDE_CLARO = new Color(0xe8, 0xf3, 0xec);
    public static final Font TITULO = new Font(Font.HELVETICA, 18, Font.BOLD, VERDE);
    public static final Font SUBTITULO = new Font(Font.HELVETICA, 12, Font.BOLD, Color.DARK_GRAY);
    public static final Font NORMAL = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.BLACK);
    public static final Font NEGRITA = new Font(Font.HELVETICA, 10, Font.BOLD, Color.BLACK);
    public static final Font PEQUENA = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.GRAY);
    public static final Font ENCABEZADO = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
    private static final NumberFormat PESOS = NumberFormat.getIntegerInstance(new Locale("es", "CO"));
    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private PdfUtil() {
    }

    public interface Contenido {
        void escribir(Document doc, PdfWriter writer) throws Exception;
    }

    /** Crea el documento (carta; horizontal si se pide), deja el contenido y devuelve los bytes. */
    public static byte[] crear(boolean horizontal, Contenido contenido) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(horizontal ? PageSize.LETTER.rotate() : PageSize.LETTER, 40, 40, 40, 50);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new PdfPageEventHelper() {
                @Override
                public void onEndPage(PdfWriter w, Document d) {
                    PdfContentByte cb = w.getDirectContent();
                    ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                            new Phrase("QuindíoStay · Plataforma de turismo del Quindío", PEQUENA), d.left(), 28, 0);
                    ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                            new Phrase("Página " + w.getPageNumber(), PEQUENA), d.right(), 28, 0);
                }
            });
            doc.open();
            doc.add(new Paragraph("QuindíoStay", TITULO));
            contenido.escribir(doc, writer);
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el PDF: " + e.getMessage(), e);
        }
    }

    public static String pesos(Object valor) {
        if (valor == null) return "";
        BigDecimal v = valor instanceof BigDecimal ? (BigDecimal) valor : new BigDecimal(valor.toString());
        return "$" + PESOS.format(v.setScale(0, java.math.RoundingMode.HALF_UP));
    }

    public static String texto(Object v) {
        if (v == null) return "";
        if (v instanceof java.sql.Date) return ((java.sql.Date) v).toLocalDate().format(DMY);
        if (v instanceof java.sql.Timestamp) return ((java.sql.Timestamp) v).toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        if (v instanceof LocalDate) return ((LocalDate) v).format(DMY);
        if (v instanceof LocalDateTime) return ((LocalDateTime) v).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        if (v instanceof java.sql.Time) return v.toString().substring(0, 5);
        if (v instanceof BigDecimal) {
            BigDecimal b = (BigDecimal) v;
            return b.stripTrailingZeros().scale() <= 0 ? PESOS.format(b) : b.setScale(1, java.math.RoundingMode.HALF_UP).toPlainString();
        }
        return v.toString();
    }

    public static String encabezado(String columna) {
        String s = columna.replace('_', ' ');
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    /** Tabla con encabezado verde. */
    public static PdfPTable tabla(List<String> columnas, List<List<String>> filas, float[] anchos, int... derecha) {
        PdfPTable t = new PdfPTable(columnas.size());
        t.setWidthPercentage(100);
        if (anchos != null) {
            try { t.setWidths(anchos); } catch (DocumentException ignored) { }
        }
        t.setHeaderRows(1);
        for (String c : columnas) {
            PdfPCell cell = new PdfPCell(new Phrase(c, ENCABEZADO));
            cell.setBackgroundColor(VERDE);
            cell.setPadding(4);
            t.addCell(cell);
        }
        boolean alt = false;
        for (List<String> fila : filas) {
            for (int i = 0; i < fila.size(); i++) {
                PdfPCell cell = new PdfPCell(new Phrase(fila.get(i), NORMAL));
                cell.setPadding(3);
                if (alt) cell.setBackgroundColor(VERDE_CLARO);
                for (int d : derecha) if (d == i) cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                t.addCell(cell);
            }
            alt = !alt;
        }
        return t;
    }

    /** Dos columnas «etiqueta: valor» sin bordes. */
    public static PdfPTable datos(String[][] pares) {
        PdfPTable t = new PdfPTable(new float[]{30, 70});
        t.setWidthPercentage(100);
        for (String[] p : pares) {
            PdfPCell a = new PdfPCell(new Phrase(p[0], NEGRITA));
            PdfPCell b = new PdfPCell(new Phrase(p[1] == null ? "" : p[1], NORMAL));
            a.setBorder(Rectangle.BOTTOM); b.setBorder(Rectangle.BOTTOM);
            a.setBorderColor(Color.LIGHT_GRAY); b.setBorderColor(Color.LIGHT_GRAY);
            a.setPadding(4); b.setPadding(4);
            t.addCell(a); t.addCell(b);
        }
        return t;
    }

    public static Paragraph espacio() {
        return new Paragraph(" ", NORMAL);
    }

    /** Gráfico de barras horizontales dibujado directamente en el PDF. */
    public static void graficoBarras(Document doc, PdfWriter writer, List<Map<String, Object>> filas,
                                     String colEtiqueta, String colValor, String titulo) throws DocumentException {
        if (filas.isEmpty()) return;
        doc.add(new Paragraph(titulo, SUBTITULO));
        int n = Math.min(filas.size(), 12);
        double max = 0;
        for (int i = 0; i < n; i++) max = Math.max(max, ((Number) filas.get(i).get(colValor)).doubleValue());
        if (max == 0) max = 1;
        float alto = 16f, separacion = 5f;
        float total = n * (alto + separacion) + 10;
        if (writer.getVerticalPosition(true) - total < doc.bottom()) doc.newPage();
        float y = writer.getVerticalPosition(true) - 14;
        PdfContentByte cb = writer.getDirectContent();
        float xBarra = doc.left() + 190, anchoMax = doc.right() - xBarra - 80;
        for (int i = 0; i < n; i++) {
            Map<String, Object> f = filas.get(i);
            double v = ((Number) f.get(colValor)).doubleValue();
            String et = String.valueOf(f.get(colEtiqueta));
            if (et.length() > 34) et = et.substring(0, 33) + "…";
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT, new Phrase(et, PEQUENA), xBarra - 6, y + 4, 0);
            cb.setColorFill(VERDE);
            cb.rectangle(xBarra, y, (float) (anchoMax * v / max), alto);
            cb.fill();
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase(colValor.equals("ingresos") ? pesos(f.get(colValor)) : texto(f.get(colValor)), PEQUENA),
                    xBarra + (float) (anchoMax * v / max) + 4, y + 4, 0);
            y -= alto + separacion;
        }
        Paragraph relleno = new Paragraph(" ");
        relleno.setSpacingBefore(total);
        doc.add(relleno);
    }
}
