package com.cerrajeria.util;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public final class TableUtils {

  public static <S> void autoAjustarColumnas(TableView<S> tabla, double anchoMaximo) {
    Text medidor = new Text();
    // Ajustá la fuente a la que uses en la tabla (o tomala de tabla.getFont())
    medidor.setFont(Font.font("System", 12));

    for (TableColumn<S, ?> col : tabla.getColumns()) {
      // ancho del encabezado + padding
      double ancho = medir(medidor, col.getText()) + 28;

      for (S fila : tabla.getItems()) {
        Object valor = col.getCellData(fila); // requiere items + cellValueFactory
        if (valor != null) {
          ancho = Math.max(ancho, medir(medidor, valor.toString()) + 28);
        }
      }

      col.setPrefWidth(Math.min(ancho, anchoMaximo));
    }
  }

  private static double medir(Text medidor, String texto) {
    if (texto == null || texto.isEmpty())
      return 0;
    medidor.setText(texto);
    return medidor.getLayoutBounds().getWidth();
  }
}
