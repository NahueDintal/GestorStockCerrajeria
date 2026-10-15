package com.cerrajeria.dao;

import com.cerrajeria.model.Categoria;
import com.cerrajeria.model.Producto;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ProductoDAO {

  // SELECT base con JOIN para traer el nombre de la categoría
  private static final String SELECT_BASE = """
          SELECT p.*, c.nombre AS categoria_nombre
          FROM producto p
          LEFT JOIN categoria c ON c.id = p.categoria_id
      """;

  // ============ INSERT ============
  public int insertar(Producto p) throws SQLException {
    String sql = """
            INSERT INTO producto
            (codigo, nombre, marca, categoria_id, tipo, modelo, material,
             medidas, color, descripcion, proveedor, precio_costo, precio_venta,
             stock, stock_minimo, ubicacion, activo, fecha_alta, imagen, observaciones)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

    try (Connection cn = ConexionDB.getConexion();
        PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

      llenar(ps, p);
      ps.executeUpdate();

      try (ResultSet rs = ps.getGeneratedKeys()) {
        if (rs.next()) {
          p.setId(rs.getInt(1));
          return p.getId();
        }
      }
    }
    return -1;
  }

  // ============ UPDATE ============
  public boolean actualizar(Producto p) throws SQLException {
    String sql = """
            UPDATE producto SET
                codigo=?, nombre=?, marca=?, categoria_id=?, tipo=?, modelo=?,
                material=?, medidas=?, color=?, descripcion=?, proveedor=?,
                precio_costo=?, precio_venta=?, stock=?, stock_minimo=?,
                ubicacion=?, activo=?, fecha_alta=?, imagen=?, observaciones=?
            WHERE id=?
        """;

    try (Connection cn = ConexionDB.getConexion();
        PreparedStatement ps = cn.prepareStatement(sql)) {

      llenar(ps, p);
      ps.setInt(21, p.getId());
      return ps.executeUpdate() > 0;
    }
  }

  // ============ DELETE (baja lógica) ============
  public boolean desactivar(int id) throws SQLException {
    String sql = "UPDATE producto SET activo = 0 WHERE id = ?";
    try (Connection cn = ConexionDB.getConexion();
        PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setInt(1, id);
      return ps.executeUpdate() > 0;
    }
  }

  // ============ SELECT por id ============
  public Producto buscarPorId(int id) throws SQLException {
    String sql = SELECT_BASE + " WHERE p.id = ?";
    try (Connection cn = ConexionDB.getConexion();
        PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setInt(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next())
          return mapear(rs);
      }
    }
    return null;
  }

  // ============ SELECT todos (solo activos) ============
  public List<Producto> listarActivos() throws SQLException {
    String sql = SELECT_BASE + " WHERE p.activo = 1 ORDER BY p.nombre";
    List<Producto> lista = new ArrayList<>();
    try (Connection cn = ConexionDB.getConexion();
        PreparedStatement ps = cn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {
      while (rs.next())
        lista.add(mapear(rs));
    }
    return lista;
  }

  // ============ SELECT todos (incluye inactivos) ============
  public List<Producto> listarTodos() throws SQLException {
    String sql = SELECT_BASE + " ORDER BY p.nombre";
    List<Producto> lista = new ArrayList<>();
    try (Connection cn = ConexionDB.getConexion();
        PreparedStatement ps = cn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {
      while (rs.next())
        lista.add(mapear(rs));
    }
    return lista;
  }

  // ============ Búsqueda por texto ============
  public List<Producto> buscar(String texto) throws SQLException {
    String sql = SELECT_BASE + """
            WHERE p.activo = 1 AND (
                LOWER(p.nombre)  LIKE ? OR
                LOWER(p.marca)   LIKE ? OR
                LOWER(p.codigo)  LIKE ? OR
                LOWER(p.modelo)  LIKE ?
            )
            ORDER BY p.nombre
        """;
    String like = "%" + (texto == null ? "" : texto.toLowerCase()) + "%";
    List<Producto> lista = new ArrayList<>();
    try (Connection cn = ConexionDB.getConexion();
        PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setString(1, like);
      ps.setString(2, like);
      ps.setString(3, like);
      ps.setString(4, like);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next())
          lista.add(mapear(rs));
      }
    }
    return lista;
  }

  // ============ Helpers ============
  private void llenar(PreparedStatement ps, Producto p) throws SQLException {
    ps.setString(1, p.getCodigo());
    ps.setString(2, p.getNombre());
    ps.setString(3, p.getMarca());
    // Si no hay categoría, guardamos NULL (más correcto que un 10 mágico)
    if (p.getCategoria() == null) {
      ps.setNull(4, Types.INTEGER);
    } else {
      ps.setInt(4, p.getCategoria().getId());
    }
    ps.setString(5, p.getTipo());
    ps.setString(6, p.getModelo());
    ps.setString(7, p.getMaterial());
    ps.setString(8, p.getMedidas());
    ps.setString(9, p.getColor());
    ps.setString(10, p.getDescripcion());
    ps.setString(11, p.getProveedor());
    ps.setDouble(12, p.getPrecioCosto());
    ps.setDouble(13, p.getPrecioVenta());
    ps.setInt(14, p.getStock());
    ps.setInt(15, p.getStockMinimo());
    ps.setString(16, p.getUbicacion());
    ps.setBoolean(17, p.isActivo());
    ps.setDate(18, Date.valueOf(p.getFechaAlta() == null ? LocalDate.now() : p.getFechaAlta()));
    ps.setString(19, p.getImagen());
    ps.setString(20, p.getObservaciones());
  }

  private Producto mapear(ResultSet rs) throws SQLException {
    Producto p = new Producto();
    p.setId(rs.getInt("id"));
    p.setCodigo(rs.getString("codigo"));
    p.setNombre(rs.getString("nombre"));
    p.setMarca(rs.getString("marca"));

    // Categoría con nombre real gracias al JOIN
    int catId = rs.getInt("categoria_id");
    if (!rs.wasNull() && catId > 0) {
      String catNombre = rs.getString("categoria_nombre");
      p.setCategoria(new Categoria(catId,
          catNombre == null ? "Sin categoría" : catNombre));
    } else {
      p.setCategoria(null);
    }

    p.setTipo(rs.getString("tipo"));
    p.setModelo(rs.getString("modelo"));
    p.setMaterial(rs.getString("material"));
    p.setMedidas(rs.getString("medidas"));
    p.setColor(rs.getString("color"));
    p.setDescripcion(rs.getString("descripcion"));
    p.setProveedor(rs.getString("proveedor"));
    p.setPrecioCosto(rs.getDouble("precio_costo"));
    p.setPrecioVenta(rs.getDouble("precio_venta"));
    p.setStock(rs.getInt("stock"));
    p.setStockMinimo(rs.getInt("stock_minimo"));
    p.setUbicacion(rs.getString("ubicacion"));
    p.setActivo(rs.getBoolean("activo"));

    Date f = rs.getDate("fecha_alta");
    p.setFechaAlta(f == null ? LocalDate.now() : f.toLocalDate());

    p.setImagen(rs.getString("imagen"));
    p.setObservaciones(rs.getString("observaciones"));
    return p;
  }
}
