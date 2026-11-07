package co.edu.uniquindio.quindiostay.controlador;

import co.edu.uniquindio.quindiostay.modelo.PrestadorAtraccion;
import co.edu.uniquindio.quindiostay.servicio.AtraccionServicio;
import co.edu.uniquindio.quindiostay.servicio.PrestadorAtraccionServicio;
import co.edu.uniquindio.quindiostay.servicio.PrestadorServicio;
import co.edu.uniquindio.quindiostay.servicio.ReglaNegocioException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

/** CRUD de Prestador_atraccion (clave primaria compuesta). */
@Controller
@RequestMapping("/afiliaciones")
public class PrestadorAtraccionControlador {

    private final PrestadorAtraccionServicio servicio;
    private final PrestadorServicio prestadorServicio;
    private final AtraccionServicio atraccionServicio;

    public PrestadorAtraccionControlador(PrestadorAtraccionServicio servicio, PrestadorServicio prestadorServicio,
                                         AtraccionServicio atraccionServicio) {
        this.servicio = servicio;
        this.prestadorServicio = prestadorServicio;
        this.atraccionServicio = atraccionServicio;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("lista", servicio.listarTodos());
        cargarOpciones(model);
        return "afiliaciones/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        PrestadorAtraccion x = new PrestadorAtraccion();
        x.setFechaAfiliacion(LocalDate.now());
        model.addAttribute("obj", x);
        cargarOpciones(model);
        return "afiliaciones/formulario";
    }

    @GetMapping("/{idPrestador}/{idAtraccion}/editar")
    public String editar(@PathVariable int idPrestador, @PathVariable int idAtraccion, Model model) {
        model.addAttribute("obj", servicio.buscarPorId(idPrestador, idAtraccion));
        cargarOpciones(model);
        return "afiliaciones/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("obj") PrestadorAtraccion obj, BindingResult resultado,
                          Model model, RedirectAttributes redirect) {
        try {
            if (resultado.hasErrors()) {
                throw new ReglaNegocioException("Revise los datos: hay un campo con formato incorrecto.");
            }
            servicio.guardar(obj);
            redirect.addFlashAttribute("exito", "Guardado correctamente.");
            return "redirect:/afiliaciones";
        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
            cargarOpciones(model);
            return "afiliaciones/formulario";
        }
    }

    @PostMapping("/{idPrestador}/{idAtraccion}/eliminar")
    public String eliminar(@PathVariable int idPrestador, @PathVariable int idAtraccion, RedirectAttributes redirect) {
        try {
            servicio.eliminar(idPrestador, idAtraccion);
            redirect.addFlashAttribute("exito", "Afiliación eliminada.");
        } catch (ReglaNegocioException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/afiliaciones";
    }

    private void cargarOpciones(Model model) {
        model.addAttribute("opcPrestador", prestadorServicio.listarOpciones());
        model.addAttribute("opcAtraccion", atraccionServicio.listarOpciones());
    }
}
