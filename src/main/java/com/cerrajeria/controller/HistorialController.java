package com.cerrajeria.controller;

import com.cerrajeria.dao.PresupuestoDAO;
import com.cerrajeria.model.ItemPresupuesto;
import com.cerrajeria.model.Presupuesto;
import com.cerrajeria.util.PresupuestoPDF;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.format.DateTimeFormatter;
import com.cerrajeria.util.AbrirArchivo;

public class HistorialController {

  @FXML
  private TextField txtBuscar;
  @FXML
  private Button btnVer, btnPDF, btnEliminar, btnRefrescar;
  @FXML
  private TableView<Presupuesto> tabla;
  @FXML
  private TableColumn<Presupuesto, Integer> colNumero;
  @FXML
  private TableColumn<Presupuesto, String> colFecha, colCliente, colTelefono;
  @FXML
  private TableColumn<Presupuesto, Double> colTotal;

  private final PresupuestoDAO dao = new PresupuestoDAO();
  private ObservableList<Presupuesto> datos;

  private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  @FXML
  public void initialize() {
    colNumero.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getNumero()));
    colFecha.setCellValueFactory(c -> new SimpleStringProperty(
        c.getValue().getFecha().format(FMT)));
    colCliente.setCellValueFactory(c -> new SimpleStringProperty(
        c.getValue().getClienteNombre()));
    colTelefono.setCellValueFactory(c -> new SimpleStringProperty(
        c.getValue().getClienteTelefono()));
    colTotal.setCellValueFactory(c -> new SimpleObjectProperty<>(
        c.getValue().getTotal()));
    colTotal.setCellFactory(col -> new TableCell<>() {
      @Override
      protected void updateItem(Double v, boolean empty) {
        super.updateItem(v, empty);
        setText(empty || v == null ? null : String.format("$%.2f", v));
      }
    });

    tabla.setRowFactory(tv -> {
      TableRow<Presupuesto> row = new TableRow<>();
      row.setOnMouseClicked(ev -> {
        if (ev.getClickCount() == 2 && !row.isEmpty())
          ver();
      });
      return row;
    });

    txtBuscar.textProperty().addListener((o, a, b) -> filtrar());
    cargar();
  }

  private void cargar() {
    try {
      datos = FXCollections.observableArrayList(dao.listar());
      tabla.setItems(datos);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void filtrar() {
    try {
      String q = txtBuscar.getText();
      datos = FXCollections.observableArrayList(
          (q == null || q.isBlank()) ? dao.listar() : dao.buscar(q));
      tabla.setItems(datos);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @FXML
  private void ver() {
    Presupuesto sel = tabla.getSelectionModel().getSelectedItem();
    if (sel == null)
      return;
    try {
      Presupuesto full = dao.buscarPorId(sel.getId());
      if (full == null)
        return;

      StringBuilder sb = new StringBuilder();
      sb.append("Presupuesto N° ").append(full.getNumero()).append("\n");
      sb.append("Fecha: ").append(full.getFecha().format(FMT)).append("\n");
      sb.append("Cliente: ").append(full.getClienteNombre()).append("\n\n");
      for (ItemPresupuesto it : full.getItems()) {
        sb.append(String.format("%d x %s — $%.2f  =  $%.2f%n",
            it.getCantidad(),
            it.getProducto().getNombre(),
            it.getPrecioUnitario(),
            it.getSubtotal()));
      }
      sb.append("\nTOTAL: $").append(String.format("%.2f", full.getTotal()));

      Alert a = new Alert(Alert.AlertType.INFORMATION);
      a.setTitle("Presupuesto N° " + full.getNumero());
      a.setHeaderText(null);
      TextArea ta = new TextArea(sb.toString());
      ta.setEditable(false);
      ta.setPrefRowCount(12);
      ta.setPrefColumnCount(50);
      a.getDialogPane().setContent(ta);
      a.showAndWait();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @FXML
  private void exportarPDF() {
    Presupuesto sel = tabla.getSelectionModel().getSelectedItem();
    if (sel == null)
      return;
    try {
      Presupuesto full = dao.buscarPorId(sel.getId());

      FileChooser fc = new FileChooser();
      fc.setTitle("Guardar presupuesto como PDF");
      fc.getExtensionFilters().add(
          new FileChooser.ExtensionFilter("PDF", "*.pdf"));
      fc.setInitialFileName("Presupuesto_" + full.getNumero() + ".pdf");
      File destino = fc.showSaveDialog(tabla.getScene().getWindow());
      if (destino == null)
        return;

      PresupuestoPDF.generar(full, destino);
      AbrirArchivo.abrir(destino); // ← reemplaza Desktop.open
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @FXML
  private void eliminar() {
    Presupuesto sel = tabla.getSelectionModel().getSelectedItem();
    if (sel == null)
      return;
    Alert a = new Alert(Alert.AlertType.CONFIRMATION);
    a.setTitle("Eliminar presupuesto");
    a.setHeaderText(null);
    a.setContentText("¿Eliminar el presupuesto N° " + sel.getNumero() +
        "?\nEsta acción no se puede deshacer.");
    if (a.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK)
      return;
    try {
      try (var cn = com.cerrajeria.dao.ConexionDB.getConexion();
          var ps = cn.prepareStatement("DELETE FROM presupuesto WHERE id = ?")) {
        ps.setInt(1, sel.getId());
        ps.executeUpdate();
      }
      cargar();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @FXML
  public void refrescar() {
    cargar();
  }
}
