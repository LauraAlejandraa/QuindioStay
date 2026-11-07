package co.edu.uniquindio.quindiostay.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Modelo de la tabla Pago: cada atributo corresponde a una columna
 * del script 01_creacion_tablas.sql (mismo nombre, escrito en camelCase).
 */
public class Pago {

    private Integer idPago;   // id_pago
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fecha;   // fecha
    private BigDecimal monto;   // monto
    private String metodo;   // metodo
    private String estado;   // estado
    private String referenciaTransaccion;   // referencia_transaccion
    private Integer idReserva;   // id_reserva

    public Integer getIdPago() { return idPago; }
    public void setIdPago(Integer idPago) { this.idPago = idPago; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }
    public String getMetodo() { return metodo; }
    public void setMetodo(String metodo) { this.metodo = metodo; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getReferenciaTransaccion() { return referenciaTransaccion; }
    public void setReferenciaTransaccion(String referenciaTransaccion) { this.referenciaTransaccion = referenciaTransaccion; }
    public Integer getIdReserva() { return idReserva; }
    public void setIdReserva(Integer idReserva) { this.idReserva = idReserva; }
}
