package com.cerrajeria.model;

public class ItemPresupuesto {

    private int id;
    private Producto producto;
    private int cantidad;
    private double precioUnitario;   // se copia al agregar (precio histórico)
    private double descuentoItem;    // %

    public ItemPresupuesto(Producto producto, int cantidad,
                           double precioUnitario, double descuentoItem) {
        this.producto = producto;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.descuentoItem = descuentoItem;
    }

    public double getSubtotal() {
        return cantidad * precioUnitario * (1 - descuentoItem / 100.0);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    public double getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(double precioUnitario) { this.precioUnitario = precioUnitario; }

    public double getDescuentoItem() { return descuentoItem; }
    public void setDescuentoItem(double descuentoItem) { this.descuentoItem = descuentoItem; }
}
