package com.cerrajeria.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Presupuesto {

    private int id;
    private int numero;
    private LocalDate fecha;
    private LocalDate validezHasta;
    private String clienteNombre;
    private String clienteTelefono;
    private double descuentoGlobal;   // %
    private String observaciones;
    private List<ItemPresupuesto> items = new ArrayList<>();

    public Presupuesto() {
        this.fecha = LocalDate.now();
        this.validezHasta = LocalDate.now().plusDays(15);
    }

    public double getSubtotal() {
        return items.stream().mapToDouble(ItemPresupuesto::getSubtotal).sum();
    }

    public double getTotal() {
        return getSubtotal() * (1 - descuentoGlobal / 100.0);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getNumero() { return numero; }
    public void setNumero(int numero) { this.numero = numero; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public LocalDate getValidezHasta() { return validezHasta; }
    public void setValidezHasta(LocalDate v) { this.validezHasta = v; }

    public String getClienteNombre() { return clienteNombre; }
    public void setClienteNombre(String c) { this.clienteNombre = c; }

    public String getClienteTelefono() { return clienteTelefono; }
    public void setClienteTelefono(String c) { this.clienteTelefono = c; }

    public double getDescuentoGlobal() { return descuentoGlobal; }
    public void setDescuentoGlobal(double d) { this.descuentoGlobal = d; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String o) { this.observaciones = o; }

    public List<ItemPresupuesto> getItems() { return items; }
    public void setItems(List<ItemPresupuesto> items) { this.items = items; }
}
