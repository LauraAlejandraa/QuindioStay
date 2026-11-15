package co.edu.uniquindio.quindiostay.servicio;

import co.edu.uniquindio.quindiostay.dao.TipoEntradaDAO;
import co.edu.uniquindio.quindiostay.modelo.TipoEntrada;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Lógica de negocio de Tipo_entrada. El controlador nunca llama al DAO
 * directamente: pasa por aquí, donde van las reglas que un CHECK no puede revisar.
 * Si MySQL rechaza algo (FK, CHECK, UNIQUE) se traduce a un mensaje entendible.
 */
@Service
public class TipoEntradaServicio {

    private final TipoEntradaDAO dao;


    public TipoEntradaServicio(TipoEntradaDAO dao) {
        this.dao = dao;
    }

    public List<TipoEntrada> listarTodos() {
        try {
            return dao.listarTodos();
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    public TipoEntrada buscarPorId(int id) {
        try {
            TipoEntrada x = dao.buscarPorId(id);
            if (x == null) {
                throw new ReglaNegocioException("No existe tipo de entrada con id " + id + ".");
            }
            return x;
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    /**
     * Lista cerrada del enunciado (CHECK ck_tipo_entrada_nombre). La venta usa estos nombres
     * (el niño necesita un adulto o adulto mayor), por eso el nombre no se cambia al editar:
     * solo se edita el % de descuento.
     */
    public static final List<String> NOMBRES = List.of("adulto", "niño", "adulto mayor", "estudiante");

    /** Tipos de la lista cerrada que todavía no están creados (lo que ofrece «Nuevo»). */
    public List<String> nombresDisponibles() {
        List<String> existentes = listarTodos().stream().map(TipoEntrada::getNombre).toList();
        return NOMBRES.stream().filter(n -> !existentes.contains(n)).toList();
    }

    /** Boletas por tipo de entrada, para la columna «Boletas» de la lista. */
    public Map<Integer, Integer> contarUsos() {
        try {
            return dao.contarBoletasPorTipo();
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    /** Crea si no tiene id, o actualiza (solo el % de descuento) si ya lo tiene. */
    public void guardar(TipoEntrada x) {
        try {
            validarDescuento(x);
            String nombre = x.getNombre() == null ? "" : x.getNombre().trim();
            if (x.getIdTipoEntrada() == null) {
                if (nombre.isEmpty()) {
                    throw new ReglaNegocioException("Escoja el tipo de entrada que va a crear.");
                }
                if (!NOMBRES.contains(nombre)) {
                    throw new ReglaNegocioException("El tipo de entrada debe ser " + String.join(", ", NOMBRES) + ".");
                }
                if (!nombresDisponibles().contains(nombre)) {
                    throw new ReglaNegocioException("El tipo de entrada «" + nombre + "» ya existe. "
                            + "Para cambiar su descuento use «Editar» en la lista de tipos de entrada.");
                }
                x.setNombre(nombre);
                dao.crear(x);
            } else {
                TipoEntrada actual = buscarPorId(x.getIdTipoEntrada());
                if (!nombre.isEmpty() && !nombre.equals(actual.getNombre())) {
                    throw new ReglaNegocioException("El nombre de un tipo de entrada no se cambia (la venta lo usa, "
                            + "por ejemplo para exigir un adulto con cada niño). Solo se edita el % de descuento.");
                }
                x.setNombre(actual.getNombre());
                dao.actualizar(x);
            }
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    /** Eliminar: solo un tipo con el que no se haya vendido ninguna boleta (FK fk_boleta_tipo_entrada). */
    public void eliminar(int id) {
        try {
            TipoEntrada x = buscarPorId(id);
            int boletas = dao.contarBoletas(id);
            if (boletas > 0) {
                throw new ReglaNegocioException("No se puede eliminar el tipo de entrada «" + x.getNombre() + "» porque "
                        + (boletas == 1 ? "hay 1 boleta vendida" : "hay " + boletas + " boletas vendidas")
                        + " con ese tipo. Si quiere cambiar su precio, edite el % de descuento.");
            }
            dao.eliminar(id);
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    private void validarDescuento(TipoEntrada x) {
        BigDecimal d = x.getPorcentajeDescuento();
        if (d == null) {
            throw new ReglaNegocioException("Escriba el % de descuento (0 si no tiene descuento).");
        }
        if (d.compareTo(BigDecimal.ZERO) < 0 || d.compareTo(new BigDecimal("100")) > 0) {
            throw new ReglaNegocioException("El porcentaje de descuento debe estar entre 0 y 100.");
        }
    }

    public Map<Integer, String> listarOpciones() {
        try {
            return dao.listarOpciones();
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }
}
