package com.comtietea.comtietea.Domain;

/**
 * Created by HP on 27/07/2017.
 */
public class FirebaseImage {
    String imagenURL;
    String imagenRuta;

    public FirebaseImage() {}

    public FirebaseImage(String imagenURL, String imagenRuta) {
        this.imagenURL = imagenURL;
        this.imagenRuta = imagenRuta;
    }

    public String getImagenURL() {
        return imagenURL;
    }

    public void setImagenURL(String imagenURL) {
        this.imagenURL = imagenURL;
    }

    public String getImagenRuta() {
        return imagenRuta;
    }

    public void setImagenRuta(String imagenRuta) {
        this.imagenRuta = imagenRuta;
    }
}
