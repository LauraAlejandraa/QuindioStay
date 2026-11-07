package co.edu.uniquindio.quindiostay.controlador;

import co.edu.uniquindio.quindiostay.dao.VistaDAO;
import co.edu.uniquindio.quindiostay.servicio.MensajesError;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.sql.SQLException;

/** Pantalla de inicio: cambia según el rol (el menú lo arma fragmentos.html). */
@Controller
public class InicioControlador {

    private final VistaDAO vistaDAO;

    public InicioControlador(VistaDAO vistaDAO) {
        this.vistaDAO = vistaDAO;
    }

    @GetMapping("/inicio")
    public String inicio(@RequestParam(required = false) String sinPermiso, Model model) {
        try {
            model.addAttribute("resumen", vistaDAO.resumenInicio());
        } catch (SQLException e) {
            model.addAttribute("error", MensajesError.traducir(e));
        }
        if (sinPermiso != null) {
            model.addAttribute("error", "Su rol no tiene permiso para entrar a esa pantalla.");
        }
        return "inicio";
    }
}
