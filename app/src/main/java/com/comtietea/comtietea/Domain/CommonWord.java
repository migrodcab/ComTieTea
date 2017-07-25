package com.comtietea.comtietea.Domain;

/**
 * Created by HP on 23/07/2017.
 */
public class CommonWord {
    String nombre;
    String imagenURL;
    int relevancia;

    public CommonWord() {   }

    public CommonWord(String nombre, String imagenURL, int relevancia) {
        this.nombre = nombre;
        this.imagenURL = imagenURL;
        this.relevancia = relevancia;
    }

    public String getNombre() {
        return nombre;
    }

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
}
