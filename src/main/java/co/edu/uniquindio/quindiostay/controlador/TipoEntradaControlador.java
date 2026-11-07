package co.edu.uniquindio.quindiostay.controlador;

import co.edu.uniquindio.quindiostay.modelo.TipoEntrada;
import co.edu.uniquindio.quindiostay.servicio.TipoEntradaServicio;
import co.edu.uniquindio.quindiostay.servicio.ReglaNegocioException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Capa de presentación del CRUD de Tipo_entrada:
 * listar, nuevo, editar, guardar y eliminar.
 */
@Controller
@RequestMapping("/tipos-entrada")
public class TipoEntradaControlador {

    private final TipoEntradaServicio servicio;

    public TipoEntradaControlador(TipoEntradaServicio servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("lista", servicio.listarTodos());
        cargarOpciones(model);
        return "tipos-entrada/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("obj", new TipoEntrada());
        cargarOpciones(model);
        return "tipos-entrada/formulario";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable int id, Model model) {
        model.addAttribute("obj", servicio.buscarPorId(id));
        cargarOpciones(model);
        return "tipos-entrada/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("obj") TipoEntrada obj, BindingResult resultado,
                          Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            model.addAttribute("error", "Revise los datos: hay un campo con formato incorrecto.");
            cargarOpciones(model);
            return "tipos-entrada/formulario";
        }
        try {
            servicio.guardar(obj);
            redirect.addFlashAttribute("exito", "Guardado correctamente.");
            return "redirect:/tipos-entrada";
        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
            cargarOpciones(model);
            return "tipos-entrada/formulario";
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
        return "redirect:/tipos-entrada";
    }

    private void cargarOpciones(Model model) {
    }
}
