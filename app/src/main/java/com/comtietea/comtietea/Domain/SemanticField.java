package com.comtietea.comtietea.Domain;

import java.util.List;

public class SemanticField implements Comparable<SemanticField> {
    int id;
    String nombre;
    FirebaseImage imagen;
    int relevancia;
    int color;
    List<CommonWord> palabrasHabituales;

    public SemanticField() {
    }

    public SemanticField(int id, String nombre, FirebaseImage imagen, int relevancia, int color, List<CommonWord> palabrasHabituales) {
        this.id = id;
        this.nombre = nombre;
        this.imagen = imagen;
        this.relevancia = relevancia;
        this.color = color;
        this.palabrasHabituales = palabrasHabituales;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() { return nombre; }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public FirebaseImage getImagen() {
        return imagen;
    }

    public void setImagen(FirebaseImage imagen) {
        this.imagen = imagen;
    }

    public int getRelevancia() {
        return relevancia;
    }

    public void setRelevancia(int relevancia) {
        this.relevancia = relevancia;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public List<CommonWord> getPalabrasHabituales() {
        return palabrasHabituales;
    }

    public void setPalabrasHabituales(List<CommonWord> palabrasHabituales) {
        this.palabrasHabituales = palabrasHabituales;
    }

    @Override
    public int compareTo(SemanticField campoSemantico) {
        if (relevancia < campoSemantico.getRelevancia()) {
            return -1;
        }
        if (relevancia > campoSemantico.getRelevancia()) {
            return 1;
        }
        return 0;
    }
}
