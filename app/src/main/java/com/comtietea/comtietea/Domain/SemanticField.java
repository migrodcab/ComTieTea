package com.comtietea.comtietea.Domain;

import java.util.List;

/**
 * Created by HP on 13/07/2017.
 */
public class SemanticField {
    String nombre;
    String imagenURL;
    int relevancia;
    List<CommonWord> palabrasHabituales;

    public SemanticField() {
    }

    public SemanticField(String nombre, String imagenURL, int relevancia, List<CommonWord> palabrasHabituales) {
        this.nombre = nombre;
        this.imagenURL = imagenURL;
        this.relevancia = relevancia;
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

    public List<CommonWord> getPalabrasHabituales() {
        return palabrasHabituales;
    }

    public void setPalabrasHabituales(List<CommonWord> palabrasHabituales) {
        this.palabrasHabituales = palabrasHabituales;
    }
}
