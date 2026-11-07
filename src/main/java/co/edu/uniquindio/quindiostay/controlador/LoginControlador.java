package co.edu.uniquindio.quindiostay.controlador;

import co.edu.uniquindio.quindiostay.modelo.Cliente;
import co.edu.uniquindio.quindiostay.modelo.PerfilCliente;
import co.edu.uniquindio.quindiostay.modelo.SesionUsuario;
import co.edu.uniquindio.quindiostay.servicio.AutenticacionServicio;
import co.edu.uniquindio.quindiostay.servicio.ClienteServicio;
import co.edu.uniquindio.quindiostay.servicio.ReglaNegocioException;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Login, salida y registro de clientes nuevos (RF01). */
@Controller
public class LoginControlador {

    private final AutenticacionServicio autenticacion;
    private final ClienteServicio clienteServicio;

    public LoginControlador(AutenticacionServicio autenticacion, ClienteServicio clienteServicio) {
        this.autenticacion = autenticacion;
        this.clienteServicio = clienteServicio;
    }

    @GetMapping({"/", "/login"})
    public String login(HttpSession session) {
        return session.getAttribute("sesion") != null ? "redirect:/inicio" : "login";
    }

    @PostMapping("/login")
    public String ingresar(@RequestParam String usuario, @RequestParam String contrasena,
                           HttpSession session, Model model) {
        try {
            SesionUsuario sesion = autenticacion.iniciarSesion(usuario, contrasena);
            session.setAttribute("sesion", sesion);
            return "redirect:/inicio";
        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("usuario", usuario);
            return "login";
        }
    }

    @GetMapping("/logout")
    public String salir(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/registro")
    public String registro(Model model) {
        model.addAttribute("cliente", new Cliente());
        PerfilCliente perfil = new PerfilCliente();
        perfil.setIdioma("español");
        perfil.setCanalPreferido("correo");
        perfil.setAnticipacionRecordatorio(1);
        perfil.setAceptaPromociones("no");
        model.addAttribute("perfil", perfil);
        return "registro";
    }

    @PostMapping("/registro")
    public String registrar(@ModelAttribute("cliente") Cliente cliente, BindingResult r1,
                            @ModelAttribute("perfil") PerfilCliente perfil, BindingResult r2,
                            @RequestParam String contrasena, @RequestParam String confirmacion,
                            Model model, RedirectAttributes redirect) {
        try {
            if (r1.hasErrors() || r2.hasErrors()) {
                throw new ReglaNegocioException("Revise los datos: hay un campo con formato incorrecto.");
            }
            if (!contrasena.equals(confirmacion)) {
                throw new ReglaNegocioException("Las contraseñas no coinciden.");
            }
            clienteServicio.registrar(cliente, contrasena, perfil);
            redirect.addFlashAttribute("exito", "Cuenta creada. Ya puede iniciar sesión con su correo.");
            return "redirect:/login";
        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
            return "registro";
        }
    }
}
