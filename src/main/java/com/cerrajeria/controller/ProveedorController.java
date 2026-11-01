package com.cerrajeria.controller;

import com.cerrajeria.dao.ProveedorDAO;
import com.cerrajeria.model.Proveedor;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;

public class ProveedorController {

    @FXML private TableView<Proveedor> tabla;
    @FXML private TableColumn<Proveedor, String> colNombre;
    @FXML private TableColumn<Proveedor, Number> colIva;
    @FXML private TableColumn<Proveedor, Number> colMarkup;
    @FXML private Button btnEditar, btnEliminar;

    private final ProveedorDAO dao = new ProveedorDAO();
    private ObservableList<Proveedor> lista;

    @FXML
    public void initialize() {
        colNombre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNombre()));
        colIva.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getIvaPorcentaje()));
        colMarkup.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getMarkupPorcentaje()));

        tabla.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            btnEditar.setDisable(b == null);
            btnEliminar.setDisable(b == null);
        });
        btnEditar.setDisable(true);
        btnEliminar.setDisable(true);

        cargarDatos();
    }

    private void cargarDatos() {
        try {
            lista = FXCollections.observableArrayList(dao.listar());
            tabla.setItems(lista);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void nuevo() {
        mostrarDialogo(null);
    }

    @FXML
    private void editar() {
        Proveedor sel = tabla.getSelectionModel().getSelectedItem();
        if (sel != null) mostrarDialogo(sel);
    }

    @FXML
    private void eliminar() {
        Proveedor sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Eliminar Proveedor");
        a.setContentText("¿Seguro que querés eliminar a " + sel.getNombre() + "?");
        if (a.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                dao.eliminar(sel.getId());
                cargarDatos();
            } catch (Exception e) {
                e.printStackTrace();
                Alert err = new Alert(Alert.AlertType.ERROR, "No se pudo eliminar (quizás tiene productos asociados).");
                err.show();
            }
        }
    }

    private void mostrarDialogo(Proveedor p) {
        Dialog<Proveedor> dlg = new Dialog<>();
        dlg.setTitle(p == null ? "Nuevo Proveedor" : "Editar Proveedor");

        TextField txtNombre = new TextField(p == null ? "" : p.getNombre());
        TextField txtIva = new TextField(p == null ? "21" : String.valueOf(p.getIvaPorcentaje()));
        TextField txtMarkup = new TextField(p == null ? "50" : String.valueOf(p.getMarkupPorcentaje()));

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));
        grid.add(new Label("Nombre:"), 0, 0); grid.add(txtNombre, 1, 0);
        grid.add(new Label("IVA %:"), 0, 1); grid.add(txtIva, 1, 1);
        grid.add(new Label("Markup %:"), 0, 2); grid.add(txtMarkup, 1, 2);

        dlg.getDialogPane().setContent(grid);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dlg.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    Proveedor nuevo = p == null ? new Proveedor() : p;
                    nuevo.setNombre(txtNombre.getText().trim());
                    nuevo.setIvaPorcentaje(Double.parseDouble(txtIva.getText().replace(",", ".")));
                    nuevo.setMarkupPorcentaje(Double.parseDouble(txtMarkup.getText().replace(",", ".")));
                    dao.guardar(nuevo);
                    return nuevo;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return null;
        });

        if (dlg.showAndWait().isPresent()) {
            cargarDatos();
        }
    }
}
