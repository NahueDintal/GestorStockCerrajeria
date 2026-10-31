package com.cerrajeria.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PdfToCsvConverter {

  // Busca el código en cualquier parte de la línea (ej: P1113248)
  private static final Pattern PATRON_CODIGO = Pattern.compile("(P\\d{6,8})");
  // Busca el precio en cualquier parte de la línea (ej: $ 58.400)
  private static final Pattern PATRON_PRECIO = Pattern.compile("\\$\\s*([\\d\\.]+)");

  public static File convertir(File pdfFile) throws IOException {
    File csvTemp = File.createTempFile("x28_import_", ".csv");

    try (PDDocument document = PDDocument.load(pdfFile);
        FileWriter writer = new FileWriter(csvTemp)) {

      writer.write("Descrip. del Grupo,Cód. Artículo,Descrip. del Artículo,PCIO,IVA,PUBLICO 50%\n");

      PDFTextStripper stripper = new PDFTextStripper();
      stripper.setSortByPosition(true);

      String categoriaActual = "X-28 ALARMAS";

      String modeloActual = null;
      String codigoActual = null;
      StringBuilder descripcionBuffer = new StringBuilder();
      Double precioActual = null;

      for (int pagina = 1; pagina <= document.getNumberOfPages(); pagina++) {
        stripper.setStartPage(pagina);
        stripper.setEndPage(pagina);
        String textoPagina = stripper.getText(document);

        String[] lineas = textoPagina.split("\n");

        for (String linea : lineas) {
          linea = linea.trim();
          if (linea.isEmpty())
            continue;

          // 1. Detectar títulos de sección (ej: "TECLADOS", "CENTRALES DE ALARMA")
          if (linea.matches("^[A-ZÁÉÍÓÚÑ\\s\\-]{5,}$") && !linea.contains("$") && !linea.contains("|")
              && linea.length() < 40) {
            if (!linea.contains("LISTA DE PRECIOS") && !linea.contains("RESIDENCIAS") && !linea.contains("ALARMAS")) {
              categoriaActual = linea;
            }
            continue;
          }

          Matcher mCodigo = PATRON_CODIGO.matcher(linea);
          Matcher mPrecio = PATRON_PRECIO.matcher(linea);

          boolean tieneCodigo = mCodigo.find();
          boolean tienePrecio = mPrecio.find();

          if (tieneCodigo) {
            // Si teníamos un producto esperando precio, lo guardamos
            if (modeloActual != null && precioActual != null) {
              escribirProducto(writer, categoriaActual, codigoActual, modeloActual, descripcionBuffer.toString(),
                  precioActual);
            }

            // Nuevo producto
            codigoActual = mCodigo.group(1);

            // El modelo es el texto antes del código, quitando el "|"
            String antesDelCodigo = linea.substring(0, mCodigo.start()).trim();
            antesDelCodigo = antesDelCodigo.replace("|", "").trim();
            // Si el texto antes del código está vacío, usamos el código como modelo
            modeloActual = antesDelCodigo.isEmpty() ? codigoActual : antesDelCodigo;

            descripcionBuffer.setLength(0);
            precioActual = null;

            // Si el precio está en la misma línea, lo tomamos
            if (tienePrecio) {
              String precioStr = mPrecio.group(1).replace(".", "");
              try {
                precioActual = Double.parseDouble(precioStr);
              } catch (NumberFormatException e) {
              }
            } else {
              // El texto después del código (si hay) es parte de la descripción
              String despuesDelCodigo = linea.substring(mCodigo.end()).trim();
              // Limpiamos iconos comunes como "MANUAL DIGITAL", "MPXH", "PS"
              despuesDelCodigo = despuesDelCodigo.replaceAll("(MANUAL DIGITAL|MPXH|PS|\\b\\d+\\s*pts?\\.?)\\s*", "")
                  .trim();
              if (!despuesDelCodigo.isEmpty()) {
                descripcionBuffer.append(despuesDelCodigo).append(" ");
              }
            }
          } else if (tienePrecio && modeloActual != null) {
            // Si tenemos un producto y encontramos un precio, lo asignamos
            String precioStr = mPrecio.group(1).replace(".", "");
            try {
              precioActual = Double.parseDouble(precioStr);
            } catch (NumberFormatException e) {
            }

            // Si hay texto antes del precio, es descripción
            String antesDelPrecio = linea.substring(0, mPrecio.start()).trim();
            // Limpiamos los puntos (ej: "10 pts.")
            antesDelPrecio = antesDelPrecio.replaceAll("\\b\\d+\\s*pts?\\.?\\s*", "").trim();
            if (!antesDelPrecio.isEmpty()) {
              descripcionBuffer.append(antesDelPrecio).append(" ");
            }
          } else if (modeloActual != null) {
            // Línea de descripción
            if (!linea.matches("^\\d+\\s*pts?\\.?.*$")) {
              descripcionBuffer.append(linea).append(" ");
            }
          }
        }
      }

      // Guardar el último producto si quedó pendiente
      if (modeloActual != null && precioActual != null) {
        escribirProducto(writer, categoriaActual, codigoActual, modeloActual, descripcionBuffer.toString(),
            precioActual);
      }
    }
    return csvTemp;
  }

  private static void escribirProducto(FileWriter writer, String categoria, String codigo, String modelo,
      String descripcion, double precio) throws IOException {
    String descFinal = descripcion.trim().replaceAll("\\s+", " ").replace(",", " ").replace("\"", "");

    // Armamos el nombre completo
    String nombreCompleto = modelo + (descFinal.isEmpty() ? "" : " - " + descFinal);

    // TRUNCAMOS para que no rompa el límite de la base de datos
    if (nombreCompleto.length() > 100) {
      nombreCompleto = nombreCompleto.substring(0, 97) + "...";
    }

    // Escribimos: Categoria, Codigo, Nombre(truncado), Precio
    writer.write(String.format("%s,%s,\"%s\",%.2f,0,0\n",
        categoria, codigo, nombreCompleto, precio));
  }
}
