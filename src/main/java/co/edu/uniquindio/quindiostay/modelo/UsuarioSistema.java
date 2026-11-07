package co.edu.uniquindio.quindiostay.modelo;



/**
 * Modelo de la tabla Usuario_sistema: cada atributo corresponde a una columna
 * del script 01_creacion_tablas.sql (mismo nombre, escrito en camelCase).
 */
public class UsuarioSistema {

    private Integer idUsuario;   // id_usuario
    private String nombreUsuario;   // nombre_usuario
    private String contrasenaCifrada;   // contrasena_cifrada
    private String estado;   // estado
    private Integer idRol;   // id_rol
    private Integer idPrestador;   // id_prestador
    /** Contraseña escrita en el formulario (no es columna: se cifra con BCrypt antes de guardar). */
    private String contrasenaNueva;

    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }
    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }
    public String getContrasenaCifrada() { return contrasenaCifrada; }
    public void setContrasenaCifrada(String contrasenaCifrada) { this.contrasenaCifrada = contrasenaCifrada; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Integer getIdRol() { return idRol; }
    public void setIdRol(Integer idRol) { this.idRol = idRol; }
    public Integer getIdPrestador() { return idPrestador; }
    public void setIdPrestador(Integer idPrestador) { this.idPrestador = idPrestador; }
    public String getContrasenaNueva() { return contrasenaNueva; }
    public void setContrasenaNueva(String contrasenaNueva) { this.contrasenaNueva = contrasenaNueva; }
}
