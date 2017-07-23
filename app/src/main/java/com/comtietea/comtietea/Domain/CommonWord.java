package com.comtietea.comtietea.Domain;

/**
 * Created by HP on 23/07/2017.
 */
public class CommonWord {
    String nombre;
    int relevancia;

    public CommonWord(String nombre, int relevancia) {
        this.nombre = nombre;
        this.relevancia = relevancia;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getRelevancia() {
        return relevancia;
    }

    public void setRelevancia(int relevancia) {
        this.relevancia = relevancia;
    }
}
