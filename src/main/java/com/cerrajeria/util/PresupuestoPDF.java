package com.cerrajeria.util;

import com.cerrajeria.model.ItemPresupuesto;
import com.cerrajeria.model.Presupuesto;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import io.github.cdimascio.dotenv.Dotenv;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class PresupuestoPDF {

  // ================================================================
  //  FLAG: cambiá a true si querés navy + dorado. Default: B&N.
  // ================================================================
  public static boolean USAR_COLORES = false;

  private static final Dotenv env = Dotenv.configure().ignoreIfMissing().load();
  private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private static final Locale AR = new Locale("es", "AR");

  // ===== Paleta =====
  private static final Color NAVY       = new Color(27, 42, 74);
  private static final Color NAVY_CLARO = new Color(232, 237, 247);
  private static final Color DORADO     = new Color(201, 162, 39);
  private static final Color NEGRO      = Color.BLACK;
  private static final Color GRIS       = new Color(100, 100, 100);
  private static final Color GRIS_CLARO = new Color(240, 240, 240);
  private static final Color BLANCO     = Color.WHITE;

  private static Color cPrimario()   { return USAR_COLORES ? NAVY        : NEGRO; }
  private static Color cAcento()     { return USAR_COLORES ? DORADO      : NEGRO; }
  private static Color cFondoSuave() { return USAR_COLORES ? NAVY_CLARO  : GRIS_CLARO; }
  private static Color cHeaderBg()   { return USAR_COLORES ? NAVY        : NEGRO; }
  private static Color cHeaderTx()   { return BLANCO; }

  public static void generar(Presupuesto p, File destino) throws Exception {
    Document doc = new Document(PageSize.A4, 34, 34, 34, 34);
    PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(destino));

    // ===== Marco exterior en cada página =====
    writer.setPageEvent(new PdfPageEventHelper() {
      @Override
      public void onEndPage(PdfWriter w, Document d) {
        PdfContentByte cb = w.getDirectContent();
        cb.setColorStroke(cPrimario());
        cb.setLineWidth(1.2f);
        float m = 20;
        cb.rectangle(m, m,
            d.getPageSize().getWidth()  - m * 2,
            d.getPageSize().getHeight() - m * 2);
        cb.stroke();
      }
    });

    doc.open();

    // ============================================================
    //  ENCABEZADO: [ Emisor ]  [ C ]  [ Presupuesto ]
    // ============================================================
    PdfPTable header = new PdfPTable(new float[] { 4, 1.2f, 3.5f });
    header.setWidthPercentage(100);

    // --- Columna izquierda: ORIGINAL + datos del emisor ---
    PdfPCell cLeft = new PdfPCell();
    cLeft.setBorder(Rectangle.NO_BORDER);
    cLeft.setPadding(6);

    Paragraph orig = new Paragraph("ORIGINAL",
        new Font(Font.HELVETICA, 7, Font.BOLD, GRIS));
    cLeft.addElement(orig);
    cLeft.addElement(new Paragraph(
        env.get("EMPRESA_NOMBRE", "Cerrajería"),
        new Font(Font.HELVETICA, 13, Font.BOLD, cPrimario())));
    cLeft.addElement(new Paragraph(
        env.get("EMPRESA_DIRECCION", ""),
        new Font(Font.HELVETICA, 9, Font.NORMAL, GRIS)));
    cLeft.addElement(new Paragraph(
        "Tel: " + env.get("EMPRESA_TELEFONO", "") +
        "  |  " + env.get("EMPRESA_EMAIL", ""),
        new Font(Font.HELVETICA, 9, Font.NORMAL, GRIS)));
    cLeft.addElement(new Paragraph(
        "CUIT: " + env.get("EMPRESA_CUIT", "") +
        "  |  IIBB: " + env.get("EMPRESA_IIBB", ""),
        new Font(Font.HELVETICA, 9, Font.NORMAL, GRIS)));
    cLeft.addElement(new Paragraph(
        "Inicio Act.: " + env.get("EMPRESA_INICIO", "") +
        "  |  " + env.get("EMPRESA_CONDICION_IVA", ""),
        new Font(Font.HELVETICA, 9, Font.NORMAL, GRIS)));
    header.addCell(cLeft);

    // --- Columna central: letra C grande ---
    PdfPCell cC = new PdfPCell();
    cC.setBorder(Rectangle.BOX);
    cC.setBorderWidth(1.2f);
    cC.setBorderColor(cPrimario());
    cC.setBackgroundColor(cFondoSuave());
    cC.setPadding(6);
    cC.setVerticalAlignment(Element.ALIGN_MIDDLE);

    Paragraph letraC = new Paragraph("C",
        new Font(Font.HELVETICA, 34, Font.BOLD, cPrimario()));
    letraC.setAlignment(Element.ALIGN_CENTER);
    cC.addElement(letraC);

    Paragraph cod = new Paragraph("COD. " + env.get("EMPRESA_PUNTO_VENTA", "00001"),
        new Font(Font.HELVETICA, 7, Font.NORMAL, cPrimario()));
    cod.setAlignment(Element.ALIGN_CENTER);
    cC.addElement(cod);
    header.addCell(cC);

    // --- Columna derecha: PRESUPUESTO + N° + fechas ---
    PdfPCell cRight = new PdfPCell();
    cRight.setBorder(Rectangle.NO_BORDER);
    cRight.setPadding(6);

    Paragraph tit = new Paragraph("PRESUPUESTO",
        new Font(Font.HELVETICA, 14, Font.BOLD, cPrimario()));
    tit.setAlignment(Element.ALIGN_RIGHT);
    cRight.addElement(tit);

    Paragraph num = new Paragraph("N° " + String.format("%05d", p.getNumero()),
        new Font(Font.HELVETICA, 12, Font.BOLD, cAcento()));
    num.setAlignment(Element.ALIGN_RIGHT);
    cRight.addElement(num);

    Paragraph fec = new Paragraph("Fecha: " + p.getFecha().format(FMT),
        new Font(Font.HELVETICA, 9, Font.NORMAL, GRIS));
    fec.setAlignment(Element.ALIGN_RIGHT);
    cRight.addElement(fec);

    if (p.getValidezHasta() != null) {
      Paragraph val = new Paragraph("Válido hasta: " + p.getValidezHasta().format(FMT),
          new Font(Font.HELVETICA, 9, Font.NORMAL, GRIS));
      val.setAlignment(Element.ALIGN_RIGHT);
      cRight.addElement(val);
    }
    header.addCell(cRight);

    doc.add(header);
    doc.add(new Paragraph(" "));

    // ============================================================
    //  RECEPTOR: cliente (izq) + condición de venta (der)
    // ============================================================
    PdfPTable rec = new PdfPTable(new float[] { 3, 1.2f });
    rec.setWidthPercentage(100);

    PdfPCell cRecLeft = new PdfPCell();
    cRecLeft.setBorder(Rectangle.BOX);
    cRecLeft.setBorderWidth(0.8f);
    cRecLeft.setBorderColor(cPrimario());
    cRecLeft.setPadding(8);

    cRecLeft.addElement(new Paragraph(
        "Señor(es): " + nvl(p.getClienteNombre()),
        new Font(Font.HELVETICA, 10, Font.BOLD, NEGRO)));
    cRecLeft.addElement(new Paragraph(
        "Teléfono: " + nvl(p.getClienteTelefono()),
        new Font(Font.HELVETICA, 9, Font.NORMAL, GRIS)));
    cRecLeft.addElement(new Paragraph(
        "Condición frente al IVA: Consumidor Final",
        new Font(Font.HELVETICA, 9, Font.NORMAL, GRIS)));
    rec.addCell(cRecLeft);

    PdfPCell cRecRight = new PdfPCell();
    cRecRight.setBorder(Rectangle.BOX);
    cRecRight.setBorderWidth(0.8f);
    cRecRight.setBorderColor(cPrimario());
    cRecRight.setPadding(8);
    cRecRight.addElement(new Paragraph("Condición de venta:",
        new Font(Font.HELVETICA, 9, Font.NORMAL, GRIS)));
    cRecRight.addElement(new Paragraph("Contado",
        new Font(Font.HELVETICA, 10, Font.BOLD, NEGRO)));
    rec.addCell(cRecRight);

    doc.add(rec);
    doc.add(new Paragraph(" "));

    // ============================================================
    //  TABLA DE ÍTEMS
    // ============================================================
    PdfPTable tabla = new PdfPTable(new float[] { 1.3f, 5, 0.9f, 1.9f, 2 });
    tabla.setWidthPercentage(100);

    String[] headers = { "Código", "Producto", "Cant.", "P. Unit.", "Subtotal" };
    for (String h : headers) {
      PdfPCell hc = new PdfPCell(new Paragraph(h,
          new Font(Font.HELVETICA, 9, Font.BOLD, cHeaderTx())));
      hc.setBackgroundColor(cHeaderBg());
      hc.setBorderColor(cPrimario());
      hc.setPadding(6);
      hc.setHorizontalAlignment(Element.ALIGN_CENTER);
      tabla.addCell(hc);
    }

    Font normal = new Font(Font.HELVETICA, 9, Font.NORMAL, NEGRO);
    for (ItemPresupuesto it : p.getItems()) {
      // Código
      PdfPCell c1 = new PdfPCell(new Paragraph(nvl(it.getProducto().getModelo()), normal));
      c1.setPadding(5);
      c1.setBorderColor(cPrimario());
      c1.setHorizontalAlignment(Element.ALIGN_CENTER);
      tabla.addCell(c1);

      // Producto
      String desc = it.getProducto().getNombre();
      if (it.getProducto().getMarca() != null && !it.getProducto().getMarca().isBlank()) {
        desc += " — " + it.getProducto().getMarca();
      }
      PdfPCell c2 = new PdfPCell(new Paragraph(desc, normal));
      c2.setPadding(5);
      c2.setBorderColor(cPrimario());
      tabla.addCell(c2);

      // Cantidad
      PdfPCell c3 = new PdfPCell(new Paragraph(String.valueOf(it.getCantidad()), normal));
      c3.setPadding(5);
      c3.setBorderColor(cPrimario());
      c3.setHorizontalAlignment(Element.ALIGN_CENTER);
      tabla.addCell(c3);

      // Precio unitario
      PdfPCell c4 = new PdfPCell(new Paragraph(moneda(it.getPrecioUnitario()), normal));
      c4.setPadding(5);
      c4.setBorderColor(cPrimario());
      c4.setHorizontalAlignment(Element.ALIGN_RIGHT);
      tabla.addCell(c4);

      // Subtotal
      PdfPCell c5 = new PdfPCell(new Paragraph(moneda(it.getSubtotal()), normal));
      c5.setPadding(5);
      c5.setBorderColor(cPrimario());
      c5.setHorizontalAlignment(Element.ALIGN_RIGHT);
      tabla.addCell(c5);
    }
    doc.add(tabla);
    doc.add(new Paragraph(" "));

    // ============================================================
    //  TOTALES (caja alineada a la derecha)
    // ============================================================
    PdfPTable tot = new PdfPTable(new float[] { 3, 2 });
    tot.setWidthPercentage(55);
    tot.setHorizontalAlignment(Element.ALIGN_RIGHT);

    Font bold      = new Font(Font.HELVETICA, 10, Font.BOLD, NEGRO);
    Font boldTotal = new Font(Font.HELVETICA, 13, Font.BOLD, cAcento());

    // Subtotal
    tot.addCell(celdaBox("Subtotal:", bold, cPrimario(), null, false));
    tot.addCell(celdaBox(moneda(p.getSubtotal()), normal, cPrimario(), null, true));

    // Otros tributos (siempre 0)
    tot.addCell(celdaBox("Importe Otros Tributos:", bold, cPrimario(), null, false));
    tot.addCell(celdaBox("$ 0,00", normal, cPrimario(), null, true));

    // Descuento (si aplica)
    if (p.getDescuentoGlobal() > 0) {
      double desc = p.getSubtotal() * p.getDescuentoGlobal() / 100.0;
      tot.addCell(celdaBox("Descuento (" + p.getDescuentoGlobal() + "%):",
          bold, cPrimario(), null, false));
      tot.addCell(celdaBox("- " + moneda(desc), normal, cPrimario(), null, true));
    }

    // TOTAL destacado
    PdfPCell ctl = new PdfPCell(new Paragraph("TOTAL:",
        new Font(Font.HELVETICA, 13, Font.BOLD, cPrimario())));
    ctl.setBorder(Rectangle.BOX);
    ctl.setBorderColor(cPrimario());
    ctl.setBackgroundColor(cFondoSuave());
    ctl.setPadding(8);
    tot.addCell(ctl);

    PdfPCell ctv = new PdfPCell(new Paragraph(moneda(p.getTotal()), boldTotal));
    ctv.setBorder(Rectangle.BOX);
    ctv.setBorderColor(cPrimario());
    ctv.setBackgroundColor(cFondoSuave());
    ctv.setPadding(8);
    ctv.setHorizontalAlignment(Element.ALIGN_RIGHT);
    ctv.setNoWrap(true);
    tot.addCell(ctv);

    doc.add(tot);

    // ============================================================
    //  OBSERVACIONES + PIE
    // ============================================================
    if (p.getObservaciones() != null && !p.getObservaciones().isBlank()) {
      doc.add(new Paragraph(" "));
      doc.add(new Paragraph("Observaciones:",
          new Font(Font.HELVETICA, 9, Font.BOLD, cPrimario())));
      doc.add(new Paragraph(p.getObservaciones(),
          new Font(Font.HELVETICA, 9, Font.NORMAL, NEGRO)));
    }

    doc.add(new Paragraph(" "));
    Paragraph pie = new Paragraph(
        "Presupuesto sujeto a disponibilidad de stock. Este documento NO constituye factura. " +
        "Los precios pueden variar sin previo aviso pasado el plazo de validez.",
        new Font(Font.HELVETICA, 8, Font.ITALIC, GRIS));
    pie.setAlignment(Element.ALIGN_CENTER);
    doc.add(pie);

    doc.close();
  }

  // ================= Helpers =================

  private static PdfPCell celdaBox(String texto, Font f, Color borde,
                                    Color fondo, boolean derecha) {
    PdfPCell c = new PdfPCell(new Paragraph(texto, f));
    c.setBorder(Rectangle.BOX);
    c.setBorderColor(borde);
    c.setPadding(6);
    if (fondo != null) c.setBackgroundColor(fondo);
    if (derecha) c.setHorizontalAlignment(Element.ALIGN_RIGHT);
    return c;
  }

  private static String moneda(double v) {
    NumberFormat nf = NumberFormat.getInstance(AR);
    nf.setMinimumFractionDigits(2);
    nf.setMaximumFractionDigits(2);
    return "$ " + nf.format(v);
  }

  private static String nvl(String s) {
    return s == null ? "" : s;
  }
}
