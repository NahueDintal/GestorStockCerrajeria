package com.cerrajeria;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import com.cerrajeria.util.Tema;

public class MainApp extends Application {

  @Override
  public void start(Stage stage) throws Exception {

    System.setProperty("java.awt.headless", "true");
    FXMLLoader loader = new FXMLLoader(
        getClass().getResource("/views/MainView.fxml"));

    Scene scene = new Scene(loader.load(), 1200, 720);

    Tema.aplicarActual(scene);

    // ========================================================
    // LÓGICA DE ZOOM (Accesibilidad: Ctrl + / Ctrl -)
    // ========================================================
    final double[] fontSize = { 14.0 }; // Tamaño base inicial
    final double MIN_SIZE = 10.0;
    final double MAX_SIZE = 32.0;

    scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
      if (event.isControlDown()) {
        // Ctrl + "+" (Teclado numérico, o la tecla = con Shift)
        if (event.getCode() == KeyCode.ADD ||
            event.getCode() == KeyCode.PLUS ||
            event.getCode() == KeyCode.EQUALS) {

          if (fontSize[0] < MAX_SIZE) {
            fontSize[0] += 2.0;
            scene.getRoot().setStyle("-fx-font-size: " + fontSize[0] + "px;");
          }
          event.consume();
        }
        // Ctrl + "-" (Teclado numérico o guion normal)
        else if (event.getCode() == KeyCode.SUBTRACT ||
            event.getCode() == KeyCode.MINUS) {

          if (fontSize[0] > MIN_SIZE) {
            fontSize[0] -= 2.0;
            scene.getRoot().setStyle("-fx-font-size: " + fontSize[0] + "px;");
          }
          event.consume();
        }
        // Ctrl + "0" (Resetear zoom al tamaño original)
        else if (event.getCode() == KeyCode.DIGIT0 ||
            event.getCode() == KeyCode.NUMPAD0) {

          fontSize[0] = 14.0;
          scene.getRoot().setStyle("-fx-font-size: " + fontSize[0] + "px;");
          event.consume();
        }
      }
    });
    // ========================================================

    // Ícono (opcional)
    var ico = getClass().getResourceAsStream("/images/hotel-icon.png");
    if (ico != null)
      stage.getIcons().add(new Image(ico));

    stage.setTitle("Cerrajería — Lista de Precios");
    stage.setScene(scene);
    stage.setMinWidth(1000);
    stage.setMinHeight(600);
    stage.show();
  }

  public static void main(String[] args) {
    launch(args);
  }
}
