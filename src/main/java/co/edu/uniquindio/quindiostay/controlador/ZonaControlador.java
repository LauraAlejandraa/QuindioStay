package co.edu.uniquindio.quindiostay.controlador;

import co.edu.uniquindio.quindiostay.modelo.Funcion;
import co.edu.uniquindio.quindiostay.modelo.SesionUsuario;
import co.edu.uniquindio.quindiostay.modelo.Zona;
import co.edu.uniquindio.quindiostay.servicio.AtraccionServicio;
import co.edu.uniquindio.quindiostay.servicio.FuncionServicio;
import co.edu.uniquindio.quindiostay.servicio.ReglaNegocioException;
import co.edu.uniquindio.quindiostay.servicio.ZonaServicio;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** CRUD de Zona (aforo y precio de cada función). El encargado solo ve las de sus atracciones. */
@Controller
@RequestMapping("/zonas")
public class ZonaControlador {

    private final ZonaServicio servicio;
    private final FuncionServicio funcionServicio;
    private final AtraccionServicio atraccionServicio;

    public ZonaControlador(ZonaServicio servicio, FuncionServicio funcionServicio, AtraccionServicio atraccionServicio) {
        this.servicio = servicio;
        this.funcionServicio = funcionServicio;
        this.atraccionServicio = atraccionServicio;
    }

    @GetMapping
    public String listar(HttpSession session, Model model) {
        SesionUsuario s = Sesiones.de(session);
        model.addAttribute("lista", s.isEncargado() ? servicio.listarPorPrestador(s.getIdPrestador())
                                                    : servicio.listarTodos());
        cargarOpciones(session, model);
        return "zonas/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(@RequestParam(required = false) Integer funcion, HttpSession session, Model model) {
        Zona z = new Zona();
        z.setIdFuncion(funcion);
        model.addAttribute("obj", z);
        cargarOpciones(session, model);
        return "zonas/formulario";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable int id, HttpSession session, Model model) {
        Zona z = servicio.buscarPorId(id);
        revisarPermiso(z.getIdFuncion(), session);
        model.addAttribute("obj", z);
        cargarOpciones(session, model);
        return "zonas/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("obj") Zona obj, BindingResult resultado, HttpSession session,
                          Model model, RedirectAttributes redirect) {
        try {
            if (resultado.hasErrors()) {
                throw new ReglaNegocioException("Revise los datos: hay un campo con formato incorrecto.");
            }
            revisarPermiso(obj.getIdFuncion(), session);
            servicio.guardar(obj);
            redirect.addFlashAttribute("exito", "Guardado correctamente.");
            return "redirect:/zonas";
        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
            cargarOpciones(session, model);
            return "zonas/formulario";
        }
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable int id, HttpSession session, RedirectAttributes redirect) {
        try {
            revisarPermiso(servicio.buscarPorId(id).getIdFuncion(), session);
            servicio.eliminar(id);   // si tiene boletas, MySQL lo impide (fk_boleta_zona RESTRICT)
            redirect.addFlashAttribute("exito", "Registro eliminado.");
        } catch (ReglaNegocioException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/zonas";
    }

    private void revisarPermiso(Integer idFuncion, HttpSession session) {
        SesionUsuario s = Sesiones.de(session);
        if (s.isEncargado() && idFuncion != null) {
            Funcion f = funcionServicio.buscarPorId(idFuncion);
            Sesiones.exigir(atraccionServicio.perteneceAPrestador(f.getIdAtraccion(), s.getIdPrestador()));
        }
    }

    private void cargarOpciones(HttpSession session, Model model) {
        SesionUsuario s = Sesiones.de(session);
        if (s.isEncargado()) {
            model.addAttribute("opcFuncion", Sesiones.filtrar(funcionServicio.listarOpciones(),
                    funcionServicio.listarPorPrestador(s.getIdPrestador()).stream().map(Funcion::getIdFuncion).toList()));
        } else {
            model.addAttribute("opcFuncion", funcionServicio.listarOpciones());
        }
    }
}
