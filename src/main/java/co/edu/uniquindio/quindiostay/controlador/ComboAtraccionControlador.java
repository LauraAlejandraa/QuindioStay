package co.edu.uniquindio.quindiostay.controlador;

import co.edu.uniquindio.quindiostay.modelo.ComboAtraccion;
import co.edu.uniquindio.quindiostay.servicio.ComboServicio;
import co.edu.uniquindio.quindiostay.servicio.AtraccionServicio;
import co.edu.uniquindio.quindiostay.servicio.ComboAtraccionServicio;
import co.edu.uniquindio.quindiostay.servicio.ReglaNegocioException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Capa de presentación del CRUD de Combo_atraccion:
 * listar, nuevo, editar, guardar y eliminar.
 */
@Controller
@RequestMapping("/combo-atracciones")
public class ComboAtraccionControlador {

    private final ComboAtraccionServicio servicio;
    private final ComboServicio comboServicio;
    private final AtraccionServicio atraccionServicio;

    public ComboAtraccionControlador(ComboAtraccionServicio servicio, ComboServicio comboServicio, AtraccionServicio atraccionServicio) {
        this.servicio = servicio;
        this.comboServicio = comboServicio;
        this.atraccionServicio = atraccionServicio;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("lista", servicio.listarTodos());
        cargarOpciones(model);
        return "combo-atracciones/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("obj", new ComboAtraccion());
        cargarOpciones(model);
        return "combo-atracciones/formulario";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable int id, Model model) {
        model.addAttribute("obj", servicio.buscarPorId(id));
        cargarOpciones(model);
        return "combo-atracciones/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("obj") ComboAtraccion obj, BindingResult resultado,
                          Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            model.addAttribute("error", "Revise los datos: hay un campo con formato incorrecto.");
            cargarOpciones(model);
            return "combo-atracciones/formulario";
        }
        try {
            servicio.guardar(obj);
            redirect.addFlashAttribute("exito", "Guardado correctamente.");
            return "redirect:/combo-atracciones";
        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
            cargarOpciones(model);
            return "combo-atracciones/formulario";
        }
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable int id, RedirectAttributes redirect) {
        try {
            servicio.eliminar(id);
            redirect.addFlashAttribute("exito", "Registro eliminado.");
        } catch (ReglaNegocioException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/combo-atracciones";
    }

    private void cargarOpciones(Model model) {
        model.addAttribute("opcCombo", comboServicio.listarOpciones());
        model.addAttribute("opcAtraccion", atraccionServicio.listarOpciones());
    }
}
