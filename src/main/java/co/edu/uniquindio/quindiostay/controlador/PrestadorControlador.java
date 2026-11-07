package co.edu.uniquindio.quindiostay.controlador;

import co.edu.uniquindio.quindiostay.modelo.Prestador;
import co.edu.uniquindio.quindiostay.servicio.MunicipioServicio;
import co.edu.uniquindio.quindiostay.servicio.TipoPrestadorServicio;
import co.edu.uniquindio.quindiostay.servicio.PrestadorServicio;
import co.edu.uniquindio.quindiostay.servicio.ReglaNegocioException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Capa de presentación del CRUD de Prestador:
 * listar, nuevo, editar, guardar y eliminar.
 */
@Controller
@RequestMapping("/prestadores")
public class PrestadorControlador {

    private final PrestadorServicio servicio;
    private final MunicipioServicio municipioServicio;
    private final TipoPrestadorServicio tipoPrestadorServicio;

    public PrestadorControlador(PrestadorServicio servicio, MunicipioServicio municipioServicio, TipoPrestadorServicio tipoPrestadorServicio) {
        this.servicio = servicio;
        this.municipioServicio = municipioServicio;
        this.tipoPrestadorServicio = tipoPrestadorServicio;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("lista", servicio.listarTodos());
        cargarOpciones(model);
        return "prestadores/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("obj", new Prestador());
        cargarOpciones(model);
        return "prestadores/formulario";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable int id, Model model) {
        model.addAttribute("obj", servicio.buscarPorId(id));
        cargarOpciones(model);
        return "prestadores/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("obj") Prestador obj, BindingResult resultado,
                          Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            model.addAttribute("error", "Revise los datos: hay un campo con formato incorrecto.");
            cargarOpciones(model);
            return "prestadores/formulario";
        }
        try {
            servicio.guardar(obj);
            redirect.addFlashAttribute("exito", "Guardado correctamente.");
            return "redirect:/prestadores";
        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
            cargarOpciones(model);
            return "prestadores/formulario";
        }
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable int id, RedirectAttributes redirect) {
        try {
            servicio.eliminar(id);
            redirect.addFlashAttribute("exito", "El prestador quedó retirado.");
        } catch (ReglaNegocioException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/prestadores";
    }

    private void cargarOpciones(Model model) {
        model.addAttribute("opcMunicipio", municipioServicio.listarOpciones());
        model.addAttribute("opcTipoPrestador", tipoPrestadorServicio.listarOpciones());
    }
}
