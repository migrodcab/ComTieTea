package com.comtietea.comtietea.Domain;

import java.util.List;

/**
 * Created by HP on 23/07/2017.
 */
public class SymbolicCode {
    int id;
    String tipo;
    List<SemanticField> camposSemanticos;

    public SymbolicCode() { }

    public SymbolicCode(int id, String tipo, List<SemanticField> camposSemanticos) {
        this.id = id;
        this.tipo = tipo;
        this.camposSemanticos = camposSemanticos;
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
}
