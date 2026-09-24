package models;

public class Candado extends Producto {
  private int cantidadLlaves;

  public Candado() {
    /* categoría se setea al cargar desde DB o al crear en UI */ }

  public int getCantidadLlaves() {
    return cantidadLlaves;
  }

  public void setCantidadLlaves(int c) {
    this.cantidadLlaves = c;
  }
}
