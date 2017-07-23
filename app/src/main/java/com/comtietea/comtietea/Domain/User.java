package com.comtietea.comtietea.Domain;

import java.util.List;

/**
 * Created by HP on 23/07/2017.
 */
public class User {
    String nombre;
    String email;
    String uid;
    List<SymbolicCode> codigosSimbolicos;

    public User() { }

    public User(String nombre, String email, String uid, List<SymbolicCode> codigosSimbolicos) {
        this.nombre = nombre;
        this.email = email;
        this.uid = uid;
        this.codigosSimbolicos = codigosSimbolicos;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public List<SymbolicCode> getCodigosSimbolicos() {
        return codigosSimbolicos;
    }

    public void setCodigosSimbolicos(List<SymbolicCode> codigosSimbolicos) {
        this.codigosSimbolicos = codigosSimbolicos;
    }
}
