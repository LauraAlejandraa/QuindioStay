package co.edu.uniquindio.quindiostay.servicio;

/**
 * Error de una regla de negocio que la base de datos no puede revisar sola
 * (aforo, niño con adulto, plan vigente, cambios de estado...).
 * El mensaje se muestra tal cual en la pantalla.
 */
public class ReglaNegocioException extends RuntimeException {
    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
