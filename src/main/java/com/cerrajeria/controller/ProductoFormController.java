package com.cerrajeria.controller;

import com.cerrajeria.dao.CategoriaDAO;
import com.cerrajeria.dao.ProductoDAO;
import com.cerrajeria.model.Categoria;
import com.cerrajeria.model.Producto;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class ProductoFormController {

    @FXML private TextField txtNombre, txtMarca, txtTipo, txtModelo;
    @FXML private TextField txtMaterial, txtMedidas, txtColor, txtProveedor;
    @FXML private TextField txtPrecioCosto, txtPrecioVenta;
    @FXML private TextField txtStock, txtStockMinimo, txtUbicacion;
    @FXML private TextArea txtDescripcion, txtObservaciones;
    @FXML private ComboBox<Categoria> cbCategoria;
    @FXML private CheckBox chkActivo;
    @FXML private Button btnGuardar, btnCancelar;

    private Producto producto;
    private boolean guardado = false;

    @FXML
    public void initialize() {
        try {
            cbCategoria.setItems(
                FXCollections.observableArrayList(new CategoriaDAO().listar()));
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void setProducto(Producto p) {
        this.producto = p;
        if (p == null) {
            chkActivo.setSelected(true);
            txtStock.setText("0");
            txtStockMinimo.setText("0");
            return;
        }
        txtNombre.setText(p.getNombre());
        txtMarca.setText(p.getMarca());
        cbCategoria.setValue(p.getCategoria());
        txtTipo.setText(p.getTipo());
        txtModelo.setText(p.getModelo());
        txtMaterial.setText(p.getMaterial());
        txtMedidas.setText(p.getMedidas());
        txtColor.setText(p.getColor());
        txtDescripcion.setText(p.getDescripcion());
        txtProveedor.setText(p.getProveedor());
        txtPrecioCosto.setText(String.valueOf(p.getPrecioCosto()));
        txtPrecioVenta.setText(String.valueOf(p.getPrecioVenta()));
        txtStock.setText(String.valueOf(p.getStock()));
        txtStockMinimo.setText(String.valueOf(p.getStockMinimo()));
        txtUbicacion.setText(p.getUbicacion());
        chkActivo.setSelected(p.isActivo());
        txtObservaciones.setText(p.getObservaciones());
    }

    public boolean isGuardado() { return guardado; }

    @FXML
    private void guardar() {
        if (!validar()) return;
        try {
            boolean esNuevo = (producto == null);
            if (esNuevo) producto = new Producto();

            producto.setNombre(txtNombre.getText().trim());
            producto.setMarca(txtMarca.getText().trim());
            producto.setCategoria(cbCategoria.getValue());
            producto.setTipo(txtTipo.getText().trim());
            producto.setModelo(txtModelo.getText().trim());
            producto.setMaterial(txtMaterial.getText().trim());
            producto.setMedidas(txtMedidas.getText().trim());
            producto.setColor(txtColor.getText().trim());
            producto.setDescripcion(txtDescripcion.getText());
            producto.setProveedor(txtProveedor.getText().trim());
            producto.setPrecioCosto(parseDouble(txtPrecioCosto.getText()));
            producto.setPrecioVenta(parseDouble(txtPrecioVenta.getText()));
            producto.setStock(parseInt(txtStock.getText()));
            producto.setStockMinimo(parseInt(txtStockMinimo.getText()));
            producto.setUbicacion(txtUbicacion.getText().trim());
            producto.setActivo(chkActivo.isSelected());
            producto.setObservaciones(txtObservaciones.getText());

            ProductoDAO dao = new ProductoDAO();
            if (esNuevo) dao.insertar(producto);
            else         dao.actualizar(producto);

            guardado = true;
            cerrar();
        } catch (Exception e) {
            alerta(Alert.AlertType.ERROR, "Error al guardar", e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML private void cancelar() { cerrar(); }

    private void cerrar() {
        ((Stage) btnCancelar.getScene().getWindow()).close();
    }

    private boolean validar() {
        StringBuilder err = new StringBuilder();
        if (txtNombre.getText().isBlank())  err.append("• Nombre obligatorio\n");
        if (cbCategoria.getValue() == null) err.append("• Elegí una categoría\n");
        if (err.length() > 0) {
            alerta(Alert.AlertType.WARNING, "Faltan datos", err.toString());
            return false;
        }
        return true;
    }

    private double parseDouble(String s) {
        if (s == null || s.isBlank()) return 0;
        try { return Double.parseDouble(s.replace(",", ".")); }
        catch (NumberFormatException e) { return 0; }
    }

    private int parseInt(String s) {
        if (s == null || s.isBlank()) return 0;
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return 0; }
    }

    private void alerta(Alert.AlertType t, String titulo, String msj) {
        Alert a = new Alert(t);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.setContentText(msj);
        a.showAndWait();
    }
}
