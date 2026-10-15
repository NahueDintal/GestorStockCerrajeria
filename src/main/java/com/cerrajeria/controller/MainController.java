package com.cerrajeria.controller;

import com.cerrajeria.util.Tema;
import com.cerrajeria.util.ToggleSwitch;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.MenuBar;

public class MainController {

  @FXML
  private MenuBar menuBar;
  @FXML
  private ToggleSwitch themeSwitch;

  @FXML
  public void initialize() {
    // Arranca en el estado guardado sin disparar el toggle
    boolean esOscuro = (Tema.getActual() == Tema.Modo.OSCURO);
    themeSwitch.setSelectedSilent(esOscuro);

    // Cuando el usuario lo mueve, cambia el tema
    themeSwitch.setOnToggle(() -> Tema.alternar(menuBar.getScene()));
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
}
