package com.cerrajeria.util;

import com.cerrajeria.model.ItemPresupuesto;
import com.cerrajeria.model.Presupuesto;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import io.github.cdimascio.dotenv.Dotenv;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;

public class PresupuestoPDF {

    private static final Dotenv env = Dotenv.configure().ignoreIfMissing().load();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final Color VERDE = new Color(46, 125, 50);
    private static final Color GRIS = new Color(240, 240, 240);

    public static void generar(Presupuesto p, File destino) throws Exception {
        Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
        PdfWriter.getInstance(doc, new FileOutputStream(destino));
        doc.open();

        // ===== Encabezado =====
        PdfPTable header = new PdfPTable(new float[]{3, 2});
        header.setWidthPercentage(100);

        PdfPCell cEmpresa = new PdfPCell();
        cEmpresa.setBorder(Rectangle.NO_BORDER);
        cEmpresa.addElement(new Paragraph(
            env.get("EMPRESA_NOMBRE", "Cerrajería"),
            new Font(Font.HELVETICA, 16, Font.BOLD, VERDE)));
        cEmpresa.addElement(new Paragraph(
            env.get("EMPRESA_DIRECCION", ""),
            new Font(Font.HELVETICA, 10)));
        cEmpresa.addElement(new Paragraph(
            "Tel: " + env.get("EMPRESA_TELEFONO", "") +
            "  |  " + env.get("EMPRESA_EMAIL", ""),
            new Font(Font.HELVETICA, 10)));
        cEmpresa.addElement(new Paragraph(
            "CUIT: " + env.get("EMPRESA_CUIT", ""),
            new Font(Font.HELVETICA, 10)));
        header.addCell(cEmpresa);

        PdfPCell cPresu = new PdfPCell();
        cPresu.setBorder(Rectangle.NO_BORDER);
        cPresu.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph titulo = new Paragraph("PRESUPUESTO",
                new Font(Font.HELVETICA, 18, Font.BOLD, VERDE));
        titulo.setAlignment(Element.ALIGN_RIGHT);
        cPresu.addElement(titulo);

        Paragraph num = new Paragraph("N° " + p.getNumero(),
                new Font(Font.HELVETICA, 14, Font.BOLD));
        num.setAlignment(Element.ALIGN_RIGHT);
        cPresu.addElement(num);

        Paragraph fecha = new Paragraph(
                "Fecha: " + p.getFecha().format(FMT),
                new Font(Font.HELVETICA, 10));
        fecha.setAlignment(Element.ALIGN_RIGHT);
        cPresu.addElement(fecha);

        if (p.getValidezHasta() != null) {
            Paragraph val = new Paragraph(
                    "Válido hasta: " + p.getValidezHasta().format(FMT),
                    new Font(Font.HELVETICA, 10));
            val.setAlignment(Element.ALIGN_RIGHT);
            cPresu.addElement(val);
        }
        header.addCell(cPresu);

        doc.add(header);
        doc.add(new Paragraph(" "));

        // ===== Datos del cliente =====
        if (p.getClienteNombre() != null && !p.getClienteNombre().isBlank()) {
            PdfPTable cli = new PdfPTable(1);
            cli.setWidthPercentage(100);
            PdfPCell c = new PdfPCell();
            c.setBackgroundColor(GRIS);
            c.setPadding(8);
            c.setBorder(Rectangle.NO_BORDER);
            c.addElement(new Paragraph("Cliente: " + p.getClienteNombre(),
                    new Font(Font.HELVETICA, 11, Font.BOLD)));
            if (p.getClienteTelefono() != null && !p.getClienteTelefono().isBlank()) {
                c.addElement(new Paragraph("Teléfono: " + p.getClienteTelefono(),
                        new Font(Font.HELVETICA, 10)));
            }
            cli.addCell(c);
            doc.add(cli);
            doc.add(new Paragraph(" "));
        }

        // ===== Tabla de ítems =====
        PdfPTable tabla = new PdfPTable(new float[]{1, 5, 2, 2});
        tabla.setWidthPercentage(100);

        String[] headers = {"Cant.", "Producto", "P. Unit.", "Subtotal"};
        for (String h : headers) {
            PdfPCell hc = new PdfPCell(new Paragraph(h,
                    new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE)));
            hc.setBackgroundColor(VERDE);
            hc.setPadding(6);
            hc.setHorizontalAlignment(Element.ALIGN_CENTER);
            tabla.addCell(hc);
        }

        Font normal = new Font(Font.HELVETICA, 10);
        for (ItemPresupuesto it : p.getItems()) {
            PdfPCell c1 = new PdfPCell(new Paragraph(String.valueOf(it.getCantidad()), normal));
            c1.setPadding(5);
            c1.setHorizontalAlignment(Element.ALIGN_CENTER);
            tabla.addCell(c1);

            String desc = it.getProducto().getNombre();
            if (it.getProducto().getMarca() != null
                    && !it.getProducto().getMarca().isBlank()) {
                desc += " — " + it.getProducto().getMarca();
            }
            PdfPCell c2 = new PdfPCell(new Paragraph(desc, normal));
            c2.setPadding(5);
            tabla.addCell(c2);

            PdfPCell c3 = new PdfPCell(new Paragraph(
                    String.format("$%.2f", it.getPrecioUnitario()), normal));
            c3.setPadding(5);
            c3.setHorizontalAlignment(Element.ALIGN_RIGHT);
            tabla.addCell(c3);

            PdfPCell c4 = new PdfPCell(new Paragraph(
                    String.format("$%.2f", it.getSubtotal()), normal));
            c4.setPadding(5);
            c4.setHorizontalAlignment(Element.ALIGN_RIGHT);
            tabla.addCell(c4);
        }
        doc.add(tabla);

        // ===== Totales =====
        doc.add(new Paragraph(" "));
        PdfPTable tot = new PdfPTable(new float[]{3, 1, 1});
        tot.setWidthPercentage(60);
        tot.setHorizontalAlignment(Element.ALIGN_RIGHT);

        Font bold = new Font(Font.HELVETICA, 11, Font.BOLD);
        Font boldVerde = new Font(Font.HELVETICA, 13, Font.BOLD, VERDE);

        tot.addCell(celdaSinBorde("Subtotal:", bold));
        tot.addCell(celdaSinBorde("", bold));
        tot.addCell(celdaDerecha(String.format("$%.2f", p.getSubtotal()), normal));

        if (p.getDescuentoGlobal() > 0) {
            tot.addCell(celdaSinBorde("Descuento (" + p.getDescuentoGlobal() + "%):", bold));
            tot.addCell(celdaSinBorde("", bold));
            double desc = p.getSubtotal() * p.getDescuentoGlobal() / 100.0;
            tot.addCell(celdaDerecha("- $" + String.format("%.2f", desc), normal));
        }

        tot.addCell(celdaSinBorde("TOTAL:", boldVerde));
        tot.addCell(celdaSinBorde("", bold));
        tot.addCell(celdaDerecha(String.format("$%.2f", p.getTotal()), boldVerde));

        doc.add(tot);

        // ===== Observaciones =====
        if (p.getObservaciones() != null && !p.getObservaciones().isBlank()) {
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Observaciones:", new Font(Font.HELVETICA, 10, Font.BOLD)));
            doc.add(new Paragraph(p.getObservaciones(), new Font(Font.HELVETICA, 10)));
        }

        doc.add(new Paragraph(" "));
        Paragraph pie = new Paragraph(
                "Presupuesto sujeto a disponibilidad de stock. " +
                "Precios pueden variar sin previo aviso pasado el plazo de validez.",
                new Font(Font.HELVETICA, 8, Font.ITALIC, Color.GRAY));
        pie.setAlignment(Element.ALIGN_CENTER);
        doc.add(pie);

        doc.close();
    }

    private static PdfPCell celdaSinBorde(String texto, Font f) {
        PdfPCell c = new PdfPCell(new Paragraph(texto, f));
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(4);
        return c;
    }

    private static PdfPCell celdaDerecha(String texto, Font f) {
        PdfPCell c = new PdfPCell(new Paragraph(texto, f));
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(4);
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return c;
    }
}
