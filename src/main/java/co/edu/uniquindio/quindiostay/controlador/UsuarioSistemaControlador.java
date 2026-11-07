package co.edu.uniquindio.quindiostay.controlador;

import co.edu.uniquindio.quindiostay.modelo.UsuarioSistema;
import co.edu.uniquindio.quindiostay.servicio.RolServicio;
import co.edu.uniquindio.quindiostay.servicio.PrestadorServicio;
import co.edu.uniquindio.quindiostay.servicio.UsuarioSistemaServicio;
import co.edu.uniquindio.quindiostay.servicio.ReglaNegocioException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Capa de presentación del CRUD de Usuario_sistema:
 * listar, nuevo, editar, guardar y eliminar.
 */
@Controller
@RequestMapping("/usuarios")
public class UsuarioSistemaControlador {

    private final UsuarioSistemaServicio servicio;
    private final RolServicio rolServicio;
    private final PrestadorServicio prestadorServicio;

    public UsuarioSistemaControlador(UsuarioSistemaServicio servicio, RolServicio rolServicio, PrestadorServicio prestadorServicio) {
        this.servicio = servicio;
        this.rolServicio = rolServicio;
        this.prestadorServicio = prestadorServicio;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("lista", servicio.listarTodos());
        cargarOpciones(model);
        return "usuarios/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("obj", new UsuarioSistema());
        cargarOpciones(model);
        return "usuarios/formulario";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable int id, Model model) {
        model.addAttribute("obj", servicio.buscarPorId(id));
        cargarOpciones(model);
        return "usuarios/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("obj") UsuarioSistema obj, BindingResult resultado,
                          Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            model.addAttribute("error", "Revise los datos: hay un campo con formato incorrecto.");
            cargarOpciones(model);
            return "usuarios/formulario";
        }
        try {
            servicio.guardar(obj);
            redirect.addFlashAttribute("exito", "Guardado correctamente.");
            return "redirect:/usuarios";
        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
            cargarOpciones(model);
            return "usuarios/formulario";
        }
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable int id, RedirectAttributes redirect) {
        try {
            servicio.eliminar(id);
            redirect.addFlashAttribute("exito", "El usuario quedó bloqueado.");
        } catch (ReglaNegocioException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/usuarios";
    }

    private void cargarOpciones(Model model) {
        model.addAttribute("opcRol", rolServicio.listarOpciones());
        model.addAttribute("opcPrestador", prestadorServicio.listarOpciones());
    }
}
