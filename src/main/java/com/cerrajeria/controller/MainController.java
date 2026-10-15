package com.cerrajeria.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;

import com.cerrajeria.util.Tema;

public class MainController {

  @FXML
  public void initialize() {
    // Por ahora no hay nada que inicializar acá.
    // Los controllers de cada pestaña se inicializan solos.
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
