package co.edu.uniquindio.quindiostay.controlador;

import co.edu.uniquindio.quindiostay.modelo.Pago;
import co.edu.uniquindio.quindiostay.servicio.ReservaServicio;
import co.edu.uniquindio.quindiostay.servicio.PagoServicio;
import co.edu.uniquindio.quindiostay.servicio.ReglaNegocioException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Capa de presentación del CRUD de Pago:
 * listar, nuevo, editar, guardar y eliminar.
 */
@Controller
@RequestMapping("/pagos")
public class PagoControlador {

    private final PagoServicio servicio;
    private final ReservaServicio reservaServicio;

    public PagoControlador(PagoServicio servicio, ReservaServicio reservaServicio) {
        this.servicio = servicio;
        this.reservaServicio = reservaServicio;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("lista", servicio.listarTodos());
        cargarOpciones(model);
        return "pagos/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("obj", new Pago());
        cargarOpciones(model);
        return "pagos/formulario";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable int id, Model model) {
        model.addAttribute("obj", servicio.buscarPorId(id));
        cargarOpciones(model);
        return "pagos/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("obj") Pago obj, BindingResult resultado,
                          Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            model.addAttribute("error", "Revise los datos: hay un campo con formato incorrecto.");
            cargarOpciones(model);
            return "pagos/formulario";
        }
        try {
            servicio.guardar(obj);
            redirect.addFlashAttribute("exito", "Guardado correctamente.");
            return "redirect:/pagos";
        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
            cargarOpciones(model);
            return "pagos/formulario";
        }
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable int id, RedirectAttributes redirect) {
        try {
            servicio.eliminar(id);
            redirect.addFlashAttribute("exito", "El pago quedó reembolsado.");
        } catch (ReglaNegocioException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/pagos";
    }

    private void cargarOpciones(Model model) {
        model.addAttribute("opcReserva", reservaServicio.listarOpciones());
    }
}
