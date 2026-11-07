package co.edu.uniquindio.quindiostay.modelo;



/**
 * Modelo de la tabla Prestador: cada atributo corresponde a una columna
 * del script 01_creacion_tablas.sql (mismo nombre, escrito en camelCase).
 */
public class Prestador {

    private Integer idPrestador;   // id_prestador
    private String nombreComercial;   // nombre_comercial
    private String nitDocumento;   // NIT_documento
    private String rnt;   // RNT
    private String direccion;   // direccion
    private String telefono;   // telefono
    private String correo;   // correo
    private String estado;   // estado
    private Integer idMunicipio;   // id_municipio
    private Integer idTipoPrestador;   // id_tipo_prestador

    public Integer getIdPrestador() { return idPrestador; }
    public void setIdPrestador(Integer idPrestador) { this.idPrestador = idPrestador; }
    public String getNombreComercial() { return nombreComercial; }
    public void setNombreComercial(String nombreComercial) { this.nombreComercial = nombreComercial; }
    public String getNitDocumento() { return nitDocumento; }
    public void setNitDocumento(String nitDocumento) { this.nitDocumento = nitDocumento; }
    public String getRnt() { return rnt; }
    public void setRnt(String rnt) { this.rnt = rnt; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Integer getIdMunicipio() { return idMunicipio; }
    public void setIdMunicipio(Integer idMunicipio) { this.idMunicipio = idMunicipio; }
    public Integer getIdTipoPrestador() { return idTipoPrestador; }
    public void setIdTipoPrestador(Integer idTipoPrestador) { this.idTipoPrestador = idTipoPrestador; }
}
