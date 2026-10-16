package com.cerrajeria.util;

import com.cerrajeria.dao.CategoriaDAO;
import com.cerrajeria.dao.ConexionDB;
import com.cerrajeria.dao.ProductoDAO;
import com.cerrajeria.model.Categoria;
import com.cerrajeria.model.Producto;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class ImportadorProductos {

  public static class Resultado {
    public int insertados, saltados, errores;
    public List<String> motivos = new ArrayList<>();

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("✅ Insertados: ").append(insertados).append("\n");
      sb.append("⏭ Saltados: ").append(saltados).append("\n");
      sb.append("❌ Errores: ").append(errores);
      if (!motivos.isEmpty()) {
        sb.append("\n\nPrimeros motivos:\n");
        int max = Math.min(5, motivos.size());
        for (int i = 0; i < max; i++) {
          sb.append("• ").append(motivos.get(i)).append("\n");
        }
      }
      return sb.toString();
    }
  }

  public static Resultado importar(File csv) throws Exception {
    Resultado r = new Resultado();
    Map<String, Integer> categorias = asegurarCategorias();

    Set<String> modelosExistentes = new HashSet<>();
    try (Connection cn = ConexionDB.getConexion();
        PreparedStatement ps = cn.prepareStatement(
            "SELECT modelo FROM producto WHERE modelo IS NOT NULL");
        ResultSet rs = ps.executeQuery()) {
      while (rs.next())
        modelosExistentes.add(rs.getString(1));
    }
    System.out.println("📦 Modelos ya en base: " + modelosExistentes.size());

    // Leer TODO el archivo primero para detectar el separador
    List<String> lineas = new ArrayList<>();
    try (BufferedReader br = new BufferedReader(new InputStreamReader(
        new FileInputStream(csv), StandardCharsets.UTF_8))) {
      String l;
      while ((l = br.readLine()) != null)
        lineas.add(l);
    }

    String sep = detectarSeparador(lineas);
    System.out.println("🔍 Separador detectado: '" + sep + "'");

    boolean enDatos = false;
    int fila = 0;
    for (String linea : lineas) {
      fila++;
      if (linea.isBlank())
        continue;

      if (!enDatos) {
        String low = linea.toLowerCase();
        if (low.contains("descrip. del grupo")
            || low.contains("cod. articulo")
            || low.contains("cód. artículo")
            || low.contains("descrip. del articulo")
            || low.contains("descrip. del artículo")) {
          enDatos = true;
          System.out.println("📋 Header en línea " + fila);
        }
        continue;
      }

      String[] p = parseCSV(linea, sep);
      if (p.length < 6) {
        r.saltados++;
        if (r.motivos.size() < 10)
          r.motivos.add("Línea " + fila + ": solo " + p.length + " columnas");
        continue;
      }

      try {
        String catNombre = p[0].trim();
        String modelo = p[1].trim();
        String nombre = p[2].trim();
        double costo = parsePrecio(p[3]);
        double venta = parsePrecio(p[5]);

        if (nombre.isEmpty()) {
          r.saltados++;
          if (r.motivos.size() < 10)
            r.motivos.add("Línea " + fila + ": nombre vacío");
          continue;
        }
        if (modelo.isEmpty()) {
          // Generamos un modelo sintético para no perder el producto
          modelo = "S/M-" + fila;
        }
        if (modelosExistentes.contains(modelo)) {
          r.saltados++;
          if (r.motivos.size() < 10)
            r.motivos.add("Línea " + fila + ": modelo ya existe (" + modelo + ")");
          continue;
        }

        Producto prod = new Producto();
        prod.setNombre(nombre);
        prod.setModelo(modelo);
        prod.setCategoria(new Categoria(
            categorias.getOrDefault(catNombre.toLowerCase(), 10),
            catNombre));
        prod.setPrecioCosto(costo);
        prod.setPrecioVenta(venta);
        prod.setStock(0);
        prod.setStockMinimo(0);
        prod.setActivo(true);
        prod.setFechaAlta(LocalDate.now());
        prod.setProveedor("COSQUIN HERRAJES");

        new ProductoDAO().insertar(prod);
        modelosExistentes.add(modelo);
        r.insertados++;

        if (r.insertados % 100 == 0)
          System.out.println("  ... " + r.insertados + " insertados");

      } catch (Exception e) {
        r.errores++;
        if (r.motivos.size() < 10)
          r.motivos.add("Línea " + fila + ": " + e.getMessage());
      }
    }
    return r;
  }

  /**
   * Detecta el separador contando cuántas veces aparece cada uno
   * en la línea con más "celdas" (la de datos).
   */
  private static String detectarSeparador(List<String> lineas) {
    String[] candidatos = { ";", ",", "\t", "|" };
    String mejor = ";";
    int mejorCuenta = 0;

    for (String linea : lineas) {
      if (linea.isBlank())
        continue;
      if (linea.toLowerCase().contains("descrip")) {
        for (String c : candidatos) {
          int n = linea.split(java.util.regex.Pattern.quote(c), -1).length;
          if (n > mejorCuenta) {
            mejorCuenta = n;
            mejor = c;
          }
        }
      }
    }
    return mejor;
  }

  // ========== Categorías ==========

  private static final String[] CATEGORIAS_EXCEL = {
      "DESTRABA PESTILLO", "CERRADURAS PARA BLINDEX",
      "CERRADURAS Y CERROJOS HERMANAD", "CERRADURAS DE SEGURIDAD",
      "CERRADURAS DE INTERIOR", "CERRADURAS PARA MUEBLES",
      "CANDADOS", "CAJA FUERTE Y TESOROS", "CORREDERAS TELESCOPICAS",
      "RUEDAS PORTON", "CONTROLES DE ACCESO", "CAZOLETAS Y PISTONES",
      "ACCESORIOS", "MANIJAS", "MANIJAS HIERRO",
      "PASADORES Y ALDABAS", "SOPORTES", "TIRANTERIA",
      "FIJACION", "GRASAS Y LUBRICANTES", "BISAGRAS Y FALLEBAS",
      "FALLEBAS", "MAQ Y HERR", "TORNILLOS",
      "FRESADOS, C/ALAS P/MECHA", "PAN-FRAMING", "PITONES",
      "TARUGOS GRAMPAS PRECINTOS", "TARUGO PARA PRECINTO Y CLAVO",
      "TARUGO ESPIGA", "TARUGOS PARA YESO", "TARUGOS STANDARD",
      "TARUGOS UNIVERSALES", "PILAS Y BATERIAS", "CHIP",
      "CARCASAS", "LLAVES"
  };

  private static Map<String, Integer> asegurarCategorias() throws SQLException {
    Map<String, Integer> mapa = new HashMap<>();
    for (Categoria c : new CategoriaDAO().listar()) {
      mapa.put(c.getNombre().toLowerCase(), c.getId());
    }

    try (Connection cn = ConexionDB.getConexion()) {
      for (String nombre : CATEGORIAS_EXCEL) {
        String key = nombre.toLowerCase();
        if (mapa.containsKey(key))
          continue;

        try (PreparedStatement ps = cn.prepareStatement(
            "INSERT INTO categoria (nombre) VALUES (?)",
            Statement.RETURN_GENERATED_KEYS)) {
          ps.setString(1, nombre);
          ps.executeUpdate();
          try (ResultSet rs = ps.getGeneratedKeys()) {
            rs.next();
            mapa.put(key, rs.getInt(1));
            System.out.println("  + Categoría nueva: " + nombre);
          }
        }
      }
    }
    return mapa;
  }

  // ========== Helpers ==========

  private static String[] parseCSV(String linea, String sep) {
    List<String> campos = new ArrayList<>();
    StringBuilder actual = new StringBuilder();
    boolean entreComillas = false;

    for (int i = 0; i < linea.length(); i++) {
      char c = linea.charAt(i);
      if (c == '"') {
        if (entreComillas && i + 1 < linea.length() && linea.charAt(i + 1) == '"') {
          actual.append('"');
          i++;
        } else {
          entreComillas = !entreComillas;
        }
      } else if (!entreComillas && linea.startsWith(sep, i)) {
        campos.add(actual.toString());
        actual.setLength(0);
        i += sep.length() - 1;
      } else {
        actual.append(c);
      }
    }
    campos.add(actual.toString());
    return campos.toArray(new String[0]);
  }

  private static double parsePrecio(String s) {
    if (s == null)
      return 0;
    s = s.trim().replace("$", "").replace(" ", "");
    if (s.isEmpty())
      return 0;
    if (s.contains(",") && s.contains(".")) {
      s = s.replace(".", "").replace(",", ".");
    } else if (s.contains(",")) {
      s = s.replace(",", ".");
    }
    try {
      return Double.parseDouble(s);
    } catch (NumberFormatException e) {
      return 0;
    }
  }
}
