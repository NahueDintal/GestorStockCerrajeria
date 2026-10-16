package com.cerrajeria.dao;

import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionDB {

  private static final Dotenv dotenv = Dotenv.configure()
      .ignoreIfMissing()
      .load();

  private static final String URL = String.format(
      "jdbc:mariadb://%s:%s/%s?useUnicode=true&characterEncoding=utf8mb4" +
          "&socketTimeout=15000&connectTimeout=5000" +
          "&sessionVariables=innodb_lock_wait_timeout=10",
      dotenv.get("DB_HOST", "localhost"),
      dotenv.get("DB_PORT", "3306"),
      dotenv.get("DB_NAME", "cerrajeria"));

  private static final String USER = dotenv.get("DB_USER", "cerrajeria");
  private static final String PASS = dotenv.get("DB_PASS", "");

  private ConexionDB() {
  }

  public static Connection getConexion() throws SQLException {
    Connection cn = DriverManager.getConnection(URL, USER, PASS);
    cn.setAutoCommit(true);
    return cn;
  }
}
