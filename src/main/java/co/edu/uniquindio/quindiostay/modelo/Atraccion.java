package co.edu.uniquindio.quindiostay.modelo;



/**
 * Modelo de la tabla Atraccion: cada atributo corresponde a una columna
 * del script 01_creacion_tablas.sql (mismo nombre, escrito en camelCase).
 */
public class Atraccion {

    private Integer idAtraccion;   // id_atraccion
    private String nombre;   // nombre
    private String descripcion;   // descripcion
    private String ubicacion;   // ubicacion

    public Integer getIdAtraccion() { return idAtraccion; }
    public void setIdAtraccion(Integer idAtraccion) { this.idAtraccion = idAtraccion; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }
}
