package co.edu.uniquindio.quindiostay.controlador;

import co.edu.uniquindio.quindiostay.modelo.SesionUsuario;
import co.edu.uniquindio.quindiostay.servicio.ReglaNegocioException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Si un error llega hasta aquí (por ejemplo, MySQL está apagado), se muestra
 * una página con un mensaje entendible en lugar del error de Java.
 */
@ControllerAdvice
public class ManejadorErrores {

    private static final Pattern PLANTILLA_RESERVA = Pattern.compile("^/plantillas/(reserva|factura|boletas)/(\\d+)$");

    @ExceptionHandler(ReglaNegocioException.class)
    public String reglaNegocio(ReglaNegocioException e, Model model, HttpServletRequest request) {
        model.addAttribute("mensaje", e.getMessage());
        model.addAttribute("urlVolver", urlVolver(request));
        return "error-app";
    }

    /**
     * Destino del botón «Volver» como enlace real (sin javascript:history.back(), que
     * no hace nada cuando la página se abrió en otra pestaña y no hay historial).
     * 1) La página de donde vino (Referer), solo si es de esta misma aplicación.
     * 2) Si no hay Referer: para las plantillas de una reserva, el detalle de esa
     *    reserva (según el rol); si no, la lista de reservas del cliente o el inicio.
     */
    static String urlVolver(HttpServletRequest request) {
        String actual = request.getRequestURI();
        String contexto = request.getContextPath();
        String referer = request.getHeader("Referer");
        if (referer != null) {
            try {
                URI uri = URI.create(referer);
                boolean mismoSitio = uri.getHost() == null
                        || (uri.getHost().equalsIgnoreCase(request.getServerName())
                            && (uri.getPort() == -1 || uri.getPort() == request.getServerPort()));
                String ruta = uri.getRawPath();
                if (mismoSitio && ruta != null && ruta.startsWith("/") && !ruta.startsWith("//") && !ruta.equals(actual)) {
                    return ruta + (uri.getRawQuery() != null ? "?" + uri.getRawQuery() : "");
                }
            } catch (IllegalArgumentException ignorada) {
                // Referer mal formado: se usa la ruta de respaldo
            }
        }
        HttpSession session = request.getSession(false);
        SesionUsuario s = session == null ? null : (SesionUsuario) session.getAttribute("sesion");
        boolean cliente = s != null && s.isCliente();
        Matcher m = PLANTILLA_RESERVA.matcher(actual.substring(contexto.length()));
        if (m.matches()) {
            return contexto + (cliente ? "/mi-cuenta/reservas/" : "/reservas/") + m.group(2);
        }
        if (s == null) {
            return contexto + "/login";
        }
        return contexto + (cliente ? "/mi-cuenta/reservas" : "/inicio");
    }
}
