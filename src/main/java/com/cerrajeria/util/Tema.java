package com.cerrajeria.util;

import javafx.scene.Scene;

import java.net.URL;
import java.util.prefs.Preferences;

public class Tema {

    public enum Modo { CLARO, OSCURO }

    private static final Preferences PREFS =
            Preferences.userNodeForPackage(Tema.class);
    private static final String KEY = "tema";

    private static Modo actual = leerPreferencia();

    private static Modo leerPreferencia() {
        try {
            return Modo.valueOf(PREFS.get(KEY, "CLARO"));
        } catch (IllegalArgumentException e) {
            return Modo.CLARO;
        }
    }

    public static Modo getActual() { return actual; }

    public static void aplicar(Scene scene, Modo modo) {
        if (scene == null) return;
        actual = modo;

        scene.getStylesheets().clear();

        String archivo = (modo == Modo.OSCURO) ? "/css/dark.css" : "/css/light.css";
        URL url = Tema.class.getResource(archivo);
        if (url != null) {
            scene.getStylesheets().add(url.toExternalForm());
        } else {
            System.err.println("⚠ No se encontró el CSS: " + archivo);
        }

        PREFS.put(KEY, modo.name());
    }

    /** Aplica el tema recordado en la primera carga. */
    public static void aplicarActual(Scene scene) {
        aplicar(scene, actual);
    }

    public static void alternar(Scene scene) {
        aplicar(scene, actual == Modo.CLARO ? Modo.OSCURO : Modo.CLARO);
    }
}
