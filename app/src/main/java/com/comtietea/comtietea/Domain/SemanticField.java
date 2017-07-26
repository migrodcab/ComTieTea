package com.comtietea.comtietea.Domain;

import java.util.List;

public class SemanticField implements Comparable<SemanticField> {
    String nombre;
    String imagenURL;
    int relevancia;
    int color;
    List<CommonWord> palabrasHabituales;

    public SemanticField() {
    }

    public SemanticField(String nombre, String imagenURL, int relevancia, int color, List<CommonWord> palabrasHabituales) {
        this.nombre = nombre;
        this.imagenURL = imagenURL;
        this.relevancia = relevancia;
        this.color = color;
        this.palabrasHabituales = palabrasHabituales;
    }

    public String getNombre() { return nombre; }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getImagenURL() {
        return imagenURL;
    }

    public void setImagenURL(String imagenURL) {
        this.imagenURL = imagenURL;
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
