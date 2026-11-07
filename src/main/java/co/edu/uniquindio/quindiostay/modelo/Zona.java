package co.edu.uniquindio.quindiostay.modelo;

import java.math.BigDecimal;

/**
 * Modelo de la tabla Zona: cada atributo corresponde a una columna
 * del script 01_creacion_tablas.sql (mismo nombre, escrito en camelCase).
 */
public class Zona {

    private Integer idZona;   // id_zona
    private String nombre;   // nombre
    private Integer aforo;   // aforo
    private BigDecimal precio;   // precio
    private Integer idFuncion;   // id_funcion

    public Integer getIdZona() { return idZona; }
    public void setIdZona(Integer idZona) { this.idZona = idZona; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Integer getAforo() { return aforo; }
    public void setAforo(Integer aforo) { this.aforo = aforo; }
    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }
    public Integer getIdFuncion() { return idFuncion; }
    public void setIdFuncion(Integer idFuncion) { this.idFuncion = idFuncion; }
}
