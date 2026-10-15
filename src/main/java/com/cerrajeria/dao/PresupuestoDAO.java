package com.cerrajeria.dao;

import com.cerrajeria.model.ItemPresupuesto;
import com.cerrajeria.model.Presupuesto;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PresupuestoDAO {

    public int numeroSiguiente() throws SQLException {
        String sql = "SELECT COALESCE(MAX(numero), 0) + 1 AS n FROM presupuesto";
        try (Connection cn = ConexionDB.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt("n");
        }
        return 1;
    }

    public int guardar(Presupuesto p) throws SQLException {
        Connection cn = ConexionDB.getConexion();
        try {
            cn.setAutoCommit(false);

            // 1) Insertar cabecera
            String sqlP = """
                INSERT INTO presupuesto
                (numero, fecha, validez_hasta, cliente_nombre, cliente_telefono,
                 descuento_global, observaciones, total)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
            int presupuestoId;
            try (PreparedStatement ps = cn.prepareStatement(sqlP, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, p.getNumero());
                ps.setDate(2, Date.valueOf(p.getFecha()));
                ps.setDate(3, p.getValidezHasta() == null ? null
                        : Date.valueOf(p.getValidezHasta()));
                ps.setString(4, p.getClienteNombre());
                ps.setString(5, p.getClienteTelefono());
                ps.setDouble(6, p.getDescuentoGlobal());
                ps.setString(7, p.getObservaciones());
                ps.setDouble(8, p.getTotal());
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    rs.next();
                    presupuestoId = rs.getInt(1);
                }
            }

            // 2) Insertar items
            String sqlI = """
                INSERT INTO presupuesto_item
                (presupuesto_id, producto_id, cantidad, precio_unitario, descuento_item)
                VALUES (?, ?, ?, ?, ?)
            """;
            try (PreparedStatement ps = cn.prepareStatement(sqlI)) {
                for (ItemPresupuesto it : p.getItems()) {
                    ps.setInt(1, presupuestoId);
                    ps.setInt(2, it.getProducto().getId());
                    ps.setInt(3, it.getCantidad());
                    ps.setDouble(4, it.getPrecioUnitario());
                    ps.setDouble(5, it.getDescuentoItem());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            cn.commit();
            p.setId(presupuestoId);
            return presupuestoId;

        } catch (SQLException e) {
            cn.rollback();
            throw e;
        } finally {
            cn.setAutoCommit(true);
        }
    }

    // Para listar historial (más adelante)
    public List<Presupuesto> listar() throws SQLException {
        String sql = "SELECT * FROM presupuesto ORDER BY fecha DESC, numero DESC";
        List<Presupuesto> lista = new ArrayList<>();
        try (Connection cn = ConexionDB.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    private Presupuesto mapear(ResultSet rs) throws SQLException {
        Presupuesto p = new Presupuesto();
        p.setId(rs.getInt("id"));
        p.setNumero(rs.getInt("numero"));
        p.setFecha(rs.getDate("fecha").toLocalDate());
        Date v = rs.getDate("validez_hasta");
        p.setValidezHasta(v == null ? null : v.toLocalDate());
        p.setClienteNombre(rs.getString("cliente_nombre"));
        p.setClienteTelefono(rs.getString("cliente_telefono"));
        p.setDescuentoGlobal(rs.getDouble("descuento_global"));
        p.setObservaciones(rs.getString("observaciones"));
        return p;
    }
}
