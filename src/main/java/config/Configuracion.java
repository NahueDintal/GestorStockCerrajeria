package config;

import io.github.cdimascio.dotenv.Dotenv;

public class Configuracion {
  private static final Dotenv dotenv = Dotenv.load();
  private static double margenGanancia = Double.parseDouble(
      dotenv.get("MARGEN_GANANCIA", "1.40"));

  public static double getMargenGanancia() {
    return margenGanancia;
  }

  public static void setMargenGanancia(double m) {
    margenGanancia = m;
  }

  public static double calcularPrecioVenta(double precioCosto) {
    return Math.round(precioCosto * margenGanancia * 100.0) / 100.0;
  }
}
