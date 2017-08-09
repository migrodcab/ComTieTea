package com.comtietea.comtietea.Domain;

import java.util.List;

public class SymbolicCode {
    int id;
    String tipo;
    List<SemanticField> camposSemanticos;
    List<CalendarObject> calendario;

    public SymbolicCode() { }

    public SymbolicCode(int id, String tipo, List<SemanticField> camposSemanticos, List<CalendarObject> calendario) {
        this.id = id;
        this.tipo = tipo;
        this.camposSemanticos = camposSemanticos;
        this.calendario = calendario;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public List<SemanticField> getCamposSemanticos() {
        return camposSemanticos;
    }

    public void setCamposSemanticos(List<SemanticField> camposSemanticos) {
        this.camposSemanticos = camposSemanticos;
    }

    public List<CalendarObject> getCalendario() {
        return calendario;
    }

    public void setCalendario(List<CalendarObject> calendario) {
        this.calendario = calendario;
    }
}
