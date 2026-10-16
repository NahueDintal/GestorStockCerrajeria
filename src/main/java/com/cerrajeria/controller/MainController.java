package com.cerrajeria.controller;

import com.cerrajeria.util.ImportadorProductos;
import com.cerrajeria.util.ImportadorX28;
import com.cerrajeria.util.Tema;
import com.cerrajeria.util.ToggleSwitch;
import com.cerrajeria.dao.ProveedorDAO;
import com.cerrajeria.model.Proveedor;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.MenuBar;
import javafx.scene.control.TabPane;
import javafx.stage.FileChooser;
import javafx.scene.control.ButtonType;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;

import java.util.List;
import java.io.File;

public class MainController {

  @FXML
  private MenuBar menuBar;
  @FXML
  private ToggleSwitch themeSwitch;
  @FXML
  private TabPane tabPane;

  // Controllers inyectados por JavaFX a partir de los fx:include
  @FXML
  private HistorialController historialViewController;
  @FXML
  private ProductoController productoViewController; // ← NUEVO

  @FXML
  public void initialize() {
    boolean esOscuro = (Tema.getActual() == Tema.Modo.OSCURO);
    themeSwitch.setSelectedSilent(esOscuro);
    themeSwitch.setOnToggle(() -> Tema.alternar(menuBar.getScene()));

    tabPane.getSelectionModel().selectedItemProperty().addListener(
        (obs, vieja, nueva) -> {
          if (nueva != null && "Historial".equals(nueva.getText())
              && historialViewController != null) {
            historialViewController.refrescar();
          }
        });
  }

  @FXML
  private void temaClaro() {
    Tema.aplicar(menuBar.getScene(), Tema.Modo.CLARO);
    themeSwitch.setSelectedSilent(false);
  }

  @FXML
  private void temaOscuro() {
    Tema.aplicar(menuBar.getScene(), Tema.Modo.OSCURO);
    themeSwitch.setSelectedSilent(true);
  }

  @FXML
  private void salir() {
    Platform.exit();
  }

  @FXML
  private void acercaDe() {
    Alert a = new Alert(Alert.AlertType.INFORMATION);
    a.setTitle("Acerca de");
    a.setHeaderText("Cerrajería — Lista de Precios");
    a.setContentText("Versión 1.0.0\n\nGestión de productos y presupuestos.");
    a.showAndWait();
  }

  @FXML
  private void importarCSV() {
    // 1) Elegir archivo — acepta .csv y .txt (X-28)
    FileChooser fc = new FileChooser();
    fc.setTitle("Seleccionar lista de precios");
    fc.getExtensionFilters().addAll(
        new FileChooser.ExtensionFilter("Listas (*.csv, *.txt)", "*.csv", "*.txt"),
        new FileChooser.ExtensionFilter("Solo CSV", "*.csv"),
        new FileChooser.ExtensionFilter("Solo TXT (X-28)", "*.txt"),
        new FileChooser.ExtensionFilter("Todos", "*.*"));

    File archivo = fc.showOpenDialog(menuBar.getScene().getWindow());
    if (archivo == null)
      return;

    try {
      // 2) Diálogo de proveedor
      List<Proveedor> proveedores = new ProveedorDAO().listar();
      Proveedor proveedor = dialogoProveedor(proveedores);
      if (proveedor == null)
        return;

      // 3) Detectar parser según archivo y/o proveedor
      ImportadorProductos.Resultado r;
      String nombreArchivo = archivo.getName().toLowerCase();
      String nombreProv = proveedor.getNombre() == null
          ? ""
          : proveedor.getNombre().toLowerCase();

      boolean esX28 = nombreArchivo.endsWith(".txt")
          || nombreArchivo.contains("x28")
          || nombreArchivo.contains("x-28")
          || nombreProv.contains("x28")
          || nombreProv.contains("x-28");

      if (esX28) {
        System.out.println("▶ Usando parser X-28 para: " + archivo.getName());
        r = ImportadorX28.importar(archivo, proveedor);
      } else {
        System.out.println("▶ Usando importador CSV genérico para: " + archivo.getName());
        r = ImportadorProductos.importar(archivo, proveedor);
      }

      // 4) Mostrar resultado
      Alert ok = new Alert(Alert.AlertType.INFORMATION);
      ok.setTitle("Importación terminada");
      ok.setHeaderText("Proveedor: " + proveedor.getNombre());
      ok.setContentText(r.toString());
      ok.showAndWait();

      // 5) Refrescar la tabla de productos
      if (productoViewController != null) {
        productoViewController.refrescar();
      }
    } catch (Exception e) {
      e.printStackTrace();
      Alert err = new Alert(Alert.AlertType.ERROR);
      err.setTitle("Error al importar");
      err.setHeaderText(null);
      err.setContentText(e.getMessage() == null ? e.toString() : e.getMessage());
      err.showAndWait();
    }
  }

  private Proveedor dialogoProveedor(List<Proveedor> existentes) throws Exception {
    Dialog<Proveedor> dlg = new Dialog<>();
    dlg.setTitle("Datos del proveedor");
    dlg.setHeaderText("¿De qué proveedor es esta lista?");

    ComboBox<Proveedor> cbProv = new ComboBox<>(
        FXCollections.observableArrayList(existentes));
    cbProv.setPromptText("Elegí o creá uno nuevo");
    TextField txtNombre = new TextField();
    txtNombre.setPromptText("Nombre del nuevo proveedor");
    TextField txtIva = new TextField("21");
    TextField txtMarkup = new TextField("50");
    Label lblPreview = new Label();

    Runnable actualizar = () -> {
      try {
        double iva = Double.parseDouble(txtIva.getText().replace(",", "."));
        double mk = Double.parseDouble(txtMarkup.getText().replace(",", "."));
        double factor = (1 + iva / 100.0) * (1 + mk / 100.0);
        lblPreview.setText(String.format(
            "Precio final = costo × %.3f  (ganancia del %.1f%% sobre costo)",
            factor, (factor - 1) * 100));
      } catch (Exception e) {
        lblPreview.setText("Revisá los porcentajes");
      }
    };
    txtIva.textProperty().addListener((o, a, b) -> actualizar.run());
    txtMarkup.textProperty().addListener((o, a, b) -> actualizar.run());

    cbProv.valueProperty().addListener((o, a, b) -> {
      if (b != null) {
        txtNombre.setText(b.getNombre());
        txtIva.setText(String.valueOf(b.getIvaPorcentaje()));
        txtMarkup.setText(String.valueOf(b.getMarkupPorcentaje()));
      }
    });

    GridPane grid = new GridPane();
    grid.setHgap(10);
    grid.setVgap(10);
    grid.setPadding(new Insets(20));
    grid.add(new Label("Proveedor existente:"), 0, 0);
    grid.add(cbProv, 1, 0);
    grid.add(new Label("Nombre:"), 0, 1);
    grid.add(txtNombre, 1, 1);
    grid.add(new Label("IVA %:"), 0, 2);
    grid.add(txtIva, 1, 2);
    grid.add(new Label("Markup %:"), 0, 3);
    grid.add(txtMarkup, 1, 3);
    grid.add(lblPreview, 0, 4, 2, 1);

    dlg.getDialogPane().setContent(grid);
    dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

    Node okBtn = dlg.getDialogPane().lookupButton(ButtonType.OK);
    okBtn.setDisable(true);
    txtNombre.textProperty().addListener((o, a, b) -> okBtn.setDisable(b == null || b.isBlank()));

    dlg.setResultConverter(bt -> {
      if (bt != ButtonType.OK)
        return null;
      try {
        Proveedor p = cbProv.getValue();
        if (p == null)
          p = new Proveedor();
        p.setNombre(txtNombre.getText().trim());
        p.setIvaPorcentaje(Double.parseDouble(txtIva.getText().replace(",", ".")));
        p.setMarkupPorcentaje(Double.parseDouble(txtMarkup.getText().replace(",", ".")));
        new ProveedorDAO().guardar(p);
        return p;
      } catch (Exception e) {
        e.printStackTrace();
        return null;
      }
    });

    actualizar.run();
    return dlg.showAndWait().orElse(null);
  }
}
