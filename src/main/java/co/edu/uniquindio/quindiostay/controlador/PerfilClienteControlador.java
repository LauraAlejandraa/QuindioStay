package co.edu.uniquindio.quindiostay.controlador;

import co.edu.uniquindio.quindiostay.modelo.PerfilCliente;
import co.edu.uniquindio.quindiostay.servicio.ClienteServicio;
import co.edu.uniquindio.quindiostay.servicio.PerfilClienteServicio;
import co.edu.uniquindio.quindiostay.servicio.ReglaNegocioException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Capa de presentación del CRUD de Perfil_cliente:
 * listar, nuevo, editar, guardar y eliminar.
 */
@Controller
@RequestMapping("/perfiles")
public class PerfilClienteControlador {

    private final PerfilClienteServicio servicio;
    private final ClienteServicio clienteServicio;

    public PerfilClienteControlador(PerfilClienteServicio servicio, ClienteServicio clienteServicio) {
        this.servicio = servicio;
        this.clienteServicio = clienteServicio;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("lista", servicio.listarTodos());
        cargarOpciones(model);
        return "perfiles/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("obj", new PerfilCliente());
        cargarOpciones(model);
        return "perfiles/formulario";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable int id, Model model) {
        model.addAttribute("obj", servicio.buscarPorId(id));
        cargarOpciones(model);
        return "perfiles/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("obj") PerfilCliente obj, BindingResult resultado,
                          Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            model.addAttribute("error", "Revise los datos: hay un campo con formato incorrecto.");
            cargarOpciones(model);
            return "perfiles/formulario";
        }
        try {
            servicio.guardar(obj);
            redirect.addFlashAttribute("exito", "Guardado correctamente.");
            return "redirect:/perfiles";
        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
            cargarOpciones(model);
            return "perfiles/formulario";
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
        return "redirect:/perfiles";
    }

    private void cargarOpciones(Model model) {
        model.addAttribute("opcCliente", clienteServicio.listarOpciones());
    }
}
