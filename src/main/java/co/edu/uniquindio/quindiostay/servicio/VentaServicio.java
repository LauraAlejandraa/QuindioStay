package co.edu.uniquindio.quindiostay.servicio;

import co.edu.uniquindio.quindiostay.dao.*;
import co.edu.uniquindio.quindiostay.modelo.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

/**
 * Venta de boletas (RF04, RF08, RF12). Aquí están las reglas que la base de datos
 * no puede revisar mirando una sola fila:
 *   1. No vender más boletas que el aforo de la zona.
 *   2. No vender boleta de niño sin una de adulto en la misma reserva.
 *   3. El plan está activo y vigente en la fecha del servicio, y su prestador
 *      está afiliado a la atracción (o a TODAS las del combo).
 *   4. Cada unidad de combo genera una boleta por cada atracción del combo y
 *      se cobra el precio especial (no el precio de las zonas).
 * Todo se guarda en UNA transacción: la reserva, sus boletas y la notificación.
 * Si una regla falla a mitad de camino, se hace ROLLBACK y no queda nada a medias.
 */
@Service
public class VentaServicio {

    private final ConexionBD conexionBD;
    private final ReservaDAO reservaDAO;
    private final BoletaDAO boletaDAO;
    private final ZonaDAO zonaDAO;
    private final FuncionDAO funcionDAO;
    private final TipoEntradaDAO tipoEntradaDAO;
    private final ComboDAO comboDAO;
    private final ClienteDAO clienteDAO;
    private final VistaDAO vistaDAO;
    private final NotificacionServicio notificaciones;

    public VentaServicio(ConexionBD conexionBD, ReservaDAO reservaDAO, BoletaDAO boletaDAO, ZonaDAO zonaDAO,
                         FuncionDAO funcionDAO, TipoEntradaDAO tipoEntradaDAO, ComboDAO comboDAO,
                         ClienteDAO clienteDAO, VistaDAO vistaDAO, NotificacionServicio notificaciones) {
        this.conexionBD = conexionBD;
        this.reservaDAO = reservaDAO;
        this.boletaDAO = boletaDAO;
        this.zonaDAO = zonaDAO;
        this.funcionDAO = funcionDAO;
        this.tipoEntradaDAO = tipoEntradaDAO;
        this.comboDAO = comboDAO;
        this.clienteDAO = clienteDAO;
        this.vistaDAO = vistaDAO;
        this.notificaciones = notificaciones;
    }

    /**
     * Valor de una boleta suelta: precio de la zona menos el descuento del tipo
     * de entrada, redondeado a la centena (igual que vista_boleta_detalle).
     */
    public static BigDecimal valorBoleta(BigDecimal precioZona, BigDecimal porcentajeDescuento) {
        return precioZona.multiply(BigDecimal.valueOf(100).subtract(porcentajeDescuento))
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
                .setScale(-2, RoundingMode.HALF_UP)
                .setScale(2, RoundingMode.UNNECESSARY);
    }

    // =================================================================
    // Venta de boletas sueltas de UNA función
    // =================================================================
    public int venderBoletas(int idCliente, String canal, int idFuncion, int idPlan, List<LineaVenta> lineas) {
        List<LineaVenta> conCantidad = lineas.stream().filter(l -> l.getCantidad() > 0).toList();
        if (conCantidad.isEmpty()) {
            throw new ReglaNegocioException("Escoja al menos una boleta.");
        }
        try {
            Funcion funcion = funcionDAO.buscarPorId(idFuncion);
            validarFuncionVendible(funcion);
            validarCliente(idCliente);
            validarPlan(idPlan, vistaDAO.planesParaVender(funcion.getIdAtraccion(), funcion.getFecha()));

            Map<Integer, TipoEntrada> tipos = tiposPorId();
            // Regla 2: niño solo con adulto en la misma reserva (aquí la reserva es esta compra)
            int ninos = 0, adultos = 0;
            for (LineaVenta l : conCantidad) {
                String tipo = tipos.get(l.getIdTipoEntrada()).getNombre();
                if ("niño".equals(tipo)) ninos += l.getCantidad();
                if ("adulto".equals(tipo)) adultos += l.getCantidad();
            }
            if (ninos > 0 && adultos == 0) {
                throw new ReglaNegocioException("Una boleta de niño solo se vende si en la misma reserva hay al menos una de adulto.");
            }

            try (Connection con = conexionBD.getConexion()) {
                con.setAutoCommit(false);
                try {
                    // Regla 1: aforo. Se suma lo pedido por zona y se compara con los cupos (FOR UPDATE).
                    Map<Integer, Integer> pedidasPorZona = new HashMap<>();
                    for (LineaVenta l : conCantidad) {
                        pedidasPorZona.merge(l.getIdZona(), l.getCantidad(), Integer::sum);
                    }
                    BigDecimal total = BigDecimal.ZERO;
                    Map<Integer, Zona> zonas = new HashMap<>();
                    for (Map.Entry<Integer, Integer> e : pedidasPorZona.entrySet()) {
                        Zona z = zonaDAO.buscarPorId(e.getKey());
                        if (z == null || !z.getIdFuncion().equals(idFuncion)) {
                            throw new ReglaNegocioException("La zona escogida no pertenece a la función.");
                        }
                        int cupos = zonaDAO.cuposDisponibles(con, z.getIdZona());
                        if (e.getValue() > cupos) {
                            throw new ReglaNegocioException("La zona " + z.getNombre() + " solo tiene " + cupos
                                    + " cupos y se pidieron " + e.getValue() + ". No se puede superar el aforo.");
                        }
                        zonas.put(z.getIdZona(), z);
                    }
                    for (LineaVenta l : conCantidad) {
                        BigDecimal valor = valorBoleta(zonas.get(l.getIdZona()).getPrecio(),
                                tipos.get(l.getIdTipoEntrada()).getPorcentajeDescuento());
                        total = total.add(valor.multiply(BigDecimal.valueOf(l.getCantidad())));
                    }

                    Reserva r = crearReserva(con, idCliente, canal, idPlan, funcion.getFecha(), total);
                    for (LineaVenta l : conCantidad) {
                        for (int i = 0; i < l.getCantidad(); i++) {
                            crearBoleta(con, r.getIdReserva(), l.getIdZona(), l.getIdTipoEntrada(), null);
                        }
                    }
                    notificaciones.confirmacionReserva(con, r);
                    con.commit();
                    return r.getIdReserva();
                } catch (SQLException | RuntimeException e) {
                    con.rollback();
                    throw e;
                }
            }
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    // =================================================================
    // Venta de un combo: una boleta por cada atracción y por cada unidad
    // =================================================================
    public int venderCombo(int idCliente, String canal, int idCombo, int unidades, int idTipoEntrada,
                           int idPlan, Map<Integer, Integer> zonaPorAtraccion) {
        if (unidades < 1) {
            throw new ReglaNegocioException("Escoja al menos una unidad del combo.");
        }
        try {
            Combo combo = comboDAO.buscarPorId(idCombo);
            if (combo == null || !"activo".equals(combo.getEstado())) {
                throw new ReglaNegocioException("El combo no está activo.");
            }
            List<Integer> atracciones = comboDAO.atraccionesDelCombo(idCombo);
            if (atracciones.size() < 2) {
                throw new ReglaNegocioException("Un combo debe incluir al menos dos atracciones.");
            }
            validarCliente(idCliente);
            TipoEntrada tipo = tiposPorId().get(idTipoEntrada);
            if (tipo == null) {
                throw new ReglaNegocioException("Escoja el tipo de entrada.");
            }
            if ("niño".equals(tipo.getNombre())) {
                throw new ReglaNegocioException("Una boleta de niño solo se vende si en la misma reserva hay una de adulto: "
                        + "compre primero el combo de adulto y luego agregue las de niño a esa reserva.");
            }

            try (Connection con = conexionBD.getConexion()) {
                con.setAutoCommit(false);
                try {
                    LocalDate primeraFecha = null;
                    for (Integer idAtraccion : atracciones) {
                        Integer idZona = zonaPorAtraccion.get(idAtraccion);
                        if (idZona == null) {
                            throw new ReglaNegocioException("Escoja la función y zona de cada atracción del combo.");
                        }
                        Zona z = zonaDAO.buscarPorId(idZona);
                        Funcion f = funcionDAO.buscarPorId(z.getIdFuncion());
                        // La boleta del combo debe ser de una atracción del combo (regla de la Entrega 1)
                        if (!f.getIdAtraccion().equals(idAtraccion)) {
                            throw new ReglaNegocioException("La zona escogida no corresponde a la atracción del combo.");
                        }
                        validarFuncionVendible(f);
                        int cupos = zonaDAO.cuposDisponibles(con, idZona);
                        if (unidades > cupos) {
                            throw new ReglaNegocioException("La zona " + z.getNombre() + " del " + f.getFecha()
                                    + " solo tiene " + cupos + " cupos. No se puede superar el aforo.");
                        }
                        if (primeraFecha == null || f.getFecha().isBefore(primeraFecha)) {
                            primeraFecha = f.getFecha();
                        }
                    }
                    // El plan debe estar vigente en la fecha del servicio (la primera función del combo)
                    validarPlan(idPlan, vistaDAO.planesParaCombo(idCombo, primeraFecha));

                    BigDecimal total = combo.getPrecioEspecial().multiply(BigDecimal.valueOf(unidades));
                    Reserva r = crearReserva(con, idCliente, canal, idPlan, primeraFecha, total);
                    for (int u = 0; u < unidades; u++) {
                        for (Integer idAtraccion : atracciones) {
                            crearBoleta(con, r.getIdReserva(), zonaPorAtraccion.get(idAtraccion), idTipoEntrada, idCombo);
                        }
                    }
                    notificaciones.confirmacionReserva(con, r);
                    con.commit();
                    return r.getIdReserva();
                } catch (SQLException | RuntimeException e) {
                    con.rollback();
                    throw e;
                }
            }
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    // =================================================================
    // Agregar una boleta suelta a una reserva que ya existe (CRUD de Boleta)
    // =================================================================
    public void agregarBoleta(int idReserva, int idZona, int idTipoEntrada) {
        try (Connection con = conexionBD.getConexion()) {
            con.setAutoCommit(false);
            try {
                Reserva r = reservaDAO.buscarPorId(con, idReserva);
                if (r == null || !("pendiente".equals(r.getEstado()) || "confirmada".equals(r.getEstado()))) {
                    throw new ReglaNegocioException("Solo se agregan boletas a reservas pendientes o confirmadas.");
                }
                Zona z = zonaDAO.buscarPorId(idZona);
                Funcion f = funcionDAO.buscarPorId(z.getIdFuncion());
                validarFuncionVendible(f);
                boolean planValido = vistaDAO.planesParaVender(f.getIdAtraccion(), f.getFecha()).stream()
                        .anyMatch(p -> ((Number) p.get("id_plan")).intValue() == r.getIdPlan());
                if (!planValido) {
                    throw new ReglaNegocioException("El plan de la reserva no sirve para esa atracción en esa fecha "
                            + "(el prestador no está afiliado o el plan no está vigente).");
                }
                TipoEntrada tipo = tiposPorId().get(idTipoEntrada);
                if ("niño".equals(tipo.getNombre()) && boletaDAO.contarPorTipo(con, idReserva, "adulto") == 0) {
                    throw new ReglaNegocioException("Una boleta de niño solo se vende si en la misma reserva hay al menos una de adulto.");
                }
                if (zonaDAO.cuposDisponibles(con, idZona) < 1) {
                    throw new ReglaNegocioException("La zona " + z.getNombre() + " ya está llena (aforo " + z.getAforo() + ").");
                }
                crearBoleta(con, idReserva, idZona, idTipoEntrada, null);
                reservaDAO.sumarValor(con, idReserva, valorBoleta(z.getPrecio(), tipo.getPorcentajeDescuento()));
                con.commit();
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    // ------------------------------------------------------------------
    private Reserva crearReserva(Connection con, int idCliente, String canal, int idPlan,
                                 LocalDate fechaServicio, BigDecimal total) throws SQLException {
        Reserva r = new Reserva();
        r.setFechaCreacion(LocalDate.now());
        r.setFechaServicio(fechaServicio);
        r.setEstado("pendiente");                 // pasa a confirmada cuando se paga completa
        r.setValorTotal(total);                   // precio congelado (Entrega 2)
        r.setCanal(canal);
        r.setIdCliente(idCliente);
        r.setIdPlan(idPlan);
        reservaDAO.crear(con, r);
        return r;
    }

    /** Inserta la boleta y le pone el código QS-000123 a partir del id que generó MySQL. */
    private void crearBoleta(Connection con, int idReserva, int idZona, int idTipoEntrada, Integer idCombo) throws SQLException {
        Boleta b = new Boleta();
        b.setCodigo("TMP-" + UUID.randomUUID().toString().substring(0, 20));
        b.setEstado("vendida");
        b.setIdReserva(idReserva);
        b.setIdZona(idZona);
        b.setIdTipoEntrada(idTipoEntrada);
        b.setIdCombo(idCombo);
        int id = boletaDAO.crear(con, b);
        boletaDAO.cambiarCodigo(con, id, String.format("QS-%06d", id));
    }

    private void validarFuncionVendible(Funcion f) {
        if (f == null) {
            throw new ReglaNegocioException("La función no existe.");
        }
        if (!"programada".equals(f.getEstado())) {
            throw new ReglaNegocioException("La función del " + f.getFecha() + " está " + f.getEstado() + ": no se venden boletas.");
        }
        if (f.getFecha().isBefore(LocalDate.now())) {
            throw new ReglaNegocioException("La función del " + f.getFecha() + " ya pasó.");
        }
    }

    private void validarCliente(int idCliente) throws SQLException {
        if (clienteDAO.buscarPorId(idCliente) == null) {
            throw new ReglaNegocioException("Escoja el cliente de la reserva.");
        }
    }

    /** Regla 3: el plan debe estar en la lista de planes que se pueden vender (activo, vigente y afiliado). */
    private void validarPlan(int idPlan, List<Map<String, Object>> planesValidos) {
        boolean ok = planesValidos.stream().anyMatch(p -> ((Number) p.get("id_plan")).intValue() == idPlan);
        if (!ok) {
            throw new ReglaNegocioException("El plan escogido no está activo y vigente en la fecha del servicio, "
                    + "o su prestador no está afiliado a la atracción.");
        }
    }

    private Map<Integer, TipoEntrada> tiposPorId() throws SQLException {
        Map<Integer, TipoEntrada> mapa = new HashMap<>();
        for (TipoEntrada t : tipoEntradaDAO.listarTodos()) {
            mapa.put(t.getIdTipoEntrada(), t);
        }
        return mapa;
    }
}
