package com.cerrajeria.model;

public class Proveedor {

    private int id;
    private String nombre;
    private double ivaPorcentaje;
    private double markupPorcentaje;
    private String observaciones;

    public Proveedor() {
        this.ivaPorcentaje = 21.0;
        this.markupPorcentaje = 50.0;
    }

    public Proveedor(int id, String nombre) {
        this();
        this.id = id;
        this.nombre = nombre;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public double getIvaPorcentaje() { return ivaPorcentaje; }
    public void setIvaPorcentaje(double ivaPorcentaje) { this.ivaPorcentaje = ivaPorcentaje; }

    public double getMarkupPorcentaje() { return markupPorcentaje; }
    public void setMarkupPorcentaje(double markupPorcentaje) { this.markupPorcentaje = markupPorcentaje; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    /** Calcula el precio de venta a partir del costo. */
    public double calcularPrecioVenta(double costo) {
        return costo * (1 + ivaPorcentaje / 100.0) * (1 + markupPorcentaje / 100.0);
    }

    /** Cuánto representa el markup sobre el costo (el 81.5% del ejemplo). */
    public double getMarkupSobreCosto() {
        return ((1 + ivaPorcentaje / 100.0) * (1 + markupPorcentaje / 100.0) - 1) * 100.0;
    }

    @Override
    public String toString() { return nombre; }
}
