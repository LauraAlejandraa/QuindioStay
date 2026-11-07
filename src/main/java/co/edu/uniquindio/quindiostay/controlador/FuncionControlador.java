package co.edu.uniquindio.quindiostay.controlador;

import co.edu.uniquindio.quindiostay.modelo.Atraccion;
import co.edu.uniquindio.quindiostay.modelo.Funcion;
import co.edu.uniquindio.quindiostay.modelo.SesionUsuario;
import co.edu.uniquindio.quindiostay.servicio.AtraccionServicio;
import co.edu.uniquindio.quindiostay.servicio.FuncionServicio;
import co.edu.uniquindio.quindiostay.servicio.ReglaNegocioException;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** CRUD de Funcion (RF07). El encargado solo ve y programa funciones de sus atracciones. */
@Controller
@RequestMapping("/funciones")
public class FuncionControlador {

    private final FuncionServicio servicio;
    private final AtraccionServicio atraccionServicio;

    public FuncionControlador(FuncionServicio servicio, AtraccionServicio atraccionServicio) {
        this.servicio = servicio;
        this.atraccionServicio = atraccionServicio;
    }

    @GetMapping
    public String listar(HttpSession session, Model model) {
        SesionUsuario s = Sesiones.de(session);
        model.addAttribute("lista", s.isEncargado() ? servicio.listarPorPrestador(s.getIdPrestador())
                                                    : servicio.listarTodos());
        cargarOpciones(session, model);
        return "funciones/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(HttpSession session, Model model) {
        Funcion f = new Funcion();
        f.setEstado("programada");
        model.addAttribute("obj", f);
        cargarOpciones(session, model);
        return "funciones/formulario";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable int id, HttpSession session, Model model) {
        Funcion f = servicio.buscarPorId(id);
        revisarPermiso(f.getIdAtraccion(), session);
        model.addAttribute("obj", f);
        cargarOpciones(session, model);
        return "funciones/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("obj") Funcion obj, BindingResult resultado, HttpSession session,
                          Model model, RedirectAttributes redirect) {
        try {
            if (resultado.hasErrors()) {
                throw new ReglaNegocioException("Revise los datos: hay un campo con formato incorrecto.");
            }
            revisarPermiso(obj.getIdAtraccion(), session);
            servicio.guardar(obj);
            redirect.addFlashAttribute("exito", "Guardado correctamente.");
            return "redirect:/funciones";
        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
            cargarOpciones(session, model);
            return "funciones/formulario";
        }
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable int id, HttpSession session, RedirectAttributes redirect) {
        try {
            revisarPermiso(servicio.buscarPorId(id).getIdAtraccion(), session);
            servicio.eliminar(id);
            redirect.addFlashAttribute("exito", "La función quedó cancelada.");
        } catch (ReglaNegocioException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/funciones";
    }

    private void revisarPermiso(Integer idAtraccion, HttpSession session) {
        SesionUsuario s = Sesiones.de(session);
        if (s.isEncargado() && idAtraccion != null) {
            Sesiones.exigir(atraccionServicio.perteneceAPrestador(idAtraccion, s.getIdPrestador()));
        }
    }

    private void cargarOpciones(HttpSession session, Model model) {
        SesionUsuario s = Sesiones.de(session);
        if (s.isEncargado()) {
            model.addAttribute("opcAtraccion", Sesiones.filtrar(atraccionServicio.listarOpciones(),
                    atraccionServicio.listarPorPrestador(s.getIdPrestador()).stream().map(Atraccion::getIdAtraccion).toList()));
        } else {
            model.addAttribute("opcAtraccion", atraccionServicio.listarOpciones());
        }
    }
}
