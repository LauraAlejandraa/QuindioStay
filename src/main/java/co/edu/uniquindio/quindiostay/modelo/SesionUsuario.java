package co.edu.uniquindio.quindiostay.modelo;

import java.io.Serializable;

/**
 * Lo que se guarda en la sesión después de iniciar sesión.
 * Puede ser un cliente (entra con su correo) o un usuario interno
 * (entra con su nombre de usuario). Según el rol cambia el menú.
 */
public class SesionUsuario implements Serializable {

    public static final String ADMIN = "administrador de la plataforma";
    public static final String ENCARGADO = "encargado de prestador";
    public static final String OPERADOR = "operador";
    public static final String CLIENTE = "cliente";

    private String rol;
    private String nombreMostrar;
    private Integer idUsuario;
    private Integer idCliente;
    private Integer idPrestador;
    private String nombrePrestador;

    public boolean isAdmin() { return ADMIN.equals(rol); }
    public boolean isEncargado() { return ENCARGADO.equals(rol); }
    public boolean isOperador() { return OPERADOR.equals(rol); }
    public boolean isCliente() { return CLIENTE.equals(rol); }
    public boolean isInterno() { return !isCliente(); }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
    public String getNombreMostrar() { return nombreMostrar; }
    public void setNombreMostrar(String nombreMostrar) { this.nombreMostrar = nombreMostrar; }
    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }
    public Integer getIdCliente() { return idCliente; }
    public void setIdCliente(Integer idCliente) { this.idCliente = idCliente; }
    public Integer getIdPrestador() { return idPrestador; }
    public void setIdPrestador(Integer idPrestador) { this.idPrestador = idPrestador; }
    public String getNombrePrestador() { return nombrePrestador; }
    public void setNombrePrestador(String nombrePrestador) { this.nombrePrestador = nombrePrestador; }
}
