package com.cerrajeria.util;

import java.io.File;

public final class AbrirArchivo {
    private AbrirArchivo() {}

    /** Abre un archivo con el visor por defecto del SO, sin usar AWT. */
    public static void abrir(File archivo) {
        if (archivo == null || !archivo.exists()) return;

        Thread t = new Thread(() -> {
            try {
                String os = System.getProperty("os.name").toLowerCase();
                ProcessBuilder pb;
                if (os.contains("win")) {
                    pb = new ProcessBuilder("cmd", "/c", "start", "",
                            archivo.getAbsolutePath());
                } else if (os.contains("mac")) {
                    pb = new ProcessBuilder("open", archivo.getAbsolutePath());
                } else {
                    // Linux: xdg-open (respeta Wayland/X11)
                    pb = new ProcessBuilder("xdg-open", archivo.getAbsolutePath());
                }
                pb.redirectErrorStream(true);
                pb.start();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }, "abrir-archivo");
        t.setDaemon(true);
        t.start();
    }
}
