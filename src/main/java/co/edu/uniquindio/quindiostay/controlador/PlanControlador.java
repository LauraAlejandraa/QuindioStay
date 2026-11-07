package co.edu.uniquindio.quindiostay.controlador;

import co.edu.uniquindio.quindiostay.modelo.Plan;
import co.edu.uniquindio.quindiostay.modelo.SesionUsuario;
import co.edu.uniquindio.quindiostay.servicio.PlanServicio;
import co.edu.uniquindio.quindiostay.servicio.PrestadorServicio;
import co.edu.uniquindio.quindiostay.servicio.ReglaNegocioException;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/** CRUD de Plan (RF03). El encargado solo gestiona los planes de su prestador. */
@Controller
@RequestMapping("/planes")
public class PlanControlador {

    private final PlanServicio servicio;
    private final PrestadorServicio prestadorServicio;

    public PlanControlador(PlanServicio servicio, PrestadorServicio prestadorServicio) {
        this.servicio = servicio;
        this.prestadorServicio = prestadorServicio;
    }

    @GetMapping
    public String listar(HttpSession session, Model model) {
        SesionUsuario s = Sesiones.de(session);
        model.addAttribute("lista", s.isEncargado() ? servicio.listarPorPrestador(s.getIdPrestador())
                                                    : servicio.listarTodos());
        cargarOpciones(session, model);
        return "planes/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(HttpSession session, Model model) {
        Plan p = new Plan();
        p.setEstado("activo");
        p.setIdPrestador(Sesiones.de(session).getIdPrestador());
        model.addAttribute("obj", p);
        cargarOpciones(session, model);
        return "planes/formulario";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable int id, HttpSession session, Model model) {
        Plan p = servicio.buscarPorId(id);
        revisarPermiso(p, session);
        model.addAttribute("obj", p);
        cargarOpciones(session, model);
        return "planes/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("obj") Plan obj, BindingResult resultado, HttpSession session,
                          Model model, RedirectAttributes redirect) {
        SesionUsuario s = Sesiones.de(session);
        try {
            if (resultado.hasErrors()) {
                throw new ReglaNegocioException("Revise los datos: hay un campo con formato incorrecto.");
            }
            if (s.isEncargado()) {
                if (obj.getIdPlan() != null) {
                    revisarPermiso(servicio.buscarPorId(obj.getIdPlan()), session);
                }
                obj.setIdPrestador(s.getIdPrestador());   // el encargado solo crea planes de su prestador
            }
            servicio.guardar(obj);
            redirect.addFlashAttribute("exito", "Guardado correctamente.");
            return "redirect:/planes";
        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
            cargarOpciones(session, model);
            return "planes/formulario";
        }
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable int id, HttpSession session, RedirectAttributes redirect) {
        try {
            revisarPermiso(servicio.buscarPorId(id), session);
            servicio.eliminar(id);
            redirect.addFlashAttribute("exito", "El plan quedó inactivo.");
        } catch (ReglaNegocioException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/planes";
    }

    private void revisarPermiso(Plan p, HttpSession session) {
        SesionUsuario s = Sesiones.de(session);
        if (s.isEncargado()) {
            Sesiones.exigir(p.getIdPrestador().equals(s.getIdPrestador()));
        }
    }

    private void cargarOpciones(HttpSession session, Model model) {
        SesionUsuario s = Sesiones.de(session);
        model.addAttribute("opcPrestador", s.isEncargado()
                ? Sesiones.filtrar(prestadorServicio.listarOpciones(), List.of(s.getIdPrestador()))
                : prestadorServicio.listarOpciones());
    }
}
