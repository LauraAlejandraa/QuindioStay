package co.edu.uniquindio.quindiostay.config;

import co.edu.uniquindio.quindiostay.modelo.SesionUsuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;
import java.util.Map;

/**
 * Revisa, antes de cada pantalla, que haya una sesión iniciada y que el rol
 * tenga permiso para entrar a esa ruta. Así el menú no es la única protección:
 * aunque alguien escriba la URL a mano, no entra si su rol no lo permite.
 */
@Component
public class InterceptorSesion implements HandlerInterceptor {

    private static final String A = SesionUsuario.ADMIN;
    private static final String E = SesionUsuario.ENCARGADO;
    private static final String O = SesionUsuario.OPERADOR;
    private static final String C = SesionUsuario.CLIENTE;

    /** Ruta -> roles que pueden entrar. Lo que no aparece aquí es solo del administrador. */
    private static final Map<String, List<String>> PERMISOS = Map.ofEntries(
            Map.entry("/inicio", List.of(A, E, O, C)),
            Map.entry("/mi-cuenta", List.of(C)),
            Map.entry("/plantillas", List.of(A, E, O, C)),
            Map.entry("/venta", List.of(A, O, C)),
            Map.entry("/taquilla", List.of(A, O)),
            Map.entry("/reportes", List.of(A)),
            Map.entry("/atracciones", List.of(A, E)),
            Map.entry("/funciones", List.of(A, E)),
            Map.entry("/zonas", List.of(A, E)),
            Map.entry("/planes", List.of(A, E)),
            Map.entry("/reservas", List.of(A, E, O)),
            Map.entry("/pagos", List.of(A, O)),
            Map.entry("/boletas", List.of(A, O)),
            Map.entry("/clientes", List.of(A, O))
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        HttpSession session = request.getSession();
        SesionUsuario sesion = (SesionUsuario) session.getAttribute("sesion");
        if (sesion == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }
        String ruta = request.getRequestURI().substring(request.getContextPath().length());
        String prefijo = "/" + ruta.split("/")[1];
        List<String> roles = PERMISOS.getOrDefault(prefijo, List.of(A));
        if (!roles.contains(sesion.getRol())) {
            response.sendRedirect(request.getContextPath() + "/inicio?sinPermiso");
            return false;
        }
        return true;
    }
}
