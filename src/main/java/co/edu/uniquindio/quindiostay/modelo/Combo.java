package co.edu.uniquindio.quindiostay.modelo;

import java.math.BigDecimal;

/**
 * Modelo de la tabla Combo: cada atributo corresponde a una columna
 * del script 01_creacion_tablas.sql (mismo nombre, escrito en camelCase).
 */
public class Combo {

    private Integer idCombo;   // id_combo
    private String nombre;   // nombre
    private String descripcion;   // descripcion
    private BigDecimal precioEspecial;   // precio_especial
    private String estado;   // estado

    public Integer getIdCombo() { return idCombo; }
    public void setIdCombo(Integer idCombo) { this.idCombo = idCombo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public BigDecimal getPrecioEspecial() { return precioEspecial; }
    public void setPrecioEspecial(BigDecimal precioEspecial) { this.precioEspecial = precioEspecial; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
