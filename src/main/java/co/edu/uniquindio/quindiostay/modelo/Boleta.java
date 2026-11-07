package co.edu.uniquindio.quindiostay.modelo;



/**
 * Modelo de la tabla Boleta: cada atributo corresponde a una columna
 * del script 01_creacion_tablas.sql (mismo nombre, escrito en camelCase).
 */
public class Boleta {

    private Integer idBoleta;   // id_boleta
    private String codigo;   // codigo
    private String estado;   // estado
    private Integer idReserva;   // id_reserva
    private Integer idZona;   // id_zona
    private Integer idTipoEntrada;   // id_tipo_entrada
    private Integer idCombo;   // id_combo

    public Integer getIdBoleta() { return idBoleta; }
    public void setIdBoleta(Integer idBoleta) { this.idBoleta = idBoleta; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Integer getIdReserva() { return idReserva; }
    public void setIdReserva(Integer idReserva) { this.idReserva = idReserva; }
    public Integer getIdZona() { return idZona; }
    public void setIdZona(Integer idZona) { this.idZona = idZona; }
    public Integer getIdTipoEntrada() { return idTipoEntrada; }
    public void setIdTipoEntrada(Integer idTipoEntrada) { this.idTipoEntrada = idTipoEntrada; }
    public Integer getIdCombo() { return idCombo; }
    public void setIdCombo(Integer idCombo) { this.idCombo = idCombo; }
}
