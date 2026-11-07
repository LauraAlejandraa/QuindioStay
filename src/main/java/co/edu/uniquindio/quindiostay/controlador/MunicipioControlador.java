package co.edu.uniquindio.quindiostay.controlador;

import co.edu.uniquindio.quindiostay.modelo.Municipio;
import co.edu.uniquindio.quindiostay.servicio.MunicipioServicio;
import co.edu.uniquindio.quindiostay.servicio.ReglaNegocioException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Capa de presentación del CRUD de Municipio:
 * listar, nuevo, editar, guardar y eliminar.
 */
@Controller
@RequestMapping("/municipios")
public class MunicipioControlador {

    private final MunicipioServicio servicio;

    public MunicipioControlador(MunicipioServicio servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("lista", servicio.listarTodos());
        cargarOpciones(model);
        return "municipios/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("obj", new Municipio());
        cargarOpciones(model);
        return "municipios/formulario";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable int id, Model model) {
        model.addAttribute("obj", servicio.buscarPorId(id));
        cargarOpciones(model);
        return "municipios/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("obj") Municipio obj, BindingResult resultado,
                          Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            model.addAttribute("error", "Revise los datos: hay un campo con formato incorrecto.");
            cargarOpciones(model);
            return "municipios/formulario";
        }
        try {
            servicio.guardar(obj);
            redirect.addFlashAttribute("exito", "Guardado correctamente.");
            return "redirect:/municipios";
        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
            cargarOpciones(model);
            return "municipios/formulario";
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
        return "redirect:/municipios";
    }

    private void cargarOpciones(Model model) {
    }
}
