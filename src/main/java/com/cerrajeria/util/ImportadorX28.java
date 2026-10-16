package com.cerrajeria.util;

import com.cerrajeria.dao.CategoriaDAO;
import com.cerrajeria.dao.ConexionDB;
import com.cerrajeria.dao.ProductoDAO;
import com.cerrajeria.model.Categoria;
import com.cerrajeria.model.Producto;
import com.cerrajeria.model.Proveedor;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.*;

/**
 * Importador específico para las listas X-28.
 * NO es CSV: es un dump de texto donde cada producto ocupa 3-4 líneas.
 */
public class ImportadorX28 {

  // "KL20 S   | P1218860   MANUAL DIGITAL"
  private static final Pattern P_PRODUCTO = Pattern.compile(
      "^(.+?)\\s*\\|\\s*(P\\d+)\\s*(.*)$");

  // "$ 104.650   10 pts."  ó  "$ 31.700   -"
  private static final Pattern P_PRECIO = Pattern.compile(
      "\\$\\s*([\\d.,]+)\\s*(\\d+\\s*pts?\\.?|-)?");

  public static ImportadorProductos.Resultado importar(File archivo, Proveedor proveedor)
      throws Exception {

    var r = new ImportadorProductos.Resultado();
    Map<String, Integer> categorias = new HashMap<>();
    for (Categoria c : new CategoriaDAO().listar()) {
      categorias.put(c.getNombre().toLowerCase(), c.getId());
    }

    double factor = (1 + proveedor.getIvaPorcentaje() / 100.0)
        * (1 + proveedor.getMarkupPorcentaje() / 100.0);
    System.out.println("🏭 Proveedor: " + proveedor.getNombre());
    System.out.printf("   → Precio final = costo × %.3f%n", factor);

    // Modelos ya en base (para no duplicar)
    Set<String> existentes = new HashSet<>();
    try (Connection cn = ConexionDB.getConexion();
         PreparedStatement ps = cn.prepareStatement(
             "SELECT modelo FROM producto WHERE modelo IS NOT NULL");
         ResultSet rs = ps.executeQuery()) {
      while (rs.next()) existentes.add(rs.getString(1));
    }
    System.out.println("📦 Modelos ya en base: " + existentes.size());

    // Leer todo
    List<String> lineas = new ArrayList<>();
    try (BufferedReader br = new BufferedReader(new InputStreamReader(
        new FileInputStream(archivo), StandardCharsets.UTF_8))) {
      String l;
      while ((l = br.readLine()) != null) lineas.add(l);
    }

    // Parser
    String categoriaActual = "Sin categoría";
    String modeloComercial = null, codigo = null, tipo = null;
    Double precio = null;
    StringBuilder descripcion = new StringBuilder();
    int fila = 0, bloqueInicio = 0;

    for (String cruda : lineas) {
      fila++;
      String limpia = quitarComillas(cruda).trim();
      if (limpia.isEmpty()) continue;

      // ¿Encabezado de sección?
      String seccion = detectarSeccion(limpia);
      if (seccion != null) {
        volcar(r, proveedor, categorias, modeloComercial, codigo, tipo,
            descripcion, precio, categoriaActual, bloqueInicio, existentes);
        modeloComercial = null; precio = null;
        descripcion.setLength(0);
        categoriaActual = seccion;
        continue;
      }

      // ¿Inicio de producto?
      Matcher mProd = P_PRODUCTO.matcher(limpia);
      if (mProd.matches()) {
        volcar(r, proveedor, categorias, modeloComercial, codigo, tipo,
            descripcion, precio, categoriaActual, bloqueInicio, existentes);
        modeloComercial = mProd.group(1).trim();
        codigo = mProd.group(2).trim();
        tipo = mProd.group(3).trim();
        precio = null;
        descripcion.setLength(0);
        bloqueInicio = fila;
        continue;
      }

      if (modeloComercial == null) continue; // ruido entre productos

      // ¿Contiene precio?
      Matcher mP = P_PRECIO.matcher(limpia);
      if (mP.find() && precio == null) {
        precio = parsePrecioArg(mP.group(1));
        // Texto antes del $ → descripción
        String antes = limpia.substring(0, mP.start()).trim();
        if (!antes.isEmpty()) descripcion.append(antes).append(' ');
        // Texto después (fuera del sufijo de puntos) → descripción
        String despues = limpia.substring(mP.end()).trim();
        if (!despues.isEmpty() && !despues.matches("^\\d+\\s*pts?\\.?.*$")) {
          descripcion.append(despues).append(' ');
        }
        continue;
      }

      // Línea de descripción
      descripcion.append(limpia).append(' ');
    }

    // Último bloque
    volcar(r, proveedor, categorias, modeloComercial, codigo, tipo,
        descripcion, precio, categoriaActual, bloqueInicio, existentes);

    return r;
  }

  // ========== Volcado de un producto a la base ==========

  private static void volcar(ImportadorProductos.Resultado r, Proveedor proveedor,
                             Map<String, Integer> categorias,
                             String modeloComercial, String codigo, String tipo,
                             StringBuilder descripcion, Double precio,
                             String categoria, int fila, Set<String> existentes)
      throws SQLException {

    if (modeloComercial == null || precio == null) return;

    if (existentes.contains(codigo)) {
      r.saltados++;
      if (r.motivos.size() < 10)
        r.motivos.add("Línea " + fila + ": código ya existe (" + codigo + ")");
      return;
    }

    String desc = descripcion.toString().trim().replaceAll("\\s+", " ");
    String nombre = modeloComercial;
    if (!desc.isBlank()) {
      String corta = desc.length() > 70 ? desc.substring(0, 67) + "..." : desc;
      nombre = modeloComercial + " - " + corta;
    }

    int catId = asegurarCategoria(categoria, categorias);

    Producto p = new Producto();
    p.setNombre(nombre);
    p.setModelo(codigo);                       // P1218860 → único
    p.setDescripcion(desc);
    p.setCategoria(new Categoria(catId, categoria));
    p.setPrecioCosto(precio);
    p.setPrecioVenta(proveedor.calcularPrecioVenta(precio));
    p.setStock(0);
    p.setStockMinimo(0);
    p.setActivo(true);
    p.setFechaAlta(LocalDate.now());
    p.setProveedor(proveedor.getNombre());
    p.setObservaciones("Comercial: " + modeloComercial +
        (tipo.isBlank() ? "" : " | " + tipo));

    new ProductoDAO().insertar(p);
    existentes.add(codigo);
    r.insertados++;

    if (r.insertados % 50 == 0)
      System.out.println("  ... " + r.insertados + " insertados");
  }

  // ========== Detección de secciones / categorías ==========

  private static String detectarSeccion(String linea) {
    String n = normalize(linea);
    if (n.contains("SISTEMASPARAAUTOS"))   return "Sistemas para Autos";
    if (n.contains("SISTEMASPARAMOTOS"))   return "Sistemas para Motos";
    if (n.contains("TRANSMISORES"))        return "Transmisores";
    if (n.contains("SONORIZACION"))        return "Sonorización";
    if (n.contains("ANTISABOTAJE"))        return "Antisabotaje";
    if (n.contains("ANTIASALTO"))          return "Antiasaltos";
    if (n.contains("CAMARASDEVIDEO"))      return "Cámaras de Video";
    if (n.contains("SENSORESDEESTACION"))  return "Sensores de Estacionamiento";
    if (n.contains("MODULOSDEDIAGNOSTICO"))return "Módulos de Diagnóstico";
    if (n.contains("CIERRAPUERTAS"))       return "Cierra Puertas";
    if (n.contains("LEVANTAVIDRIOS"))      return "Levantavidrios";
    if (n.contains("ENCENDIDOAUTOMATICO")) return "Encendido Automático";
    if (n.contains("SEGUIMIENTO"))         return "Seguimiento y Localización";
    if (n.contains("KITABRECIERRA"))       return "Kits Abre-Cierra";
    if (n.contains("MOTORES"))             return "Motores";
    return null;
  }

  private static int asegurarCategoria(String nombre, Map<String, Integer> cache)
      throws SQLException {
    String key = nombre.toLowerCase();
    if (cache.containsKey(key)) return cache.get(key);

    try (Connection cn = ConexionDB.getConexion();
         PreparedStatement ps = cn.prepareStatement(
             "INSERT INTO categoria (nombre) VALUES (?)",
             Statement.RETURN_GENERATED_KEYS)) {
      ps.setString(1, nombre);
      ps.executeUpdate();
      try (ResultSet rs = ps.getGeneratedKeys()) {
        rs.next();
        int id = rs.getInt(1);
        cache.put(key, id);
        System.out.println("  + Categoría nueva: " + nombre);
        return id;
      }
    }
  }

  // ========== Helpers ==========

  private static String normalize(String s) {
    return Normalizer.normalize(s, Normalizer.Form.NFD)
        .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
        .replaceAll("\\s+", "")
        .toUpperCase();
  }

  private static String quitarComillas(String s) {
    s = s.trim();
    if (s.startsWith("\"")) s = s.substring(1);
    if (s.endsWith("\""))   s = s.substring(0, s.length() - 1);
    return s;
  }

  private static double parsePrecioArg(String s) {
    if (s == null) return 0;
    s = s.trim().replace("$", "").replace(" ", "");
    // Formato argentino "104.650" → 104650
    if (s.matches("\\d{1,3}(\\.\\d{3})+")) s = s.replace(".", "");
    s = s.replace(",", ".");
    try {
      return Double.parseDouble(s);
    } catch (NumberFormatException e) {
      return 0;
    }
  }
}
