package controllers;

import java.io.IOException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.AnchorPane;
import models.Categoria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DashboardController {

  private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

  // --- Botones del sidebar (los ids que tenés en tu FXML) ---
  @FXML
  private ToggleButton btnPlanilla; // "Presupuesto"
  @FXML
  private ToggleButton btnReservas; // "Cerraduras"
  @FXML
  private ToggleButton btnHabitaciones; // "Candados"
  @FXML
  private ToggleButton btnClientes; // "Llaves"
  @FXML
  private ToggleButton btnReportes; // "Cámaras"
  @FXML
  private ToggleButton btnPersonal; // "Alarmas X-28"
  @FXML
  private ToggleButton btnConfiguracion; // "Monitoreo"
  @FXML
  private ToggleButton btnManijas;

  // --- Área central ---
  @FXML
  private AnchorPane centerPane;

  // Categorías (id, nombre, etiqueta) — coinciden con las seeds de la DB
  private static final Categoria CAT_CERRADURA = new Categoria(4, "cerradura", "Cerraduras");
  private static final Categoria CAT_CANDADO = new Categoria(3, "candado", "Candados");
  private static final Categoria CAT_LLAVE = new Categoria(5, "llave", "Llaves");
  private static final Categoria CAT_CAMARA = new Categoria(2, "camara", "Cámaras");
  private static final Categoria CAT_ALARMA = new Categoria(1, "alarma", "Alarmas");
  private static final Categoria CAT_MANIJA = new Categoria(6, "manija", "Manijas");

  @FXML
  private void initialize() {
    // Agrupamos los toggles por código, así no hace falta tocar el FXML
    ToggleGroup grupo = new ToggleGroup();
    btnPlanilla.setToggleGroup(grupo);
    btnReservas.setToggleGroup(grupo);
    btnHabitaciones.setToggleGroup(grupo);
    btnClientes.setToggleGroup(grupo);
    btnReportes.setToggleGroup(grupo);
    btnPersonal.setToggleGroup(grupo);
    btnConfiguracion.setToggleGroup(grupo);
    btnManijas.setToggleGroup(grupo);

    // Asociaciones: botón → categoría → vista de productos
    asociar(btnReservas, CAT_CERRADURA);
    asociar(btnHabitaciones, CAT_CANDADO);
    asociar(btnClientes, CAT_LLAVE);
    asociar(btnReportes, CAT_CAMARA);
    asociar(btnPersonal, CAT_ALARMA);
    asociar(btnManijas, CAT_MANIJA);

    // Botones especiales (todavía no tienen vista)
    btnPlanilla.setOnAction(e -> mostrarPlaceholder("Presupuesto"));
    btnConfiguracion.setOnAction(e -> mostrarPlaceholder("Monitoreo"));

    // Vista inicial
    btnHabitaciones.setSelected(true);
    abrirProductos(CAT_CANDADO);
  }

  /**
   * Asocia un ToggleButton con su categoría. Si el usuario hace clic sobre
   * el que ya está seleccionado, no se deselecciona (queda fijo).
   */
  private void asociar(ToggleButton btn, Categoria cat) {
    btn.setOnAction(e -> {
      if (!btn.isSelected()) {
        btn.setSelected(true);
        return;
      }
      abrirProductos(cat);
    });
  }

  // ============================================================
  // Carga de la vista de productos
  // ============================================================
  private void abrirProductos(Categoria cat) {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ProductView.fxml"));
      Parent root = loader.load();
      ProductController ctrl = loader.getController();
      ctrl.setCategoria(cat);
      setCentro(root);
    } catch (IOException e) {
      log.error("Error cargando ProductView", e);
      new Alert(Alert.AlertType.ERROR,
          "No se pudo cargar la vista: " + e.getMessage()).showAndWait();
    }
  }

  private void mostrarPlaceholder(String titulo) {
    Label lbl = new Label(titulo + " — próximamente");
    lbl.setStyle("-fx-font-size: 22px; -fx-text-fill: #7f8c8d;");
    AnchorPane.setTopAnchor(lbl, 40.0);
    AnchorPane.setLeftAnchor(lbl, 40.0);
    centerPane.getChildren().setAll(lbl);
  }

  private void setCentro(Parent root) {
    AnchorPane.setTopAnchor(root, 0.0);
    AnchorPane.setBottomAnchor(root, 0.0);
    AnchorPane.setLeftAnchor(root, 0.0);
    AnchorPane.setRightAnchor(root, 0.0);
    centerPane.getChildren().setAll(root);
  }
}
