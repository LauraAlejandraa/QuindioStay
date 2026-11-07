package co.edu.uniquindio.quindiostay.modelo;



/**
 * Modelo de la tabla Rol: cada atributo corresponde a una columna
 * del script 01_creacion_tablas.sql (mismo nombre, escrito en camelCase).
 */
public class Rol {

    private Integer idRol;   // id_rol
    private String nombre;   // nombre

    public Integer getIdRol() { return idRol; }
    public void setIdRol(Integer idRol) { this.idRol = idRol; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}
