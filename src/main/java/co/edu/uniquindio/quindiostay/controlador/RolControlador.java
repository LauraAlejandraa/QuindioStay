package co.edu.uniquindio.quindiostay.controlador;

import co.edu.uniquindio.quindiostay.modelo.Rol;
import co.edu.uniquindio.quindiostay.servicio.RolServicio;
import co.edu.uniquindio.quindiostay.servicio.ReglaNegocioException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Capa de presentación del CRUD de Rol:
 * listar, nuevo, editar, guardar y eliminar.
 */
@Controller
@RequestMapping("/roles")
public class RolControlador {

    private final RolServicio servicio;

    public RolControlador(RolServicio servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("lista", servicio.listarTodos());
        cargarOpciones(model);
        return "roles/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("obj", new Rol());
        cargarOpciones(model);
        return "roles/formulario";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable int id, Model model) {
        model.addAttribute("obj", servicio.buscarPorId(id));
        cargarOpciones(model);
        return "roles/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("obj") Rol obj, BindingResult resultado,
                          Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            model.addAttribute("error", "Revise los datos: hay un campo con formato incorrecto.");
            cargarOpciones(model);
            return "roles/formulario";
        }
        try {
            servicio.guardar(obj);
            redirect.addFlashAttribute("exito", "Guardado correctamente.");
            return "redirect:/roles";
        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
            cargarOpciones(model);
            return "roles/formulario";
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
        return "redirect:/roles";
    }

    private void cargarOpciones(Model model) {
    }
}
