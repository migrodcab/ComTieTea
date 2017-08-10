package com.comtietea.comtietea.Domain;

import java.util.List;


public class CalendarObject implements Comparable<CalendarObject> {
    int id;
    String fecha;
    String diaSemana;
    String mes;
    List<ActivitySchedule> actividades;

    public CalendarObject() {}

    public CalendarObject(int id, String fecha, String diaSemana, String mes, List<ActivitySchedule> actividades) {
        this.id = id;
        this.fecha = fecha;
        this.diaSemana = diaSemana;
        this.mes = mes;
        this.actividades = actividades;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getDiaSemana() {
        return diaSemana;
    }

    public void setDiaSemana(String diaSemana) {
        this.diaSemana = diaSemana;
    }

    public String getMes() {
        return mes;
    }

    public void setMes(String mes) {
        this.mes = mes;
    }


    public List<ActivitySchedule> getActividades() {
        return actividades;
    }

    public void setActividades(List<ActivitySchedule> actividades) {
        this.actividades = actividades;
    }

    @Override
    public int compareTo(CalendarObject calendarObject) {
        if (fecha.compareTo(calendarObject.getFecha()) < 0) {
            return 1;
        }
        if (fecha.compareTo(calendarObject.getFecha()) > 0) {
            return -1;
        }
        return 0;
    }
}
