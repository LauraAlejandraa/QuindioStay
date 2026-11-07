package co.edu.uniquindio.quindiostay.dao;

/** Pequeñas ayudas para los DAO. */
public final class JdbcUtil {

    private JdbcUtil() {
    }

    /** Un campo opcional que llega vacío del formulario se guarda como NULL. */
    public static String vacioANull(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }
}
