package co.edu.uniquindio.quindiostay.modelo;



/**
 * Modelo de la tabla Municipio: cada atributo corresponde a una columna
 * del script 01_creacion_tablas.sql (mismo nombre, escrito en camelCase).
 */
public class Municipio {

    private Integer idMunicipio;   // id_municipio
    private String nombre;   // nombre
    private String codigoDane;   // codigo_DANE

    public Integer getIdMunicipio() { return idMunicipio; }
    public void setIdMunicipio(Integer idMunicipio) { this.idMunicipio = idMunicipio; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCodigoDane() { return codigoDane; }
    public void setCodigoDane(String codigoDane) { this.codigoDane = codigoDane; }
}
