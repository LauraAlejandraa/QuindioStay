package co.edu.uniquindio.quindiostay.modelo;

import java.math.BigDecimal;

/**
 * Modelo de la tabla Tipo_entrada: cada atributo corresponde a una columna
 * del script 01_creacion_tablas.sql (mismo nombre, escrito en camelCase).
 */
public class TipoEntrada {

    private Integer idTipoEntrada;   // id_tipo_entrada
    private String nombre;   // nombre
    private BigDecimal porcentajeDescuento;   // porcentaje_descuento

    public Integer getIdTipoEntrada() { return idTipoEntrada; }
    public void setIdTipoEntrada(Integer idTipoEntrada) { this.idTipoEntrada = idTipoEntrada; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public BigDecimal getPorcentajeDescuento() { return porcentajeDescuento; }
    public void setPorcentajeDescuento(BigDecimal porcentajeDescuento) { this.porcentajeDescuento = porcentajeDescuento; }
}
