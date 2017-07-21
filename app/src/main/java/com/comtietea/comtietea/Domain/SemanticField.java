package com.comtietea.comtietea.Domain;

/**
 * Created by HP on 13/07/2017.
 */
public class SemanticField {
    String name;
    int relevance;

    public SemanticField() {
    }

    public SemanticField(String name, int relevance) {
        this.name = name;
        this.relevance = relevance;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getRelevance() {
        return relevance;
    }

    public void setRelevance(int relevance) {
        this.relevance = relevance;
    }
}
