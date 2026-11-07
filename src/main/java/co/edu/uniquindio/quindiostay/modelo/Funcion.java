package co.edu.uniquindio.quindiostay.modelo;

import java.time.LocalDate;
import java.time.LocalTime;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Modelo de la tabla Funcion: cada atributo corresponde a una columna
 * del script 01_creacion_tablas.sql (mismo nombre, escrito en camelCase).
 */
public class Funcion {

    private Integer idFuncion;   // id_funcion
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fecha;   // fecha
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime horaApertura;   // hora_apertura
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime horaCierre;   // hora_cierre
    private String estado;   // estado
    private Integer idAtraccion;   // id_atraccion

    public Integer getIdFuncion() { return idFuncion; }
    public void setIdFuncion(Integer idFuncion) { this.idFuncion = idFuncion; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public LocalTime getHoraApertura() { return horaApertura; }
    public void setHoraApertura(LocalTime horaApertura) { this.horaApertura = horaApertura; }
    public LocalTime getHoraCierre() { return horaCierre; }
    public void setHoraCierre(LocalTime horaCierre) { this.horaCierre = horaCierre; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Integer getIdAtraccion() { return idAtraccion; }
    public void setIdAtraccion(Integer idAtraccion) { this.idAtraccion = idAtraccion; }
}
