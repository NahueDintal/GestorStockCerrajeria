package com.cerrajeria.dao;

import com.cerrajeria.model.Proveedor;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProveedorDAO {

    public List<Proveedor> listar() throws SQLException {
        String sql = "SELECT * FROM proveedor ORDER BY nombre";
        List<Proveedor> lista = new ArrayList<>();
        try (Connection cn = ConexionDB.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public Proveedor buscarPorNombre(String nombre) throws SQLException {
        String sql = "SELECT * FROM proveedor WHERE LOWER(nombre) = LOWER(?)";
        try (Connection cn = ConexionDB.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        }
        return null;
    }

    public int guardar(Proveedor p) throws SQLException {
        String sql = """
            INSERT INTO proveedor (nombre, iva_porcentaje, markup_porcentaje, observaciones)
            VALUES (?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                iva_porcentaje = VALUES(iva_porcentaje),
                markup_porcentaje = VALUES(markup_porcentaje),
                observaciones = VALUES(observaciones)
        """;
        try (Connection cn = ConexionDB.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getNombre());
            ps.setDouble(2, p.getIvaPorcentaje());
            ps.setDouble(3, p.getMarkupPorcentaje());
            ps.setString(4, p.getObservaciones());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) { p.setId(rs.getInt(1)); return p.getId(); }
            }
            return 0;
        }
    }

    private Proveedor mapear(ResultSet rs) throws SQLException {
        Proveedor p = new Proveedor();
        p.setId(rs.getInt("id"));
        p.setNombre(rs.getString("nombre"));
        p.setIvaPorcentaje(rs.getDouble("iva_porcentaje"));
        p.setMarkupPorcentaje(rs.getDouble("markup_porcentaje"));
        p.setObservaciones(rs.getString("observaciones"));
        return p;
    }
}
