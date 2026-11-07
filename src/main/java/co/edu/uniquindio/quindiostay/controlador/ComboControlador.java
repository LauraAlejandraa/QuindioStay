package co.edu.uniquindio.quindiostay.controlador;

import co.edu.uniquindio.quindiostay.modelo.Combo;
import co.edu.uniquindio.quindiostay.servicio.ComboServicio;
import co.edu.uniquindio.quindiostay.servicio.ReglaNegocioException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Capa de presentación del CRUD de Combo:
 * listar, nuevo, editar, guardar y eliminar.
 */
@Controller
@RequestMapping("/combos")
public class ComboControlador {

    private final ComboServicio servicio;

    public ComboControlador(ComboServicio servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("lista", servicio.listarTodos());
        cargarOpciones(model);
        return "combos/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("obj", new Combo());
        cargarOpciones(model);
        return "combos/formulario";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable int id, Model model) {
        model.addAttribute("obj", servicio.buscarPorId(id));
        cargarOpciones(model);
        return "combos/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("obj") Combo obj, BindingResult resultado,
                          Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            model.addAttribute("error", "Revise los datos: hay un campo con formato incorrecto.");
            cargarOpciones(model);
            return "combos/formulario";
        }
        try {
            servicio.guardar(obj);
            redirect.addFlashAttribute("exito", "Guardado correctamente.");
            return "redirect:/combos";
        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
            cargarOpciones(model);
            return "combos/formulario";
        }
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable int id, RedirectAttributes redirect) {
        try {
            servicio.eliminar(id);
            redirect.addFlashAttribute("exito", "El registro quedó inactivo.");
        } catch (ReglaNegocioException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/combos";
    }

    private void cargarOpciones(Model model) {
    }
}
