package co.edu.uniquindio.quindiostay.controlador;

import co.edu.uniquindio.quindiostay.modelo.Cuenta;
import co.edu.uniquindio.quindiostay.servicio.ClienteServicio;
import co.edu.uniquindio.quindiostay.servicio.CuentaServicio;
import co.edu.uniquindio.quindiostay.servicio.ReglaNegocioException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Capa de presentación del CRUD de Cuenta:
 * listar, nuevo, editar, guardar y eliminar.
 */
@Controller
@RequestMapping("/cuentas")
public class CuentaControlador {

    private final CuentaServicio servicio;
    private final ClienteServicio clienteServicio;

    public CuentaControlador(CuentaServicio servicio, ClienteServicio clienteServicio) {
        this.servicio = servicio;
        this.clienteServicio = clienteServicio;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("lista", servicio.listarTodos());
        cargarOpciones(model);
        return "cuentas/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("obj", new Cuenta());
        cargarOpciones(model);
        return "cuentas/formulario";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable int id, Model model) {
        model.addAttribute("obj", servicio.buscarPorId(id));
        cargarOpciones(model);
        return "cuentas/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("obj") Cuenta obj, BindingResult resultado,
                          Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            model.addAttribute("error", "Revise los datos: hay un campo con formato incorrecto.");
            cargarOpciones(model);
            return "cuentas/formulario";
        }
        try {
            servicio.guardar(obj);
            redirect.addFlashAttribute("exito", "Guardado correctamente.");
            return "redirect:/cuentas";
        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
            cargarOpciones(model);
            return "cuentas/formulario";
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
        return "redirect:/cuentas";
    }

    private void cargarOpciones(Model model) {
        model.addAttribute("opcCliente", clienteServicio.listarOpciones());
    }
}
