package com.cerrajeria.dao;

import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionDB {

    private static final Dotenv dotenv = Dotenv.configure()
            .ignoreIfMissing()   // si no hay .env, no explota al cargar la clase
            .load();

    private static final String URL = String.format(
            "jdbc:mariadb://%s:%s/%s?useUnicode=true&characterEncoding=utf8mb4",
            dotenv.get("DB_HOST", "localhost"),
            dotenv.get("DB_PORT", "3306"),
            dotenv.get("DB_NAME", "cerrajeria")
    );

    private static final String USER = dotenv.get("DB_USER", "cerrajeria");
    private static final String PASS = dotenv.get("DB_PASS", "");

    private static Connection conexion;

    private ConexionDB() { }

    public static Connection getConexion() throws SQLException {
        if (conexion == null || conexion.isClosed()) {
            conexion = DriverManager.getConnection(URL, USER, PASS);
            conexion.setAutoCommit(true);
        }
        return conexion;
    }

    public static void cerrar() {
        if (conexion != null) {
            try { conexion.close(); }
            catch (SQLException ignored) { }
            finally { conexion = null; }
        }
    }
}
