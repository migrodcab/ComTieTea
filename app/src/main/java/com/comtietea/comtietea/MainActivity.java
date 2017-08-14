package com.comtietea.comtietea;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.comtietea.comtietea.Domain.CommonWord;
import com.comtietea.comtietea.Domain.FirebaseImage;
import com.comtietea.comtietea.Domain.FirebaseReferences;
import com.comtietea.comtietea.Domain.SemanticField;
import com.comtietea.comtietea.Domain.SymbolicCode;
import com.comtietea.comtietea.Domain.User;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInApi;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

    private GoogleApiClient googleApiClient;

    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener firebaseAuthListener;

    private FirebaseStorage firebaseStorage;
    private StorageReference storageRef;

    private FirebaseDatabase db;

    private String uid;
    private String uidAux;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final MainActivity mainActivity = this;

        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();

        googleApiClient =  new GoogleApiClient.Builder(this).enableAutoManage(this, this).addApi(Auth.GOOGLE_SIGN_IN_API, googleSignInOptions).build();

        firebaseStorage = FirebaseStorage.getInstance();
        storageRef = firebaseStorage.getReferenceFromUrl("gs://comtietea.appspot.com/");

        db = FirebaseDatabase.getInstance();
        final DatabaseReference dbRef = db.getReference(FirebaseReferences.USER_REFERENCE);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                final FirebaseUser user = firebaseAuth.getCurrentUser();
                if(user != null) {
                    uidAux = user.getUid();
                    uid = user.getUid();
                    dbRef.orderByChild("uid").equalTo(uidAux).limitToFirst(1).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if(dataSnapshot.getChildrenCount() == 0 && !uidAux.equals("")) {
                                List<SymbolicCode> codigosSimbolicos = null;

                                codigosSimbolicos = cargaCodigosSimbolicos();

                                final User u = new User(user.getDisplayName(), user.getEmail(), user.getUid(), codigosSimbolicos);
                                dbRef.child(uid).setValue(u);

                                uidAux = "";
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                } else {
                    goToLoginActivity();
                }
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();

        firebaseAuth.addAuthStateListener(firebaseAuthListener);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if(firebaseAuthListener != null) {
            firebaseAuth.removeAuthStateListener(firebaseAuthListener);
        }
    }

    /*private void setUserData(FirebaseUser user) {
        textView.setText(user.getDisplayName());
        emailTextView.setText(user.getEmail());
        idTextView.setText(user.getUid());
        Glide.with(this).load(user.getPhotoUrl()).into(imageView);
    }*/

    private void goToLoginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    public void palabrasButton(View view) {
        Intent i = new Intent(this, ActionsActivity.class);
        i.putExtra("type", "Palabras");
        i.putExtra("uid", uid);
        i.putExtra("codSimId", "0");
        startActivity(i);
    }

    public void dibujosButton(View view) {
        Intent i = new Intent(this, ActionsActivity.class);
        i.putExtra("type", "Dibujos");
        i.putExtra("uid", uid);
        i.putExtra("codSimId", "1");
        startActivity(i);
    }

    public void imagenesButton(View view) {
        Intent i = new Intent(this, ActionsActivity.class);
        i.putExtra("type", "Imagenes");
        i.putExtra("uid", uid);
        i.putExtra("codSimId", "2");
        startActivity(i);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_actions_profile, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent(this, ProfileInfoActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private List<SymbolicCode> cargaCodigosSimbolicos() {
        List<SymbolicCode> res = new ArrayList<SymbolicCode>();

        List<SemanticField> camposSemanticosPalabras = cargaCamposSemanticos("Palabras");
        SymbolicCode codigoSimbolicoPalabra = new SymbolicCode(0, "Palabras", camposSemanticosPalabras, null);

        List<SemanticField> camposSemanticosDibujos = cargaCamposSemanticos("Dibujos");
        SymbolicCode codigoSimbolicoDibujo = new SymbolicCode(1, "Dibujos", camposSemanticosDibujos, null);

        List<SemanticField> camposSemanticosImagenes = cargaCamposSemanticos("Imagenes");
        SymbolicCode codigoSimbolicoImagen = new SymbolicCode(2, "Imagenes", camposSemanticosImagenes, null);

        res.add(codigoSimbolicoPalabra);
        res.add(codigoSimbolicoDibujo);
        res.add(codigoSimbolicoImagen);

        return res;
    }

    private List<SemanticField> cargaCamposSemanticos(String codigoSimbolico) {
        List<SemanticField> res = new ArrayList<SemanticField>();

        List<CommonWord> palabrasFamilia;
        SemanticField familia = null;

        List<CommonWord> palabrasAcciones;
        SemanticField acciones = null;

        List<CommonWord> palabrasColegio;
        SemanticField colegio = null;

        List<CommonWord> palabrasCasa;
        SemanticField casa = null;

        List<CommonWord> palabrasOcio;
        SemanticField ocio = null;

        List<CommonWord> palabrasAnimales;
        SemanticField animales = null;

        List<CommonWord> palabrasColores;
        SemanticField colores = null;

        List<CommonWord> palabrasAdjetivos;
        SemanticField adjetivos = null;

        List<CommonWord> palabrasEmociones;
        SemanticField emociones = null;

        List<CommonWord> palabrasProfesiones;
        SemanticField profesiones = null;

        List<CommonWord> palabrasComida;
        SemanticField comida = null;

        List<CommonWord> palabrasAseo;
        SemanticField aseo = null;

        List<CommonWord> palabrasEstacionesYTiempo;
        SemanticField estacionesYTiempo = null;

        List<CommonWord> palabrasRopa;
        SemanticField ropa = null;

        List<CommonWord> palabrasSaludYCuerpoHumano;
        SemanticField saludYCuerpoHumano = null;

        List<CommonWord> palabrasTransportes;
        SemanticField transportes = null;

        int colorFamilia = Color.argb(255, 180, 4, 0);
        int colorAcciones = Color.parseColor("#366BB3");
        int colorColegio = Color.argb(255, 255, 255, 0);
        int colorCasa = Color.parseColor("#88AF6A");
        int colorOcio = Color.parseColor("#405D73");
        int colorAnimales = Color.parseColor("#6E4505");
        int colorColores = Color.parseColor("#5A056E");
        int colorAdjetivos = Color.parseColor("#04600E");
        int colorEmociones = Color.parseColor("#562353");
        int colorProfesiones = Color.parseColor("#CF6E3B");
        int colorComida = Color.parseColor("#EDC160");
        int colorAseo = Color.parseColor("#F908F9");
        int colorEstacionesYTiempo = Color.parseColor("#08F9F2");
        int colorRopa = Color.parseColor("#B0B9B9");
        int colorSaludYCuerpoHumano = Color.parseColor("#9472BB");
        int colorTransportes = Color.parseColor("#9C4A4A");

        switch (codigoSimbolico) {
            case "Palabras":
                palabrasFamilia = cargaPalabrasHabituales(codigoSimbolico, "Familia");
                familia = new SemanticField(0, "Familia", null, 10, colorFamilia, palabrasFamilia);

                palabrasAcciones = cargaPalabrasHabituales(codigoSimbolico, "Acciones");
                acciones = new SemanticField(1, "Acciones", null, 9, colorAcciones, palabrasAcciones);

                palabrasColegio = cargaPalabrasHabituales(codigoSimbolico, "Colegio");
                colegio = new SemanticField(2, "Colegio", null, 8, colorColegio, palabrasColegio);

                palabrasCasa = cargaPalabrasHabituales(codigoSimbolico, "Casa");
                casa = new SemanticField(3, "Casa", null, 7, colorCasa, palabrasCasa);

                palabrasOcio = cargaPalabrasHabituales(codigoSimbolico, "Ocio");
                ocio = new SemanticField(4, "Ocio", null, 7, colorOcio, palabrasOcio);

                palabrasAnimales = cargaPalabrasHabituales(codigoSimbolico, "Animales");
                animales = new SemanticField(5, "Animales", null, 6, colorAnimales, palabrasAnimales);

                palabrasColores = cargaPalabrasHabituales(codigoSimbolico, "Colores");
                colores = new SemanticField(6, "Colores", null, 6, colorColores, palabrasColores);

                palabrasAdjetivos = cargaPalabrasHabituales(codigoSimbolico, "Adjetivos");
                adjetivos = new SemanticField(7, "Adjetivos", null, 5, colorAdjetivos, palabrasAdjetivos);

                palabrasEmociones = cargaPalabrasHabituales(codigoSimbolico, "Emociones");
                emociones = new SemanticField(8, "Emociones", null, 5, colorEmociones, palabrasEmociones);

                palabrasProfesiones = cargaPalabrasHabituales(codigoSimbolico, "Profesiones");
                profesiones = new SemanticField(9, "Profesiones", null, 4, colorProfesiones, palabrasProfesiones);

                palabrasComida = cargaPalabrasHabituales(codigoSimbolico, "Comida");
                comida = new SemanticField(10, "Comida", null, 4, colorComida, palabrasComida);

                palabrasAseo = cargaPalabrasHabituales(codigoSimbolico, "Aseo");
                aseo = new SemanticField(11, "Aseo", null, 3, colorAseo, palabrasAseo);

                palabrasEstacionesYTiempo = cargaPalabrasHabituales(codigoSimbolico, "Estaciones y Tiempo");
                estacionesYTiempo = new SemanticField(12, "Estaciones y Tiempo", null, 3, colorEstacionesYTiempo, palabrasEstacionesYTiempo);

                palabrasRopa = cargaPalabrasHabituales(codigoSimbolico, "Ropa");
                ropa = new SemanticField(13, "Ropa", null, 2, colorRopa, palabrasRopa);

                palabrasSaludYCuerpoHumano = cargaPalabrasHabituales(codigoSimbolico, "Salud y Cuerpo Humano");
                saludYCuerpoHumano = new SemanticField(14, "Salud y Cuerpo Humano", null, 2, colorSaludYCuerpoHumano, palabrasSaludYCuerpoHumano);

                palabrasTransportes = cargaPalabrasHabituales(codigoSimbolico, "Transportes");
                transportes = new SemanticField(15, "Transportes", null, 1, colorTransportes, palabrasTransportes);
                break;

            case "Dibujos":
                palabrasFamilia = cargaPalabrasHabituales(codigoSimbolico, "Familia");
                familia = new SemanticField(0, "Familia", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Ffamilia_dibujo.png?alt=media&token=deba80eb-a62e-4b18-ad8e-a2f0c7de8b0d", "gs://comtietea.appspot.com/images/default/familia_dibujo.png"), 10, colorFamilia, palabrasFamilia);

                palabrasAcciones = cargaPalabrasHabituales(codigoSimbolico, "Acciones");
                acciones = new SemanticField(1, "Acciones", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Facci%C3%B3n_dibujo.jpg?alt=media&token=22d0bc48-f6fe-4bdf-a531-eef7bd8c333c","gs://comtietea.appspot.com/images/default/acción_dibujo.jpg"), 9, colorAcciones, palabrasAcciones);

                palabrasColegio = cargaPalabrasHabituales(codigoSimbolico, "Colegio");
                colegio = new SemanticField(2, "Colegio", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcolegio_dibujo.jpg?alt=media&token=dce6f6bd-ffb2-499b-b316-d6bdb33889a7", "gs://comtietea.appspot.com/images/default/colegio_dibujo.jpg"), 8, colorColegio, palabrasColegio);

                palabrasCasa = cargaPalabrasHabituales(codigoSimbolico, "Casa");
                casa = new SemanticField(3, "Casa", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcasa_dibujo.png?alt=media&token=749512fa-cf70-4707-b880-7156ad4ab125","gs://comtietea.appspot.com/images/default/casa_dibujo.png"), 7, colorCasa, palabrasCasa);

                palabrasOcio = cargaPalabrasHabituales(codigoSimbolico, "Ocio");
                ocio = new SemanticField(4, "Ocio", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Focio_dibujo.jpg?alt=media&token=29f9467f-1844-4b80-9ec6-50dd460a7cdf","gs://comtietea.appspot.com/images/default/ocio_dibujo.jpg"), 7, colorOcio, palabrasOcio);

                palabrasAnimales = cargaPalabrasHabituales(codigoSimbolico, "Animales");
                animales = new SemanticField(5, "Animales", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fanimales_dibujo.jpg?alt=media&token=37441298-ccc4-43e0-8bf1-a1a515fc1ae0", "gs://comtietea.appspot.com/images/default/animales_dibujo.jpg"), 6, colorAnimales, palabrasAnimales);

                palabrasColores = cargaPalabrasHabituales(codigoSimbolico, "Colores");
                colores = new SemanticField(6, "Colores", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcolor_dibujo.png?alt=media&token=2b5846ee-8540-4c0e-a2a1-c0a14e675a61","gs://comtietea.appspot.com/images/default/color_dibujo.png"), 6, colorColores, palabrasColores);

                palabrasAdjetivos = cargaPalabrasHabituales(codigoSimbolico, "Adjetivos");
                adjetivos = new SemanticField(7, "Adjetivos", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fadjetivo_dibujo.png?alt=media&token=3b3869a1-9856-45a3-9f63-210b2684f369","gs://comtietea.appspot.com/images/default/adjetivo_dibujo.png"), 5, colorAdjetivos, palabrasAdjetivos);

                palabrasEmociones = cargaPalabrasHabituales(codigoSimbolico, "Emociones");
                emociones = new SemanticField(8, "Emociones", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Femociones_dibujo.png?alt=media&token=e9782282-7464-49bd-aadd-4032c726e681","gs://comtietea.appspot.com/images/default/emociones_dibujo.png"), 5, colorEmociones, palabrasEmociones);

                palabrasProfesiones = cargaPalabrasHabituales(codigoSimbolico, "Profesiones");
                profesiones = new SemanticField(9, "Profesiones", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fprofesi%C3%B3n_dibujo.jpg?alt=media&token=3ddad120-4994-4eb1-a6d7-d17b2149a7a3","gs://comtietea.appspot.com/images/default/profesión_dibujo.jpg"), 4, colorProfesiones, palabrasProfesiones);

                palabrasComida = cargaPalabrasHabituales(codigoSimbolico, "Comida");
                comida = new SemanticField(10, "Comida", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcomida_dibujo.png?alt=media&token=27e177d6-f557-4174-b288-ab78eab800bb","gs://comtietea.appspot.com/images/default/comida_dibujo.png"), 4, colorComida, palabrasComida);

                palabrasAseo = cargaPalabrasHabituales(codigoSimbolico, "Aseo");
                aseo = new SemanticField(11, "Aseo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Faseo_dibujo.jpg?alt=media&token=20eac5ed-3962-49f3-bb71-1b4ee1360cde","gs://comtietea.appspot.com/images/default/aseo_dibujo.jpg"), 3, colorAseo, palabrasAseo);

                palabrasEstacionesYTiempo = cargaPalabrasHabituales(codigoSimbolico, "Estaciones y Tiempo");
                estacionesYTiempo = new SemanticField(12, "Estaciones y Tiempo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Festaciones_dibujo.PNG?alt=media&token=74f53d8e-827c-4b31-a9da-bc8f5e6604f9","gs://comtietea.appspot.com/images/default/estaciones_dibujo.PNG"), 3, colorEstacionesYTiempo, palabrasEstacionesYTiempo);

                palabrasRopa = cargaPalabrasHabituales(codigoSimbolico, "Ropa");
                ropa = new SemanticField(13, "Ropa", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fropa_dibujo.png?alt=media&token=83cd1677-d21a-4a40-8511-070319383a1f","gs://comtietea.appspot.com/images/default/ropa_dibujo.png"), 2, colorRopa, palabrasRopa);

                palabrasSaludYCuerpoHumano = cargaPalabrasHabituales(codigoSimbolico, "Salud y Cuerpo Humano");
                saludYCuerpoHumano = new SemanticField(14, "Salud y Cuerpo Humano", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fsalud_dibujo.jpg?alt=media&token=ecbf2350-dc97-4d54-a648-d3696ad87d7a","gs://comtietea.appspot.com/images/default/salud_dibujo.jpg"), 2, colorSaludYCuerpoHumano, palabrasSaludYCuerpoHumano);

                palabrasTransportes = cargaPalabrasHabituales(codigoSimbolico, "Transportes");
                transportes = new SemanticField(15, "Transportes", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Ftransporte_dibujo.jpg?alt=media&token=18e1ec0b-3426-4b7d-ad2f-a30b91db56cf","gs://comtietea.appspot.com/images/default/transporte_dibujo.jpg"), 1, colorTransportes, palabrasTransportes);

                break;

            case "Imagenes":
                palabrasFamilia = cargaPalabrasHabituales(codigoSimbolico, "Familia");
                familia = new SemanticField(0, "Familia", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Ffamilia_foto.jpg?alt=media&token=b2b4ca19-611e-48ae-8718-8d206c1f0c13", "gs://comtietea.appspot.com/images/default/familia_foto.png"), 10, colorFamilia, palabrasFamilia);

                palabrasAcciones = cargaPalabrasHabituales(codigoSimbolico, "Acciones");
                acciones = new SemanticField(1, "Acciones", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Facci%C3%B3n_foto.jpg?alt=media&token=9ddffbfa-6c46-4eca-8cbd-fce6f8a0f050","gs://comtietea.appspot.com/images/default/acción_foto.jpg"), 9, colorAcciones, palabrasAcciones);

                palabrasColegio = cargaPalabrasHabituales(codigoSimbolico, "Colegio");
                colegio = new SemanticField(2, "Colegio", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcolegio_foto.jpg?alt=media&token=9cb13dd6-13a1-4072-a430-52b4eb1e5365","gs://comtietea.appspot.com/images/default/colegio_foto.jpg"), 8, colorColegio, palabrasColegio);

                palabrasCasa = cargaPalabrasHabituales(codigoSimbolico, "Casa");
                casa = new SemanticField(3, "Casa", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcasa_foto.jpg?alt=media&token=09e4b609-19dd-406e-a0a2-56c478bf184e","gs://comtietea.appspot.com/images/default/casa_foto.jpg"), 7, colorCasa, palabrasCasa);

                palabrasOcio = cargaPalabrasHabituales(codigoSimbolico, "Ocio");
                ocio = new SemanticField(4, "Ocio", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Focio_foto.jpg?alt=media&token=9f8fdc3b-6705-42e2-a29c-7a55ab1f77b5","gs://comtietea.appspot.com/images/default/ocio_foto.jpg"), 7, colorOcio, palabrasOcio);

                palabrasAnimales = cargaPalabrasHabituales(codigoSimbolico, "Animales");
                animales = new SemanticField(5, "Animales", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fanimales_foto.PNG?alt=media&token=b2a00091-15ba-4fad-b067-0621ec5018ad","gs://comtietea.appspot.com/images/default/animales_foto.PNG"), 6, colorAnimales, palabrasAnimales);

                palabrasColores = cargaPalabrasHabituales(codigoSimbolico, "Colores");
                colores = new SemanticField(6, "Colores", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcolor_foto.jpg?alt=media&token=f8b48ec4-f686-4d84-b805-68a14cb0ac5b","gs://comtietea.appspot.com/images/default/color_foto.jpg"), 6, colorColores, palabrasColores);

                palabrasAdjetivos = cargaPalabrasHabituales(codigoSimbolico, "Adjetivos");
                adjetivos = new SemanticField(7, "Adjetivos", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fadjetivo_foto.jpg?alt=media&token=dd868878-be30-4aca-b49f-4b889477ffbc", "gs://comtietea.appspot.com/images/default/adjetivo_foto.jpg"), 5, colorAdjetivos, palabrasAdjetivos);

                palabrasEmociones = cargaPalabrasHabituales(codigoSimbolico, "Emociones");
                emociones = new SemanticField(8, "Emociones", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Femociones_foto.jpg?alt=media&token=100bf43c-dc99-4548-ad46-484aee559575","https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Femociones_foto.jpg?alt=media&token=100bf43c-dc99-4548-ad46-484aee559575"), 5, colorEmociones, palabrasEmociones);

                palabrasProfesiones = cargaPalabrasHabituales(codigoSimbolico, "Profesiones");
                profesiones = new SemanticField(9, "Profesiones", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fprofesi%C3%B3n_foto.jpg?alt=media&token=d6731d11-2abe-4387-8640-442a0e764747","gs://comtietea.appspot.com/images/default/profesión_foto.jpg"), 4, colorProfesiones, palabrasProfesiones);

                palabrasComida = cargaPalabrasHabituales(codigoSimbolico, "Comida");
                comida = new SemanticField(10, "Comida", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcomida_foto.jpg?alt=media&token=578472e2-b437-42b3-a5a3-09dc185fd541","gs://comtietea.appspot.com/images/default/comida_foto.jpg"), 4, colorComida, palabrasComida);

                palabrasAseo = cargaPalabrasHabituales(codigoSimbolico, "Aseo");
                aseo = new SemanticField(11, "Aseo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Faseo_foto.jpg?alt=media&token=f122d38f-19c3-430e-bc49-af8096b3c642","gs://comtietea.appspot.com/images/default/aseo_foto.jpg"), 3, colorAseo, palabrasAseo);

                palabrasEstacionesYTiempo = cargaPalabrasHabituales(codigoSimbolico, "Estaciones y Tiempo");
                estacionesYTiempo = new SemanticField(12, "Estaciones y Tiempo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Festaciones_foto.PNG?alt=media&token=067b5e16-15a4-4347-a6f7-5f8be9393844","gs://comtietea.appspot.com/images/default/estaciones_foto.PNG"), 3, colorEstacionesYTiempo, palabrasEstacionesYTiempo);

                palabrasRopa = cargaPalabrasHabituales(codigoSimbolico, "Ropa");
                ropa = new SemanticField(13, "Ropa", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fropa_foto.PNG?alt=media&token=2cbd1099-234c-40f2-9776-b171ef352860","https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fropa_foto.PNG?alt=media&token=2cbd1099-234c-40f2-9776-b171ef352860"), 2, colorRopa, palabrasRopa);

                palabrasSaludYCuerpoHumano = cargaPalabrasHabituales(codigoSimbolico, "Salud y Cuerpo Humano");
                saludYCuerpoHumano = new SemanticField(14, "Salud y Cuerpo Humano", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fsalud_foto.jpg?alt=media&token=52b48e7d-577c-4fc9-b451-14666965fc32","gs://comtietea.appspot.com/images/default/salud_foto.jpg"), 2, colorSaludYCuerpoHumano, palabrasSaludYCuerpoHumano);

                palabrasTransportes = cargaPalabrasHabituales(codigoSimbolico, "Transportes");
                transportes = new SemanticField(15, "Transportes", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Ftransporte_foto.jpg?alt=media&token=de79dde7-3ce3-4320-bc04-746bdbfa9e00","gs://comtietea.appspot.com/images/default/transporte_foto.jpg"), 1, colorTransportes, palabrasTransportes);

                break;
        }

        res.add(familia);
        res.add(acciones);
        res.add(colegio);
        res.add(casa);
        res.add(ocio);
        res.add(animales);
        res.add(colores);
        res.add(adjetivos);
        res.add(emociones);
        res.add(profesiones);
        res.add(comida);
        res.add(aseo);
        res.add(estacionesYTiempo);
        res.add(ropa);
        res.add(saludYCuerpoHumano);
        res.add(transportes);

        return res;
    }

    private List<CommonWord> cargaPalabrasHabituales(String codigoSimbolico, String campoSemantico) {
        List<CommonWord> res = new ArrayList<CommonWord>();

        switch (codigoSimbolico) {
            case "Palabras":
                switch (campoSemantico) {
                    case "Colegio":
                        CommonWord boli = new CommonWord(0, "Bolígrafo", null, 5);
                        CommonWord lapiz = new CommonWord(1, "Lápiz", null, 4);

                        res.add(boli);
                        res.add(lapiz);
                        break;

                    case "Familia":
                        CommonWord familia = new CommonWord(0, "Familia", null, 10);
                        CommonWord padre = new CommonWord(1, "Padre", null, 9);
                        CommonWord madre = new CommonWord(2, "Madre", null, 9);
                        CommonWord hijo = new CommonWord(3, "Hijo", null, 9);
                        CommonWord hija = new CommonWord(4, "Hija", null, 9);
                        CommonWord marido = new CommonWord(5, "Marido", null, 8);
                        CommonWord mujer = new CommonWord(6, "Mujer", null, 8);
                        CommonWord hermano = new CommonWord(7, "Hermano", null, 7);
                        CommonWord hermana = new CommonWord(8, "Hermana", null, 7);
                        CommonWord abuelo = new CommonWord(9, "Abuelo", null, 6);
                        CommonWord abuela = new CommonWord(10, "Abuela", null, 6);
                        CommonWord nieto = new CommonWord(11, "Nieto", null, 6);
                        CommonWord nieta = new CommonWord(12, "Nieta", null, 6);
                        CommonWord tio = new CommonWord(13, "Tío", null, 5);
                        CommonWord tia = new CommonWord(14, "Tía", null, 5);
                        CommonWord sobrino = new CommonWord(15, "Sobrino", null, 5);
                        CommonWord sobrina = new CommonWord(16, "Sobrina", null, 5);
                        CommonWord primo = new CommonWord(17, "Primo", null, 5);
                        CommonWord prima = new CommonWord(18, "Prima", null, 5);
                        CommonWord cunado = new CommonWord(19, "Cuñado", null, 4);
                        CommonWord cunada = new CommonWord(20, "Cuñada", null, 4);
                        CommonWord suegro = new CommonWord(21, "Suegro", null, 3);
                        CommonWord suegra = new CommonWord(22, "Suegra", null, 3);
                        CommonWord yerno = new CommonWord(23, "Yerno", null, 2);
                        CommonWord nuera = new CommonWord(24, "Nuera", null, 2);

                        res.add(familia);
                        res.add(padre);
                        res.add(madre);
                        res.add(hijo);
                        res.add(hija);
                        res.add(marido);
                        res.add(mujer);
                        res.add(hermano);
                        res.add(hermana);
                        res.add(abuelo);
                        res.add(abuela);
                        res.add(nieto);
                        res.add(nieta);
                        res.add(tio);
                        res.add(tia);
                        res.add(sobrino);
                        res.add(sobrina);
                        res.add(primo);
                        res.add(prima);
                        res.add(cunado);
                        res.add(cunada);
                        res.add(suegro);
                        res.add(suegra);
                        res.add(yerno);
                        res.add(nuera);
                        break;
                }
                break;

            case "Dibujos":
                switch (campoSemantico) {
                    case "Colegio":
                        CommonWord boli = new CommonWord(0, "Bolígrafo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fboli_dibujo.png?alt=media&token=303830d6-689e-4207-a50f-74dc795c34df", "gs://comtietea.appspot.com/images/default/boli_dibujo.png"), 5);
                        CommonWord lapiz = new CommonWord(1, "Lápiz", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Flapiz_dibujo.png?alt=media&token=0283555f-c65c-434f-bda4-5d3e89768ef2", "gs://comtietea.appspot.com/images/default/lapiz_dibujo.png"), 4);

                        res.add(boli);
                        res.add(lapiz);
                        break;

                    case "Familia":
                        CommonWord familia = new CommonWord(0, "Familia", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Ffamilia_dibujo.png?alt=media&token=deba80eb-a62e-4b18-ad8e-a2f0c7de8b0d","gs://comtietea.appspot.com/images/default/familia_dibujo.png"), 10);

                        CommonWord padre = new CommonWord(1, "Padre", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fpadre_dibujo.png?alt=media&token=0d5bc5a1-775b-4200-8e2c-194623e62eb6","gs://comtietea.appspot.com/images/default/padre_dibujo.png"), 9);

                        CommonWord madre = new CommonWord(2, "Madre", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fmadre_dibujo.png?alt=media&token=9dc431ed-362d-4f93-bc13-30ac2c935f7e","gs://comtietea.appspot.com/images/default/madre_dibujo.png"), 9);

                        CommonWord hijo = new CommonWord(3, "Hijo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fhijo_dibujo.png?alt=media&token=385ec5de-b58d-4728-b9ce-2a31c688c590","gs://comtietea.appspot.com/images/default/hijo_dibujo.png"), 9);

                        CommonWord hija = new CommonWord(4, "Hija", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fhija_dibujo.jpg?alt=media&token=dbc92c37-25b8-4609-b3ff-66017d327333","gs://comtietea.appspot.com/images/default/hija_dibujo.jpg"), 9);

                        CommonWord marido = new CommonWord(5, "Marido", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fmarido_dibujo.png?alt=media&token=0c1d99f7-380d-474c-b1f7-9cb2beda7755","gs://comtietea.appspot.com/images/default/marido_dibujo.png"), 8);

                        CommonWord mujer = new CommonWord(6, "Mujer", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fmujer_dibujo.png?alt=media&token=05c71367-48af-49e0-94ef-00c40e5f7071","gs://comtietea.appspot.com/images/default/mujer_dibujo.png"), 8);

                        CommonWord hermano = new CommonWord(7, "Hermano", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fhermano_dibujo.jpg?alt=media&token=5d432bf2-c830-4c7f-a31a-f595874c9e47","gs://comtietea.appspot.com/images/default/hermano_dibujo.jpg"), 7);

                        CommonWord hermana = new CommonWord(8, "Hermana", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fhermana_dibujo.jpg?alt=media&token=2945d751-21d6-4c94-9257-9ce77fb204db","gs://comtietea.appspot.com/images/default/hermana_dibujo.jpg"), 7);

                        CommonWord abuelo = new CommonWord(9, "Abuelo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fabuelo_dibujo.png?alt=media&token=7161db0b-9b56-4b08-ad59-60bab1f02244","gs://comtietea.appspot.com/images/default/abuelo_dibujo.png"), 6);

                        CommonWord abuela = new CommonWord(10, "Abuela", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fabuela_dibujo.png?alt=media&token=390624c2-4045-4297-829c-f63dd9f1d8c0","gs://comtietea.appspot.com/images/default/abuela_dibujo.png"), 6);

                        CommonWord nieto = new CommonWord(11, "Nieto", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fnieto_dibujo.jpg?alt=media&token=a99f4b7c-c9bb-4d22-a3f0-585aea636038", "gs://comtietea.appspot.com/images/default/nieto_dibujo.jpg"), 6);

                        CommonWord nieta = new CommonWord(12, "Nieta", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fnieta_dibujo.jpg?alt=media&token=3ff3a5a0-3e49-4624-adf3-2a954a022e59","gs://comtietea.appspot.com/images/default/nieta_dibujo.jpg"), 6);

                        CommonWord tio = new CommonWord(13, "Tío", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Ftio_dibujo.jpg?alt=media&token=45976433-6bf2-490f-8d14-c9be07f835e8","gs://comtietea.appspot.com/images/default/tio_dibujo.jpg"), 5);

                        CommonWord tia = new CommonWord(14, "Tía", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Ft%C3%ADa_dibujo.png?alt=media&token=be3f028d-c505-4f48-9beb-56e6d9502d44","gs://comtietea.appspot.com/images/default/tía_dibujo.png"), 5);

                        CommonWord sobrino = new CommonWord(15, "Sobrino", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fsobrino_dibujo.png?alt=media&token=afb20299-8990-4b53-9554-6295447a8182","gs://comtietea.appspot.com/images/default/sobrino_dibujo.png"), 5);

                        CommonWord sobrina = new CommonWord(16, "Sobrina", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fsobrina_dibujo.png?alt=media&token=4ae39f2f-fb8c-4ab8-8bda-972af68c739e","gs://comtietea.appspot.com/images/default/sobrina_dibujo.png"), 5);

                        CommonWord primo = new CommonWord(17, "Primo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fprimo_dibujo.jpg?alt=media&token=864e1ae7-d38e-4888-820c-e61ff6888f3f","gs://comtietea.appspot.com/images/default/primo_dibujo.jpg"), 5);

                        CommonWord prima = new CommonWord(18, "Prima", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fprima_dibujo.jpg?alt=media&token=b69d0b93-0347-48a3-a89a-1c5b051cd917","gs://comtietea.appspot.com/images/default/prima_dibujo.jpg"), 5);

                        CommonWord cunado = new CommonWord(19, "Cuñado", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcu%C3%B1ado_dibujo.png?alt=media&token=7db99065-2c5f-4acc-8b47-5c6e83784d8b","gs://comtietea.appspot.com/images/default/cuñado_dibujo.png"), 4);

                        CommonWord cunada = new CommonWord(20, "Cuñada", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcu%C3%B1ada_dibujo.jpg?alt=media&token=61607b85-32ea-4a56-85bb-5c96e6112802","gs://comtietea.appspot.com/images/default/cuñada_dibujo.jpg"), 4);

                        CommonWord suegro = new CommonWord(21, "Suegro", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fsuegro_dibujo.png?alt=media&token=1c9dfc4f-2d43-48d7-9ac6-ba06022f6023", "gs://comtietea.appspot.com/images/default/suegro_dibujo.png"), 3);

                        CommonWord suegra = new CommonWord(22, "Suegra", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fsuegra_dibujo.png?alt=media&token=c70e6b97-93cd-4a82-ba92-a0c20404f370", "gs://comtietea.appspot.com/images/default/suegra_dibujo.png"), 3);

                        CommonWord yerno = new CommonWord(23, "Yerno", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fyerno_dibujo.jpg?alt=media&token=09eddef1-69cb-44e9-8fb3-8de38b6d2f1d","gs://comtietea.appspot.com/images/default/yerno_dibujo.jpg"), 2);

                        CommonWord nuera = new CommonWord(24, "Nuera", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fnuera_dibujo.png?alt=media&token=80d2441f-74c1-408c-8170-3378a35d0b71","gs://comtietea.appspot.com/images/default/nuera_dibujo.png"), 2);

                        res.add(familia);
                        res.add(padre);
                        res.add(madre);
                        res.add(hijo);
                        res.add(hija);
                        res.add(marido);
                        res.add(mujer);
                        res.add(hermano);
                        res.add(hermana);
                        res.add(abuelo);
                        res.add(abuela);
                        res.add(nieto);
                        res.add(nieta);
                        res.add(tio);
                        res.add(tia);
                        res.add(sobrino);
                        res.add(sobrina);
                        res.add(primo);
                        res.add(prima);
                        res.add(cunado);
                        res.add(cunada);
                        res.add(suegro);
                        res.add(suegra);
                        res.add(yerno);
                        res.add(nuera);
                        break;
                }
                break;

            case "Imagenes":
                switch (campoSemantico) {
                    case "Colegio":
                        CommonWord boli = new CommonWord(0, "Bolígrafo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fboli_foto.jpg?alt=media&token=7a74ef05-6294-4853-a3ae-30df3e2ac0e9", "gs://comtietea.appspot.com/images/default/boli_foto.png"), 5);
                        CommonWord lapiz = new CommonWord(1, "Lápiz", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Flapiz_foto.jpg?alt=media&token=7323a398-a15d-4741-a8bd-11dc96ec4384", "gs://comtietea.appspot.com/images/default/lapiz_foto.png"), 4);

                        res.add(boli);
                        res.add(lapiz);
                        break;

                    case "Familia":
                        CommonWord familia = new CommonWord(0, "Familia", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Ffamilia_foto.jpg?alt=media&token=b2b4ca19-611e-48ae-8718-8d206c1f0c13","gs://comtietea.appspot.com/images/default/familia_foto.jpg"), 10);

                        CommonWord padre = new CommonWord(1, "Padre", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fpadre_foto.jpg?alt=media&token=82b2a9b6-2783-4a2f-b53b-2c471d4d6b3d","gs://comtietea.appspot.com/images/default/padre_foto.jpg"), 9);

                        CommonWord madre = new CommonWord(2, "Madre", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fmadre_foto.jpg?alt=media&token=0b37a661-334a-4010-b656-b5a86630f3fb","gs://comtietea.appspot.com/images/default/madre_foto.jpg"), 9);

                        CommonWord hijo = new CommonWord(3, "Hijo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fhijo_foto.jpg?alt=media&token=3052741a-54d5-4ee0-bce3-298a8a298689","gs://comtietea.appspot.com/images/default/hijo_foto.jpg"), 9);

                        CommonWord hija = new CommonWord(4, "Hija", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fhija_foto.jpg?alt=media&token=d2ef7944-cbf2-4237-96d0-d02e2630175b","gs://comtietea.appspot.com/images/default/hija_foto.jpg"), 9);

                        CommonWord marido = new CommonWord(5, "Marido", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fmarido_foto.jpg?alt=media&token=5f36bd43-fad2-40dd-9224-f9e8b321ec98","gs://comtietea.appspot.com/images/default/marido_foto.jpg"), 8);

                        CommonWord mujer = new CommonWord(6, "Mujer", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fmujer_foto.jpg?alt=media&token=3ce9e95a-984a-42b6-b8d3-dc427fd804f2","gs://comtietea.appspot.com/images/default/mujer_foto.jpg"), 8);

                        CommonWord hermano = new CommonWord(7, "Hermano", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fhermano_foto.jpg?alt=media&token=c36ff10e-ed5b-4642-9c7f-50056f70de5d","gs://comtietea.appspot.com/images/default/hermano_foto.jpg"), 7);

                        CommonWord hermana = new CommonWord(8, "Hermana", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fhermana_foto.jpg?alt=media&token=e9f516f0-e808-4a53-bbdd-a3bc7814cb66","gs://comtietea.appspot.com/images/default/hermana_foto.jpg"), 7);

                        CommonWord abuelo = new CommonWord(9, "Abuelo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fabuelo_foto.jpg?alt=media&token=de76399f-4755-44bf-b8a5-ffed5d95cc64","gs://comtietea.appspot.com/images/default/abuelo_foto.jpg"), 6);

                        CommonWord abuela = new CommonWord(10, "Abuela", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fabuela_foto.jpg?alt=media&token=8e683be1-1e48-41ae-8e21-72287c79ade8","gs://comtietea.appspot.com/images/default/abuela_foto.jpg"), 6);

                        CommonWord nieto = new CommonWord(11, "Nieto", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fnieto_foto.jpg?alt=media&token=73827d23-8b7a-4649-92d0-21dea506c208","gs://comtietea.appspot.com/images/default/nieto_foto.jpg"), 6);

                        CommonWord nieta = new CommonWord(12, "Nieta", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fnieta_foto.jpg?alt=media&token=4048d81c-5688-4e34-8ff9-eacc27a408c5","gs://comtietea.appspot.com/images/default/nieta_foto.jpg"), 6);

                        CommonWord tio = new CommonWord(13, "Tío", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2FT%C3%ADo_foto.jpg?alt=media&token=d5549369-59f4-4928-be16-557d697764c0","gs://comtietea.appspot.com/images/default/Tío_foto.jpg"), 5);

                        CommonWord tia = new CommonWord(14, "Tía", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Ft%C3%ADa_foto.jpg?alt=media&token=92d60f3a-981d-405c-9135-c85d216acf64","gs://comtietea.appspot.com/images/default/tía_foto.jpg"), 5);

                        CommonWord sobrino = new CommonWord(15, "Sobrino", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fsobrino_foto.jpg?alt=media&token=3346179e-dc97-4dc3-92a7-68c0b12a5ff2","gs://comtietea.appspot.com/images/default/sobrino_foto.jpg"), 5);

                        CommonWord sobrina = new CommonWord(16, "Sobrina", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fsobrina_foto.jpg?alt=media&token=143619a6-ca1c-4c96-b828-5b0377d6c613","gs://comtietea.appspot.com/images/default/sobrina_foto.jpg"), 5);

                        CommonWord primo = new CommonWord(17, "Primo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fprimo_foto.jpg?alt=media&token=7b566fcd-06c1-4605-99b3-992ba2f6ddc0","gs://comtietea.appspot.com/images/default/primo_foto.jpg"), 5);

                        CommonWord prima = new CommonWord(18, "Prima", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fprima_foto.jpg?alt=media&token=2139c831-7752-44fa-997d-998557596e88","gs://comtietea.appspot.com/images/default/prima_foto.jpg"), 5);

                        CommonWord cunado = new CommonWord(19, "Cuñado", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcu%C3%B1ado_foto.jpg?alt=media&token=672055d2-3691-46a6-9d62-1ad1ac608494","gs://comtietea.appspot.com/images/default/cuñado_foto.jpg"), 4);

                        CommonWord cunada = new CommonWord(20, "Cuñada", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcu%C3%B1ada_foto.jpg?alt=media&token=d0e9e5c7-92a7-4a18-a8b9-a70300e2f8b9","gs://comtietea.appspot.com/images/default/cuñada_foto.jpg"), 4);

                        CommonWord suegro = new CommonWord(21, "Suegro", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fsuegro_foto.jpg?alt=media&token=821ed53f-1c90-40a2-bd20-00cc43ff2593","gs://comtietea.appspot.com/images/default/suegro_foto.jpg"), 3);

                        CommonWord suegra = new CommonWord(22, "Suegra", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fsuegra_foto.jpg?alt=media&token=bfee6757-cc24-4a24-bf81-38a52add2d05","gs://comtietea.appspot.com/images/default/suegra_foto.jpg"), 3);

                        CommonWord yerno = new CommonWord(23, "Yerno", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fyerno_foto.jpg?alt=media&token=719c672c-6cfa-46eb-9f0a-d045703f56b7","gs://comtietea.appspot.com/images/default/yerno_foto.jpg"), 2);

                        CommonWord nuera = new CommonWord(24, "Nuera", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fnuera_foto.jpg?alt=media&token=ad9da982-b582-4954-90ac-996240fcdeb2","gs://comtietea.appspot.com/images/default/nuera_foto.jpg"), 2);

                        res.add(familia);
                        res.add(padre);
                        res.add(madre);
                        res.add(hijo);
                        res.add(hija);
                        res.add(marido);
                        res.add(mujer);
                        res.add(hermano);
                        res.add(hermana);
                        res.add(abuelo);
                        res.add(abuela);
                        res.add(nieto);
                        res.add(nieta);
                        res.add(tio);
                        res.add(tia);
                        res.add(sobrino);
                        res.add(sobrina);
                        res.add(primo);
                        res.add(prima);
                        res.add(cunado);
                        res.add(cunada);
                        res.add(suegro);
                        res.add(suegra);
                        res.add(yerno);
                        res.add(nuera);
                        break;
                }
                break;
        }

        return res;
    }
}
