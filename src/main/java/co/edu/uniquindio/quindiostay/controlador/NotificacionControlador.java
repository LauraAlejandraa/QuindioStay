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

import java.time.LocalDateTime;

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
        model.addAttribute("opcCliente", clienteServicio.listarOpciones());
        model.addAttribute("opcReserva", reservaServicio.listarOpciones());
        model.addAttribute("editables", NotificacionServicio.EDITABLES);
        return "notificaciones/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        Notificacion n = new Notificacion();
        n.setEstado("pendiente");
        model.addAttribute("obj", n);
        cargarOpciones(model, null);
        return "notificaciones/formulario";
    }

    /** Pendiente o fallida: se edita. Enviada o leída: se muestra solo para ver (historial). */
    @GetMapping("/{id}/editar")
    public String editar(@PathVariable int id, Model model) {
        Notificacion n = servicio.buscarPorId(id);
        model.addAttribute("obj", n);
        cargarOpciones(model, n);
        return "notificaciones/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("obj") Notificacion obj, BindingResult resultado,
                          Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            model.addAttribute("error", "Revise los datos: hay un campo con formato incorrecto.");
            cargarOpciones(model, guardada(obj));
            return "notificaciones/formulario";
        }
        try {
            servicio.guardar(obj);
            redirect.addFlashAttribute("exito", "Guardado correctamente.");
            return "redirect:/notificaciones";
        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
            cargarOpciones(model, guardada(obj));
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

    /** Simula el envío de una notificación pendiente o fallida (RF05). */
    @PostMapping("/{id}/enviar")
    public String enviar(@PathVariable int id, RedirectAttributes redirect) {
        try {
            servicio.marcarEnviada(id);
            redirect.addFlashAttribute("exito", "Notificación enviada.");
        } catch (ReglaNegocioException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/notificaciones";
    }

    @GetMapping("/promocion")
    public String formularioPromocion(Model model) {
        model.addAttribute("porCanal", servicio.destinatariosPromocionPorCanal());
        return "notificaciones/promocion";
    }

    /** Envía (programa) una promoción a todos los clientes que la aceptan (RF05). */
    @PostMapping("/promocion")
    public String enviarPromocion(@RequestParam String mensaje,
                                  @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaProgramada,
                                  RedirectAttributes redirect) {
        try {
            int n = servicio.enviarPromocion(mensaje, fechaProgramada);
            redirect.addFlashAttribute("exito", "Promoción programada para " + n + " cliente(s).");
        } catch (ReglaNegocioException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/notificaciones";
    }

    /** La versión guardada en la base (null si es nueva o no existe). */
    private Notificacion guardada(Notificacion obj) {
        if (obj.getIdNotificacion() == null) return null;
        try {
            return servicio.buscarPorId(obj.getIdNotificacion());
        } catch (ReglaNegocioException e) {
            return null;
        }
    }

    /**
     * Opciones del formulario. Cliente: solo los que aceptan promociones (con su canal preferido);
     * si la notificación ya tenía un cliente que dejó de aceptarlas, se agrega para que no
     * aparezca «(ninguno)» al editar. Reserva: con el nombre del cliente y su canal preferido.
     */
    private void cargarOpciones(Model model, Notificacion actual) {
        java.util.Map<Integer, String> clientes = servicio.opcionesClientePromocion();
        java.util.Map<Integer, String> reservas = servicio.opcionesReserva();
        Notificacion sel = (Notificacion) model.getAttribute("obj");
        Integer idSel = sel != null ? sel.getIdCliente() : null;
        if (idSel != null && !clientes.containsKey(idSel)) {
            String nombre = clienteServicio.listarOpciones().get(idSel);
            clientes.put(idSel, (nombre != null ? nombre : "Cliente " + idSel) + " · no acepta promociones");
        }
        model.addAttribute("opcCliente", clientes);
        model.addAttribute("opcReserva", reservas);
        boolean editable = actual == null || NotificacionServicio.esEditable(actual);
        model.addAttribute("editable", editable);
        model.addAttribute("estadosPosibles", NotificacionServicio.estadosPosibles(actual));
        model.addAttribute("ayudaEstados", NotificacionServicio.AYUDA_ESTADOS);
        model.addAttribute("fechaRealGuardada", actual == null ? null : actual.getFechaRealEnvio());
        model.addAttribute("estadoGuardado", actual == null ? null : actual.getEstado());
    }
}
