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
                        CommonWord papa = new CommonWord(0, "Papá", null, 10);
                        CommonWord primo = new CommonWord(1, "Primo", null, 3);

                        res.add(papa);
                        res.add(primo);
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
                        CommonWord papa = new CommonWord(0, "Papá", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fpadre_dibujo.png?alt=media&token=0d5bc5a1-775b-4200-8e2c-194623e62eb6", "gs://comtietea.appspot.com/images/default/padre_dibujo.png"), 10);
                        CommonWord primo = new CommonWord(1, "Primo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fprimo_dibujo.jpg?alt=media&token=864e1ae7-d38e-4888-820c-e61ff6888f3f", "gs://comtietea.appspot.com/images/default/primo_dibujo.png"), 3);

                        res.add(papa);
                        res.add(primo);
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
                        CommonWord papa = new CommonWord(0, "Papá", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fpadre_foto.jpg?alt=media&token=82b2a9b6-2783-4a2f-b53b-2c471d4d6b3d", "gs://comtietea.appspot.com/images/default/padre_foto.png"), 10);
                        CommonWord primo = new CommonWord(1, "Primo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fprimo_foto.jpg?alt=media&token=7b566fcd-06c1-4605-99b3-992ba2f6ddc0", "gs://comtietea.appspot.com/images/default/primo_foto.png"), 3);

                        res.add(papa);
                        res.add(primo);
                        break;
                }
                break;
        }

        return res;
    }
}
