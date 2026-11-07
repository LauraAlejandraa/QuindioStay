package co.edu.uniquindio.quindiostay.controlador;

import co.edu.uniquindio.quindiostay.modelo.Atraccion;
import co.edu.uniquindio.quindiostay.modelo.SesionUsuario;
import co.edu.uniquindio.quindiostay.servicio.AtraccionServicio;
import co.edu.uniquindio.quindiostay.servicio.ReglaNegocioException;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * CRUD de Atraccion (el prototipo de la Entrega 2).
 * El administrador ve todas; el encargado solo las afiliadas a su prestador
 * (se filtra por Prestador_atraccion).
 */
@Controller
@RequestMapping("/atracciones")
public class AtraccionControlador {

    private final AtraccionServicio servicio;

    public AtraccionControlador(AtraccionServicio servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String buscar, HttpSession session, Model model) {
        SesionUsuario s = Sesiones.de(session);
        if (s.isEncargado()) {
            model.addAttribute("lista", servicio.listarPorPrestador(s.getIdPrestador()));
        } else if (buscar != null && !buscar.isBlank()) {
            model.addAttribute("lista", servicio.buscarPorNombre(buscar));   // botón «Consultar»
        } else {
            model.addAttribute("lista", servicio.listarTodos());
        }
        model.addAttribute("buscar", buscar);
        return "atracciones/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("obj", new Atraccion());
        return "atracciones/formulario";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable int id, HttpSession session, Model model) {
        revisarPermiso(id, session);
        model.addAttribute("obj", servicio.buscarPorId(id));
        return "atracciones/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("obj") Atraccion obj, BindingResult resultado, HttpSession session,
                          Model model, RedirectAttributes redirect) {
        SesionUsuario s = Sesiones.de(session);
        try {
            if (resultado.hasErrors()) {
                throw new ReglaNegocioException("Revise los datos: hay un campo con formato incorrecto.");
            }
            if (s.isEncargado() && obj.getIdAtraccion() == null) {
                servicio.crearParaPrestador(obj, s.getIdPrestador());   // atracción + afiliación
            } else {
                if (obj.getIdAtraccion() != null) {
                    revisarPermiso(obj.getIdAtraccion(), session);
                }
                servicio.guardar(obj);
            }
            redirect.addFlashAttribute("exito", "Guardado correctamente.");
            return "redirect:/atracciones";
        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
            return "atracciones/formulario";
        }
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable int id, HttpSession session, RedirectAttributes redirect) {
        try {
            revisarPermiso(id, session);
            servicio.eliminar(id);   // si tiene funciones o afiliaciones, MySQL lo impide (RESTRICT)
            redirect.addFlashAttribute("exito", "Registro eliminado.");
        } catch (ReglaNegocioException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/atracciones";
    }

    private void revisarPermiso(int idAtraccion, HttpSession session) {
        SesionUsuario s = Sesiones.de(session);
        if (s.isEncargado()) {
            Sesiones.exigir(servicio.perteneceAPrestador(idAtraccion, s.getIdPrestador()));
        }
    }
}
