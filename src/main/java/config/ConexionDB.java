package config;

import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConexionDB {
  private static final Logger log = LoggerFactory.getLogger(ConexionDB.class);
  private static final Dotenv dotenv = Dotenv.load();
  private static final String URL = dotenv.get("DB_URL");
  private static final String USER = dotenv.get("DB_USER");
  private static final String PASS = dotenv.get("DB_PASS");

  static {
    try {
      Class.forName("com.mysql.cj.jdbc.Driver");
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("Driver MySQL no encontrado", e);
    }
  }

  public static Connection getConexion() throws SQLException {
    return DriverManager.getConnection(URL, USER, PASS);
  }

  // Helper para cerrar sin repetir try/catch
  public static void cerrar(AutoCloseable... recursos) {
    for (AutoCloseable r : recursos) {
      if (r != null)
        try {
          r.close();
        } catch (Exception e) {
          log.warn("Error cerrando", e);
        }
    }
  }
}
