package co.edu.uniquindio.quindiostay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de la aplicación QuindíoStay.
 * Arquitectura en capas (Entrega 2):
 *   controlador (presentación) -> servicio (lógica de negocio) -> dao (acceso a datos) -> MySQL
 */
@SpringBootApplication
public class QuindioStayApplication {
    public static void main(String[] args) {
        SpringApplication.run(QuindioStayApplication.class, args);
    }
}
