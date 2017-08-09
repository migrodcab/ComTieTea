package com.comtietea.comtietea.Domain;

import java.util.List;


public class CalendarObject implements Comparable<CalendarObject> {
    String id;
    String fecha;
    String diaSemana;
    String mes;
    List<ActivitySchedule> activitiesSchedule;

    public CalendarObject() {}

    public CalendarObject(String id, String fecha, String diaSemana, String mes, List<ActivitySchedule> activitiesSchedule) {
        this.id = id;
        this.fecha = fecha;
        this.diaSemana = diaSemana;
        this.mes = mes;
        this.activitiesSchedule = activitiesSchedule;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
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

    public List<ActivitySchedule> getActivitiesSchedule() {
        return activitiesSchedule;
    }

    public void setActivitiesSchedule(List<ActivitySchedule> activitiesSchedule) {
        this.activitiesSchedule = activitiesSchedule;
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
