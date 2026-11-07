package co.edu.uniquindio.quindiostay.controlador;

import co.edu.uniquindio.quindiostay.dao.CuentaDAO;
import co.edu.uniquindio.quindiostay.dao.PerfilClienteDAO;
import co.edu.uniquindio.quindiostay.dao.VistaDAO;
import co.edu.uniquindio.quindiostay.modelo.*;
import co.edu.uniquindio.quindiostay.servicio.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Map;

/**
 * Lo que ve el cliente (RF01, RF02, RF04, RF05): su perfil, sus reservas,
 * sus pagos y sus notificaciones. Nunca ve datos de otro cliente: todo se
 * filtra por el id_cliente de la sesión.
 */
@Controller
@RequestMapping("/mi-cuenta")
public class MiCuentaControlador {

    private final ReservaServicio reservaServicio;
    private final PagoServicio pagoServicio;
    private final NotificacionServicio notificacionServicio;
    private final ClienteServicio clienteServicio;
    private final PerfilClienteDAO perfilDAO;
    private final CuentaDAO cuentaDAO;
    private final VistaDAO vistaDAO;
    private final BCryptPasswordEncoder cifrador = new BCryptPasswordEncoder();

    public MiCuentaControlador(ReservaServicio reservaServicio, PagoServicio pagoServicio,
                               NotificacionServicio notificacionServicio, ClienteServicio clienteServicio,
                               PerfilClienteDAO perfilDAO, CuentaDAO cuentaDAO, VistaDAO vistaDAO) {
        this.reservaServicio = reservaServicio;
        this.pagoServicio = pagoServicio;
        this.notificacionServicio = notificacionServicio;
        this.clienteServicio = clienteServicio;
        this.perfilDAO = perfilDAO;
        this.cuentaDAO = cuentaDAO;
        this.vistaDAO = vistaDAO;
    }

    @GetMapping("/reservas")
    public String reservas(HttpSession session, Model model) throws SQLException {
        model.addAttribute("lista", vistaDAO.reservasDeCliente(Sesiones.de(session).getIdCliente()));
        return "mi-cuenta/reservas";
    }

    @GetMapping("/reservas/{id}")
    public String detalle(@PathVariable int id, HttpSession session, Model model) {
        Map<String, Object> d = reservaServicio.detalle(id);
        esSuya(d, session);
        model.addAttribute("r", d);
        model.addAttribute("base", "/mi-cuenta/reservas");
        return "reservas/detalle";
    }

    @PostMapping("/reservas/{id}/pagar")
    public String pagar(@PathVariable int id, @RequestParam BigDecimal monto, @RequestParam String metodo,
                        @RequestParam(defaultValue = "exitoso") String resultado,
                        HttpSession session, RedirectAttributes redirect) {
        try {
            esSuya(reservaServicio.detalle(id), session);
            if ("efectivo".equals(metodo)) {
                throw new ReglaNegocioException("En línea se paga con tarjeta, PSE o transferencia. El efectivo se paga en el mostrador.");
            }
            pagoServicio.registrar(id, monto, metodo, resultado, null);
            if ("exitoso".equals(resultado)) {
                redirect.addFlashAttribute("exito", "¡Pago aprobado! Puede descargar el comprobante en la tabla de pagos.");
            } else {
                redirect.addFlashAttribute("error", "La pasarela rechazó el pago. Intente con otro medio.");
            }
        } catch (ReglaNegocioException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/mi-cuenta/reservas/" + id;
    }

    @PostMapping("/reservas/{id}/cancelar")
    public String cancelar(@PathVariable int id, HttpSession session, RedirectAttributes redirect) {
        try {
            esSuya(reservaServicio.detalle(id), session);
            BigDecimal reembolso = reservaServicio.cancelar(id);
            redirect.addFlashAttribute("exito", "Reserva cancelada. "
                    + (reembolso.signum() > 0 ? "Se le reembolsará el 80 % de lo pagado: $" + String.format("%,.0f", reembolso).replace(',', '.')
                                              : "No hay reembolso: faltaban 5 días o menos para el servicio."));
        } catch (ReglaNegocioException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/mi-cuenta/reservas/" + id;
    }

    @GetMapping("/notificaciones")
    public String notificaciones(HttpSession session, Model model) {
        model.addAttribute("lista", notificacionServicio.listarPorCliente(Sesiones.de(session).getIdCliente()));
        return "mi-cuenta/notificaciones";
    }

    @PostMapping("/notificaciones/{id}/leida")
    public String leida(@PathVariable int id, HttpSession session, RedirectAttributes redirect) {
        try {
            notificacionServicio.marcarLeida(id, Sesiones.de(session).getIdCliente());
        } catch (ReglaNegocioException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/mi-cuenta/notificaciones";
    }

    @GetMapping("/perfil")
    public String perfil(HttpSession session, Model model) throws SQLException {
        int idCliente = Sesiones.de(session).getIdCliente();
        model.addAttribute("cliente", clienteServicio.buscarPorId(idCliente));
        PerfilCliente p = perfilDAO.buscarPorCliente(idCliente);
        if (p == null) {
            p = new PerfilCliente();
            p.setIdioma("español");
            p.setCanalPreferido("correo");
            p.setAnticipacionRecordatorio(1);
            p.setAceptaPromociones("no");
        }
        model.addAttribute("perfil", p);
        return "mi-cuenta/perfil";
    }

    @PostMapping("/perfil")
    public String guardarPerfil(@ModelAttribute("cliente") Cliente cliente, BindingResult r1,
                                @ModelAttribute("perfil") PerfilCliente perfil, BindingResult r2,
                                HttpSession session, Model model, RedirectAttributes redirect) {
        int idCliente = Sesiones.de(session).getIdCliente();
        try {
            if (r1.hasErrors() || r2.hasErrors()) {
                throw new ReglaNegocioException("Revise los datos: hay un campo con formato incorrecto.");
            }
            cliente.setIdCliente(idCliente);           // nunca se toma el id del formulario
            clienteServicio.guardar(cliente);
            PerfilCliente actual = perfilDAO.buscarPorCliente(idCliente);
            perfil.setIdCliente(idCliente);
            if (actual == null) {
                perfil.setIdPerfil(null);
                perfilDAO.crear(perfil);
            } else {
                perfil.setIdPerfil(actual.getIdPerfil());
                perfilDAO.actualizar(perfil);
            }
            Sesiones.de(session).setNombreMostrar(cliente.getPrimerNombre() + " " + cliente.getPrimerApellido());
            redirect.addFlashAttribute("exito", "Sus datos y preferencias quedaron guardados.");
            return "redirect:/mi-cuenta/perfil";
        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
            return "mi-cuenta/perfil";
        } catch (SQLException e) {
            model.addAttribute("error", MensajesError.traducir(e));
            return "mi-cuenta/perfil";
        }
    }

    @PostMapping("/contrasena")
    public String cambiarContrasena(@RequestParam String actual, @RequestParam String nueva,
                                    HttpSession session, RedirectAttributes redirect) {
        try {
            Cuenta c = cuentaDAO.buscarPorCliente(Sesiones.de(session).getIdCliente());
            if (!cifrador.matches(actual, c.getContrasenaCifrada())) {
                throw new ReglaNegocioException("La contraseña actual no es correcta.");
            }
            if (nueva.length() < 8) {
                throw new ReglaNegocioException("La nueva contraseña debe tener al menos 8 caracteres.");
            }
            c.setContrasenaCifrada(cifrador.encode(nueva));
            cuentaDAO.actualizar(c);
            redirect.addFlashAttribute("exito", "Contraseña cambiada.");
        } catch (ReglaNegocioException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        } catch (SQLException e) {
            redirect.addFlashAttribute("error", MensajesError.traducir(e));
        }
        return "redirect:/mi-cuenta/perfil";
    }

    private void esSuya(Map<String, Object> reserva, HttpSession session) {
        if (!Sesiones.de(session).getIdCliente().equals(reserva.get("id_cliente"))) {
            throw new ReglaNegocioException("Esa reserva no es suya.");
        }
    }
}
