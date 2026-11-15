package co.edu.uniquindio.quindiostay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Punto de entrada de la aplicación QuindíoStay.
 * Arquitectura en capas (Entrega 2):
 *   controlador (presentación) -> servicio (lógica de negocio) -> dao (acceso a datos) -> MySQL
 * @EnableScheduling activa el envío simulado automático de notificaciones
 * (NotificacionServicio.enviarProgramadasAutomaticamente, cada minuto).
 */
@SpringBootApplication
@EnableScheduling
public class QuindioStayApplication {
    public static void main(String[] args) {
        SpringApplication.run(QuindioStayApplication.class, args);
    }
}
