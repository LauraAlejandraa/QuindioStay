package co.edu.uniquindio.quindiostay.controlador;

import co.edu.uniquindio.quindiostay.modelo.Notificacion;
import co.edu.uniquindio.quindiostay.servicio.ClienteServicio;
import co.edu.uniquindio.quindiostay.servicio.ReservaServicio;
import co.edu.uniquindio.quindiostay.servicio.NotificacionServicio;
import co.edu.uniquindio.quindiostay.servicio.ReglaNegocioException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Capa de presentación del CRUD de Notificacion:
 * listar, nuevo, editar, guardar y eliminar.
 */
@Controller
@RequestMapping("/notificaciones")
public class NotificacionControlador {

    private final NotificacionServicio servicio;
    private final ClienteServicio clienteServicio;
    private final ReservaServicio reservaServicio;

    public NotificacionControlador(NotificacionServicio servicio, ClienteServicio clienteServicio, ReservaServicio reservaServicio) {
        this.servicio = servicio;
        this.clienteServicio = clienteServicio;
        this.reservaServicio = reservaServicio;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("lista", servicio.listarTodos());
        cargarOpciones(model);
        return "notificaciones/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("obj", new Notificacion());
        cargarOpciones(model);
        return "notificaciones/formulario";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable int id, Model model) {
        model.addAttribute("obj", servicio.buscarPorId(id));
        cargarOpciones(model);
        return "notificaciones/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("obj") Notificacion obj, BindingResult resultado,
                          Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            model.addAttribute("error", "Revise los datos: hay un campo con formato incorrecto.");
            cargarOpciones(model);
            return "notificaciones/formulario";
        }
        try {
            servicio.guardar(obj);
            redirect.addFlashAttribute("exito", "Guardado correctamente.");
            return "redirect:/notificaciones";
        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
            cargarOpciones(model);
            return "notificaciones/formulario";
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
        return "redirect:/notificaciones";
    }

    private void cargarOpciones(Model model) {
        model.addAttribute("opcCliente", clienteServicio.listarOpciones());
        model.addAttribute("opcReserva", reservaServicio.listarOpciones());
    }
}
