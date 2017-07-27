package com.comtietea.comtietea.Domain;

public class CommonWord implements Comparable<CommonWord> {
    int id;
    String nombre;
    FirebaseImage imagen;
    int relevancia;

    public CommonWord() {   }

    public CommonWord(int id, String nombre, FirebaseImage imagen, int relevancia) {
        this.id = id;
        this.nombre = nombre;
        this.imagen = imagen;
        this.relevancia = relevancia;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

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

    @Override
    public int compareTo(CommonWord palabraHabitual) {
        if (relevancia < palabraHabitual.getRelevancia()) {
            return -1;
        }
        if (relevancia > palabraHabitual.getRelevancia()) {
            return 1;
        }
        return 0;
    }
}
