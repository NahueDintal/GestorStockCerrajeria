package com.cerrajeria.controller;

import com.cerrajeria.dao.ProveedorDAO;
import com.cerrajeria.model.Proveedor;
import com.cerrajeria.util.ImportadorProductos;
import com.cerrajeria.util.ImportadorXlsx;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.File;
import java.text.Normalizer;
import java.util.*;

public class DialogoImportarXlsx extends Dialog<ImportadorProductos.Resultado> {

    private final Workbook wb;
    private List<String> encabezados = List.of();

    private final ComboBox<Integer> cbHoja  = new ComboBox<>();
    private final Spinner<Integer> spnHeader = new Spinner<>(0, 50, 0);
    private final ComboBox<String> cbCod = new ComboBox<>(), cbNom = new ComboBox<>(),
        cbCat = new ComboBox<>(), cbMar = new ComboBox<>(),
        cbDes = new ComboBox<>(), cbPre = new ComboBox<>();
    private final ComboBox<Proveedor> cbProv = new ComboBox<>();
    private final CheckBox chkFinal = new CheckBox("El precio de la planilla ya es precio de venta final");
    private final TableView<ImportadorXlsx.Fila> tabla = new TableView<>();
    private final Label lblInfo = new Label();

    public DialogoImportarXlsx(File archivo) throws Exception {
        this.wb = ImportadorXlsx.abrir(archivo);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("Importar lista de precios (Excel) — " + archivo.getName());

        // hojas
        List<Integer> idxHojas = new ArrayList<>();
        for (int i = 0; i < wb.getNumberOfSheets(); i++) idxHojas.add(i);
        cbHoja.setItems(FXCollections.observableArrayList(idxHojas));
        cbHoja.setCellFactory(l -> new ListCell<>() {
            @Override protected void updateItem(Integer i, boolean empty) {
                super.updateItem(i, empty);
                setText(empty || i == null ? "" : wb.getSheetName(i));
            }
        });
        cbHoja.setButtonCell(cbHoja.getCellFactory().call(null));
        cbHoja.getSelectionModel().selectedItemProperty().addListener((o,a,b) -> {
            if (b != null) cargarEncabezados();
        });
        spnHeader.valueProperty().addListener((o,a,b) -> cargarEncabezados());

        cbProv.setItems(FXCollections.observableArrayList(new ProveedorDAO().listar()));

        // preview
        TableColumn<ImportadorXlsx.Fila, String> c1 = new TableColumn<>("Código");
        c1.setCellValueFactory(x -> new SimpleStringProperty(x.getValue().codigo));
        c1.setPrefWidth(110);
        TableColumn<ImportadorXlsx.Fila, String> c2 = new TableColumn<>("Nombre");
        c2.setCellValueFactory(x -> new SimpleStringProperty(x.getValue().nombre));
        c2.setPrefWidth(320);
        TableColumn<ImportadorXlsx.Fila, String> c3 = new TableColumn<>("Categoría");
        c3.setCellValueFactory(x -> new SimpleStringProperty(x.getValue().categoria));
        c3.setPrefWidth(150);
        TableColumn<ImportadorXlsx.Fila, Double> c4 = new TableColumn<>("Precio");
        c4.setCellValueFactory(x -> new SimpleObjectProperty<>(x.getValue().precio));
        c4.setPrefWidth(100);
        tabla.getColumns().addAll(c1, c2, c3, c4);
        tabla.setPrefHeight(320);

        // form
        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(8); g.setPadding(new Insets(10));
        int r = 0;
        g.add(new Label("Hoja:"),              0, r); g.add(cbHoja, 1, r++);
        g.add(new Label("Fila encabezado:"),   0, r); g.add(spnHeader, 1, r++);
        g.add(new Label("Proveedor:"),         0, r); g.add(cbProv, 1, r++);
        g.add(new Label("Columna código:"),    0, r); g.add(cbCod, 1, r++);
        g.add(new Label("Columna nombre:"),    0, r); g.add(cbNom, 1, r++);
        g.add(new Label("Columna categoría:"), 0, r); g.add(cbCat, 1, r++);
        g.add(new Label("Columna marca:"),     0, r); g.add(cbMar, 1, r++);
        g.add(new Label("Columna descripción:"),0,r); g.add(cbDes, 1, r++);
        g.add(new Label("Columna precio:"),    0, r); g.add(cbPre, 1, r++);
        g.add(chkFinal, 0, r, 2, 1);

        VBox root = new VBox(10, g, lblInfo, tabla);
        root.setPadding(new Insets(10));
        getDialogPane().setContent(root);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        getDialogPane().setPrefWidth(950);

        Button ok = (Button) getDialogPane().lookupButton(ButtonType.OK);
        ok.setDisable(true);

        // refresh al cambiar cualquier combo
        Runnable refresh = () -> {
            try {
                if (cbHoja.getValue() == null || cbProv.getValue() == null) { ok.setDisable(true); return; }
                List<ImportadorXlsx.Fila> filas = ImportadorXlsx.leer(
                    wb, cbHoja.getValue(), spnHeader.getValue(), mapeo());
                tabla.setItems(FXCollections.observableArrayList(filas));
                lblInfo.setText(filas.size() + " filas listas");
                ok.setDisable(filas.isEmpty());
            } catch (Exception e) {
                lblInfo.setText("Error: " + e.getMessage());
                ok.setDisable(true);
            }
        };
        for (ComboBox<String> cb : List.of(cbCod, cbNom, cbCat, cbMar, cbDes, cbPre))
            cb.valueProperty().addListener((o,a,b) -> refresh.run());
        cbProv.valueProperty().addListener((o,a,b) -> refresh.run());

        setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            try {
                return ImportadorXlsx.insertar(ImportadorXlsx.leer(
                    wb, cbHoja.getValue(), spnHeader.getValue(), mapeo()),
                    cbProv.getValue(), chkFinal.isSelected());
            } catch (Exception e) {
                new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
                return null;
            }
        });

        cbHoja.getSelectionModel().selectFirst();
    }

    private void cargarEncabezados() {
        encabezados = ImportadorXlsx.leerEncabezados(wb, cbHoja.getValue(), spnHeader.getValue());
        List<String> opts = new ArrayList<>();
        opts.add("— (ninguna) —");
        opts.addAll(encabezados);
        for (ComboBox<String> cb : List.of(cbCod, cbNom, cbCat, cbMar, cbDes, cbPre))
            cb.setItems(FXCollections.observableArrayList(opts));

        cbCod.getSelectionModel().select(autoDetect("codigo"));
        cbNom.getSelectionModel().select(autoDetect("nombre"));
        cbCat.getSelectionModel().select(autoDetect("categoria"));
        cbMar.getSelectionModel().select(autoDetect("marca"));
        cbDes.getSelectionModel().select(autoDetect("descripcion"));
        cbPre.getSelectionModel().select(autoDetect("precio"));
    }

    private String autoDetect(String campo) {
        String[] claves = switch (campo) {
            case "codigo"      -> new String[]{"cod", "sku", "articulo", "art"};
            case "nombre"      -> new String[]{"nombre", "descrip", "detalle", "producto"};
            case "categoria"   -> new String[]{"categ", "rubro", "linea", "familia"};
            case "marca"       -> new String[]{"marca", "brand"};
            case "descripcion" -> new String[]{"observ", "nota", "coment"};
            case "precio"      -> new String[]{"precio", "venta", "lista", "pvp", "importe"};
            default -> new String[0];
        };
        for (String e : encabezados) {
            String n = norm(e);
            for (String k : claves) if (n.contains(k)) return e;
        }
        return "— (ninguna) —";
    }

    private Map<String, Integer> mapeo() {
        Map<String, Integer> m = new HashMap<>();
        m.put("codigo",      idx(cbCod, 0));
        m.put("nombre",      idx(cbNom, 1));
        m.put("categoria",   idx(cbCat, -1));
        m.put("marca",       idx(cbMar, -1));
        m.put("descripcion", idx(cbDes, -1));
        m.put("precio",      idx(cbPre, -1));
        return m;
    }

    private int idx(ComboBox<String> cb, int fallback) {
        String s = cb.getValue();
        if (s == null || s.startsWith("—")) return fallback;
        return encabezados.indexOf(s);
    }

    private static String norm(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase().trim();
    }
}
