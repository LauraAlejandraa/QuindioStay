package co.edu.uniquindio.quindiostay.controlador;

import co.edu.uniquindio.quindiostay.modelo.Boleta;
import co.edu.uniquindio.quindiostay.servicio.ReservaServicio;
import co.edu.uniquindio.quindiostay.servicio.ZonaServicio;
import co.edu.uniquindio.quindiostay.servicio.TipoEntradaServicio;
import co.edu.uniquindio.quindiostay.servicio.ComboServicio;
import co.edu.uniquindio.quindiostay.servicio.BoletaServicio;
import co.edu.uniquindio.quindiostay.servicio.ReglaNegocioException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Capa de presentación del CRUD de Boleta:
 * listar, nuevo, editar, guardar y eliminar.
 */
@Controller
@RequestMapping("/boletas")
public class BoletaControlador {

    private final BoletaServicio servicio;
    private final ReservaServicio reservaServicio;
    private final ZonaServicio zonaServicio;
    private final TipoEntradaServicio tipoEntradaServicio;
    private final ComboServicio comboServicio;

    public BoletaControlador(BoletaServicio servicio, ReservaServicio reservaServicio, ZonaServicio zonaServicio, TipoEntradaServicio tipoEntradaServicio, ComboServicio comboServicio) {
        this.servicio = servicio;
        this.reservaServicio = reservaServicio;
        this.zonaServicio = zonaServicio;
        this.tipoEntradaServicio = tipoEntradaServicio;
        this.comboServicio = comboServicio;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("lista", servicio.listarTodos());
        cargarOpciones(model);
        return "boletas/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("obj", new Boleta());
        cargarOpciones(model);
        return "boletas/formulario";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable int id, Model model) {
        model.addAttribute("obj", servicio.buscarPorId(id));
        cargarOpciones(model);
        return "boletas/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("obj") Boleta obj, BindingResult resultado,
                          Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            model.addAttribute("error", "Revise los datos: hay un campo con formato incorrecto.");
            cargarOpciones(model);
            return "boletas/formulario";
        }
        try {
            servicio.guardar(obj);
            redirect.addFlashAttribute("exito", "Guardado correctamente.");
            return "redirect:/boletas";
        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
            cargarOpciones(model);
            return "boletas/formulario";
        }
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable int id, RedirectAttributes redirect) {
        try {
            servicio.eliminar(id);
            redirect.addFlashAttribute("exito", "La boleta quedó anulada.");
        } catch (ReglaNegocioException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/boletas";
    }

    private void cargarOpciones(Model model) {
        model.addAttribute("opcReserva", reservaServicio.listarOpciones());
        model.addAttribute("opcZona", zonaServicio.listarOpciones());
        model.addAttribute("opcTipoEntrada", tipoEntradaServicio.listarOpciones());
        model.addAttribute("opcCombo", comboServicio.listarOpciones());
    }
}
