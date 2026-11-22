package com.cerrajeria.util;

import com.cerrajeria.dao.CategoriaDAO;
import com.cerrajeria.dao.ConexionDB;
import com.cerrajeria.dao.ProductoDAO;
import com.cerrajeria.model.Categoria;
import com.cerrajeria.model.Producto;
import com.cerrajeria.model.Proveedor;
import org.apache.poi.ss.usermodel.*;

import java.io.File;
import java.sql.*;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.*;
import java.io.File;
import java.util.Map;
import java.util.HashMap;

public class ImportadorXlsx {

  public static class Fila {
    public int nro;
    public String codigo = "";
    public String nombre = "";
    public String categoria = "";
    public String marca = "";
    public String descripcion = "";
    public Double precio;
  }

  public static Workbook abrir(File archivo) throws Exception {
    return WorkbookFactory.create(archivo);
  }

  public static List<String> leerEncabezados(Workbook wb, int hoja, int filaHeader) {
    Sheet s = wb.getSheetAt(hoja);
    Row r = s.getRow(filaHeader);
    List<String> out = new ArrayList<>();
    if (r == null)
      return out;
    DataFormatter fmt = new DataFormatter();
    for (int i = 0; i < r.getLastCellNum(); i++) {
      Cell c = r.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
      out.add(c == null ? "" : fmt.formatCellValue(c).trim());
    }
    return out;
  }

  /** mapa: campo → índice de columna (-1 = no usar) */
  public static List<Fila> leer(Workbook wb, int hoja, int filaHeader,
      Map<String, Integer> mapa) {
    Sheet s = wb.getSheetAt(hoja);
    DataFormatter fmt = new DataFormatter();
    int idxCod = mapa.getOrDefault("codigo", -1);
    int idxNom = mapa.getOrDefault("nombre", -1);
    int idxCat = mapa.getOrDefault("categoria", -1);
    int idxMar = mapa.getOrDefault("marca", -1);
    int idxDes = mapa.getOrDefault("descripcion", -1);
    int idxPre = mapa.getOrDefault("precio", -1);

    List<Fila> out = new ArrayList<>();
    for (int i = filaHeader + 1; i <= s.getLastRowNum(); i++) {
      Row r = s.getRow(i);
      if (r == null)
        continue;
      String cod = txt(r, idxCod, fmt);
      String nom = txt(r, idxNom, fmt);
      if (cod.isEmpty() && nom.isEmpty())
        continue;
      String n = normalizar(nom);
      if (n.startsWith("total") || n.startsWith("subtotal"))
        continue;

      Fila f = new Fila();
      f.nro = i + 1;
      f.codigo = cod;
      f.nombre = nom;
      f.categoria = txt(r, idxCat, fmt);
      f.marca = txt(r, idxMar, fmt);
      f.descripcion = txt(r, idxDes, fmt);
      f.precio = num(r, idxPre);
      out.add(f);
    }
    return out;
  }

  public static ImportadorProductos.Resultado insertar(List<Fila> filas, Proveedor prov,
      boolean precioEsFinal) throws Exception {
    var res = new ImportadorProductos.Resultado();

    Map<String, Integer> categorias = new HashMap<>();
    for (Categoria c : new CategoriaDAO().listar())
      categorias.put(c.getNombre().toLowerCase(), c.getId());

    Set<String> existentes = new HashSet<>();
    try (Connection cn = ConexionDB.getConexion();
        PreparedStatement ps = cn.prepareStatement(
            "SELECT modelo FROM producto WHERE modelo IS NOT NULL");
        ResultSet rs = ps.executeQuery()) {
      while (rs.next())
        existentes.add(rs.getString(1));
    }
    System.out.println("📦 Modelos ya en base: " + existentes.size());

    double factor = (1 + prov.getIvaPorcentaje() / 100.0)
        * (1 + prov.getMarkupPorcentaje() / 100.0);
    System.out.printf("🏭 %s → factor %.3f%n", prov.getNombre(), factor);

    for (Fila f : filas) {
      try {
        if (f.precio == null || f.precio <= 0) {
          res.saltados++;
          if (res.motivos.size() < 10)
            res.motivos.add("Fila " + f.nro + ": sin precio");
          continue;
        }
        String modelo = f.codigo.isBlank() ? "S/M-" + f.nro : f.codigo;
        if (existentes.contains(modelo)) {
          res.saltados++;
          if (res.motivos.size() < 10)
            res.motivos.add("Fila " + f.nro + ": código ya existe (" + modelo + ")");
          continue;
        }
        String nombre = f.nombre.isBlank() ? modelo : f.nombre;

        double costo, venta;
        if (precioEsFinal) {
          venta = f.precio;
          costo = venta / factor;
        } else {
          costo = f.precio;
          venta = prov.calcularPrecioVenta(costo);
        }

        String catNombre = f.categoria.isBlank() ? "Sin categoría" : f.categoria;
        int catId = asegurarCategoria(catNombre, categorias);

        Producto p = new Producto();
        p.setNombre(nombre);
        p.setModelo(modelo);
        p.setMarca(f.marca);
        p.setDescripcion(f.descripcion);
        p.setCategoria(new Categoria(catId, catNombre));
        p.setPrecioCosto(costo);
        p.setPrecioVenta(venta);
        p.setStock(0);
        p.setStockMinimo(0);
        p.setActivo(true);
        p.setFechaAlta(LocalDate.now());
        p.setProveedor(prov.getNombre());

        new ProductoDAO().insertar(p);
        existentes.add(modelo);
        res.insertados++;
        if (res.insertados % 50 == 0)
          System.out.println("  ... " + res.insertados + " insertados");
      } catch (Exception e) {
        res.errores++;
        if (res.motivos.size() < 10)
          res.motivos.add("Fila " + f.nro + ": " + e.getMessage());
      }
    }
    return res;
  }

  private static String txt(Row r, int col, DataFormatter fmt) {
    if (col < 0)
      return "";
    Cell c = r.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
    return c == null ? "" : fmt.formatCellValue(c).trim();
  }

  private static Double num(Row r, int col) {
    if (col < 0)
      return null;
    Cell c = r.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
    if (c == null)
      return null;
    if (c.getCellType() == CellType.NUMERIC)
      return c.getNumericCellValue();
    if (c.getCellType() == CellType.FORMULA) {
      try {
        return c.getCachedFormulaResultType() == CellType.NUMERIC
            ? c.getNumericCellValue()
            : null;
      } catch (Exception e) {
        return null;
      }
    }
    return parseNumero(new DataFormatter().formatCellValue(c));
  }

  /** Interpreta "1.234,56", "1,234.56", "1234.56", "1.234" */
  private static Double parseNumero(String s) {
    if (s == null)
      return null;
    s = s.trim().replace("$", "").replace(" ", "").replace("ARS", "");
    if (s.isEmpty())
      return null;
    if (s.contains(",") && s.contains(".")) {
      if (s.lastIndexOf(",") > s.lastIndexOf("."))
        s = s.replace(".", "").replace(",", ".");
      else
        s = s.replace(",", "");
    } else if (s.contains(",")) {
      int i = s.lastIndexOf(",");
      s = s.substring(i + 1).length() <= 2
          ? s.replace(",", ".")
          : s.replace(",", "");
    } else if (s.contains(".")) {
      int i = s.lastIndexOf(".");
      String antes = s.substring(0, i);
      String despues = s.substring(i + 1);
      if (despues.length() == 3 && antes.replace(".", "").length() <= 3)
        s = s.replace(".", "");
    }
    try {
      return Double.parseDouble(s);
    } catch (Exception e) {
      return null;
    }
  }

  private static String normalizar(String s) {
    if (s == null)
      return "";
    return Normalizer.normalize(s, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "").toLowerCase().trim();
  }

  private static int asegurarCategoria(String nombre, Map<String, Integer> cache)
      throws SQLException {
    String key = nombre.toLowerCase();
    if (cache.containsKey(key))
      return cache.get(key);
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

  /**
   * Importa un .xlsx de forma 100% automática: detecta hoja, header y columnas
   * solo.
   */
  public static ImportadorProductos.Resultado importar(File archivo, Proveedor prov) throws Exception {
    try (Workbook wb = WorkbookFactory.create(archivo)) {
      int hoja = 0;
      int filaHeader = detectarFilaHeader(wb, hoja);
      Map<String, Integer> mapa = detectarColumnas(wb, hoja, filaHeader);
      System.out.println("   Hoja: " + wb.getSheetName(hoja)
          + " | Header: fila " + (filaHeader + 1)
          + " | Columnas: " + mapa);
      List<Fila> filas = leer(wb, hoja, filaHeader, mapa);
      return insertar(filas, prov, false);
    }
  }

  private static int detectarFilaHeader(Workbook wb, int hoja) {
    Sheet s = wb.getSheetAt(hoja);
    DataFormatter fmt = new DataFormatter();
    int max = Math.min(25, s.getLastRowNum());
    // Agregamos palabras clave que usa este proveedor
    String[] claves = { "cod", "sku", "art", "nombre", "descrip", "detalle",
        "pcio", "precio", "venta", "lista", "categ", "grupo", "rubro", "importe" };

    for (int i = 0; i <= max; i++) {
      Row r = s.getRow(i);
      if (r == null)
        continue;
      int coincidencias = 0;
      for (Cell c : r) {
        String v = normalizar(fmt.formatCellValue(c));
        for (String k : claves)
          if (v.contains(k)) {
            coincidencias++;
            break;
          }
      }
      // Si encuentra al menos 3 coincidencias (ej: cod, grupo, art, pcio), es el
      // encabezado
      if (coincidencias >= 3)
        return i;
    }
    return 0;
  }

  private static Map<String, Integer> detectarColumnas(Workbook wb, int hoja, int filaHeader) {
    Sheet s = wb.getSheetAt(hoja);
    Row header = s.getRow(filaHeader);
    Map<String, Integer> mapa = new HashMap<>();
    DataFormatter fmt = new DataFormatter();

    if (header == null) {
      mapa.put("codigo", 1);
      mapa.put("nombre", 2);
      mapa.put("precio", 3);
      mapa.put("categoria", 0);
      return mapa;
    }

    for (int i = 0; i < header.getLastCellNum(); i++) {
      Cell c = header.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
      if (c == null)
        continue;
      String h = normalizar(fmt.formatCellValue(c));
      if (h.isEmpty())
        continue;

      // 1. Código (Cód. Artículo, SKU, etc)
      if (!mapa.containsKey("codigo") && (h.contains("cod") || h.contains("sku"))) {
        mapa.put("codigo", i);
      }
      // 2. Categoría (Descrip. del Grupo, Rubro, Familia, etc)
      else if (!mapa.containsKey("categoria") && (h.contains("grupo") || h.contains("rubro") || h.contains("categ")
          || h.contains("linea") || h.contains("familia"))) {
        mapa.put("categoria", i);
      }
      // 3. Precio (PCIO, Precio, Costo, Importe, etc)
      else if (!mapa.containsKey("precio") && (h.contains("pcio") || h.contains("precio") || h.contains("costo")
          || h.contains("importe") || h.contains("lista"))) {
        mapa.put("precio", i);
      }
      // 4. Nombre (Descrip. del Artículo, Producto, Detalle)
      else if (!mapa.containsKey("nombre")
          && (h.contains("art") || h.contains("descrip") || h.contains("producto") || h.contains("detalle"))) {
        mapa.put("nombre", i);
      }
      // 5. Marca
      else if (!mapa.containsKey("marca") && h.contains("marca")) {
        mapa.put("marca", i);
      }
    }

    // Fallbacks de seguridad por si el proveedor cambia el formato
    if (!mapa.containsKey("codigo"))
      mapa.put("codigo", 1);
    if (!mapa.containsKey("nombre"))
      mapa.put("nombre", 2);
    if (!mapa.containsKey("precio"))
      mapa.put("precio", 3);
    if (!mapa.containsKey("categoria"))
      mapa.put("categoria", 0);

    return mapa;
  }
}
