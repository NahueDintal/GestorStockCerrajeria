package com.cerrajeria;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/views/MainView.fxml"));

        Scene scene = new Scene(loader.load(), 1200, 720);

        // CSS (si todavía no lo tenés, comentá esta línea)
        var css = getClass().getResource("/css/style.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());

        // Ícono (opcional)
        var ico = getClass().getResourceAsStream("/images/hotel-icon.png");
        if (ico != null) stage.getIcons().add(new Image(ico));

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
