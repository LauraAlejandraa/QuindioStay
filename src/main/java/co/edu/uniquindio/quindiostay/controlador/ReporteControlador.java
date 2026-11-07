package co.edu.uniquindio.quindiostay.controlador;

import co.edu.uniquindio.quindiostay.modelo.Reporte;
import co.edu.uniquindio.quindiostay.servicio.PdfServicio;
import co.edu.uniquindio.quindiostay.servicio.ReporteServicio;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Pantalla de reportes (RF13): tabla en pantalla, gráfico en los reportes 6, 9 y 10 y botón de PDF. */
@Controller
@RequestMapping("/reportes")
public class ReporteControlador {

    private final ReporteServicio servicio;
    private final PdfServicio pdf;

    public ReporteControlador(ReporteServicio servicio, PdfServicio pdf) {
        this.servicio = servicio;
        this.pdf = pdf;
    }

    @GetMapping
    public String indice(Model model) {
        model.addAttribute("reportes", servicio.listar());
        return "reportes/indice";
    }

    @GetMapping("/{n}")
    public String ver(@PathVariable int n, @RequestParam Map<String, String> filtros, Model model) {
        Reporte r = servicio.buscar(n);
        List<Map<String, Object>> filas = servicio.ejecutar(r, filtros);
        model.addAttribute("r", r);
        model.addAttribute("filas", filas);
        model.addAttribute("columnas", filas.isEmpty() ? List.of() : PdfServicio.columnasVisibles(filas));
        model.addAttribute("filtros", filtros);
        if (r.isGrafico()) {
            model.addAttribute("etiquetas", filas.stream().limit(12).map(f -> String.valueOf(f.get(r.getColumnaEtiqueta()))).toList());
            model.addAttribute("valores", filas.stream().limit(12).map(f -> f.get(r.getColumnaValor())).toList());
        }
        return "reportes/reporte";
    }

    @GetMapping("/{n}/pdf")
    public ResponseEntity<byte[]> exportar(@PathVariable int n, @RequestParam Map<String, String> filtros) {
        Reporte r = servicio.buscar(n);
        byte[] bytes = pdf.reporte(r, servicio.ejecutar(r, filtros), filtros);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=reporte-" + n + ".pdf").body(bytes);
    }
}
