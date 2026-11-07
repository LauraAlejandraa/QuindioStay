package co.edu.uniquindio.quindiostay.modelo;



/**
 * Modelo de la tabla Perfil_cliente: cada atributo corresponde a una columna
 * del script 01_creacion_tablas.sql (mismo nombre, escrito en camelCase).
 */
public class PerfilCliente {

    private Integer idPerfil;   // id_perfil
    private String idioma;   // idioma
    private String canalPreferido;   // canal_preferido
    private Integer anticipacionRecordatorio;   // anticipacion_recordatorio
    private String aceptaPromociones;   // acepta_promociones
    private Integer idCliente;   // id_cliente

    public Integer getIdPerfil() { return idPerfil; }
    public void setIdPerfil(Integer idPerfil) { this.idPerfil = idPerfil; }
    public String getIdioma() { return idioma; }
    public void setIdioma(String idioma) { this.idioma = idioma; }
    public String getCanalPreferido() { return canalPreferido; }
    public void setCanalPreferido(String canalPreferido) { this.canalPreferido = canalPreferido; }
    public Integer getAnticipacionRecordatorio() { return anticipacionRecordatorio; }
    public void setAnticipacionRecordatorio(Integer anticipacionRecordatorio) { this.anticipacionRecordatorio = anticipacionRecordatorio; }
    public String getAceptaPromociones() { return aceptaPromociones; }
    public void setAceptaPromociones(String aceptaPromociones) { this.aceptaPromociones = aceptaPromociones; }
    public Integer getIdCliente() { return idCliente; }
    public void setIdCliente(Integer idCliente) { this.idCliente = idCliente; }
}
