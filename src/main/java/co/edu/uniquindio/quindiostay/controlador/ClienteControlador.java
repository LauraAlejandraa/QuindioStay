package co.edu.uniquindio.quindiostay.controlador;

import co.edu.uniquindio.quindiostay.modelo.Cliente;
import co.edu.uniquindio.quindiostay.servicio.ClienteServicio;
import co.edu.uniquindio.quindiostay.servicio.ReglaNegocioException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Capa de presentación del CRUD de Cliente:
 * listar, nuevo, editar, guardar y eliminar.
 */
@Controller
@RequestMapping("/clientes")
public class ClienteControlador {

    private final ClienteServicio servicio;

    public ClienteControlador(ClienteServicio servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("lista", servicio.listarTodos());
        cargarOpciones(model);
        return "clientes/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("obj", new Cliente());
        cargarOpciones(model);
        return "clientes/formulario";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable int id, Model model) {
        model.addAttribute("obj", servicio.buscarPorId(id));
        cargarOpciones(model);
        return "clientes/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("obj") Cliente obj, BindingResult resultado,
                          Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            model.addAttribute("error", "Revise los datos: hay un campo con formato incorrecto.");
            cargarOpciones(model);
            return "clientes/formulario";
        }
        try {
            servicio.guardar(obj);
            redirect.addFlashAttribute("exito", "Guardado correctamente.");
            return "redirect:/clientes";
        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
            cargarOpciones(model);
            return "clientes/formulario";
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
        return "redirect:/clientes";
    }

    private void cargarOpciones(Model model) {
    }
}
