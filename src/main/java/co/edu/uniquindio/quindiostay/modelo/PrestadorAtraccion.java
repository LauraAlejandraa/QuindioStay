package co.edu.uniquindio.quindiostay.modelo;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Modelo de Prestador_atraccion (rompe la N:M entre Prestador y Atraccion).
 * Es la única tabla con clave primaria compuesta: (id_prestador, id_atraccion).
 */
public class PrestadorAtraccion {

    private Integer idPrestador;    // id_prestador (PK y FK)
    private Integer idAtraccion;    // id_atraccion (PK y FK)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaAfiliacion;
    /** Solo para el formulario: indica si es una afiliación que ya existe (editar). */
    private boolean existente;

    public Integer getIdPrestador() { return idPrestador; }
    public void setIdPrestador(Integer idPrestador) { this.idPrestador = idPrestador; }
    public Integer getIdAtraccion() { return idAtraccion; }
    public void setIdAtraccion(Integer idAtraccion) { this.idAtraccion = idAtraccion; }
    public LocalDate getFechaAfiliacion() { return fechaAfiliacion; }
    public void setFechaAfiliacion(LocalDate fechaAfiliacion) { this.fechaAfiliacion = fechaAfiliacion; }
    public boolean isExistente() { return existente; }
    public void setExistente(boolean existente) { this.existente = existente; }
}
