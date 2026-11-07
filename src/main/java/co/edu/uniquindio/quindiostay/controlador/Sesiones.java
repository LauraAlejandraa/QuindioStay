package co.edu.uniquindio.quindiostay.controlador;

import co.edu.uniquindio.quindiostay.modelo.SesionUsuario;
import co.edu.uniquindio.quindiostay.servicio.ReglaNegocioException;
import jakarta.servlet.http.HttpSession;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/** Ayudas para leer la sesión en los controladores. */
final class Sesiones {

    private Sesiones() {
    }

    static SesionUsuario de(HttpSession session) {
        return (SesionUsuario) session.getAttribute("sesion");
    }

    /** Si el usuario es encargado y el registro no es de su prestador, no lo deja seguir. */
    static void exigir(boolean condicion) {
        if (!condicion) {
            throw new ReglaNegocioException("Ese registro no pertenece a su prestador.");
        }
    }

    /** Deja en el mapa de opciones solo las claves permitidas. */
    static Map<Integer, String> filtrar(Map<Integer, String> opciones, Collection<Integer> permitidas) {
        Map<Integer, String> r = new LinkedHashMap<>();
        opciones.forEach((k, v) -> {
            if (permitidas.contains(k)) {
                r.put(k, v);
            }
        });
        return r;
    }
}
