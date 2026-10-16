package com.cerrajeria.controller;

import com.cerrajeria.util.Tema;
import com.cerrajeria.util.ToggleSwitch;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.MenuBar;
import javafx.scene.control.TabPane;
import com.cerrajeria.util.ImportadorProductos;
import javafx.stage.FileChooser;
import javafx.scene.control.ButtonType;

import java.io.File;

public class MainController {

  @FXML
  private MenuBar menuBar;
  @FXML
  private ToggleSwitch themeSwitch;
  @FXML
  private TabPane tabPane;

  // JavaFX inyecta automáticamente el controller del fx:include
  // cuando el id del include termina en "Controller"
  @FXML
  private HistorialController historialViewController;

  @FXML
  public void initialize() {
    boolean esOscuro = (Tema.getActual() == Tema.Modo.OSCURO);
    themeSwitch.setSelectedSilent(esOscuro);
    themeSwitch.setOnToggle(() -> Tema.alternar(menuBar.getScene()));

    // 👇 Refresca el historial cada vez que entrás a esa pestaña
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
    FileChooser fc = new FileChooser();
    fc.setTitle("Seleccionar CSV de productos");
    fc.getExtensionFilters().add(
        new FileChooser.ExtensionFilter("CSV", "*.csv"));
    File archivo = fc.showOpenDialog(menuBar.getScene().getWindow());
    if (archivo == null)
      return;

    // Preguntar si está seguro
    Alert conf = new Alert(Alert.AlertType.CONFIRMATION);
    conf.setTitle("Importar productos");
    conf.setHeaderText(null);
    conf.setContentText(
        "Se van a importar los productos de:\n" + archivo.getName() +
            "\n\nLos que ya existan (mismo modelo) se saltean.");
    if (conf.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK)
      return;

    try {
      ImportadorProductos.Resultado r = ImportadorProductos.importar(archivo);

      Alert ok = new Alert(Alert.AlertType.INFORMATION);
      ok.setTitle("Importación terminada");
      ok.setHeaderText(null);
      ok.setContentText(r.toString());
      ok.showAndWait();
    } catch (Exception e) {
      e.printStackTrace();
      Alert err = new Alert(Alert.AlertType.ERROR);
      err.setTitle("Error al importar");
      err.setHeaderText(null);
      err.setContentText(e.getMessage());
      err.showAndWait();
    }
  }
}
