package co.edu.uniquindio.quindiostay.controlador;

import co.edu.uniquindio.quindiostay.modelo.SesionUsuario;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Pone la sesión en todas las pantallas para que el menú sepa qué mostrar. */
@ControllerAdvice
public class AtributosGlobales {

    @ModelAttribute("sesion")
    public SesionUsuario sesion(HttpSession session) {
        return (SesionUsuario) session.getAttribute("sesion");
    }
}
