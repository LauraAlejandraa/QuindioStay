package co.edu.uniquindio.quindiostay.dao;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Clase de conexión: lee la URL, el usuario y la contraseña de
 * application.properties y entrega conexiones JDBC a MySQL.
 * Cada DAO pide una conexión, hace su consulta y la cierra (try-with-resources).
 */
@Component
public class ConexionBD {

    @Value("${quindiostay.bd.url}")
    private String url;

    @Value("${quindiostay.bd.usuario}")
    private String usuario;

    @Value("${quindiostay.bd.contrasena}")
    private String contrasena;

    public Connection getConexion() throws SQLException {
        return DriverManager.getConnection(url, usuario, contrasena);
    }
}
