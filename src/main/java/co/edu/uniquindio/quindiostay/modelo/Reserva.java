package co.edu.uniquindio.quindiostay.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Modelo de la tabla Reserva: cada atributo corresponde a una columna
 * del script 01_creacion_tablas.sql (mismo nombre, escrito en camelCase).
 */
public class Reserva {

    private Integer idReserva;   // id_reserva
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaCreacion;   // fecha_creacion
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaServicio;   // fecha_servicio
    private String estado;   // estado
    private BigDecimal valorTotal;   // valor_total
    private String canal;   // canal
    private Integer idCliente;   // id_cliente
    private Integer idPlan;   // id_plan

    public Integer getIdReserva() { return idReserva; }
    public void setIdReserva(Integer idReserva) { this.idReserva = idReserva; }
    public LocalDate getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDate fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public LocalDate getFechaServicio() { return fechaServicio; }
    public void setFechaServicio(LocalDate fechaServicio) { this.fechaServicio = fechaServicio; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public BigDecimal getValorTotal() { return valorTotal; }
    public void setValorTotal(BigDecimal valorTotal) { this.valorTotal = valorTotal; }
    public String getCanal() { return canal; }
    public void setCanal(String canal) { this.canal = canal; }
    public Integer getIdCliente() { return idCliente; }
    public void setIdCliente(Integer idCliente) { this.idCliente = idCliente; }
    public Integer getIdPlan() { return idPlan; }
    public void setIdPlan(Integer idPlan) { this.idPlan = idPlan; }
}
