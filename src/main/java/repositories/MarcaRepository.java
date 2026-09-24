package repositories;

import config.ConexionDB;
import java.sql.*;
import java.util.*;
import models.Marca;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MarcaRepository implements Repository<Marca> {

    private static final Logger log = LoggerFactory.getLogger(MarcaRepository.class);

    @Override
    public List<Marca> findAll() {
        return ejecutar("SELECT id_marca, nombre, activo FROM marcas WHERE activo = TRUE ORDER BY nombre");
    }

    public List<Marca> findInactivas() {
        return ejecutar("SELECT id_marca, nombre, activo FROM marcas WHERE activo = FALSE ORDER BY nombre");
    }

    @Override
    public Optional<Marca> findById(int id) {
        try (Connection cn = ConexionDB.getConexion();
             PreparedStatement ps = cn.prepareStatement(
                "SELECT id_marca, nombre, activo FROM marcas WHERE id_marca = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapear(rs));
            }
        } catch (SQLException e) { log.error("findById marca", e); }
        return Optional.empty();
    }

    /** Chequeo previo para dar mejor mensaje de error antes de violar la UNIQUE. */
    public boolean existeNombre(String nombre, Integer exceptoId) {
        String sql = "SELECT 1 FROM marcas WHERE LOWER(nombre) = LOWER(?)"
                   + (exceptoId != null ? " AND id_marca <> ?" : "");
        try (Connection cn = ConexionDB.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            if (exceptoId != null) ps.setInt(2, exceptoId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { log.error("existeNombre", e); return false; }
    }

    /** Cuántos productos tiene asociados (para mostrarlo en el panel de detalle). */
    public int contarProductos(int idMarca) {
        try (Connection cn = ConexionDB.getConexion();
             PreparedStatement ps = cn.prepareStatement(
                "SELECT COUNT(*) FROM productos WHERE id_marca = ? AND activo = TRUE")) {
            ps.setInt(1, idMarca);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) { log.error("contarProductos", e); }
        return 0;
    }

    @Override
    public Marca save(Marca m) {
        try (Connection cn = ConexionDB.getConexion();
             PreparedStatement ps = cn.prepareStatement(
                "INSERT INTO marcas (nombre) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, m.getNombre().trim());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) m.setIdMarca(rs.getInt(1));
            }
            return m;
        } catch (SQLException e) {
            log.error("save marca", e);
            throw new RuntimeException("No se pudo guardar la marca", e);
        }
    }

    @Override
    public boolean update(Marca m) {
        try (Connection cn = ConexionDB.getConexion();
             PreparedStatement ps = cn.prepareStatement(
                "UPDATE marcas SET nombre = ? WHERE id_marca = ?")) {
            ps.setString(1, m.getNombre().trim());
            ps.setInt(2, m.getIdMarca());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("update marca", e);
            return false;
        }
    }

    @Override
    public boolean delete(int id) {
        try (Connection cn = ConexionDB.getConexion();
             PreparedStatement ps = cn.prepareStatement(
                "UPDATE marcas SET activo = FALSE WHERE id_marca = ? AND activo = TRUE")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { log.error("delete marca", e); return false; }
    }

    public boolean reactivar(int id) {
        try (Connection cn = ConexionDB.getConexion();
             PreparedStatement ps = cn.prepareStatement(
                "UPDATE marcas SET activo = TRUE WHERE id_marca = ? AND activo = FALSE")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { log.error("reactivar marca", e); return false; }
    }

    // ------------------------------------------------------------
    private List<Marca> ejecutar(String sql) {
        List<Marca> lista = new ArrayList<>();
        try (Connection cn = ConexionDB.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) { log.error("ejecutar marcas", e); }
        return lista;
    }

    private Marca mapear(ResultSet rs) throws SQLException {
        Marca m = new Marca(rs.getInt("id_marca"), rs.getString("nombre"));
        m.setActivo(rs.getBoolean("activo"));
        return m;
    }
}
