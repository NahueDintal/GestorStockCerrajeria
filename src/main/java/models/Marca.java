package models;

public class Marca {
    private int idMarca;
    private String nombre;

    public Marca() {}
    public Marca(int id, String nombre) { this.idMarca = id; this.nombre = nombre; }
    // getters/setters...
    @Override public String toString() { return nombre; }
}
