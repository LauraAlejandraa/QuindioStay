package co.edu.uniquindio.quindiostay.modelo;



/**
 * Modelo de la tabla Combo_atraccion: cada atributo corresponde a una columna
 * del script 01_creacion_tablas.sql (mismo nombre, escrito en camelCase).
 */
public class ComboAtraccion {

    private Integer idComboAtraccion;   // id_combo_atraccion
    private Integer idCombo;   // id_combo
    private Integer idAtraccion;   // id_atraccion

    public Integer getIdComboAtraccion() { return idComboAtraccion; }
    public void setIdComboAtraccion(Integer idComboAtraccion) { this.idComboAtraccion = idComboAtraccion; }
    public Integer getIdCombo() { return idCombo; }
    public void setIdCombo(Integer idCombo) { this.idCombo = idCombo; }
    public Integer getIdAtraccion() { return idAtraccion; }
    public void setIdAtraccion(Integer idAtraccion) { this.idAtraccion = idAtraccion; }
}
