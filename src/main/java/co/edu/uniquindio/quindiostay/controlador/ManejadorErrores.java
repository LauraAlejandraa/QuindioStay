package co.edu.uniquindio.quindiostay.controlador;

import co.edu.uniquindio.quindiostay.servicio.ReglaNegocioException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Si un error llega hasta aquí (por ejemplo, MySQL está apagado), se muestra
 * una página con un mensaje entendible en lugar del error de Java.
 */
@ControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(ReglaNegocioException.class)
    public String reglaNegocio(ReglaNegocioException e, Model model) {
        model.addAttribute("mensaje", e.getMessage());
        return "error-app";
    }
}
