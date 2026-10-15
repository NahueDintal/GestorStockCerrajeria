package com.cerrajeria.model;

import java.util.Objects;

public class Categoria {

    private int id;
    private String nombre;

    public Categoria() { }

    public Categoria(int id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    @Override
    public String toString() {
        return nombre; // el ComboBox muestra el nombre
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Categoria c)) return false;
        return id == c.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
