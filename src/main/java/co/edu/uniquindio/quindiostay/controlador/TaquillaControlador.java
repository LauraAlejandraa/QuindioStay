package co.edu.uniquindio.quindiostay.controlador;

import co.edu.uniquindio.quindiostay.servicio.BoletaServicio;
import co.edu.uniquindio.quindiostay.servicio.ReglaNegocioException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Taquilla (RF09): el operador escribe o escanea el código de la boleta y la marca como usada. */
@Controller
@RequestMapping("/taquilla")
public class TaquillaControlador {

    private final BoletaServicio boletaServicio;

    public TaquillaControlador(BoletaServicio boletaServicio) {
        this.boletaServicio = boletaServicio;
    }

    @GetMapping
    public String pantalla() {
        return "taquilla";
    }

    @PostMapping
    public String validar(@RequestParam String codigo, Model model) {
        try {
            model.addAttribute("boleta", boletaServicio.validarEnTaquilla(codigo));
            model.addAttribute("exito", "Boleta válida: puede ingresar. Quedó marcada como usada.");
        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
        }
        model.addAttribute("codigo", codigo);
        return "taquilla";
    }
}
