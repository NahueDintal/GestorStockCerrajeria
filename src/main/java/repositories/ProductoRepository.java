package repositories;

import config.ConexionDB;
import config.Configuracion;
import java.sql.*;
import java.util.*;
import models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProductoRepository implements Repository<Producto> {

    private static final Logger log = LoggerFactory.getLogger(ProductoRepository.class);

    // ============================================================
    //  SELECT base — trae producto + categoría + marca + detalles
    //  con LEFT JOIN para que un solo query resuelva todo.
    // ============================================================
    private static final String SELECT_BASE = """
        SELECT
            p.id_producto, p.codigo, p.modelo, p.tipo, p.descripcion,
            p.precio_costo, p.precio_venta, p.stock, p.stock_minimo, p.activo,
            c.id_categoria AS cat_id, c.nombre AS cat_nombre, c.etiqueta AS cat_etiqueta,
            m.id_marca     AS mar_id, m.nombre AS mar_nombre,
            cd.cantidad_llaves,
            cr.cantidad_combinaciones,
            md.material
        FROM productos p
        JOIN categorias c            ON p.id_categoria = c.id_categoria
        LEFT JOIN marcas m           ON p.id_marca     = m.id_marca
        LEFT JOIN candado_detalle   cd ON p.id_producto = cd.id_producto
        LEFT JOIN cerradura_detalle cr ON p.id_producto = cr.id_producto
        LEFT JOIN manija_detalle    md ON p.id_producto = md.id_producto
        """;

    private static final String ORDEN = " ORDER BY m.nombre, p.modelo, p.codigo";

    // ============================================================
    //  LECTURA
    // ============================================================

    @Override
    public List<Producto> findAll() {
        return ejecutarLista(SELECT_BASE + " WHERE p.activo = TRUE" + ORDEN);
    }

    /** Para la vista de "Eliminados". */
    public List<Producto> findInactivos() {
        return ejecutarLista(SELECT_BASE + " WHERE p.activo = FALSE" + ORDEN);
    }

    @Override
    public Optional<Producto> findById(int id) {
        String sql = SELECT_BASE + " WHERE p.id_producto = ?";
        try (Connection cn = ConexionDB.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapear(rs));
            }
        } catch (SQLException e) {
            log.error("Error en findById({})", id, e);
        }
        return Optional.empty();
    }

    public List<Producto> findByCategoria(int idCategoria) {
        String sql = SELECT_BASE + " WHERE p.activo = TRUE AND p.id_categoria = ?" + ORDEN;
        return ejecutarLista(sql, ps -> ps.setInt(1, idCategoria));
    }

    /**
     * Búsqueda desde la DB (si en algún momento querés filtrar del lado del server
     * en vez de en memoria). Hoy no la usa el controller, pero la dejamos lista.
     */
    public List<Producto> buscar(String query, Integer idCategoria) {
        StringBuilder sb = new StringBuilder(SELECT_BASE)
            .append(" WHERE p.activo = TRUE");
        if (idCategoria != null) sb.append(" AND p.id_categoria = ?");
        sb.append("""
             AND (
                 LOWER(COALESCE(p.codigo,''))      LIKE ?
              OR LOWER(COALESCE(p.modelo,''))      LIKE ?
              OR LOWER(COALESCE(p.tipo,''))        LIKE ?
              OR LOWER(COALESCE(p.descripcion,'')) LIKE ?
              OR LOWER(COALESCE(m.nombre,''))      LIKE ?
             )
            """).append(ORDEN);

        String like = "%" + (query == null ? "" : query.toLowerCase().trim()) + "%";
        return ejecutarLista(sb.toString(), ps -> {
            int i = 1;
            if (idCategoria != null) ps.setInt(i++, idCategoria);
            for (int k = 0; k < 5; k++) ps.setString(i++, like);
        });
    }

    // ============================================================
    //  ESCRITURA
    // ============================================================

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
                bindProducto(ps, p, false);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) p.setIdProducto(rs.getInt(1));
                }
            }

            insertarDetalle(cn, p);
            cn.commit();
            log.info("Producto creado id={} ({})", p.getIdProducto(), p.getCategoria().getNombre());
            return p;

        } catch (SQLException e) {
            rollback(cn);
            log.error("Error guardando producto", e);
            throw new RuntimeException("No se pudo guardar el producto", e);
        } finally {
            cerrarTransaccion(cn);
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
                bindProducto(ps, p, true);
                if (ps.executeUpdate() == 0) {
                    cn.rollback();
                    log.warn("update sin filas afectadas id={}", p.getIdProducto());
                    return false;
                }
            }

            actualizarDetalle(cn, p);
            cn.commit();
            log.info("Producto actualizado id={}", p.getIdProducto());
            return true;

        } catch (SQLException e) {
            rollback(cn);
            log.error("Error actualizando producto id={}", p.getIdProducto(), e);
            return false;
        } finally {
            cerrarTransaccion(cn);
        }
    }

    @Override
    public boolean delete(int id) {
        // Soft delete: marcamos activo = FALSE
        try (Connection cn = ConexionDB.getConexion();
             PreparedStatement ps = cn.prepareStatement(
                 "UPDATE productos SET activo = FALSE WHERE id_producto = ? AND activo = TRUE")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error en delete({})", id, e);
            return false;
        }
    }

    /** Reactiva un producto que estaba "eliminado". */
    public boolean reactivar(int id) {
        try (Connection cn = ConexionDB.getConexion();
             PreparedStatement ps = cn.prepareStatement(
                 "UPDATE productos SET activo = TRUE WHERE id_producto = ? AND activo = FALSE")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error en reactivar({})", id, e);
            return false;
        }
    }

    /** Borrado físico real (con CASCADE en las tablas de detalle). */
    public boolean deleteHard(int id) {
        try (Connection cn = ConexionDB.getConexion();
             PreparedStatement ps = cn.prepareStatement(
                 "DELETE FROM productos WHERE id_producto = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error en deleteHard({})", id, e);
            return false;
        }
    }

    // ============================================================
    //  HELPERS INTERNOS
    // ============================================================

    @FunctionalInterface
    private interface Binder { void bind(PreparedStatement ps) throws SQLException; }

    private List<Producto> ejecutarLista(String sql) {
        return ejecutarLista(sql, null);
    }

    private List<Producto> ejecutarLista(String sql, Binder binder) {
        List<Producto> lista = new ArrayList<>();
        try (Connection cn = ConexionDB.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            if (binder != null) binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            log.error("Error ejecutando lista", e);
        }
        return lista;
    }

    /** Setea los parámetros comunes del INSERT/UPDATE (las 10 primeras columnas). */
    private void bindProducto(PreparedStatement ps, Producto p, boolean incluirId) throws SQLException {
        int i = 1;
        ps.setString(i++, p.getCodigo());
        ps.setInt(i++, p.getCategoria().getIdCategoria());
        if (p.getMarca() != null) ps.setInt(i++, p.getMarca().getIdMarca());
        else                      ps.setNull(i++, Types.INTEGER);
        ps.setString(i++, p.getModelo());
        ps.setString(i++, p.getTipo());
        ps.setString(i++, p.getDescripcion());
        ps.setDouble(i++, p.getPrecioCosto());
        ps.setDouble(i++, Configuracion.calcularPrecioVenta(p.getPrecioCosto()));
        ps.setInt(i++, p.getStock());
        ps.setInt(i++, p.getStockMinimo());
        if (incluirId) ps.setInt(i, p.getIdProducto());
    }

    private void insertarDetalle(Connection cn, Producto p) throws SQLException {
        if (p instanceof Candado c) {
            try (PreparedStatement ps = cn.prepareStatement(
                "INSERT INTO candado_detalle (id_producto, cantidad_llaves) VALUES (?, ?)")) {
                ps.setInt(1, c.getIdProducto());
                if (c.getCantidadLlaves() > 0) ps.setInt(2, c.getCantidadLlaves());
                else                            ps.setNull(2, Types.INTEGER);
                ps.executeUpdate();
            }
        } else if (p instanceof Cerradura c) {
            try (PreparedStatement ps = cn.prepareStatement(
                "INSERT INTO cerradura_detalle (id_producto, cantidad_combinaciones) VALUES (?, ?)")) {
                ps.setInt(1, c.getIdProducto());
                if (c.getCantidadCombinaciones() > 0) ps.setInt(2, c.getCantidadCombinaciones());
                else                                   ps.setNull(2, Types.INTEGER);
                ps.executeUpdate();
            }
        } else if (p instanceof Manija m) {
            try (PreparedStatement ps = cn.prepareStatement(
                "INSERT INTO manija_detalle (id_producto, material) VALUES (?, ?)")) {
                ps.setInt(1, m.getIdProducto());
                ps.setString(2, m.getMaterial());
                ps.executeUpdate();
            }
        }
        // Alarma, Camara, Llave → sin detalle
    }

    private void actualizarDetalle(Connection cn, Producto p) throws SQLException {
        // Como el detalle es 1:1 y tiene una sola fila, hacemos DELETE + INSERT.
        String tabla = null;
        if (p instanceof Candado)         tabla = "candado_detalle";
        else if (p instanceof Cerradura)  tabla = "cerradura_detalle";
        else if (p instanceof Manija)     tabla = "manija_detalle";

        if (tabla == null) return;

        try (PreparedStatement ps = cn.prepareStatement(
                "DELETE FROM " + tabla + " WHERE id_producto = ?")) {
            ps.setInt(1, p.getIdProducto());
            ps.executeUpdate();
        }
        insertarDetalle(cn, p);
    }

    /**
     * Construye el objeto correcto según la categoría y le carga TODOS los
     * campos, incluidos los detalles específicos que vienen del LEFT JOIN.
     */
    private Producto mapear(ResultSet rs) throws SQLException {
        String catNombre = rs.getString("cat_nombre");
        Categoria cat = new Categoria(
            rs.getInt("cat_id"), catNombre, rs.getString("cat_etiqueta"));

        Marca marca = null;
        int idMarca = rs.getInt("mar_id");
        if (!rs.wasNull()) marca = new Marca(idMarca, rs.getString("mar_nombre"));

        Producto p = switch (catNombre) {
            case "alarma"    -> new Alarma();
            case "camara"    -> new Camara();
            case "candado"   -> new Candado();
            case "cerradura" -> new Cerradura();
            case "llave"     -> new Llave();
            case "manija"    -> new Manija();
            default -> throw new SQLException("Categoría desconocida: " + catNombre);
        };

        // Campos comunes
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

        // Detalles específicos (vienen en el mismo ResultSet gracias a los LEFT JOIN)
        if (p instanceof Candado c) {
            int llaves = rs.getInt("cantidad_llaves");
            if (!rs.wasNull()) c.setCantidadLlaves(llaves);
        } else if (p instanceof Cerradura c) {
            int comb = rs.getInt("cantidad_combinaciones");
            if (!rs.wasNull()) c.setCantidadCombinaciones(comb);
        } else if (p instanceof Manija m) {
            m.setMaterial(rs.getString("material"));
        }

        return p;
    }

    private void rollback(Connection cn) {
        if (cn != null) try { cn.rollback(); } catch (SQLException ignored) {}
    }

    private void cerrarTransaccion(Connection cn) {
        if (cn != null) try {
            cn.setAutoCommit(true);
            cn.close();
        } catch (SQLException ignored) {}
    }
}
