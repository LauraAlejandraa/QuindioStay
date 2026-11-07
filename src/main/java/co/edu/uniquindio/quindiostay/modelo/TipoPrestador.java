package co.edu.uniquindio.quindiostay.modelo;



/**
 * Modelo de la tabla Tipo_prestador: cada atributo corresponde a una columna
 * del script 01_creacion_tablas.sql (mismo nombre, escrito en camelCase).
 */
public class TipoPrestador {

    private Integer idTipoPrestador;   // id_tipo_prestador
    private String nombre;   // nombre

    public Integer getIdTipoPrestador() { return idTipoPrestador; }
    public void setIdTipoPrestador(Integer idTipoPrestador) { this.idTipoPrestador = idTipoPrestador; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}
