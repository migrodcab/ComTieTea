package com.comtietea.comtietea.Domain;

public class ActivitySchedule implements Comparable<ActivitySchedule> {
    int id;
    String nombre;
    String hora;
    String alarma;
    String aviso;
    String antelacion;
    int camSemId;
    int palHabId;
    int color;

    public ActivitySchedule() {}

    public ActivitySchedule(int id, String nombre, String hora, String alarma, String aviso, String antelacion, int camSemId, int palHabId, int color) {
        this.id = id;
        this.nombre = nombre;
        this.hora = hora;
        this.alarma = alarma;
        this.aviso = aviso;
        this.antelacion = antelacion;
        this.camSemId = camSemId;
        this.palHabId = palHabId;
        this.color = color;
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

    public String getHora() {
        return hora;
    }

    public void setHora(String hora) {
        this.hora = hora;
    }

    public String getAlarma() {
        return alarma;
    }

    public void setAlarma(String alarma) {
        this.alarma = alarma;
    }

    public String getAviso() {
        return aviso;
    }

    public void setAviso(String aviso) {
        this.aviso = aviso;
    }

    public String getAntelacion() {
        return antelacion;
    }

    public void setAntelacion(String antelacion) {
        this.antelacion = antelacion;
    }

    public int getCamSemId() {
        return camSemId;
    }

    public void setCamSemId(int camSemId) {
        this.camSemId = camSemId;
    }

    public int getPalHabId() {
        return palHabId;
    }

    public void setPalHabId(int palHabId) {
        this.palHabId = palHabId;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    @Override
    public int compareTo(ActivitySchedule activitySchedule) {
        if (hora.compareTo(activitySchedule.getHora()) < 0) {
            return 1;
        }
        if (hora.compareTo(activitySchedule.getHora()) > 0) {
            return -1;
        }
        return 0;
    }
}
