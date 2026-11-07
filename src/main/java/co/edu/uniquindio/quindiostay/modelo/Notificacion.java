package co.edu.uniquindio.quindiostay.modelo;

import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Modelo de la tabla Notificacion: cada atributo corresponde a una columna
 * del script 01_creacion_tablas.sql (mismo nombre, escrito en camelCase).
 */
public class Notificacion {

    private Integer idNotificacion;   // id_notificacion
    private String tipo;   // tipo
    private String canal;   // canal
    private String mensaje;   // mensaje
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime fechaProgramada;   // fecha_programada
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime fechaRealEnvio;   // fecha_real_envio
    private String estado;   // estado
    private Integer idCliente;   // id_cliente
    private Integer idReserva;   // id_reserva

    public Integer getIdNotificacion() { return idNotificacion; }
    public void setIdNotificacion(Integer idNotificacion) { this.idNotificacion = idNotificacion; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getCanal() { return canal; }
    public void setCanal(String canal) { this.canal = canal; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public LocalDateTime getFechaProgramada() { return fechaProgramada; }
    public void setFechaProgramada(LocalDateTime fechaProgramada) { this.fechaProgramada = fechaProgramada; }
    public LocalDateTime getFechaRealEnvio() { return fechaRealEnvio; }
    public void setFechaRealEnvio(LocalDateTime fechaRealEnvio) { this.fechaRealEnvio = fechaRealEnvio; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Integer getIdCliente() { return idCliente; }
    public void setIdCliente(Integer idCliente) { this.idCliente = idCliente; }
    public Integer getIdReserva() { return idReserva; }
    public void setIdReserva(Integer idReserva) { this.idReserva = idReserva; }
}
