package com.comtietea.comtietea.Domain;

import java.util.List;

/**
 * Created by HP on 23/07/2017.
 */
public class SymbolicCode {
    String tipo;
    List<SemanticField> camposSemanticos;

    public SymbolicCode() { }

    public SymbolicCode(String tipo, List<SemanticField> camposSemanticos) {
        this.tipo = tipo;
        this.camposSemanticos = camposSemanticos;
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
