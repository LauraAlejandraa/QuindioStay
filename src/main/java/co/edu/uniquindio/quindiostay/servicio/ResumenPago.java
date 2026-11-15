package co.edu.uniquindio.quindiostay.servicio;

import co.edu.uniquindio.quindiostay.dao.VistaDAO;
import co.edu.uniquindio.quindiostay.pdf.PdfUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Resumen del dinero de una reserva para la factura, el comprobante y el detalle.
 *
 * En una reserva cancelada la vista_estado_pago_reserva da "pagado $0 / saldo = valor
 * total", porque al reembolsar los pagos exitosos pasan a 'reembolsado' y ya no cuentan.
 * Eso confunde: el cliente sí pagó y no debe nada. Aquí se arma lo que se muestra:
 *   - pagado_cliente: lo que el cliente pagó (exitosos + reembolsados).
 *   - reembolsado: 80 % de los pagos reembolsados (regla de cancelación con más de 5 días).
 *   - retenido: pagado_cliente - reembolsado.
 *   - saldo_mostrar: 0 en una reserva cancelada (no hay nada por pagar).
 *   - nota_reembolso: texto de la línea "Reembolsado".
 * En una reserva no cancelada pagado_cliente = total_pagado y saldo_mostrar = saldo.
 */
public final class ResumenPago {

    private ResumenPago() {
    }

    public static Map<String, Object> calcular(VistaDAO dao, int idReserva, String estadoReserva,
                                               BigDecimal totalPagado, BigDecimal saldo) throws SQLException {
        List<Map<String, Object>> f = dao.consultar(
                "SELECT COALESCE(SUM(CASE WHEN estado = 'exitoso' THEN monto END), 0) AS exitoso, "
              + "COALESCE(SUM(CASE WHEN estado = 'reembolsado' THEN monto END), 0) AS reembolsado "
              + "FROM Pago WHERE id_reserva = ?", idReserva);
        BigDecimal exitoso = (BigDecimal) f.get(0).get("exitoso");
        BigDecimal pagosReembolsados = (BigDecimal) f.get(0).get("reembolsado");
        Map<String, Object> m = new HashMap<>();
        boolean cancelada = "cancelada".equals(estadoReserva);
        m.put("cancelada", cancelada);
        if (!cancelada) {
            m.put("pagado_cliente", totalPagado);
            m.put("saldo_mostrar", saldo);
            return m;
        }
        BigDecimal pagado = exitoso.add(pagosReembolsados);
        BigDecimal reembolso = pagosReembolsados.multiply(ReservaServicio.PORCENTAJE_REEMBOLSO)
                .setScale(2, RoundingMode.HALF_UP);
        String nota;
        if (reembolso.signum() > 0) {
            nota = PdfUtil.pesos(reembolso) + " (80 % de lo pagado, por cancelar con más de "
                    + ReservaServicio.DIAS_MINIMOS_REEMBOLSO + " días de anticipación)";
        } else if (pagado.signum() > 0) {
            nota = "$0 (sin reembolso por cancelar con " + ReservaServicio.DIAS_MINIMOS_REEMBOLSO + " días o menos)";
        } else {
            nota = "$0 (no había pagos cuando se canceló)";
        }
        m.put("pagado_cliente", pagado);
        m.put("reembolsado", reembolso);
        m.put("retenido", pagado.subtract(reembolso));
        m.put("saldo_mostrar", BigDecimal.ZERO);
        m.put("nota_reembolso", nota);
        return m;
    }
}
