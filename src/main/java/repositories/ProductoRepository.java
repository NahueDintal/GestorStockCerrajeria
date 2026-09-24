package repositories;

import config.ConexionDB;
import config.Configuracion;
import models.*;
import java.sql.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProductoRepository implements Repository<Producto> {
  private static final Logger log = LoggerFactory.getLogger(ProductoRepository.class);

  private static final String SELECT_BASE = """
      SELECT p.*,
             c.id_categoria AS cat_id, c.nombre AS cat_nombre, c.etiqueta AS cat_etiqueta,
             m.id_marca AS mar_id, m.nombre AS mar_nombre
      FROM productos p
      JOIN categorias c ON p.id_categoria = c.id_categoria
      LEFT JOIN marcas m ON p.id_marca = m.id_marca
      WHERE p.activo = TRUE
      """;

  @Override
  public List<Producto> findAll() {
    List<Producto> lista = new ArrayList<>();
    try (Connection cn = ConexionDB.getConexion();
        PreparedStatement ps = cn.prepareStatement(SELECT_BASE + " ORDER BY c.nombre, m.nombre, p.modelo");
        ResultSet rs = ps.executeQuery()) {
      while (rs.next())
        lista.add(mapear(rs));
    } catch (SQLException e) {
      log.error("Error en findAll", e);
    }
    return lista;
  }

  @Override
  public Optional<Producto> findById(int id) {
    try (Connection cn = ConexionDB.getConexion();
        PreparedStatement ps = cn.prepareStatement(SELECT_BASE + " AND p.id_producto = ?")) {
      ps.setInt(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next())
          return Optional.of(mapear(rs));
      }
    } catch (SQLException e) {
      log.error("Error en findById", e);
    }
    return Optional.empty();
  }

  public List<Producto> findByCategoria(int idCategoria) {
    List<Producto> lista = new ArrayList<>();
    try (Connection cn = ConexionDB.getConexion();
        PreparedStatement ps = cn.prepareStatement(SELECT_BASE + " AND p.id_categoria = ?")) {
      ps.setInt(1, idCategoria);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next())
          lista.add(mapear(rs));
      }
    } catch (SQLException e) {
      log.error("Error en findByCategoria", e);
    }
    return lista;
  }

  @Override
  public Producto save(Producto p) {
    Connection cn = null;
    try {
      cn = ConexionDB.getConexion();
      cn.setAutoCommit(false);

      String sql = """
          INSERT INTO productos
          (codigo, id_categoria, id_marca, modelo, tipo, descripcion,
           precio_costo, precio_venta, stock, stock_minimo)
          VALUES (?,?,?,?,?,?,?,?,?,?)
          """;
      try (PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
        ps.setString(1, p.getCodigo());
        ps.setInt(2, p.getCategoria().getIdCategoria());
        if (p.getMarca() != null)
          ps.setInt(3, p.getMarca().getIdMarca());
        else
          ps.setNull(3, Types.INTEGER);
        ps.setString(4, p.getModelo());
        ps.setString(5, p.getTipo());
        ps.setString(6, p.getDescripcion());
        ps.setDouble(7, p.getPrecioCosto());
        ps.setDouble(8, Configuracion.calcularPrecioVenta(p.getPrecioCosto()));
        ps.setInt(9, p.getStock());
        ps.setInt(10, p.getStockMinimo());
        ps.executeUpdate();

        try (ResultSet rs = ps.getGeneratedKeys()) {
          if (rs.next())
            p.setIdProducto(rs.getInt(1));
        }
      }

      insertarDetalle(cn, p); // toca la tabla que corresponda
      cn.commit();
      return p;
    } catch (SQLException e) {
      log.error("Error en save", e);
      if (cn != null)
        try {
          cn.rollback();
        } catch (SQLException ex) {
          /* ignore */ }
      throw new RuntimeException("No se pudo guardar el producto", e);
    } finally {
      if (cn != null)
        try {
          cn.setAutoCommit(true);
          cn.close();
        } catch (SQLException ex) {
          /* ignore */ }
    }
  }

  @Override
  public boolean update(Producto p) {
    Connection cn = null;
    try {
      cn = ConexionDB.getConexion();
      cn.setAutoCommit(false);

      String sql = """
          UPDATE productos SET
            codigo=?, id_categoria=?, id_marca=?, modelo=?, tipo=?, descripcion=?,
            precio_costo=?, precio_venta=?, stock=?, stock_minimo=?
          WHERE id_producto=?
          """;
      try (PreparedStatement ps = cn.prepareStatement(sql)) {
        ps.setString(1, p.getCodigo());
        ps.setInt(2, p.getCategoria().getIdCategoria());
        if (p.getMarca() != null)
          ps.setInt(3, p.getMarca().getIdMarca());
        else
          ps.setNull(3, Types.INTEGER);
        ps.setString(4, p.getModelo());
        ps.setString(5, p.getTipo());
        ps.setString(6, p.getDescripcion());
        ps.setDouble(7, p.getPrecioCosto());
        ps.setDouble(8, Configuracion.calcularPrecioVenta(p.getPrecioCosto()));
        ps.setInt(9, p.getStock());
        ps.setInt(10, p.getStockMinimo());
        ps.setInt(11, p.getIdProducto());
        if (ps.executeUpdate() == 0) {
          cn.rollback();
          return false;
        }
      }

      actualizarDetalle(cn, p);
      cn.commit();
      return true;
    } catch (SQLException e) {
      log.error("Error en update", e);
      if (cn != null)
        try {
          cn.rollback();
        } catch (SQLException ex) {
          /* ignore */ }
      return false;
    } finally {
      if (cn != null)
        try {
          cn.setAutoCommit(true);
          cn.close();
        } catch (SQLException ex) {
          /* ignore */ }
    }
  }

  @Override
  public boolean delete(int id) {
    // soft delete: no borramos, desactivamos
    try (Connection cn = ConexionDB.getConexion();
        PreparedStatement ps = cn.prepareStatement(
            "UPDATE productos SET activo=FALSE WHERE id_producto=?")) {
      ps.setInt(1, id);
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      log.error("Error en delete", e);
      return false;
    }
  }

  // ---------- helpers ----------

  private void insertarDetalle(Connection cn, Producto p) throws SQLException {
    if (p instanceof Candado c) {
      try (PreparedStatement ps = cn.prepareStatement(
          "INSERT INTO candado_detalle (id_producto, cantidad_llaves) VALUES (?,?)")) {
        ps.setInt(1, c.getIdProducto());
        ps.setInt(2, c.getCantidadLlaves());
        ps.executeUpdate();
      }
    } else if (p instanceof Cerradura c) {
      try (PreparedStatement ps = cn.prepareStatement(
          "INSERT INTO cerradura_detalle (id_producto, cantidad_combinaciones) VALUES (?,?)")) {
        ps.setInt(1, c.getIdProducto());
        ps.setInt(2, c.getCantidadCombinaciones());
        ps.executeUpdate();
      }
    } else if (p instanceof Manija m) {
      try (PreparedStatement ps = cn.prepareStatement(
          "INSERT INTO manija_detalle (id_producto, material) VALUES (?,?)")) {
        ps.setInt(1, m.getIdProducto());
        ps.setString(2, m.getMaterial());
        ps.executeUpdate();
      }
    }
    // Alarma, Camara, Llave no tienen detalle
  }

  private void actualizarDetalle(Connection cn, Producto p) throws SQLException {
    // Por simplicidad: DELETE + INSERT (son 1 fila)
    String tabla = null;
    if (p instanceof Candado)
      tabla = "candado_detalle";
    else if (p instanceof Cerradura)
      tabla = "cerradura_detalle";
    else if (p instanceof Manija)
      tabla = "manija_detalle";
    if (tabla == null)
      return;

    try (PreparedStatement ps = cn.prepareStatement("DELETE FROM " + tabla + " WHERE id_producto=?")) {
      ps.setInt(1, p.getIdProducto());
      ps.executeUpdate();
    }
    insertarDetalle(cn, p);
  }

  private Producto mapear(ResultSet rs) throws SQLException {
    String catNombre = rs.getString("cat_nombre");
    Categoria cat = new Categoria(
        rs.getInt("cat_id"), catNombre, rs.getString("cat_etiqueta"));

    Marca marca = null;
    int idMarca = rs.getInt("mar_id");
    if (!rs.wasNull())
      marca = new Marca(idMarca, rs.getString("mar_nombre"));

    Producto p = switch (catNombre) {
      case "alarma" -> new Alarma();
      case "camara" -> new Camara();
      case "candado" -> new Candado();
      case "cerradura" -> new Cerradura();
      case "llave" -> new Llave();
      case "manija" -> new Manija();
      default -> throw new SQLException("Categoría desconocida: " + catNombre);
    };

    p.setIdProducto(rs.getInt("id_producto"));
    p.setCodigo(rs.getString("codigo"));
    p.setCategoria(cat);
    p.setMarca(marca);
    p.setModelo(rs.getString("modelo"));
    p.setTipo(rs.getString("tipo"));
    p.setDescripcion(rs.getString("descripcion"));
    p.setPrecioCosto(rs.getDouble("precio_costo"));
    p.setPrecioVenta(rs.getDouble("precio_venta"));
    p.setStock(rs.getInt("stock"));
    p.setStockMinimo(rs.getInt("stock_minimo"));
    p.setActivo(rs.getBoolean("activo"));

    cargarDetalle(p); // trae cantidad_llaves / material / etc.
    return p;
  }

  private void cargarDetalle(Producto p) throws SQLException {
    if (p instanceof Candado c) {
      try (Connection cn = ConexionDB.getConexion();
          PreparedStatement ps = cn.prepareStatement(
              "SELECT cantidad_llaves FROM candado_detalle WHERE id_producto=?")) {
        ps.setInt(1, p.getIdProducto());
        try (ResultSet rs = ps.executeQuery()) {
          if (rs.next())
            c.setCantidadLlaves(rs.getInt(1));
        }
      }
    } else if (p instanceof Cerradura c) {
      // análogo con cantidad_combinaciones
    } else if (p instanceof Manija m) {
      // análogo con material
    }
  }
}
