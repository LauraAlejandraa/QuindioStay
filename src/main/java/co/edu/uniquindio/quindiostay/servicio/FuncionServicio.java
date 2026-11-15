package co.edu.uniquindio.quindiostay.servicio;

import co.edu.uniquindio.quindiostay.dao.FuncionDAO;
import co.edu.uniquindio.quindiostay.modelo.Funcion;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Lógica de negocio de Funcion. El controlador nunca llama al DAO
 * directamente: pasa por aquí, donde van las reglas que un CHECK no puede revisar.
 * Si MySQL rechaza algo (FK, CHECK, UNIQUE) se traduce a un mensaje entendible.
 *
 * Estados de una función (iguales para el administrador y el encargado):
 *   programada -> realizada   solo cuando la función ya terminó (fecha y hora de cierre pasadas).
 *                             Lo hace la aplicación sola cada minuto y al abrir la lista.
 *   programada -> cancelada   solo si no tiene boletas vendidas en reservas pendientes o confirmadas.
 *   realizada y cancelada son estados finales: ya no se cambian ni se reabren.
 */
@Service
public class FuncionServicio {

    /** Duración mínima de una función: apertura y cierre separados al menos media hora. */
    public static final int DURACION_MINIMA_MINUTOS = 30;
    public static final String PROGRAMADA = "programada";
    public static final String REALIZADA = "realizada";
    public static final String CANCELADA = "cancelada";

    /** Qué significa cada estado (ayuda del formulario). */
    public static final Map<String, String> AYUDA_ESTADOS = new LinkedHashMap<>();

    static {
        AYUDA_ESTADOS.put(PROGRAMADA, "Todavía no termina: se pueden vender boletas. Al pasar su hora de cierre la aplicación la marca sola como realizada.");
        AYUDA_ESTADOS.put(REALIZADA, "Ya se hizo (su fecha y hora de cierre pasaron). Es un estado final: no se vuelve a cambiar.");
        AYUDA_ESTADOS.put(CANCELADA, "No se va a hacer. Solo se puede si no tiene boletas vendidas en reservas pendientes o confirmadas. Es un estado final.");
    }

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final FuncionDAO dao;

    public FuncionServicio(FuncionDAO dao) {
        this.dao = dao;
    }

    public List<Funcion> listarTodos() {
        try {
            marcarRealizadas();
            return dao.listarTodos();
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    public Funcion buscarPorId(int id) {
        try {
            Funcion x = dao.buscarPorId(id);
            if (x == null) {
                throw new ReglaNegocioException("No existe función con id " + id + ".");
            }
            return x;
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    /** Crea si no tiene id, o actualiza si ya lo tiene, revisando las reglas de estado. */
    public void guardar(Funcion x) {
        try {
            validarDatos(x);
            // Duración mínima: al crear, o al cambiar las horas (una función vieja no se rechaza por esto
            // si no se tocan sus horas; así una realizada/cancelada sigue diciendo que es final).
            // La BD lo respalda con el CHECK ck_funcion_duracion.
            Funcion previa = x.getIdFuncion() == null ? null : dao.buscarPorId(x.getIdFuncion());
            if (previa == null || !previa.getHoraApertura().equals(x.getHoraApertura())
                    || !previa.getHoraCierre().equals(x.getHoraCierre())) {
                long minutos = java.time.Duration.between(x.getHoraApertura(), x.getHoraCierre()).toMinutes();
                if (minutos < DURACION_MINIMA_MINUTOS) {
                    throw new ReglaNegocioException("La función debe durar al menos " + DURACION_MINIMA_MINUTOS
                            + " minutos (de " + x.getHoraApertura() + " a " + x.getHoraCierre() + " son " + minutos
                            + "). Una función de pocos minutos no deja tiempo para vender ni para validar boletas en taquilla.");
                }
            }
            if (x.getIdFuncion() == null) {
                validarNueva(x);
                dao.crear(x);
            } else {
                Funcion actual = buscarPorId(x.getIdFuncion());
                if (validarCambio(actual, x)) {
                    dao.actualizar(x);
                }
            }
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    /** Eliminar = cancelar (Tabla 39 de la Entrega 2): no se borra la fila, cambia a 'cancelada'. */
    public void eliminar(int id) {
        try {
            Funcion f = buscarPorId(id);
            validarCancelacion(f);
            dao.eliminar(id);
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    public Map<Integer, String> listarOpciones() {
        try {
            return dao.listarOpciones();
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    public List<Funcion> listarPorPrestador(int idPrestador) {
        try {
            marcarRealizadas();
            return dao.listarPorPrestador(idPrestador);
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    /** Pasa a 'realizada' las funciones programadas que ya terminaron. Devuelve cuántas cambió. */
    public int marcarRealizadas() {
        try {
            return dao.marcarRealizadas(Reloj.hoy(), Reloj.horaActual());
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    /** Tarea programada: cada minuto marca como realizadas las funciones que ya terminaron. */
    @Scheduled(fixedDelay = 60_000, initialDelay = 10_000)
    public void marcarRealizadasAutomaticamente() {
        try {
            marcarRealizadas();
        } catch (RuntimeException e) {
            // Si la base no está disponible en este minuto, se reintenta en el siguiente.
            System.err.println("Paso automático de funciones a realizada: " + e.getMessage());
        }
    }

    /** Estados que se pueden escoger en el formulario, según el estado guardado de la función. */
    public List<String> estadosPosibles(Funcion actual) {
        if (actual == null || actual.getIdFuncion() == null) {
            return List.of(PROGRAMADA);   // una función nueva siempre empieza programada
        }
        if (!PROGRAMADA.equals(actual.getEstado())) {
            return List.of(actual.getEstado());   // realizada y cancelada son finales
        }
        if (Reloj.yaTermino(actual.getFecha(), actual.getHoraCierre())) {
            return List.of(REALIZADA, CANCELADA);
        }
        return List.of(PROGRAMADA, CANCELADA);
    }

    /** true si la función ya no se puede editar (realizada o cancelada). */
    public boolean esFinal(Funcion f) {
        return f != null && (REALIZADA.equals(f.getEstado()) || CANCELADA.equals(f.getEstado()));
    }

    // ------------------------------------------------------------------ reglas

    /** Datos mínimos: los mismos para crear y editar. */
    private void validarDatos(Funcion x) {
        if (x.getIdAtraccion() == null) {
            throw new ReglaNegocioException("Escoja la atracción de la función.");
        }
        if (x.getFecha() == null || x.getHoraApertura() == null || x.getHoraCierre() == null) {
            throw new ReglaNegocioException("Escriba la fecha, la hora de apertura y la hora de cierre de la función.");
        }
        if (!x.getHoraCierre().isAfter(x.getHoraApertura())) {
            throw new ReglaNegocioException("La hora de cierre debe ser posterior a la hora de apertura.");
        }

        if (x.getEstado() == null || !AYUDA_ESTADOS.containsKey(x.getEstado())) {
            throw new ReglaNegocioException("El estado de la función debe ser programada, realizada o cancelada.");
        }
    }

    /** Una función nueva nace 'programada' y con su cierre todavía por llegar. */
    private void validarNueva(Funcion x) {
        if (!PROGRAMADA.equals(x.getEstado())) {
            throw new ReglaNegocioException("Una función nueva se crea como «programada». Pasa a realizada sola cuando termina, "
                    + "o se cancela después si hace falta.");
        }
        if (Reloj.yaTermino(x.getFecha(), x.getHoraCierre())) {
            throw new ReglaNegocioException("No se puede programar una función para el " + x.getFecha().format(FECHA)
                    + " que cierra a las " + x.getHoraCierre() + ": esa fecha y hora ya pasaron.");
        }
    }

    /**
     * Revisa la edición de una función existente. Devuelve false si no cambió nada
     * (no hace falta escribir en la base).
     */
    private boolean validarCambio(Funcion actual, Funcion nueva) throws SQLException {
        boolean cambiaHorario = !Objects.equals(actual.getFecha(), nueva.getFecha())
                || !Objects.equals(actual.getHoraApertura(), nueva.getHoraApertura())
                || !Objects.equals(actual.getHoraCierre(), nueva.getHoraCierre());
        boolean cambiaAtraccion = !Objects.equals(actual.getIdAtraccion(), nueva.getIdAtraccion());
        boolean cambiaEstado = !Objects.equals(actual.getEstado(), nueva.getEstado());

        // (a) realizada y cancelada son finales: no se edita nada ni se reabren.
        if (esFinal(actual)) {
            if (!cambiaHorario && !cambiaAtraccion && !cambiaEstado) {
                return false;
            }
            throw new ReglaNegocioException("La función #" + actual.getIdFuncion() + " ya está " + actual.getEstado()
                    + ": es un estado final y no se puede modificar (ni el estado, ni la fecha, ni el horario, ni la atracción).");
        }

        // De aquí en adelante la función guardada está 'programada'.
        switch (nueva.getEstado()) {
            case PROGRAMADA -> {
                // (d) no se deja programada una función que ya terminó, ni se mueve al pasado.
                if (Reloj.yaTermino(nueva.getFecha(), nueva.getHoraCierre())) {
                    if (cambiaHorario) {
                        int boletas = dao.contarBoletasVendidas(actual.getIdFuncion());
                        throw new ReglaNegocioException("No se puede mover la función al " + nueva.getFecha().format(FECHA)
                                + " a las " + nueva.getHoraCierre() + ": esa fecha y hora ya pasaron"
                                + (boletas > 0 ? " y la función tiene " + boletas + " boleta(s) vendida(s)" : "") + ".");
                    }
                    throw new ReglaNegocioException("La función ya terminó (" + actual.getFecha().format(FECHA) + ", cerró a las "
                            + actual.getHoraCierre() + "): no puede seguir programada. Márquela como realizada.");
                }
            }
            case REALIZADA -> {
                // (b) realizada solo desde programada y cuando la función ya terminó, con su fecha y horario reales.
                if (cambiaHorario || cambiaAtraccion) {
                    throw new ReglaNegocioException("Para marcar la función como realizada no cambie su fecha, horario ni atracción.");
                }
                if (!Reloj.yaTermino(actual.getFecha(), actual.getHoraCierre())) {
                    throw new ReglaNegocioException("La función todavía no ha terminado (es el " + actual.getFecha().format(FECHA)
                            + " y cierra a las " + actual.getHoraCierre() + "): no se puede marcar como realizada.");
                }
            }
            case CANCELADA -> {
                // (c) no se cancela con boletas vendidas en reservas vivas.
                validarCancelacion(actual);
                if (cambiaHorario || cambiaAtraccion) {
                    throw new ReglaNegocioException("Para cancelar la función no cambie su fecha, horario ni atracción.");
                }
            }
            default -> throw new ReglaNegocioException("Estado de función no válido.");
        }
        return true;
    }

    /** Solo se cancela una función programada sin boletas vendidas en reservas pendientes o confirmadas. */
    private void validarCancelacion(Funcion f) throws SQLException {
        if (CANCELADA.equals(f.getEstado())) {
            throw new ReglaNegocioException("La función ya está cancelada.");
        }
        if (REALIZADA.equals(f.getEstado())) {
            throw new ReglaNegocioException("La función ya se realizó: no se puede cancelar.");
        }
        int boletas = dao.contarBoletasEnReservasVivas(f.getIdFuncion());
        if (boletas > 0) {
            throw new ReglaNegocioException("No se puede cancelar la función: tiene " + boletas
                    + (boletas == 1 ? " boleta vendida" : " boletas vendidas")
                    + " en reservas pendientes o confirmadas. Primero cancele esas reservas (para devolver el dinero) "
                    + "o anule sus boletas, y luego cancele la función.");
        }
    }
}
