package co.edu.uniquindio.quindiostay.controlador;

import co.edu.uniquindio.quindiostay.modelo.Reserva;
import co.edu.uniquindio.quindiostay.modelo.SesionUsuario;
import co.edu.uniquindio.quindiostay.servicio.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Reservas para los usuarios internos. Crear = «Vender boletas»; eliminar = cancelar.
 * El encargado solo ve (no modifica) las reservas de los planes de su prestador.
 */
@Controller
@RequestMapping("/reservas")
public class ReservaControlador {

    private final ReservaServicio servicio;
    private final PagoServicio pagoServicio;
    private final ClienteServicio clienteServicio;
    private final PlanServicio planServicio;

    public ReservaControlador(ReservaServicio servicio, PagoServicio pagoServicio,
                              ClienteServicio clienteServicio, PlanServicio planServicio) {
        this.servicio = servicio;
        this.pagoServicio = pagoServicio;
        this.clienteServicio = clienteServicio;
        this.planServicio = planServicio;
    }

    @GetMapping
    public String listar(HttpSession session, Model model) {
        SesionUsuario s = Sesiones.de(session);
        model.addAttribute("lista", s.isEncargado() ? servicio.listarPorPrestador(s.getIdPrestador())
                                                    : servicio.listarTodos());
        cargarOpciones(model);
        return "reservas/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo() {
        return "redirect:/venta";   // una reserva nace con sus boletas
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable int id, HttpSession session, Model model) {
        Map<String, Object> d = servicio.detalle(id);
        revisarPermiso(d, session);
        model.addAttribute("r", d);
        model.addAttribute("base", "/reservas");
        return "reservas/detalle";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable int id, HttpSession session, Model model) {
        soloAdminUOperador(session);
        model.addAttribute("obj", servicio.buscarPorId(id));
        cargarOpciones(model);
        return "reservas/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("obj") Reserva obj, BindingResult resultado, HttpSession session,
                          Model model, RedirectAttributes redirect) {
        try {
            soloAdminUOperador(session);
            servicio.guardar(obj);
            redirect.addFlashAttribute("exito", "Reserva actualizada.");
            return "redirect:/reservas/" + obj.getIdReserva();
        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("obj", servicio.buscarPorId(obj.getIdReserva()));
            cargarOpciones(model);
            return "reservas/formulario";
        }
    }

    @PostMapping("/{id}/estado")
    public String cambiarEstado(@PathVariable int id, @RequestParam String estado, HttpSession session,
                                RedirectAttributes redirect) {
        try {
            soloAdminUOperador(session);
            servicio.cambiarEstado(id, estado);
            redirect.addFlashAttribute("exito", "La reserva quedó " + estado + ".");
        } catch (ReglaNegocioException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/reservas/" + id;
    }

    @PostMapping({"/{id}/eliminar", "/{id}/cancelar"})
    public String cancelar(@PathVariable int id, HttpSession session, RedirectAttributes redirect) {
        try {
            soloAdminUOperador(session);
            BigDecimal reembolso = servicio.cancelar(id);
            redirect.addFlashAttribute("exito", "Reserva cancelada y boletas anuladas. "
                    + (reembolso.signum() > 0 ? "Reembolso del 80 %: $" + String.format("%,.0f", reembolso).replace(',', '.')
                                              : "Sin reembolso (faltaban 5 días o menos, o no había pagos)."));
        } catch (ReglaNegocioException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/reservas/" + id;
    }

    @PostMapping("/{id}/pagar")
    public String pagar(@PathVariable int id, @RequestParam BigDecimal monto, @RequestParam String metodo,
                        @RequestParam(defaultValue = "exitoso") String resultado,
                        @RequestParam(required = false) String referencia,
                        HttpSession session, RedirectAttributes redirect) {
        try {
            soloAdminUOperador(session);
            pagoServicio.registrar(id, monto, metodo, resultado, referencia);
            redirect.addFlashAttribute(resultado.equals("exitoso") ? "exito" : "error",
                    resultado.equals("exitoso") ? "Pago registrado." : "La pasarela rechazó el pago (quedó como " + resultado + ").");
        } catch (ReglaNegocioException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/reservas/" + id;
    }

    private void revisarPermiso(Map<String, Object> d, HttpSession session) {
        SesionUsuario s = Sesiones.de(session);
        if (s.isEncargado()) {
            Sesiones.exigir(s.getIdPrestador().equals(d.get("id_prestador")));
        }
    }

    private void soloAdminUOperador(HttpSession session) {
        if (Sesiones.de(session).isEncargado()) {
            throw new ReglaNegocioException("El encargado de prestador consulta las reservas, pero no las modifica.");
        }
    }

    private void cargarOpciones(Model model) {
        model.addAttribute("opcCliente", clienteServicio.listarOpciones());
        model.addAttribute("opcPlan", planServicio.listarOpciones());
    }
}
