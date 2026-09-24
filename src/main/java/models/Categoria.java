package models;

public class Categoria {
    private int idCategoria;
    private String nombre;    // 'candado'
    private String etiqueta;  // 'Candados'

    public Categoria() {}
    public Categoria(int id, String nombre, String etiqueta) {
        this.idCategoria = id; this.nombre = nombre; this.etiqueta = etiqueta;
    }
    // getters/setters...
    @Override public String toString() { return etiqueta; }  // útil para ComboBox
}
