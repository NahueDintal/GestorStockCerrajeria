package com.cerrajeria;

import javafx.application.Application;
import javafx.stage.Stage;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.Socket;

public class MainApp extends Application {

    private static Process mariaDbProcess;
    private static File installDir;

    public static void main(String[] args) {
        // Forzar AWT headless (ya lo teníamos por OpenPDF)
        System.setProperty("java.awt.headless", "true");

        // Localizar install dir (donde está el .env)
        installDir = localizarInstallDir();

        // Arrancar MariaDB antes de JavaFX
        iniciarMariaDB();

        // Hook de cierre
        Runtime.getRuntime().addShutdownHook(new Thread(MainApp::detenerMariaDB));

        launch(args);
    }

    private static File localizarInstallDir() {
        String local = System.getenv("LOCALAPPDATA");
        if (local != null) {
            File f = new File(local, "CerrajeriaLujan");
            if (new File(f, ".env").exists()) return f;
        }
        // Fallback: directorio de trabajo
        return new File(System.getProperty("user.dir"));
    }

    private static void iniciarMariaDB() {
        if (puertoAbierto(3307)) {
            System.out.println("MariaDB ya corriendo en 3307.");
            return;
        }

        File bin = new File(installDir, "mariadb/bin/mariadbd.exe");
        if (!bin.exists()) {
            System.err.println("No se encontró: " + bin.getAbsolutePath());
            return;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(
                bin.getAbsolutePath(),
                "--datadir=" + new File(installDir, "data").getAbsolutePath(),
                "--port=3307",
                "--bind-address=127.0.0.1"
            );
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(
                new File(installDir, "mariadb.log")));

            mariaDbProcess = pb.start();
            System.out.println("MariaDB iniciando...");

            for (int i = 0; i < 60; i++) {
                Thread.sleep(500);
                if (puertoAbierto(3307)) {
                    System.out.println("MariaDB listo en 3307.");
                    return;
                }
                if (!mariaDbProcess.isAlive()) {
                    System.err.println("MariaDB murió al arrancar. Ver mariadb.log");
                    return;
                }
            }
            System.err.println("Timeout esperando MariaDB.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void detenerMariaDB() {
        if (mariaDbProcess == null || !mariaDbProcess.isAlive()) return;
        try {
            File admin = new File(installDir, "mariadb/bin/mariadb-admin.exe");
            if (admin.exists()) {
                // shutdown limpio
                ProcessBuilder pb = new ProcessBuilder(
                    admin.getAbsolutePath(),
                    "-u", "root",
                    "-P", "3307",
                    "shutdown"
                );
                pb.start().waitFor();
            }
            mariaDbProcess.waitFor();
        } catch (Exception e) {
            mariaDbProcess.destroyForcibly();
        }
    }

    private static boolean puertoAbierto(int puerto) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress("127.0.0.1", puerto), 500);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void start(Stage stage) {
        // tu código actual de JavaFX...
    }
}
