package com.cerrajeria.model;

import java.time.LocalDate;

public class Producto {

  private int id; // 0 = nuevo, >0 = ya persistido
  private String codigo;
  private String nombre;
  private String marca;
  private Categoria categoria;
  private String tipo;
  private String modelo;
  private String material;
  private String medidas;
  private String color;
  private String descripcion;
  private String proveedor;

  private double precioCosto;
  private double precioVenta;

  private int stock;
  private int stockMinimo;
  private String ubicacion;

  private boolean activo;
  private LocalDate fechaAlta;
  private String imagen;
  private String observaciones;

  public Producto() {
    this.categoria = null;
    this.activo = true;
    this.fechaAlta = LocalDate.now();
  }

  // --- Getters y setters ---
  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getCodigo() {
    return codigo;
  }

  public void setCodigo(String codigo) {
    this.codigo = codigo;
  }

  public String getNombre() {
    return nombre;
  }

  public void setNombre(String nombre) {
    this.nombre = nombre;
  }

  public String getMarca() {
    return marca;
  }

  public void setMarca(String marca) {
    this.marca = marca;
  }

  public Categoria getCategoria() {
    return categoria;
  }

  public void setCategoria(Categoria categoria) {
    this.categoria = categoria;
  }

  public String getTipo() {
    return tipo;
  }

  public void setTipo(String tipo) {
    this.tipo = tipo;
  }

  public String getModelo() {
    return modelo;
  }

  public void setModelo(String modelo) {
    this.modelo = modelo;
  }

  public String getMaterial() {
    return material;
  }

  public void setMaterial(String material) {
    this.material = material;
  }

  public String getMedidas() {
    return medidas;
  }

  public void setMedidas(String medidas) {
    this.medidas = medidas;
  }

  public String getColor() {
    return color;
  }

  public void setColor(String color) {
    this.color = color;
  }

  public String getDescripcion() {
    return descripcion;
  }

  public void setDescripcion(String descripcion) {
    this.descripcion = descripcion;
  }

  public String getProveedor() {
    return proveedor;
  }

  public void setProveedor(String proveedor) {
    this.proveedor = proveedor;
  }

  public double getPrecioCosto() {
    return precioCosto;
  }

  public void setPrecioCosto(double precioCosto) {
    this.precioCosto = precioCosto;
  }

  public double getPrecioVenta() {
    return precioVenta;
  }

  public void setPrecioVenta(double precioVenta) {
    this.precioVenta = precioVenta;
  }

  public int getStock() {
    return stock;
  }

  public void setStock(int stock) {
    this.stock = stock;
  }

  public int getStockMinimo() {
    return stockMinimo;
  }

  public void setStockMinimo(int stockMinimo) {
    this.stockMinimo = stockMinimo;
  }

  public String getUbicacion() {
    return ubicacion;
  }

  public void setUbicacion(String ubicacion) {
    this.ubicacion = ubicacion;
  }

  public boolean isActivo() {
    return activo;
  }

  public void setActivo(boolean activo) {
    this.activo = activo;
  }

  public LocalDate getFechaAlta() {
    return fechaAlta;
  }

  public void setFechaAlta(LocalDate fechaAlta) {
    this.fechaAlta = fechaAlta;
  }

  public String getImagen() {
    return imagen;
  }

  public void setImagen(String imagen) {
    this.imagen = imagen;
  }

  public String getObservaciones() {
    return observaciones;
  }

  public void setObservaciones(String observaciones) {
    this.observaciones = observaciones;
  }

  /** Utilidad para saber si hay que reponer. */
  public boolean necesitaReposicion() {
    return stock <= stockMinimo;
  }

  /** Margen de ganancia en porcentaje. */
  public double getMargen() {
    if (precioCosto <= 0)
      return 0;
    return (precioVenta - precioCosto) / precioCosto * 100.0;
  }

  @Override
  public String toString() {
    return codigo + " — " + nombre;
  }
}
