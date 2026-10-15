package com.cerrajeria.controller;

import com.cerrajeria.dao.CategoriaDAO;
import com.cerrajeria.dao.ProductoDAO;
import com.cerrajeria.model.Categoria;
import com.cerrajeria.model.Producto;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ProductoController {

    @FXML private TextField txtBuscar;
    @FXML private ComboBox<Categoria> cbFiltroCategoria;
    @FXML private Button btnNuevo, btnEditar, btnEliminar, btnRefrescar;
    @FXML private TableView<Producto> tabla;
    @FXML private TableColumn<Producto, String>  colNombre, colMarca, colCategoria;
    @FXML private TableColumn<Producto, Double>  colPrecio;
    @FXML private TableColumn<Producto, Integer> colStock;

    private final ProductoDAO dao = new ProductoDAO();
    private ObservableList<Producto> todos;
    private FilteredList<Producto> filtrados;

    @FXML
    public void initialize() {
        configurarColumnas();
        configurarFiltros();
        cargarDatos();
        configurarDobleClic();
        actualizarBotones();
    }

    private void configurarColumnas() {
        colNombre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNombre()));
        colMarca.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMarca()));
        colCategoria.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getCategoria() == null ? "" : c.getValue().getCategoria().getNombre()));
        colPrecio.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getPrecioVenta()));
        colStock.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getStock()));

        colPrecio.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("$%.2f", v));
            }
        });

        colStock.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(String.valueOf(v));
                Producto p = getTableView().getItems().get(getIndex());
                setStyle(p.necesitaReposicion()
                    ? "-fx-text-fill: red; -fx-font-weight: bold;" : "");
            }
        });
    }

    private void configurarFiltros() {
        try {
            var lista = FXCollections.observableArrayList(new CategoriaDAO().listar());
            lista.add(0, new Categoria(-1, "— Todas —"));
            cbFiltroCategoria.setItems(lista);
            cbFiltroCategoria.getSelectionModel().selectFirst();
        } catch (Exception e) { e.printStackTrace(); }

        txtBuscar.textProperty().addListener((o, a, b) -> aplicarFiltros());
        cbFiltroCategoria.valueProperty().addListener((o, a, b) -> aplicarFiltros());
    }

    private void cargarDatos() {
        try {
            todos = FXCollections.observableArrayList(dao.listarActivos());
            filtrados = new FilteredList<>(todos, p -> true);
            SortedList<Producto> ordenados = new SortedList<>(filtrados);
            ordenados.comparatorProperty().bind(tabla.comparatorProperty());
            tabla.setItems(ordenados);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void aplicarFiltros() {
        String q = txtBuscar.getText() == null ? "" : txtBuscar.getText().toLowerCase();
        Categoria cat = cbFiltroCategoria.getValue();
        boolean filtrarPorCat = (cat != null && cat.getId() != -1);
        filtrados.setPredicate(p -> {
            boolean okCat = !filtrarPorCat
                || (p.getCategoria() != null && p.getCategoria().getId() == cat.getId());
            boolean okTxt = q.isEmpty()
                || (p.getNombre() != null && p.getNombre().toLowerCase().contains(q))
                || (p.getMarca()  != null && p.getMarca().toLowerCase().contains(q))
                || (p.getModelo() != null && p.getModelo().toLowerCase().contains(q));
            return okCat && okTxt;
        });
    }

    private void configurarDobleClic() {
        tabla.setRowFactory(tv -> {
            TableRow<Producto> row = new TableRow<>();
            row.setOnMouseClicked(ev -> {
                if (ev.getClickCount() == 2 && !row.isEmpty()) editar();
            });
            return row;
        });
    }

    private void actualizarBotones() {
        tabla.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            btnEditar.setDisable(b == null);
            btnEliminar.setDisable(b == null);
        });
        btnEditar.setDisable(true);
        btnEliminar.setDisable(true);
    }

    @FXML private void nuevo() { abrirFormulario(null); }

    @FXML
    private void editar() {
        Producto sel = tabla.getSelectionModel().getSelectedItem();
        if (sel != null) abrirFormulario(sel);
    }

    private void abrirFormulario(Producto p) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/views/ProductoFormView.fxml"));
            Parent root = loader.load();

            ProductoFormController ctrl = loader.getController();
            ctrl.setProducto(p);

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle(p == null ? "Nuevo producto" : "Editar producto");
            dialog.setScene(new Scene(root, 720, 620));
            dialog.setResizable(false);
            dialog.showAndWait();

            if (ctrl.isGuardado()) {
                cargarDatos();
                aplicarFiltros();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void eliminar() {
        Producto sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Eliminar producto");
        a.setHeaderText(null);
        a.setContentText("¿Desactivar \"" + sel.getNombre() + "\"?");
        if (a.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            dao.desactivar(sel.getId());
            cargarDatos();
            aplicarFiltros();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void refrescar() {
        cargarDatos();
        aplicarFiltros();
    }
}
