package co.edu.uniquindio.quindiostay.servicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * Fecha y hora "de ahora" para las reglas de negocio, siempre en la hora de
 * Colombia (America/Bogota), aunque el servidor o MySQL estén en otra zona.
 */
public final class Reloj {

    public static final ZoneId ZONA = ZoneId.of("America/Bogota");

    private Reloj() {
    }

    public static LocalDate hoy() {
        return LocalDate.now(ZONA);
    }

    public static LocalTime horaActual() {
        return LocalTime.now(ZONA).withNano(0);
    }

    public static LocalDateTime ahora() {
        return LocalDateTime.now(ZONA).withNano(0);
    }

    /** Una función ya terminó si su fecha pasó, o si es hoy y su hora de cierre ya pasó. */
    public static boolean yaTermino(LocalDate fecha, LocalTime horaCierre) {
        LocalDate hoy = hoy();
        if (fecha.isBefore(hoy)) {
            return true;
        }
        return fecha.equals(hoy) && horaCierre != null && !horaCierre.isAfter(horaActual());
    }
}
