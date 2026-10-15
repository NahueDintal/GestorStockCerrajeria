package com.cerrajeria.controller;

import com.cerrajeria.dao.CategoriaDAO;
import com.cerrajeria.dao.ProductoDAO;
import com.cerrajeria.dao.PresupuestoDAO;
import com.cerrajeria.model.*;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.IntegerStringConverter;

public class PresupuestoController {

  // --- Productos ---
  @FXML
  private TextField txtBuscarProducto;
  @FXML
  private ComboBox<Categoria> cbFiltroCategoria;
  @FXML
  private TableView<Producto> tablaProductos;
  @FXML
  private TableColumn<Producto, String> colProdCodigo, colProdNombre, colProdMarca;
  @FXML
  private TableColumn<Producto, Double> colProdPrecio;

  // --- Carrito ---
  @FXML
  private TableView<ItemPresupuesto> tablaCarrito;
  @FXML
  private TableColumn<ItemPresupuesto, String> colItemProducto;
  @FXML
  private TableColumn<ItemPresupuesto, Integer> colItemCantidad;
  @FXML
  private TableColumn<ItemPresupuesto, Double> colItemPrecio;
  @FXML
  private TableColumn<ItemPresupuesto, Double> colItemSubtotal;
  @FXML
  private TableColumn<ItemPresupuesto, Void> colItemQuitar;

  // --- Datos del cliente / totales ---
  @FXML
  private TextField txtClienteNombre, txtClienteTelefono;
  @FXML
  private TextField txtDescuentoGlobal;
  @FXML
  private TextArea txtObservaciones;
  @FXML
  private Label lblNumero, lblSubtotal, lblTotal;

  private final ProductoDAO prodDAO = new ProductoDAO();
  private final PresupuestoDAO presDAO = new PresupuestoDAO();

  private ObservableList<Producto> todosProductos;
  private FilteredList<Producto> productosFiltrados;
  private final ObservableList<ItemPresupuesto> carrito = FXCollections.observableArrayList();

  @FXML
  public void initialize() {
    configurarTablaProductos();
    configurarTablaCarrito();
    configurarFiltros();
    configurarDobleClicProductos();
    cargarProductos();
    cargarNumeroSiguiente();

    // Recalcular totales cuando cambia algo del carrito
    carrito.addListener((javafx.collections.ListChangeListener<ItemPresupuesto>) c -> recalcular());

    txtDescuentoGlobal.textProperty().addListener((o, a, b) -> recalcular());
  }

  // ============ Productos ============
  private void configurarTablaProductos() {
    colProdCodigo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCodigo()));
    colProdNombre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNombre()));
    colProdMarca.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMarca()));
    colProdPrecio.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getPrecioVenta()));
    colProdPrecio.setCellFactory(col -> new TableCell<>() {
      @Override
      protected void updateItem(Double v, boolean empty) {
        super.updateItem(v, empty);
        setText(empty || v == null ? null : String.format("$%.2f", v));
      }
    });
  }

  private void configurarFiltros() {
    try {
      cbFiltroCategoria.setItems(FXCollections.observableArrayList(new CategoriaDAO().listar()));
    } catch (Exception e) {
      e.printStackTrace();
    }

    txtBuscarProducto.textProperty().addListener((o, a, b) -> aplicarFiltros());
    cbFiltroCategoria.valueProperty().addListener((o, a, b) -> aplicarFiltros());
  }

  private void cargarProductos() {
    try {
      todosProductos = FXCollections.observableArrayList(prodDAO.listarActivos());
      productosFiltrados = new FilteredList<>(todosProductos, p -> true);
      SortedList<Producto> ordenados = new SortedList<>(productosFiltrados);
      ordenados.comparatorProperty().bind(tablaProductos.comparatorProperty());
      tablaProductos.setItems(ordenados);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void aplicarFiltros() {
    String q = txtBuscarProducto.getText() == null ? "" : txtBuscarProducto.getText().toLowerCase();
    Categoria cat = cbFiltroCategoria.getValue();
    productosFiltrados.setPredicate(p -> {
      boolean okCat = (cat == null
          || (p.getCategoria() != null && p.getCategoria().getId() == cat.getId()));
      boolean okTxt = q.isEmpty()
          || (p.getNombre() != null && p.getNombre().toLowerCase().contains(q))
          || (p.getMarca() != null && p.getMarca().toLowerCase().contains(q))
          || (p.getCodigo() != null && p.getCodigo().toLowerCase().contains(q));
      return okCat && okTxt;
    });
  }

  private void configurarDobleClicProductos() {
    tablaProductos.setRowFactory(tv -> {
      TableRow<Producto> row = new TableRow<>();
      row.setOnMouseClicked(ev -> {
        if (ev.getClickCount() == 2 && !row.isEmpty()) {
          agregarAlCarrito(row.getItem());
        }
      });
      return row;
    });
  }

  @FXML
  private void agregarSeleccionado() {
    Producto sel = tablaProductos.getSelectionModel().getSelectedItem();
    if (sel != null)
      agregarAlCarrito(sel);
  }

  private void agregarAlCarrito(Producto p) {
    carrito.stream()
        .filter(i -> i.getProducto().getId() == p.getId())
        .findFirst()
        .ifPresentOrElse(
            i -> i.setCantidad(i.getCantidad() + 1),
            () -> carrito.add(new ItemPresupuesto(p, 1, p.getPrecioVenta(), 0)));
    tablaCarrito.refresh();
    recalcular();
  }

  // ============ Carrito ============
  private void configurarTablaCarrito() {
    colItemProducto.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getProducto().getNombre()));

    colItemCantidad.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getCantidad()));
    colItemCantidad.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
    colItemCantidad.setOnEditCommit(ev -> {
      ItemPresupuesto it = ev.getRowValue();
      int nueva = Math.max(1, ev.getNewValue());
      it.setCantidad(nueva);
      tablaCarrito.refresh();
      recalcular();
    });

    colItemPrecio.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getPrecioUnitario()));
    colItemPrecio.setCellFactory(col -> new TableCell<>() {
      @Override
      protected void updateItem(Double v, boolean empty) {
        super.updateItem(v, empty);
        setText(empty || v == null ? null : String.format("$%.2f", v));
      }
    });

    colItemSubtotal.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getSubtotal()));
    colItemSubtotal.setCellFactory(col -> new TableCell<>() {
      @Override
      protected void updateItem(Double v, boolean empty) {
        super.updateItem(v, empty);
        setText(empty || v == null ? null : String.format("$%.2f", v));
      }
    });

    // Botón quitar por fila
    colItemQuitar.setCellFactory(col -> new TableCell<>() {
      private final Button btn = new Button("✖");
      {
        btn.setOnAction(e -> {
          ItemPresupuesto it = getTableView().getItems().get(getIndex());
          carrito.remove(it);
        });
        btn.setStyle("-fx-base: #E57373; -fx-padding: 2 8 2 8;");
      }

      @Override
      protected void updateItem(Void v, boolean empty) {
        super.updateItem(v, empty);
        setGraphic(empty ? null : btn);
      }
    });

    tablaCarrito.setItems(carrito);
    tablaCarrito.setEditable(true);
  }

  private void recalcular() {
    double subtotal = carrito.stream().mapToDouble(ItemPresupuesto::getSubtotal).sum();
    double desc = parseDouble(txtDescuentoGlobal.getText());
    double total = subtotal * (1 - desc / 100.0);
    lblSubtotal.setText(String.format("$%.2f", subtotal));
    lblTotal.setText(String.format("$%.2f", total));
  }

  private void cargarNumeroSiguiente() {
    try {
      lblNumero.setText("N° " + presDAO.numeroSiguiente());
    } catch (Exception e) {
      lblNumero.setText("N° ?");
    }
  }

  // ============ Guardar ============
  @FXML
  private void guardar() {
    if (carrito.isEmpty()) {
      alerta(Alert.AlertType.WARNING, "Carrito vacío",
          "Agregá al menos un producto antes de guardar.");
      return;
    }
    try {
      Presupuesto p = new Presupuesto();
      p.setNumero(presDAO.numeroSiguiente());
      p.setClienteNombre(txtClienteNombre.getText());
      p.setClienteTelefono(txtClienteTelefono.getText());
      p.setDescuentoGlobal(parseDouble(txtDescuentoGlobal.getText()));
      p.setObservaciones(txtObservaciones.getText());
      p.setItems(new java.util.ArrayList<>(carrito));

      presDAO.guardar(p);

      alerta(Alert.AlertType.INFORMATION, "Presupuesto guardado",
          "Se guardó el presupuesto N° " + p.getNumero() +
              "\nTotal: " + String.format("$%.2f", p.getTotal()));

      limpiar();
    } catch (Exception e) {
      e.printStackTrace();
      alerta(Alert.AlertType.ERROR, "Error al guardar", e.getMessage());
    }
  }

  @FXML
  private void limpiar() {
    carrito.clear();
    txtClienteNombre.clear();
    txtClienteTelefono.clear();
    txtDescuentoGlobal.clear();
    txtObservaciones.clear();
    cargarNumeroSiguiente();
    recalcular();
  }

  private double parseDouble(String s) {
    if (s == null || s.isBlank())
      return 0;
    try {
      return Double.parseDouble(s.replace(",", "."));
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  private void alerta(Alert.AlertType t, String titulo, String msj) {
    Alert a = new Alert(t);
    a.setTitle(titulo);
    a.setHeaderText(null);
    a.setContentText(msj);
    a.showAndWait();
  }
}
