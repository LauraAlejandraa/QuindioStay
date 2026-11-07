package co.edu.uniquindio.quindiostay.modelo;



/**
 * Modelo de la tabla Cuenta: cada atributo corresponde a una columna
 * del script 01_creacion_tablas.sql (mismo nombre, escrito en camelCase).
 */
public class Cuenta {

    private Integer idCuenta;   // id_cuenta
    private String contrasenaCifrada;   // contrasena_cifrada
    private Integer idCliente;   // id_cliente
    /** Contraseña escrita en el formulario (no es columna: se cifra con BCrypt antes de guardar). */
    private String contrasenaNueva;

    public Integer getIdCuenta() { return idCuenta; }
    public void setIdCuenta(Integer idCuenta) { this.idCuenta = idCuenta; }
    public String getContrasenaCifrada() { return contrasenaCifrada; }
    public void setContrasenaCifrada(String contrasenaCifrada) { this.contrasenaCifrada = contrasenaCifrada; }
    public Integer getIdCliente() { return idCliente; }
    public void setIdCliente(Integer idCliente) { this.idCliente = idCliente; }
    public String getContrasenaNueva() { return contrasenaNueva; }
    public void setContrasenaNueva(String contrasenaNueva) { this.contrasenaNueva = contrasenaNueva; }
}
