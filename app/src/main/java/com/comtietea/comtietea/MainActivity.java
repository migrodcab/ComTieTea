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
                    uid = user.getUid();
                    dbRef.orderByChild("uid").equalTo(uid).limitToFirst(1).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if(dataSnapshot.getChildrenCount() == 0) {
                                List<SymbolicCode> codigosSimbolicos = null;

                                codigosSimbolicos = cargaCodigosSimbolicos();

                                final User u = new User(user.getDisplayName(), user.getEmail(), user.getUid(), codigosSimbolicos);
                                dbRef.child(uid).setValue(u);
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
            case R.id.action_info:
                Intent intent2 = new Intent(this, AcercaDeActivity.class);
                startActivity(intent2);
                return super.onOptionsItemSelected(item);
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
                colegio = new SemanticField(2, "Colegio", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcolegio_dibujo.jpg?alt=media&token=a7d3dba3-8a71-458d-922c-7be55fa23e75", "gs://comtietea.appspot.com/images/default/colegio_dibujo.jpg"), 8, colorColegio, palabrasColegio);

                palabrasCasa = cargaPalabrasHabituales(codigoSimbolico, "Casa");
                casa = new SemanticField(3, "Casa", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcasa_dibujo.png?alt=media&token=171df0d1-b4fa-4675-941d-abf9c0b3d772","gs://comtietea.appspot.com/images/default/casa_dibujo.png"), 7, colorCasa, palabrasCasa);

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
                comida = new SemanticField(10, "Comida", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcomida_dibujo.png?alt=media&token=dbc6051c-438f-4d60-a476-50baf7297129","gs://comtietea.appspot.com/images/default/comida_dibujo.png"), 4, colorComida, palabrasComida);

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
                        CommonWord colegio = new CommonWord(0, "Colegio", null, 10);
                        CommonWord profesor = new CommonWord(1, "Profesor", null, 9);
                        CommonWord companero = new CommonWord(2, "Compañero", null, 9);
                        CommonWord director = new CommonWord(3, "Director", null, 9);
                        CommonWord recreo = new CommonWord(4, "Recreo", null, 8);
                        CommonWord gimnasio = new CommonWord(5, "Gimnasio", null, 8);
                        CommonWord lengua = new CommonWord(6, "Lengua", null, 7);
                        CommonWord mates = new CommonWord(7, "Matemáticas", null, 7);
                        CommonWord biologia = new CommonWord(8, "Biología", null, 7);
                        CommonWord musica = new CommonWord(9, "Música", null, 7);
                        CommonWord ingles = new CommonWord(10, "Inglés", null, 7);
                        CommonWord educacionFisica = new CommonWord(11, "Educación física", null, 7);
                        CommonWord pizarra = new CommonWord(12, "Pizarra", null, 6);
                        CommonWord mesa = new CommonWord(13, "Mesa", null, 6);
                        CommonWord silla = new CommonWord(14, "Silla", null, 6);
                        CommonWord libro = new CommonWord(15, "Libro", null, 5);
                        CommonWord cuaderno = new CommonWord(16, "Cuaderno", null, 5);
                        CommonWord diccionario = new CommonWord(17, "Diccionario", null, 5);
                        CommonWord estuche = new CommonWord(18, "Estuche", null, 4);
                        CommonWord mochila = new CommonWord(19, "Mochila", null, 4);
                        CommonWord boli = new CommonWord(20, "Bolígrafo", null, 3);
                        CommonWord lapiz = new CommonWord(21, "Lápiz", null, 3);
                        CommonWord colores = new CommonWord(22, "Colores", null, 2);
                        CommonWord goma = new CommonWord(23, "Goma", null, 2);
                        CommonWord sacapuntas = new CommonWord(24, "Sacapuntas", null, 2);
                        CommonWord rotuladores = new CommonWord(25, "Rotuladores", null, 2);
                        CommonWord clase = new CommonWord(26, "Clase", null, 1);

                        res.add(colegio);
                        res.add(profesor);
                        res.add(companero);
                        res.add(director);
                        res.add(recreo);
                        res.add(gimnasio);
                        res.add(lengua);
                        res.add(mates);
                        res.add(biologia);
                        res.add(musica);
                        res.add(ingles);
                        res.add(educacionFisica);
                        res.add(pizarra);
                        res.add(mesa);
                        res.add(silla);
                        res.add(libro);
                        res.add(cuaderno);
                        res.add(diccionario);
                        res.add(estuche);
                        res.add(mochila);
                        res.add(boli);
                        res.add(lapiz);
                        res.add(colores);
                        res.add(goma);
                        res.add(sacapuntas);
                        res.add(rotuladores);
                        res.add(clase);
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

                    case "Acciones":
                        CommonWord accion = new CommonWord(0, "Acción", null, 10);
                        CommonWord beber = new CommonWord(1, "Beber", null, 9);
                        CommonWord comer = new CommonWord(2, "Comer", null, 9);
                        CommonWord despertar = new CommonWord(3, "Despertar", null, 9);
                        CommonWord levantar = new CommonWord(4, "Levantar", null, 9);
                        CommonWord dormir = new CommonWord(5, "Dormir", null, 9);
                        CommonWord jugar = new CommonWord(6, "Jugar", null, 8);
                        CommonWord trabajar = new CommonWord(7, "Trabajar", null, 8);
                        CommonWord estudiar = new CommonWord(8, "Estudiar", null, 8);
                        CommonWord sentar = new CommonWord(9, "Sentar", null, 7);
                        CommonWord pedir = new CommonWord(10, "Pedir", null, 7);
                        CommonWord ver = new CommonWord(11, "Ver", null, 6);
                        CommonWord escuchar = new CommonWord(12, "Escuchar", null, 6);
                        CommonWord hablar = new CommonWord(13, "Hablar", null, 6);
                        CommonWord oler = new CommonWord(14, "Oler", null, 6);
                        CommonWord leer = new CommonWord(15, "Leer", null, 5);
                        CommonWord escribir = new CommonWord(16, "Escribir", null, 5);
                        CommonWord dibujar = new CommonWord(17, "Dibujar", null, 5);
                        CommonWord pintar = new CommonWord(18, "Pintar", null, 5);
                        CommonWord morir = new CommonWord(19, "Morir", null, 4);
                        CommonWord nacer = new CommonWord(20, "Nacer", null, 4);
                        CommonWord correr = new CommonWord(21, "Correr", null, 4);
                        CommonWord ensenar = new CommonWord(22, "Enseñar", null, 4);
                        CommonWord dudar = new CommonWord(23, "Dudar", null, 4);
                        CommonWord peinar = new CommonWord(24, "Peinar", null, 3);
                        CommonWord secar = new CommonWord(25, "Secar", null, 3);
                        CommonWord cepillarDientes = new CommonWord(26, "Cepillarse los dientes", null, 3);
                        CommonWord lavar = new CommonWord(27, "Lavar", null, 3);
                        CommonWord limpiar = new CommonWord(28, "Limpiar", null, 3);
                        CommonWord ir = new CommonWord(29, "Ir", null, 2);
                        CommonWord pensar = new CommonWord(30, "Pensar", null, 2);
                        CommonWord abrazar = new CommonWord(31, "Abrazar", null, 1);
                        CommonWord besar = new CommonWord(32, "Besar", null, 1);
                        CommonWord querer = new CommonWord(33, "Querer", null, 1);

                        res.add(accion);
                        res.add(beber);
                        res.add(comer);
                        res.add(despertar);
                        res.add(levantar);
                        res.add(dormir);
                        res.add(jugar);
                        res.add(trabajar);
                        res.add(estudiar);
                        res.add(sentar);
                        res.add(pedir);
                        res.add(ver);
                        res.add(escuchar);
                        res.add(hablar);
                        res.add(oler);
                        res.add(leer);
                        res.add(escribir);
                        res.add(dibujar);
                        res.add(pintar);
                        res.add(morir);
                        res.add(nacer);
                        res.add(correr);
                        res.add(ensenar);
                        res.add(dudar);
                        res.add(peinar);
                        res.add(secar);
                        res.add(cepillarDientes);
                        res.add(lavar);
                        res.add(limpiar);
                        res.add(ir);
                        res.add(pensar);
                        res.add(abrazar);
                        res.add(besar);
                        res.add(querer);
                        break;

                    case "Casa":
                        CommonWord casa = new CommonWord(0, "Casa", null, 10);
                        CommonWord cocina = new CommonWord(1, "Cocina", null, 9);
                        CommonWord bano = new CommonWord(2, "Baño", null, 8);
                        CommonWord dormitorio = new CommonWord(3, "Dormitorio", null, 7);
                        CommonWord salon = new CommonWord(4, "Salón", null, 6);
                        CommonWord comedor = new CommonWord(5, "Comedor", null, 5);
                        CommonWord jardin = new CommonWord(6, "Jardín", null, 4);
                        CommonWord balcon = new CommonWord(7, "Balcón", null, 3);

                        res.add(casa);
                        res.add(cocina);
                        res.add(bano);
                        res.add(dormitorio);
                        res.add(salon);
                        res.add(comedor);
                        res.add(jardin);
                        res.add(balcon);
                        break;

                    case "Ocio":
                        CommonWord ocio = new CommonWord(0, "Ocio", null, 10);
                        CommonWord videojuego = new CommonWord(1, "Videojuego", null, 9);
                        CommonWord juegoMesa = new CommonWord(2, "Juego de mesa", null, 9);
                        CommonWord juguete = new CommonWord(3, "Juguete", null, 8);
                        CommonWord pelota = new CommonWord(4, "Pelota", null, 8);
                        CommonWord cine = new CommonWord(5, "Cine", null, 7);
                        CommonWord escucharMusica = new CommonWord(6, "Escuchar música", null, 7);
                        CommonWord baile = new CommonWord(7, "Baile", null, 7);
                        CommonWord teatro = new CommonWord(8, "Teatro", null, 7);
                        CommonWord lectura = new CommonWord(9, "Lectura", null, 6);
                        CommonWord viajar = new CommonWord(10, "Viajar", null, 6);
                        CommonWord deporte = new CommonWord(11, "Deporte", null, 5);
                        CommonWord futbol = new CommonWord(12, "Fútbol", null, 4);
                        CommonWord baloncesto = new CommonWord(13, "Baloncesto", null, 4);
                        CommonWord tenis = new CommonWord(14, "Tenis", null, 4);
                        CommonWord gimnasia = new CommonWord(15, "Gimnasia", null, 3);
                        CommonWord padel = new CommonWord(16, "Pádel", null, 3);
                        CommonWord ciclismo = new CommonWord(17, "Ciclismo", null, 3);
                        CommonWord natacion = new CommonWord(18, "Natación", null, 3);
                        CommonWord parque = new CommonWord(19, "Parque", null, 2);

                        res.add(ocio);
                        res.add(videojuego);
                        res.add(juegoMesa);
                        res.add(juguete);
                        res.add(pelota);
                        res.add(cine);
                        res.add(escucharMusica);
                        res.add(baile);
                        res.add(teatro);
                        res.add(lectura);
                        res.add(viajar);
                        res.add(deporte);
                        res.add(futbol);
                        res.add(baloncesto);
                        res.add(tenis);
                        res.add(gimnasia);
                        res.add(padel);
                        res.add(ciclismo);
                        res.add(natacion);
                        res.add(parque);
                        break;

                    case "Animales":
                        CommonWord animal = new CommonWord(0, "Animal", null, 10);
                        CommonWord perro = new CommonWord(1, "Perro", null, 9);
                        CommonWord gato = new CommonWord(2, "Gato", null, 9);
                        CommonWord conejo = new CommonWord(3, "Conejo", null, 8);
                        CommonWord pajaro = new CommonWord(4, "Pájaro", null, 8);
                        CommonWord pez = new CommonWord(5, "Pez", null, 8);
                        CommonWord hamster = new CommonWord(6, "Hamster", null, 8);
                        CommonWord leon = new CommonWord(7, "León", null, 7);
                        CommonWord tigre = new CommonWord(8, "Tigre", null, 7);
                        CommonWord jirafa = new CommonWord(9, "Jirafa", null, 7);
                        CommonWord cebra = new CommonWord(10, "Cebra", null, 6);
                        CommonWord oso = new CommonWord(11, "Oso", null, 6);
                        CommonWord serpiente = new CommonWord(12, "Serpiente", null, 6);
                        CommonWord mariposa = new CommonWord(13, "Mariposa", null, 5);
                        CommonWord mosquito = new CommonWord(14, "Mosquito", null, 5);
                        CommonWord arana = new CommonWord(15, "Araa", null, 5);
                        CommonWord tiburon = new CommonWord(16, "Tiburón", null, 4);
                        CommonWord ballena = new CommonWord(17, "Ballena", null, 4);
                        CommonWord delfin = new CommonWord(18, "Delfín", null, 4);
                        CommonWord caballo = new CommonWord(19, "Caballo", null, 3);
                        CommonWord cerdo = new CommonWord (20, "Cerdo", null, 3);
                        CommonWord gallina = new CommonWord(21, "Gallina", null, 3);
                        CommonWord oveja = new CommonWord(22, "Oveja", null, 3);
                        CommonWord vaca = new CommonWord(23, "Vaca", null, 3);
                        CommonWord elefante = new CommonWord(24, "Elefante", null, 2);
                        CommonWord mono = new CommonWord(25, "Mono", null, 2);

                        res.add(animal);
                        res.add(perro);
                        res.add(gato);
                        res.add(conejo);
                        res.add(pajaro);
                        res.add(pez);
                        res.add(hamster);
                        res.add(leon);
                        res.add(tigre);
                        res.add(jirafa);
                        res.add(cebra);
                        res.add(oso);
                        res.add(serpiente);
                        res.add(mariposa);
                        res.add(mosquito);
                        res.add(arana);
                        res.add(tiburon);
                        res.add(ballena);
                        res.add(delfin);
                        res.add(caballo);
                        res.add(cerdo);
                        res.add(gallina);
                        res.add(oveja);
                        res.add(vaca);
                        res.add(elefante);
                        res.add(mono);
                        break;

                    case "Colores":
                        CommonWord color = new CommonWord(0, "Color", null, 10);
                        CommonWord negro = new CommonWord(1, "Negro", null, 9);
                        CommonWord blanco = new CommonWord(2, "Blanco", null, 9);
                        CommonWord azul = new CommonWord(3, "Azul", null, 8);
                        CommonWord verde = new CommonWord(4, "Verde", null, 8);
                        CommonWord rosa = new CommonWord(5, "Rosa", null, 7);
                        CommonWord rojo = new CommonWord(6, "Rojo", null, 7);
                        CommonWord amarillo = new CommonWord(7, "Amarillo", null, 6);
                        CommonWord naranja = new CommonWord(8, "Naranja", null, 5);
                        CommonWord marron = new CommonWord(9, "Marrón", null, 4);
                        CommonWord morado = new CommonWord(10, "Morado", null, 3);
                        CommonWord gris = new CommonWord(11, "Gris", null, 2);

                        res.add(color);
                        res.add(negro);
                        res.add(blanco);
                        res.add(azul);
                        res.add(verde);
                        res.add(rosa);
                        res.add(rojo);
                        res.add(amarillo);
                        res.add(naranja);
                        res.add(marron);
                        res.add(morado);
                        res.add(gris);
                        break;

                    case "Adjetivos":
                        CommonWord adjetivo = new CommonWord(0, "Adjetivo", null, 10);
                        CommonWord grande = new CommonWord(1, "Grande", null, 9);
                        CommonWord pequeno = new CommonWord(2, "Pequeño", null, 9);
                        CommonWord alto = new CommonWord(3, "Alto", null, 8);
                        CommonWord bajo = new CommonWord(4, "Bajo", null, 8);
                        CommonWord largo = new CommonWord(5, "Largo", null, 7);
                        CommonWord corto = new CommonWord(6, "Corto", null, 7);
                        CommonWord gordo = new CommonWord(7, "Gordo", null, 6);
                        CommonWord delgado = new CommonWord(8, "Delgado", null, 6);
                        CommonWord pesado = new CommonWord(9, "Pesado", null, 5);
                        CommonWord ligero = new CommonWord(10, "Ligero", null, 5);
                        CommonWord claro = new CommonWord(11, "Claro", null, 4);
                        CommonWord oscuro = new CommonWord(12, "Oscuro", null, 4);
                        CommonWord viejo = new CommonWord(13, "Viejo", null, 3);
                        CommonWord nuevo = new CommonWord(14, "Nuevo", null, 3);
                        CommonWord joven = new CommonWord(15, "Joven", null, 3);
                        CommonWord antiguo = new CommonWord(16, "Antiguo", null, 3);
                        CommonWord liso = new CommonWord(17, "Liso", null, 2);
                        CommonWord rugoso = new CommonWord(18, "Rugoso", null, 2);
                        CommonWord feo = new CommonWord(19, "Feo", null, 1);
                        CommonWord guapo = new CommonWord (20, "Guapo", null, 1);

                        res.add(adjetivo);
                        res.add(grande);
                        res.add(pequeno);
                        res.add(alto);
                        res.add(bajo);
                        res.add(largo);
                        res.add(corto);
                        res.add(gordo);
                        res.add(delgado);
                        res.add(pesado);
                        res.add(ligero);
                        res.add(claro);
                        res.add(oscuro);
                        res.add(viejo);
                        res.add(nuevo);
                        res.add(joven);
                        res.add(antiguo);
                        res.add(liso);
                        res.add(rugoso);
                        res.add(feo);
                        res.add(guapo);
                        break;

                    case "Emociones":
                        CommonWord emocion = new CommonWord(0, "Emoción", null, 10);
                        CommonWord tristeza = new CommonWord(1, "Tristeza", null, 9);
                        CommonWord felicidad = new CommonWord(2, "Felicidad", null, 8);
                        CommonWord miedo = new CommonWord(3, "Miedo", null, 7);
                        CommonWord asco = new CommonWord(4, "Asco", null, 6);
                        CommonWord enfado = new CommonWord(5, "Enfado", null, 5);
                        CommonWord sorpresa = new CommonWord(6, "Sorpresa", null, 4);
                        CommonWord amor = new CommonWord(7, "Amor", null, 3);

                        res.add(emocion);
                        res.add(tristeza);
                        res.add(felicidad);
                        res.add(miedo);
                        res.add(asco);
                        res.add(enfado);
                        res.add(sorpresa);
                        res.add(amor);
                        break;

                    case "Profesiones":
                        CommonWord profesion = new CommonWord(0, "Profesión", null, 10);
                        CommonWord profesora = new CommonWord(1, "Profesora", null, 9);
                        CommonWord medico = new CommonWord(2, "Médico", null, 9);
                        CommonWord policia = new CommonWord(3, "Policía", null, 9);
                        CommonWord bombero = new CommonWord(4, "Bombero", null, 8);
                        CommonWord camarero = new CommonWord(5, "Camarero", null, 7);
                        CommonWord electricista = new CommonWord(6, "Electricista", null, 6);
                        CommonWord enfermero = new CommonWord(7, "Enfermero", null, 5);
                        CommonWord psicologo = new CommonWord(8, "Psicólogo", null, 4);
                        CommonWord abogado = new CommonWord(9, "Abogado", null, 3);
                        CommonWord cocinero = new CommonWord(10, "Cocinero", null, 2);

                        res.add(profesion);
                        res.add(profesora);
                        res.add(medico);
                        res.add(policia);
                        res.add(bombero);
                        res.add(camarero);
                        res.add(electricista);
                        res.add(enfermero);
                        res.add(psicologo);
                        res.add(abogado);
                        res.add(cocinero);
                        break;

                    case "Comida":
                        CommonWord comida = new CommonWord(0, "Comida", null, 10);
                        CommonWord verdura = new CommonWord(1, "Verdura", null, 9);
                        CommonWord tomate = new CommonWord(2, "Tomate", null, 8);
                        CommonWord lechuga = new CommonWord(3, "Lechuga", null, 8);
                        CommonWord cebolla = new CommonWord(4, "Cebolla", null, 8);
                        CommonWord patatas = new CommonWord(5, "Patatas", null, 8);
                        CommonWord fruta = new CommonWord(6, "Fruta", null, 7);
                        CommonWord manzana = new CommonWord(7, "Manzana", null, 6);
                        CommonWord pera = new CommonWord(8, "Pera", null, 6);
                        CommonWord platano = new CommonWord(9, "Plátano", null, 6);
                        CommonWord fresa = new CommonWord(10, "Fresa", null, 6);
                        CommonWord melon = new CommonWord(11, "Melón", null, 6);
                        CommonWord sandia = new CommonWord(12, "Sandía", null, 6);
                        CommonWord cereza = new CommonWord(13, "Cereza", null, 6);
                        CommonWord pina = new CommonWord(14, "Piña", null, 6);
                        CommonWord pescado = new CommonWord(15, "Pescado", null, 5);
                        CommonWord carne = new CommonWord(16, "Carne", null, 4);
                        CommonWord pollo = new CommonWord(17, "Pollo", null, 3);
                        CommonWord filete = new CommonWord(18, "Filete", null, 3);
                        CommonWord hamburguesa = new CommonWord(19, "Hamburguesa", null, 3);
                        CommonWord salchicha = new CommonWord(20, "Salchicha", null, 3);
                        CommonWord bebida = new CommonWord(21, "Bebida", null, 2);
                        CommonWord agua = new CommonWord(22, "Agua", null, 2);
                        CommonWord cocacola = new CommonWord(23, "Coca-Cola", null, 2);
                        CommonWord refresco = new CommonWord(24, "Refresco", null, 2);
                        CommonWord zumo = new CommonWord(25, "Zumo", null, 2);
                        CommonWord leche = new CommonWord(26, "Leche", null, 2);
                        CommonWord batido = new CommonWord(27, "Batido", null, 2);
                        CommonWord colacao = new CommonWord(28, "Cola-Cao", null, 2);
                        CommonWord cafe = new CommonWord(29, "Café", null, 2);
                        CommonWord cerveza = new CommonWord(30, "Cerveza", null, 2);
                        CommonWord vino = new CommonWord(31, "Vino", null, 2);
                        CommonWord pizza = new CommonWord(32, "Pizza", null, 1);
                        CommonWord bocadillo = new CommonWord(33, "Bocadillo", null, 1);
                        CommonWord jamon = new CommonWord(34, "Jamón", null, 1);
                        CommonWord embutido = new CommonWord(35, "Embutido", null, 1);
                        CommonWord tortilla = new CommonWord(36, "Tortilla", null, 1);
                        CommonWord pasta = new CommonWord(37, "Pasta", null, 1);
                        CommonWord queso = new CommonWord(38, "Queso", null, 1);
                        CommonWord cereales = new CommonWord(39, "Cereales", null, 1);
                        CommonWord sopa = new CommonWord(40, "Sopa", null, 1);
                        CommonWord arroz = new CommonWord(41, "Arroz", null, 1);
                        CommonWord lentejas = new CommonWord(42, "Lentejas", null, 1);
                        CommonWord chocolate = new CommonWord(43, "Chocolate", null, 1);
                        CommonWord huevosFritos = new CommonWord(44, "Huevos fritos", null, 1);
                        CommonWord paella = new CommonWord(45, "Paella", null, 1);

                        res.add(comida);
                        res.add(verdura);
                        res.add(tomate);
                        res.add(lechuga);
                        res.add(cebolla);
                        res.add(patatas);
                        res.add(fruta);
                        res.add(manzana);
                        res.add(pera);
                        res.add(platano);
                        res.add(fresa);
                        res.add(melon);
                        res.add(sandia);
                        res.add(cereza);
                        res.add(pina);
                        res.add(pescado);
                        res.add(carne);
                        res.add(pollo);
                        res.add(filete);
                        res.add(hamburguesa);
                        res.add(salchicha);
                        res.add(bebida);
                        res.add(agua);
                        res.add(cocacola);
                        res.add(refresco);
                        res.add(zumo);
                        res.add(leche);
                        res.add(batido);
                        res.add(colacao);
                        res.add(cafe);
                        res.add(cerveza);
                        res.add(vino);
                        res.add(pizza);
                        res.add(bocadillo);
                        res.add(jamon);
                        res.add(embutido);
                        res.add(tortilla);
                        res.add(pasta);
                        res.add(queso);
                        res.add(cereales);
                        res.add(sopa);
                        res.add(arroz);
                        res.add(lentejas);
                        res.add(chocolate);
                        res.add(huevosFritos);
                        res.add(paella);
                        break;

                    case "Aseo":
                        CommonWord aseo = new CommonWord(0, "Aseo", null, 10);
                        CommonWord ducha = new CommonWord(1, "Ducha", null, 9);
                        CommonWord banera = new CommonWord(2, "Bañera", null, 9);
                        CommonWord wc = new CommonWord(3, "WC", null, 8);
                        CommonWord peine = new CommonWord(4, "Peine", null, 7);
                        CommonWord cepilloDientes = new CommonWord(5, "Cepillo de dientes", null, 7);
                        CommonWord toalla = new CommonWord(6, "Toalla", null, 6);
                        CommonWord papelHigienico = new CommonWord(7, "Papel higiénico", null, 5);
                        CommonWord toallitas = new CommonWord(8, "Toallitas", null, 5);
                        CommonWord maquillaje = new CommonWord(9, "Maquillaje", null, 4);
                        CommonWord ducharse = new CommonWord(10, "Ducharse", null, 3);
                        CommonWord cepillarseDientes = new CommonWord(11, "Cepillarse los dientes", null, 3);

                        res.add(aseo);
                        res.add(ducha);
                        res.add(banera);
                        res.add(wc);
                        res.add(peine);
                        res.add(cepilloDientes);
                        res.add(toalla);
                        res.add(papelHigienico);
                        res.add(toallitas);
                        res.add(maquillaje);
                        res.add(ducharse);
                        res.add(cepillarseDientes);
                        break;

                    case "Estaciones y Tiempo":
                        CommonWord estaciones = new CommonWord(0, "Estaciones", null, 10);
                        CommonWord primavera = new CommonWord(1, "Primavera", null, 9);
                        CommonWord verano = new CommonWord(2, "Verano", null, 9);
                        CommonWord otono = new CommonWord(3, "Otoño", null, 9);
                        CommonWord invierno = new CommonWord(4, "Invierno", null, 9);
                        CommonWord tiempo = new CommonWord(5, "Tiempo", null, 8);
                        CommonWord frio = new CommonWord(6, "Frío", null, 7);
                        CommonWord calor = new CommonWord(7, "Calor", null, 7);
                        CommonWord sol = new CommonWord(8, "Sol", null, 6);
                        CommonWord nube = new CommonWord(9, "Nube", null, 5);
                        CommonWord lluvia = new CommonWord(10, "Lluvia", null, 4);
                        CommonWord tormenta = new CommonWord(11, "Tormenta", null, 3);
                        CommonWord nieve = new CommonWord(12, "Nieve", null, 2);

                        res.add(estaciones);
                        res.add(primavera);
                        res.add(verano);
                        res.add(otono);
                        res.add(invierno);
                        res.add(tiempo);
                        res.add(frio);
                        res.add(calor);
                        res.add(sol);
                        res.add(nube);
                        res.add(lluvia);
                        res.add(tormenta);
                        res.add(nieve);
                        break;

                    case "Ropa":
                        CommonWord ropa = new CommonWord(0, "Ropa", null, 10);
                        CommonWord camiseta = new CommonWord(1, "Camiseta", null, 9);
                        CommonWord camisa = new CommonWord(2, "Camisa", null, 9);
                        CommonWord pantalon = new CommonWord(3, "Pantalón", null, 9);
                        CommonWord falda = new CommonWord(4, "Falda", null, 8);
                        CommonWord vestido = new CommonWord(5, "Vestido", null, 8);
                        CommonWord sudadera = new CommonWord(6, "Sudadera", null, 8);
                        CommonWord jersey = new CommonWord(7, "Jersey", null, 7);
                        CommonWord chaqueta = new CommonWord(8, "Chaqueta", null, 7);
                        CommonWord abrigo = new CommonWord(9, "Abrigo", null, 7);
                        CommonWord chandal = new CommonWord(10, "Chándal", null, 6);
                        CommonWord pijama = new CommonWord(11, "Pijama", null, 6);
                        CommonWord bragas = new CommonWord(12, "Bragas", null, 5);
                        CommonWord sujetador = new CommonWord(13, "Sujetador", null, 5);
                        CommonWord calzoncillos = new CommonWord(14, "Calzoncillos", null, 5);
                        CommonWord calcetines = new CommonWord(15, "Calcetines", null, 4);
                        CommonWord zapatos = new CommonWord(16, "Zapatos", null, 4);
                        CommonWord chanclas = new CommonWord(17, "Chanclas", null, 3);
                        CommonWord zapatosTacon = new CommonWord(18, "Zapatos de tacón", null, 3);
                        CommonWord zapatillas = new CommonWord(19, "Zapatillas", null, 3);
                        CommonWord deportes = new CommonWord(20, "Deportes", null, 2);
                        CommonWord sandalias = new CommonWord(21, "Sandalias", null, 2);
                        CommonWord bufanda = new CommonWord(22, "Bufanda", null, 1);
                        CommonWord gorro = new CommonWord(23, "Gorro", null, 1);
                        CommonWord guantes = new CommonWord(24, "Guantes", null, 1);

                        res.add(ropa);
                        res.add(camiseta);
                        res.add(camisa);
                        res.add(pantalon);
                        res.add(falda);
                        res.add(vestido);
                        res.add(sudadera);
                        res.add(jersey);
                        res.add(chaqueta);
                        res.add(abrigo);
                        res.add(chandal);
                        res.add(pijama);
                        res.add(bragas);
                        res.add(sujetador);
                        res.add(calzoncillos);
                        res.add(calcetines);
                        res.add(zapatos);
                        res.add(chanclas);
                        res.add(zapatosTacon);
                        res.add(zapatillas);
                        res.add(deportes);
                        res.add(sandalias);
                        res.add(bufanda);
                        res.add(gorro);
                        res.add(guantes);
                        break;

                    case "Salud y Cuerpo Humano":
                        CommonWord salud = new CommonWord(0, "Salud", null, 10);
                        CommonWord sano = new CommonWord(1, "Sano", null, 9);
                        CommonWord enfermo = new CommonWord(2, "Enfermo", null, 9);
                        CommonWord dolor = new CommonWord(3, "Dolor", null, 8);
                        CommonWord resfriado = new CommonWord(4, "Resfriado", null, 8);
                        CommonWord fiebre = new CommonWord(5, "Fiebre", null, 8);
                        CommonWord vomito = new CommonWord(6, "Vómito", null, 8);
                        CommonWord sangre = new CommonWord(7, "Sangre", null, 8);
                        CommonWord hospital = new CommonWord(8, "Hospital", null, 7);
                        CommonWord ambulancia = new CommonWord(9, "Ambulancia", null, 7);
                        CommonWord medicamento = new CommonWord(10, "Medicamento", null, 6);
                        CommonWord pastilla = new CommonWord(11, "Pastilla", null, 6);
                        CommonWord jarabe = new CommonWord(12, "Jarabe", null, 6);
                        CommonWord venda = new CommonWord(13, "Venda", null, 5);
                        CommonWord cuerpo = new CommonWord(14, "Cuerpo", null, 4);
                        CommonWord cabeza = new CommonWord(15, "Cabeza", null, 3);
                        CommonWord cuello = new CommonWord(16, "Cuello", null, 3);
                        CommonWord boca = new CommonWord(17, "Boca", null, 3);
                        CommonWord nariz = new CommonWord(18, "Nariz", null, 3);
                        CommonWord ojo = new CommonWord(19, "Ojo", null, 3);
                        CommonWord oreja = new CommonWord(20, "Oreja", null, 3);
                        CommonWord brazo = new CommonWord(21, "Brazo", null, 2);
                        CommonWord codo = new CommonWord(22, "Codo", null, 2);
                        CommonWord mano = new CommonWord(23, "Mano", null, 2);
                        CommonWord muneca = new CommonWord(24, "Muñeca", null, 2);
                        CommonWord dedo = new CommonWord(25, "Dedo", null, 2);
                        CommonWord piernas = new CommonWord(26, "Piernas", null, 2);
                        CommonWord rodilla = new CommonWord(27, "Rodilla", null, 2);
                        CommonWord pie = new CommonWord(28, "Pie", null, 2);
                        CommonWord tobillo = new CommonWord(29, "Tobillo", null, 2);
                        CommonWord hombro = new CommonWord(30, "Hombro", null, 2);
                        CommonWord espalda = new CommonWord(31, "Espalda", null, 2);
                        CommonWord barriga = new CommonWord(32, "Barriga", null, 2);

                        res.add(salud);
                        res.add(sano);
                        res.add(enfermo);
                        res.add(dolor);
                        res.add(resfriado);
                        res.add(fiebre);
                        res.add(vomito);
                        res.add(sangre);
                        res.add(hospital);
                        res.add(ambulancia);
                        res.add(medicamento);
                        res.add(pastilla);
                        res.add(jarabe);
                        res.add(venda);
                        res.add(cuerpo);
                        res.add(cabeza);
                        res.add(cuello);
                        res.add(boca);
                        res.add(nariz);
                        res.add(ojo);
                        res.add(oreja);
                        res.add(brazo);
                        res.add(codo);
                        res.add(mano);
                        res.add(muneca);
                        res.add(dedo);
                        res.add(piernas);
                        res.add(rodilla);
                        res.add(pie);
                        res.add(tobillo);
                        res.add(hombro);
                        res.add(espalda);
                        res.add(barriga);
                        break;

                    case "Transportes":
                        CommonWord transporte = new CommonWord(0, "Transporte", null, 10);
                        CommonWord coche = new CommonWord(1, "Coche", null, 9);
                        CommonWord moto = new CommonWord(2, "Moto", null, 9);
                        CommonWord camion = new CommonWord(3, "Camión", null, 8);
                        CommonWord furgoneta = new CommonWord(4, "Furgoneta", null, 8);
                        CommonWord avion = new CommonWord(5, "Avión", null, 7);
                        CommonWord barco = new CommonWord(6, "Barco", null, 6);
                        CommonWord bicicleta = new CommonWord(7, "Bicicleta", null, 5);
                        CommonWord tractor = new CommonWord(8, "Tractor", null, 4);
                        CommonWord helicoptero = new CommonWord(9, "Helicóptero", null, 3);
                        CommonWord autobus = new CommonWord(10, "Autobús", null, 2);
                        CommonWord tren = new CommonWord(11, "Tren", null, 1);

                        res.add(transporte);
                        res.add(coche);
                        res.add(moto);
                        res.add(camion);
                        res.add(furgoneta);
                        res.add(avion);
                        res.add(barco);
                        res.add(bicicleta);
                        res.add(tractor);
                        res.add(helicoptero);
                        res.add(autobus);
                        res.add(tren);
                        break;
                }
                break;

            case "Dibujos":
                switch (campoSemantico) {
                    case "Colegio":
                        CommonWord colegio = new CommonWord(0, "Colegio", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcolegio_dibujo.jpg?alt=media&token=a7d3dba3-8a71-458d-922c-7be55fa23e75", "gs://comtietea.appspot.com/images/default/colegio_dibujo.jpg"), 10);

                        CommonWord profesor = new CommonWord(1, "Profesor", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fprofesora_dibujo.jpg?alt=media&token=18c00d93-1960-46c9-9d8c-f50d96fbae39", "gs://comtietea.appspot.com/images/default/profesora_dibujo.jpg"), 9);

                        CommonWord companero = new CommonWord(2, "Compañero", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcompa%C3%B1ero_dibujo.png?alt=media&token=ee9d8d45-20a6-4c75-b75a-4918a37ac0e5", "gs://comtietea.appspot.com/images/default/compañero_dibujo.png"), 9);

                        CommonWord director = new CommonWord(3, "Director", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fdirector_dibujo.png?alt=media&token=ccbcd733-dee5-4554-ab2b-f2d7be29b531", "gs://comtietea.appspot.com/images/default/director_dibujo.png"), 9);

                        CommonWord recreo = new CommonWord(4, "Recreo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Frecreo_dibujo.jpg?alt=media&token=9194fd67-4c8f-4567-a349-de7f619f8584", "gs://comtietea.appspot.com/images/default/recreo_dibujo.jpg"), 8);

                        CommonWord gimnasio = new CommonWord(5, "Gimnasio", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fgimnasio_dibujo.png?alt=media&token=a164fc19-fa15-4296-9323-dfc1a4eed3d1", "gs://comtietea.appspot.com/images/default/gimnasio_dibujo.png"), 8);

                        CommonWord lengua = new CommonWord(6, "Lengua", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Flengua_dibujo.jpg?alt=media&token=265f3c6d-38b5-4cc0-b56c-cba8cfd002d7", "gs://comtietea.appspot.com/images/default/lengua_dibujo.jpg"), 7);

                        CommonWord mates = new CommonWord(7, "Matemáticas", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fmatem%C3%A1ticas_dibujo.jpg?alt=media&token=e153a3ea-add3-45f8-91cb-d50428a7ec88", "gs://comtietea.appspot.com/images/default/matemáticas_dibujo.jpg"), 7);

                        CommonWord biologia = new CommonWord(8, "Biología", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fbiologia_dibujo.png?alt=media&token=e3deec9e-67eb-417f-b436-22fe4e3f5d23", "gs://comtietea.appspot.com/images/default/biologia_dibujo.png"), 7);

                        CommonWord musica = new CommonWord(9, "Música", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fmusica_dibujo.PNG?alt=media&token=056ddb69-a457-479e-a815-aab5060401d3", "gs://comtietea.appspot.com/images/default/musica_dibujo.PNG"), 7);

                        CommonWord ingles = new CommonWord(10, "Inglés", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fingl%C3%A9s_dibujo.png?alt=media&token=80359a27-3b1b-48b9-b01c-2f2c7351890f", "gs://comtietea.appspot.com/images/default/inglés_dibujo.png"), 7);

                        CommonWord educacionFisica = new CommonWord(11, "Educación física", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fe.fisica_dibujo.jpg?alt=media&token=6d808e16-b359-45ee-ab4a-18f8e99e015e", "gs://comtietea.appspot.com/images/default/e.fisica_dibujo.jpg"), 7);

                        CommonWord pizarra = new CommonWord(12, "Pizarra", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fpizarra_dibujo.jpg?alt=media&token=8448f570-7730-4d14-99a2-289718106917", "gs://comtietea.appspot.com/images/default/pizarra_dibujo.jpg"), 6);

                        CommonWord mesa = new CommonWord(13, "Mesa", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fmesa_dibujo.png?alt=media&token=f0559294-0801-409b-87eb-cb8d9017f190", "gs://comtietea.appspot.com/images/default/mesa_dibujo.png"), 6);

                        CommonWord silla = new CommonWord(14, "Silla", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fsilla_dibujo.jpg?alt=media&token=53906e92-4787-4e66-910a-e89bddf08e91", "gs://comtietea.appspot.com/images/default/silla_dibujo.jpg"), 6);

                        CommonWord libro = new CommonWord(15, "Libro", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Flibro_dibujo.png?alt=media&token=e1ea2265-4c93-4e3b-80ff-671dde946738", "gs://comtietea.appspot.com/images/default/libro_dibujo.png"), 5);

                        CommonWord cuaderno = new CommonWord(16, "Cuaderno", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcuaderno_dibujo.jpg?alt=media&token=203abf07-32b5-4ac2-9826-d5b145a59853", "gs://comtietea.appspot.com/images/default/cuaderno_dibujo.jpg"), 5);

                        CommonWord diccionario = new CommonWord(17, "Diccionario", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fdiccionario_dibujo.jpg?alt=media&token=beaf8ab0-9212-4171-8cca-a930bdfbb819", "gs://comtietea.appspot.com/images/default/diccionario_dibujo.jpg"), 5);

                        CommonWord estuche = new CommonWord(18, "Estuche", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Festuche_dibujo.jpg?alt=media&token=dabb1a86-27bd-45d3-9553-a2ee626c4b9f", "gs://comtietea.appspot.com/images/default/estuche_dibujo.jpg"), 4);

                        CommonWord mochila = new CommonWord(19, "Mochila", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fmochila_dibujo.png?alt=media&token=67dbec85-c4b0-4cde-b58c-3dd783224548", "gs://comtietea.appspot.com/images/default/mochila_dibujo.png"), 4);

                        CommonWord boli = new CommonWord(20, "Bolígrafo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fboli_dibujo.png?alt=media&token=59f56b62-a15a-4137-bd91-65cab4e267ab", "gs://comtietea.appspot.com/images/default/boli_dibujo.png"), 3);

                        CommonWord lapiz = new CommonWord(21, "Lápiz", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Flapiz_dibujo.png?alt=media&token=0283555f-c65c-434f-bda4-5d3e89768ef2", "gs://comtietea.appspot.com/images/default/lapiz_dibujo.png"), 3);

                        CommonWord colores = new CommonWord(22, "Colores", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcolores_dibujo.png?alt=media&token=e57cd95a-bf3f-47f4-9867-040671b64cdf", "gs://comtietea.appspot.com/images/default/colores_dibujo.png"), 2);

                        CommonWord goma = new CommonWord(23, "Goma", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fgoma_dibujo.jpg?alt=media&token=a8fd114d-8884-4de6-9a14-19a6a07b6689", "gs://comtietea.appspot.com/images/default/goma_dibujo.jpg"), 2);

                        CommonWord sacapuntas = new CommonWord(24, "Sacapuntas", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fsacapuntas_dibujo.jpg?alt=media&token=b1deb007-7625-425e-8ca5-ba463707d6e0", "gs://comtietea.appspot.com/images/default/sacapuntas_dibujo.jpg"), 2);

                        CommonWord rotuladores = new CommonWord(25, "Rotuladores", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Frotuladores_dibujo.jpg?alt=media&token=846ba999-b1b1-4233-aec1-db04e5aa72ad", "gs://comtietea.appspot.com/images/default/rotuladores_dibujo.jpg"), 2);

                        CommonWord clase = new CommonWord(26, "Clase", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fclase_dibujo.jpg?alt=media&token=7aa31c43-174d-4e4f-a36b-07fb653e99df", "gs://comtietea.appspot.com/images/default/clase_dibujo.jpg"), 1);

                        res.add(colegio);
                        res.add(profesor);
                        res.add(companero);
                        res.add(director);
                        res.add(recreo);
                        res.add(gimnasio);
                        res.add(lengua);
                        res.add(mates);
                        res.add(biologia);
                        res.add(musica);
                        res.add(ingles);
                        res.add(educacionFisica);
                        res.add(pizarra);
                        res.add(mesa);
                        res.add(silla);
                        res.add(libro);
                        res.add(cuaderno);
                        res.add(diccionario);
                        res.add(estuche);
                        res.add(mochila);
                        res.add(boli);
                        res.add(lapiz);
                        res.add(colores);
                        res.add(goma);
                        res.add(sacapuntas);
                        res.add(rotuladores);
                        res.add(clase);
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

                    case "Acciones":
                        CommonWord accion = new CommonWord(0, "Acción", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Facci%C3%B3n_dibujo.jpg?alt=media&token=22d0bc48-f6fe-4bdf-a531-eef7bd8c333c", "gs://comtietea.appspot.com/images/default/acción_dibujo.jpg"), 10);

                        CommonWord beber = new CommonWord(1, "Beber", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fbeber_dibujo.jpg?alt=media&token=576ac2ed-5334-4e28-9bce-50a58fe8834e", "gs://comtietea.appspot.com/images/default/beber_dibujo.jpg"), 9);

                        CommonWord comer = new CommonWord(2, "Comer", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcomer_dibujo.jpg?alt=media&token=f70461e6-9789-4673-9a40-fec65c3a4433", "gs://comtietea.appspot.com/images/default/comer_dibujo.jpg"), 9);

                        CommonWord despertar = new CommonWord(3, "Despertar", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fdespertar_dibujo.jpg?alt=media&token=e4265fbe-6a52-4005-a8cb-bace3fa0b4e1", "gs://comtietea.appspot.com/images/default/despertar_dibujo.jpg"), 9);

                        CommonWord levantar = new CommonWord(4, "Levantar", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Flevantar_dibujo.png?alt=media&token=71ae7e9b-0347-4472-8e08-fc853ffc8381", "gs://comtietea.appspot.com/images/default/levantar_dibujo.png"), 9);

                        CommonWord dormir = new CommonWord(5, "Dormir", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fdormir_dibujo.png?alt=media&token=ccb8e38a-48db-49af-9ec7-657d29962946", "gs://comtietea.appspot.com/images/default/dormir_dibujo.png"), 9);

                        CommonWord jugar = new CommonWord(6, "Jugar", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fjugar_dibujo.jpg?alt=media&token=1d9dcb83-95af-4364-87cb-bda11b6ef379", "gs://comtietea.appspot.com/images/default/jugar_dibujo.jpg"), 8);

                        CommonWord trabajar = new CommonWord(7, "Trabajar", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Ftrabajar_dibujo.jpg?alt=media&token=aab32d2c-448c-462a-900d-01315fed7f6c", "gs://comtietea.appspot.com/images/default/trabajar_dibujo.jpg"), 8);

                        CommonWord estudiar = new CommonWord(8, "Estudiar", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Festudiar_dibujo.jpg?alt=media&token=5e304ca3-da1c-4377-8462-865ea2b9afaf", "gs://comtietea.appspot.com/images/default/estudiar_dibujo.jpg"), 8);

                        CommonWord sentar = new CommonWord(9, "Sentar", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fsentar_dibujo.jpg?alt=media&token=3b062af9-8ccd-4e87-948e-e6dbcbefd33b", "gs://comtietea.appspot.com/images/default/sentar_dibujo.jpg"), 7);

                        CommonWord pedir = new CommonWord(10, "Pedir", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fpedir_dibujo.jpg?alt=media&token=d8b3edbb-5a43-4508-a944-b6d49b4c8606", "gs://comtietea.appspot.com/images/default/pedir_dibujo.jpg"), 7);

                        CommonWord ver = new CommonWord(11, "Ver", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fver_dibujo.jpg?alt=media&token=0c6bad96-b85e-417b-b97f-6eb3f6eea319", "gs://comtietea.appspot.com/images/default/ver_dibujo.jpg"), 6);

                        CommonWord escuchar = new CommonWord(12, "Escuchar", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fescuchar_dibujo.png?alt=media&token=2b56b07c-54a2-4063-8eb9-5770e68c22e8", "gs://comtietea.appspot.com/images/default/escuchar_dibujo.png"), 6);

                        CommonWord hablar = new CommonWord(13, "Hablar", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fhablar_dibujo.png?alt=media&token=3622ca15-0848-4d71-bb2e-607a75d8eb20", "gs://comtietea.appspot.com/images/default/hablar_dibujo.png"), 6);

                        CommonWord oler = new CommonWord(14, "Oler", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Foler_dibujo.png?alt=media&token=fb8347a4-0fb1-4c42-b800-216f6d4eb088", "gs://comtietea.appspot.com/images/default/oler_dibujo.png"), 6);

                        CommonWord leer = new CommonWord(15, "Leer", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fleer_dibujo.jpg?alt=media&token=72dab0ae-3ce8-42c4-8076-6961126c7a3a", "gs://comtietea.appspot.com/images/default/leer_dibujo.jpg"), 5);

                        CommonWord escribir = new CommonWord(16, "Escribir", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fescribir_dibujo.jpg?alt=media&token=701b4633-130a-44f2-b15b-d41ffdb78bba", "gs://comtietea.appspot.com/images/default/escribir_dibujo.jpg"), 5);

                        CommonWord dibujar = new CommonWord(17, "Dibujar", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fdibujar_dibujo.jpg?alt=media&token=77292075-089a-4d14-9754-f217eac3bd27", "gs://comtietea.appspot.com/images/default/dibujar_dibujo.jpg"), 5);

                        CommonWord pintar = new CommonWord(18, "Pintar", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fpintar_dibujo.png?alt=media&token=776db9a6-d5bf-417c-bee6-f4269e6c43bd", "gs://comtietea.appspot.com/images/default/pintar_dibujo.png"), 5);

                        CommonWord morir = new CommonWord(19, "Morir", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fmorir_dibujo.jpg?alt=media&token=4428001f-5c6f-4418-9372-a4e31d3aa3b5", "gs://comtietea.appspot.com/images/default/morir_dibujo.jpg"), 4);

                        CommonWord nacer = new CommonWord(20, "Nacer", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fnacer_dibujo.png?alt=media&token=363ed481-4149-463e-ac77-608e553f59ce", "gs://comtietea.appspot.com/images/default/nacer_dibujo.png"), 4);

                        CommonWord correr = new CommonWord(21, "Correr", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcorrer_dibujo.png?alt=media&token=a93688b9-f490-43f5-9154-526669b53726", "gs://comtietea.appspot.com/images/default/correr_dibujo.png"), 4);

                        CommonWord ensenar = new CommonWord(22, "Enseñar", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fense%C3%B1ar_dibujo.jpg?alt=media&token=f2fe7f1e-8187-4d0f-8210-8d6e279e4b3e", "gs://comtietea.appspot.com/images/default/enseñar_dibujo.jpg"), 4);

                        CommonWord dudar = new CommonWord(23, "Dudar", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fdudar_dibujo.jpg?alt=media&token=9277771a-66e3-4511-9df4-88bef6eaf5cc", "gs://comtietea.appspot.com/images/default/dudar_dibujo.jpg"), 4);

                        CommonWord peinar = new CommonWord(24, "Peinar", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fpeinar_dibujo.png?alt=media&token=6fbcbdec-e779-4a0d-a4c1-d1a2c5e34e81", "gs://comtietea.appspot.com/images/default/peinar_dibujo.png"), 3);

                        CommonWord secar = new CommonWord(25, "Secar", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fsecar_dibujo.png?alt=media&token=ee37c3b6-24c8-4843-b99e-189f7f1d5a23", "gs://comtietea.appspot.com/images/default/secar_dibujo.png"), 3);

                        CommonWord cepillarDientes = new CommonWord(26, "Cepillarse los dientes", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcepillarse_dientes_dibujo.png?alt=media&token=5e7caa3e-6709-43ba-86e9-a5132b1b0a26", "gs://comtietea.appspot.com/images/default/cepillarse_dientes_dibujo.png"), 3);

                        CommonWord lavar = new CommonWord(27, "Lavar", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Flavar_dibujo.jpg?alt=media&token=ecab8f5e-221a-42f7-a492-e79583a52c2e", "gs://comtietea.appspot.com/images/default/lavar_dibujo.jpg"), 3);

                        CommonWord limpiar = new CommonWord(28, "Limpiar", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Flimpiar_dibujo.jpg?alt=media&token=fccc1153-b4ff-412f-8b89-112287e5d710", "gs://comtietea.appspot.com/images/default/limpiar_dibujo.jpg"), 3);

                        CommonWord ir = new CommonWord(29, "Ir", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fir_dibujo.PNG?alt=media&token=48262d55-b0de-435e-9ea0-cfb520aa8088", "gs://comtietea.appspot.com/images/default/ir_dibujo.PNG"), 2);

                        CommonWord pensar = new CommonWord(30, "Pensar", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fpensar_dibujo.jpg?alt=media&token=d03122f3-6989-40e2-95fa-b12e3e4419fb", "gs://comtietea.appspot.com/images/default/pensar_dibujo.jpg"), 2);

                        CommonWord abrazar = new CommonWord(31, "Abrazar", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fabrazar_dibujo.png?alt=media&token=5ac6bb28-d4f4-4647-9977-fce33948939d", "gs://comtietea.appspot.com/images/default/abrazar_dibujo.png"), 1);

                        CommonWord besar = new CommonWord(32, "Besar", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fbesar_dibujo.png?alt=media&token=ac66d21c-3349-4b25-a293-fb8e2543ca35", "gs://comtietea.appspot.com/images/default/besar_dibujo.png"), 1);

                        CommonWord querer = new CommonWord(33, "Querer", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fquerer_dibujo.png?alt=media&token=998bc515-2007-4f0d-a6e2-6177635ce422", "gs://comtietea.appspot.com/images/default/querer_dibujo.png"), 1);

                        res.add(accion);
                        res.add(beber);
                        res.add(comer);
                        res.add(despertar);
                        res.add(levantar);
                        res.add(dormir);
                        res.add(jugar);
                        res.add(trabajar);
                        res.add(estudiar);
                        res.add(sentar);
                        res.add(pedir);
                        res.add(ver);
                        res.add(escuchar);
                        res.add(hablar);
                        res.add(oler);
                        res.add(leer);
                        res.add(escribir);
                        res.add(dibujar);
                        res.add(pintar);
                        res.add(morir);
                        res.add(nacer);
                        res.add(correr);
                        res.add(ensenar);
                        res.add(dudar);
                        res.add(peinar);
                        res.add(secar);
                        res.add(cepillarDientes);
                        res.add(lavar);
                        res.add(limpiar);
                        res.add(ir);
                        res.add(pensar);
                        res.add(abrazar);
                        res.add(besar);
                        res.add(querer);
                        break;

                    case "Casa":
                        CommonWord casa = new CommonWord(0, "Casa", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcasa_dibujo.png?alt=media&token=171df0d1-b4fa-4675-941d-abf9c0b3d772", "gs://comtietea.appspot.com/images/default/casa_dibujo.png"), 10);

                        CommonWord cocina = new CommonWord(1, "Cocina", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcocina_dibujo.jpg?alt=media&token=6ec95cde-2732-47d1-9ada-c35edeaf85e7", "gs://comtietea.appspot.com/images/default/cocina_dibujo.jpg"), 9);

                        CommonWord bano = new CommonWord(2, "Baño", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fba%C3%B1o_dibujo.jpg?alt=media&token=8973c337-9b7c-47ca-8f45-b6ac3786c872", "gs://comtietea.appspot.com/images/default/baño_dibujo.jpg"), 8);

                        CommonWord dormitorio = new CommonWord(3, "Dormitorio", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fdormitorio_dibujo.jpg?alt=media&token=f9215dd8-0491-41c8-9312-55cebba30b08", "gs://comtietea.appspot.com/images/default/dormitorio_dibujo.jpg"), 7);

                        CommonWord salon = new CommonWord(4, "Salón", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fsalon_dibujo.jpg?alt=media&token=94c266cf-4bdb-45bb-acab-85c73d6442ce", "gs://comtietea.appspot.com/images/default/salon_dibujo.jpg"), 6);

                        CommonWord comedor = new CommonWord(5, "Comedor", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcomedor_dibujo.jpg?alt=media&token=36f15e26-608d-4b32-97ae-34a2708ea0b8", "gs://comtietea.appspot.com/images/default/comedor_dibujo.jpg"), 5);

                        CommonWord jardin = new CommonWord(6, "Jardín", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fjard%C3%ADn_dibujo.jpg?alt=media&token=7182d063-fd14-47eb-b583-678420a49dbb", "gs://comtietea.appspot.com/images/default/jardín_dibujo.jpg"), 4);

                        CommonWord balcon = new CommonWord(7, "Balcón", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fbalc%C3%B3n_dibujo.jpg?alt=media&token=cd29e27e-312b-477c-b80c-00f8ea4e4133", "gs://comtietea.appspot.com/images/default/balcón_dibujo.jpg"), 3);

                        res.add(casa);
                        res.add(cocina);
                        res.add(bano);
                        res.add(dormitorio);
                        res.add(salon);
                        res.add(comedor);
                        res.add(jardin);
                        res.add(balcon);
                        break;

                    case "Ocio":
                        CommonWord ocio = new CommonWord(0, "Ocio", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Focio_dibujo.jpg?alt=media&token=29f9467f-1844-4b80-9ec6-50dd460a7cdf", "gs://comtietea.appspot.com/images/default/ocio_dibujo.jpg"), 10);

                        CommonWord videojuego = new CommonWord(1, "Videojuego", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fvideojuego_dibujo.png?alt=media&token=f9bd2d7f-bf23-4b0c-8e88-60f0ad785949", "gs://comtietea.appspot.com/images/default/videojuego_dibujo.png"), 9);

                        CommonWord juegoMesa = new CommonWord(2, "Juego de mesa", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fjuego_de_mesa_dibujo.jpg?alt=media&token=5d5a9351-adbb-412b-88e6-181f798b545c", "gs://comtietea.appspot.com/images/default/juego_de_mesa_dibujo.jpg"), 9);

                        CommonWord juguete = new CommonWord(3, "Juguete", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fjuguete_dibujo.jpg?alt=media&token=146ef096-ea1e-4df7-9d6c-d8817c1812cb", "gs://comtietea.appspot.com/images/default/juguete_dibujo.jpg"), 8);

                        CommonWord pelota = new CommonWord(4, "Pelota", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fpelota_dibujo.png?alt=media&token=a758ca90-a69d-428c-990d-627bf1bd292e", "gs://comtietea.appspot.com/images/default/pelota_dibujo.png"), 8);

                        CommonWord cine = new CommonWord(5, "Cine", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcine_dibujo.png?alt=media&token=2e17f858-15c2-401e-81cc-552d78ecf83a", "gs://comtietea.appspot.com/images/default/cine_dibujo.png"), 7);

                        CommonWord escucharMusica = new CommonWord(6, "Escuchar música", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fescuchar_musica_dibujo.png?alt=media&token=72ae49c3-fc68-46f5-9f0f-9d2541991e39", "gs://comtietea.appspot.com/images/default/escuchar_musica_dibujo.png"), 7);

                        CommonWord baile = new CommonWord(7, "Baile", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fbaile_dibujo.jpg?alt=media&token=fb1a3c1c-0865-4352-81ad-8c58919ba6a8", "gs://comtietea.appspot.com/images/default/baile_dibujo.jpg"), 7);

                        CommonWord teatro = new CommonWord(8, "Teatro", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fteatro_dibujo.jpg?alt=media&token=0085ad59-000d-444d-9ef7-fbb78d1a850b", "gs://comtietea.appspot.com/images/default/teatro_dibujo.jpg"), 7);

                        CommonWord lectura = new CommonWord(9, "Lectura", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Flectura_dibujo.png?alt=media&token=309366a8-ba85-4ac8-96ef-f5df49495545", "gs://comtietea.appspot.com/images/default/lectura_dibujo.png"), 6);

                        CommonWord viajar = new CommonWord(10, "Viajar", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fviajar_dibujo.png?alt=media&token=636a4d7a-750a-4a22-bcec-b796d8e524f3", "gs://comtietea.appspot.com/images/default/viajar_dibujo.png"), 6);

                        CommonWord deporte = new CommonWord(11, "Deporte", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fdeporte_dibujo.png?alt=media&token=0e13a199-de15-465a-ba63-cfa70bb2735e", "gs://comtietea.appspot.com/images/default/deporte_dibujo.png"), 5);

                        CommonWord futbol = new CommonWord(12, "Fútbol", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Ffutbol_dibujo.jpg?alt=media&token=c515cd68-f5cf-4d92-8dfc-db819c9225f4", "gs://comtietea.appspot.com/images/default/futbol_dibujo.jpg"), 4);

                        CommonWord baloncesto = new CommonWord(13, "Baloncesto", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fbaloncesto_dibujo.png?alt=media&token=1fe91aa6-d17c-4617-9892-c6e1646b09ca", "gs://comtietea.appspot.com/images/default/baloncesto_dibujo.png"), 4);

                        CommonWord tenis = new CommonWord(14, "Tenis", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Ftenis_dibujo.jpg?alt=media&token=81812f71-9720-42f9-8cf6-45302b87c675", "gs://comtietea.appspot.com/images/default/tenis_dibujo.jpg"), 4);

                        CommonWord gimnasia = new CommonWord(15, "Gimnasia", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fgimansia_dibujo.jpg?alt=media&token=5002d852-1145-4e11-bf10-fc5dedff8d4d", "gs://comtietea.appspot.com/images/default/gimansia_dibujo.jpg"), 3);

                        CommonWord padel = new CommonWord(16, "Pádel", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fpadel_dibujo.jpg?alt=media&token=6ad7632a-1ab9-4960-be0f-2bae17dfd1be", "gs://comtietea.appspot.com/images/default/padel_dibujo.jpg"), 3);

                        CommonWord ciclismo = new CommonWord(17, "Ciclismo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fciclismo_dibujo.jpg?alt=media&token=0f49ffcf-563a-4d59-8497-592c3b3c64f6", "gs://comtietea.appspot.com/images/default/ciclismo_dibujo.jpg"), 3);

                        CommonWord natacion = new CommonWord(18, "Natación", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fnataci%C3%B3n_dibujo.png?alt=media&token=4c09e346-d723-4e33-88b7-345c084693ed", "gs://comtietea.appspot.com/images/default/natación_dibujo.png"), 3);

                        CommonWord parque = new CommonWord(19, "Parque", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fparque_dibujo.jpg?alt=media&token=7fe19db1-7033-4f91-83f5-55bc42190d81", "gs://comtietea.appspot.com/images/default/parque_dibujo.jpg"), 2);

                        res.add(ocio);
                        res.add(videojuego);
                        res.add(juegoMesa);
                        res.add(juguete);
                        res.add(pelota);
                        res.add(cine);
                        res.add(escucharMusica);
                        res.add(baile);
                        res.add(teatro);
                        res.add(lectura);
                        res.add(viajar);
                        res.add(deporte);
                        res.add(futbol);
                        res.add(baloncesto);
                        res.add(tenis);
                        res.add(gimnasia);
                        res.add(padel);
                        res.add(ciclismo);
                        res.add(natacion);
                        res.add(parque);
                        break;

                    case "Animales":
                        CommonWord animal = new CommonWord(0, "Animal", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fanimales_dibujo.jpg?alt=media&token=37441298-ccc4-43e0-8bf1-a1a515fc1ae0", "gs://comtietea.appspot.com/images/default/animales_dibujo.jpg"), 10);

                        CommonWord perro = new CommonWord(1, "Perro", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fperro_dibujo.jpg?alt=media&token=f3a53ce8-d86f-4939-b5c3-0ab59385df88", "gs://comtietea.appspot.com/images/default/perro_dibujo.jpg"), 9);

                        CommonWord gato = new CommonWord(2, "Gato", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fgato_dibujo.jpg?alt=media&token=ed6b34a2-92c2-4fc5-a253-72dc68e7bb83", "gs://comtietea.appspot.com/images/default/gato_dibujo.jpg"), 9);

                        CommonWord conejo = new CommonWord(3, "Conejo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fconejo_dibujo.jpg?alt=media&token=b402c805-fd09-4e50-b2cc-ff9631726668", "gs://comtietea.appspot.com/images/default/conejo_dibujo.jpg"), 8);

                        CommonWord pajaro = new CommonWord(4, "Pájaro", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fpajaro_dibujo.png?alt=media&token=a3c4d264-ddb0-4dd4-b84d-15998cab0741", "gs://comtietea.appspot.com/images/default/pajaro_dibujo.png"), 8);

                        CommonWord pez = new CommonWord(5, "Pez", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fpez_dibujo.png?alt=media&token=337001df-4dbc-4789-957d-325e48a05dfd", "gs://comtietea.appspot.com/images/default/pez_dibujo.png"), 8);

                        CommonWord hamster = new CommonWord(6, "Hamster", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fhamster_dibujo.jpg?alt=media&token=b8a6e523-2fc3-4b28-9db4-d953f624678c", "gs://comtietea.appspot.com/images/default/hamster_dibujo.jpg"), 8);

                        CommonWord leon = new CommonWord(7, "León", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fle%C3%B3n_dibujo.png?alt=media&token=c7e68739-51c4-45bd-9a7b-17a6d82b9b23", "gs://comtietea.appspot.com/images/default/león_dibujo.png"), 7);

                        CommonWord tigre = new CommonWord(8, "Tigre", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Ftrigre_dibujo.png?alt=media&token=64961e28-d7c6-4fbe-b97e-ac6eaaf47dbc", "gs://comtietea.appspot.com/images/default/trigre_dibujo.png"), 7);

                        CommonWord jirafa = new CommonWord(9, "Jirafa", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fjirafa_dibujo.jpg?alt=media&token=d2b4863d-18ce-43b3-912f-d852414fa4e5", "gs://comtietea.appspot.com/images/default/jirafa_dibujo.jpg"), 7);

                        CommonWord cebra = new CommonWord(10, "Cebra", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcebra_dibujo.jpg?alt=media&token=1d649d33-8d08-4c65-8777-ec09bd2bb91f", "gs://comtietea.appspot.com/images/default/cebra_dibujo.jpg"), 6);

                        CommonWord oso = new CommonWord(11, "Oso", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Foso_dibujo.jpg?alt=media&token=05fea32f-6435-4115-855d-15aabb3d5b54", "gs://comtietea.appspot.com/images/default/oso_dibujo.jpg"), 6);

                        CommonWord serpiente = new CommonWord(12, "Serpiente", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fserpiente_dibujo.png?alt=media&token=e20dc4ae-fed7-4c57-b2a9-36ec782bd8fd", "gs://comtietea.appspot.com/images/default/serpiente_dibujo.png"), 6);

                        CommonWord mariposa = new CommonWord(13, "Mariposa", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fmariposa_dibujo.jpg?alt=media&token=5ead0158-bd9d-4d2a-a73b-9de5ee9669c7", "gs://comtietea.appspot.com/images/default/mariposa_dibujo.jpg"), 5);

                        CommonWord mosquito = new CommonWord(14, "Mosquito", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fmosquito_dibujo.png?alt=media&token=01446025-c09a-4266-aee7-6c186469f51f", "gs://comtietea.appspot.com/images/default/mosquito_dibujo.png"), 5);

                        CommonWord arana = new CommonWord(15, "Araña", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fara%C3%B1a_dibujo.PNG?alt=media&token=09f26988-d2ce-4e54-8204-cc8432e46ce6", "gs://comtietea.appspot.com/images/default/araña_dibujo.PNG"), 5);

                        CommonWord tiburon = new CommonWord(16, "Tiburón", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Ftiburon_foto.jpg?alt=media&token=1b6fd97a-de14-4a94-9ebe-1df05ab4dd0a", "gs://comtietea.appspot.com/images/default/tiburon_foto.jpg"), 4);

                        CommonWord ballena = new CommonWord(17, "Ballena", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fballena_dibujo.png?alt=media&token=eb3b6f3a-bf94-4e56-9063-3f6f054c8801", "gs://comtietea.appspot.com/images/default/ballena_dibujo.png"), 4);

                        CommonWord delfin = new CommonWord(18, "Delfín", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fdelfin_foto.png?alt=media&token=93b13b42-7295-4d04-ac3a-747ebf2da09e", "gs://comtietea.appspot.com/images/default/delfin_foto.png"), 4);

                        CommonWord caballo = new CommonWord(19, "Caballo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcaballo_dibujo.jpg?alt=media&token=74e83bd6-1485-4688-8055-aa7e68f1a1c4", "gs://comtietea.appspot.com/images/default/caballo_dibujo.jpg"), 3);

                        CommonWord cerdo = new CommonWord (20, "Cerdo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcerdo_dibujo.png?alt=media&token=97f2a025-3474-4108-9605-3cd099a3f49b", "gs://comtietea.appspot.com/images/default/cerdo_dibujo.png"), 3);

                        CommonWord gallina = new CommonWord(21, "Gallina", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fgallina_dibujo.png?alt=media&token=6ee35af4-4af1-4e99-bbbb-a06d9dd19520", "gs://comtietea.appspot.com/images/default/gallina_dibujo.png"), 3);

                        CommonWord oveja = new CommonWord(22, "Oveja", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Foveja_dibujo.png?alt=media&token=d6c3b548-1e18-4a03-b3e4-0240bc36a04d", "gs://comtietea.appspot.com/images/default/oveja_dibujo.png"), 3);

                        CommonWord vaca = new CommonWord(23, "Vaca", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fvaca_dibujo.jpg?alt=media&token=c191a2e2-55a9-45b0-8dca-2b7852eef27e", "gs://comtietea.appspot.com/images/default/vaca_dibujo.jpg"), 3);

                        CommonWord elefante = new CommonWord(24, "Elefante", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Felefante_dibujo.jpg?alt=media&token=0a65bb27-fae6-456d-a440-52f19ea54751", "gs://comtietea.appspot.com/images/default/elefante_dibujo.jpg"), 2);

                        CommonWord mono = new CommonWord(25, "Mono", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fmono_dibujo.jpg?alt=media&token=dbf8dd8d-f714-494f-992b-858cbcfa8f10", "gs://comtietea.appspot.com/images/default/mono_dibujo.jpg"), 2);

                        res.add(animal);
                        res.add(perro);
                        res.add(gato);
                        res.add(conejo);
                        res.add(pajaro);
                        res.add(pez);
                        res.add(hamster);
                        res.add(leon);
                        res.add(tigre);
                        res.add(jirafa);
                        res.add(cebra);
                        res.add(oso);
                        res.add(serpiente);
                        res.add(mariposa);
                        res.add(mosquito);
                        res.add(arana);
                        res.add(tiburon);
                        res.add(ballena);
                        res.add(delfin);
                        res.add(caballo);
                        res.add(cerdo);
                        res.add(gallina);
                        res.add(oveja);
                        res.add(vaca);
                        res.add(elefante);
                        res.add(mono);
                        break;

                    case "Colores":
                        CommonWord color = new CommonWord(0, "Color", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcolor_dibujo.png?alt=media&token=2b5846ee-8540-4c0e-a2a1-c0a14e675a61", "gs://comtietea.appspot.com/images/default/color_dibujo.png"), 10);

                        CommonWord negro = new CommonWord(1, "Negro", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fnegro.PNG?alt=media&token=1a51e037-6eb5-4516-996b-d34a322d200e", "gs://comtietea.appspot.com/images/default/negro.PNG"), 9);

                        CommonWord blanco = new CommonWord(2, "Blanco", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fblanco.PNG?alt=media&token=93486d07-3935-44f3-bfc2-c948078183ae", "gs://comtietea.appspot.com/images/default/blanco.PNG"), 9);

                        CommonWord azul = new CommonWord(3, "Azul", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fazul.PNG?alt=media&token=30ffbce4-8587-4742-92a4-dae9d927ab39", "gs://comtietea.appspot.com/images/default/azul.PNG"), 8);

                        CommonWord verde = new CommonWord(4, "Verde", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fverde.PNG?alt=media&token=a24733f3-b560-4767-aee7-19ffdf2c8ecc", "gs://comtietea.appspot.com/images/default/verde.PNG"), 8);

                        CommonWord rosa = new CommonWord(5, "Rosa", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Frosa.PNG?alt=media&token=a40a0b8a-e5d2-48af-9b45-653b40d4409a", "gs://comtietea.appspot.com/images/default/rosa.PNG"), 7);

                        CommonWord rojo = new CommonWord(6, "Rojo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Frojo.PNG?alt=media&token=f2be6ee5-dcb0-4df7-bcf6-6889fe345fdb", "gs://comtietea.appspot.com/images/default/rojo.PNG"), 7);

                        CommonWord amarillo = new CommonWord(7, "Amarillo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Famarillo.PNG?alt=media&token=88f6388c-ce7d-445d-a300-de15219edaf8", "gs://comtietea.appspot.com/images/default/amarillo.PNG"), 6);

                        CommonWord naranja = new CommonWord(8, "Naranja", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fnaranja.PNG?alt=media&token=03c9e9fd-cab4-4080-9550-62f7668c608b", "gs://comtietea.appspot.com/images/default/naranja.PNG"), 5);

                        CommonWord marron = new CommonWord(9, "Marrón", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fmarr%C3%B3n.PNG?alt=media&token=e757ed4c-16de-4376-b2d2-d6f6c959fee2", "gs://comtietea.appspot.com/images/default/marrón.PNG"), 4);

                        CommonWord morado = new CommonWord(10, "Morado", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fmorado.PNG?alt=media&token=bf63525c-09e9-4a72-867c-01ec6d4b7ef4", "gs://comtietea.appspot.com/images/default/morado.PNG"), 3);

                        CommonWord gris = new CommonWord(11, "Gris", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fgris.PNG?alt=media&token=c7836319-e4a7-410f-8157-52419c2eb4c0", "gs://comtietea.appspot.com/images/default/gris.PNG"), 2);

                        res.add(color);
                        res.add(negro);
                        res.add(blanco);
                        res.add(azul);
                        res.add(verde);
                        res.add(rosa);
                        res.add(rojo);
                        res.add(amarillo);
                        res.add(naranja);
                        res.add(marron);
                        res.add(morado);
                        res.add(gris);
                        break;

                    case "Adjetivos":
                        CommonWord adjetivo = new CommonWord(0, "Adjetivo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fadjetivo_dibujo.png?alt=media&token=3b3869a1-9856-45a3-9f63-210b2684f369", "gs://comtietea.appspot.com/images/default/adjetivo_dibujo.png"), 10);

                        CommonWord grande = new CommonWord(1, "Grande", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fgrande_dibujo.PNG?alt=media&token=9f9bf2b8-8e5b-4f6e-9bd0-d11367a42ccc", "gs://comtietea.appspot.com/images/default/grande_dibujo.PNG"), 9);

                        CommonWord pequeno = new CommonWord(2, "Pequeño", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fpeque%C3%B1o_dibujo.PNG?alt=media&token=3d36da6e-9945-4951-a227-21f22deef2d5", "gs://comtietea.appspot.com/images/default/pequeño_dibujo.PNG"), 9);

                        CommonWord alto = new CommonWord(3, "Alto", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Falto_dibujo.PNG?alt=media&token=ce8170d7-6d6f-4446-920e-56bc5ec4aa50", "gs://comtietea.appspot.com/images/default/alto_dibujo.PNG"), 8);

                        CommonWord bajo = new CommonWord(4, "Bajo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fbajo_dibujo.PNG?alt=media&token=b0e5628d-ce59-41f1-8253-dc8bb0caa9d7", "gs://comtietea.appspot.com/images/default/bajo_dibujo.PNG"), 8);

                        CommonWord largo = new CommonWord(5, "Largo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Flargo_dibujo.PNG?alt=media&token=36444937-341b-4887-9976-16e55d0768e4", "gs://comtietea.appspot.com/images/default/largo_dibujo.PNG"), 7);

                        CommonWord corto = new CommonWord(6, "Corto", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcorto_dibujo.PNG?alt=media&token=3900a808-8305-4213-99e0-37491e9332f6", "gs://comtietea.appspot.com/images/default/corto_dibujo.PNG"), 7);

                        CommonWord gordo = new CommonWord(7, "Gordo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fgordo_dibujo.PNG?alt=media&token=3f34a68d-2203-4ae3-8e8e-8f4c6cfb50ac", "gs://comtietea.appspot.com/images/default/gordo_dibujo.PNG"), 6);

                        CommonWord delgado = new CommonWord(8, "Delgado", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fdelgado_dibujo.PNG?alt=media&token=b9e3926b-ee5f-44a3-b214-52bbaec756f0", "gs://comtietea.appspot.com/images/default/delgado_dibujo.PNG"), 6);

                        CommonWord pesado = new CommonWord(9, "Pesado", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fpesado_dibujo.png?alt=media&token=500b4dcb-638b-447a-8358-9323a4fbb058", "gs://comtietea.appspot.com/images/default/pesado_dibujo.png"), 5);

                        CommonWord ligero = new CommonWord(10, "Ligero", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fligero_dibujo.PNG?alt=media&token=ab720e7b-5778-43da-967b-bff83bf8384b", "gs://comtietea.appspot.com/images/default/ligero_dibujo.PNG"), 5);

                        CommonWord claro = new CommonWord(11, "Claro", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fclaro_dibujo.png?alt=media&token=206692a1-2df8-426c-9da4-e8a08043ef4a","gs://comtietea.appspot.com/images/default/claro_dibujo.png"), 4);

                        CommonWord oscuro = new CommonWord(12, "Oscuro", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Foscuro_dibujo.PNG?alt=media&token=25577d28-6843-4908-b4ed-04511ef5bb3a", "gs://comtietea.appspot.com/images/default/oscuro_dibujo.PNG"), 4);

                        CommonWord viejo = new CommonWord(13, "Viejo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fviejo_dibujo.png?alt=media&token=b841f431-2870-4b4a-8a05-c16a6aa4d669", "gs://comtietea.appspot.com/images/default/viejo_dibujo.png"), 3);

                        CommonWord nuevo = new CommonWord(14, "Nuevo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fnuevo_dibujo.png?alt=media&token=c8de3b3d-a83b-456c-9a03-511ace2f3c88", "gs://comtietea.appspot.com/images/default/nuevo_dibujo.png"), 3);

                        CommonWord joven = new CommonWord(15, "Joven", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fjoven_dibujo.png?alt=media&token=7c9a84c0-90c0-4f13-aa2e-9389e305aabe", "gs://comtietea.appspot.com/images/default/joven_dibujo.png"), 3);

                        CommonWord antiguo = new CommonWord(16, "Antiguo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fantiguo_dibujo.png?alt=media&token=7fd19426-e253-4f70-9743-663d6c0595b1", "gs://comtietea.appspot.com/images/default/antiguo_dibujo.png"), 3);

                        CommonWord liso = new CommonWord(17, "Liso", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fliso_dibujo.png?alt=media&token=92e858b4-c944-4cb3-9565-5cb002bcd5c4", "gs://comtietea.appspot.com/images/default/liso_dibujo.png"), 2);

                        CommonWord rugoso = new CommonWord(18, "Rugoso", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Farrugado_dibujo.png?alt=media&token=5b3f4552-2579-4870-b8b4-f475e9ede60b", "gs://comtietea.appspot.com/images/default/arrugado_dibujo.png"), 2);

                        CommonWord feo = new CommonWord(19, "Feo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Ffeo_dibujo.png?alt=media&token=cd8ef8a6-24c4-4866-9174-1570e66233c4", "gs://comtietea.appspot.com/images/default/feo_dibujo.png"), 1);

                        CommonWord guapo = new CommonWord (20, "Guapo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fguapa_dibujo.png?alt=media&token=4d65d10e-0ddc-42e9-83ac-9eab1e3b2ad8", "gs://comtietea.appspot.com/images/default/guapa_dibujo.png"), 1);

                        res.add(adjetivo);
                        res.add(grande);
                        res.add(pequeno);
                        res.add(alto);
                        res.add(bajo);
                        res.add(largo);
                        res.add(corto);
                        res.add(gordo);
                        res.add(delgado);
                        res.add(pesado);
                        res.add(ligero);
                        res.add(claro);
                        res.add(oscuro);
                        res.add(viejo);
                        res.add(nuevo);
                        res.add(joven);
                        res.add(antiguo);
                        res.add(liso);
                        res.add(rugoso);
                        res.add(feo);
                        res.add(guapo);
                        break;

                    case "Emociones":
                        CommonWord emocion = new CommonWord(0, "Emoción", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Femociones_dibujo.png?alt=media&token=e9782282-7464-49bd-aadd-4032c726e681", "gs://comtietea.appspot.com/images/default/emociones_dibujo.png"), 10);

                        CommonWord tristeza = new CommonWord(1, "Tristeza", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Ftristeza_dibujo.png?alt=media&token=c32eebda-b1a1-4bbe-859a-6aa97035eba8", "gs://comtietea.appspot.com/images/default/tristeza_dibujo.png"), 9);

                        CommonWord felicidad = new CommonWord(2, "Felicidad", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Ffeliz_dibujo.jpg?alt=media&token=9d2d96ec-cd12-4aab-9d64-b35f9880cdfe", "gs://comtietea.appspot.com/images/default/feliz_dibujo.jpg"), 8);

                        CommonWord miedo = new CommonWord(3, "Miedo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fmiedo_dibujo.png?alt=media&token=e0cf0c9c-2d2e-446b-9f92-e278bf875213", "gs://comtietea.appspot.com/images/default/miedo_dibujo.png"), 7);

                        CommonWord asco = new CommonWord(4, "Asco", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fasco_dibujo.jpg?alt=media&token=70091adc-cb61-4dad-b8cd-e9ccd68d633d", "gs://comtietea.appspot.com/images/default/asco_dibujo.jpg"), 6);

                        CommonWord enfado = new CommonWord(5, "Enfado", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fenfado_dibujo.png?alt=media&token=1ff7ea69-284a-4e42-937c-b00b4628ac89", "gs://comtietea.appspot.com/images/default/enfado_dibujo.png"), 5);

                        CommonWord sorpresa = new CommonWord(6, "Sorpresa", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fsorpresa_dibujo.PNG?alt=media&token=8c7689cf-2896-4772-abef-abda380052cf", "gs://comtietea.appspot.com/images/default/sorpresa_dibujo.PNG"), 4);

                        CommonWord amor = new CommonWord(7, "Amor", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Famor_dibujo.jpg?alt=media&token=f0a2be60-3bbe-47b9-8bcc-d717c603864a", "gs://comtietea.appspot.com/images/default/amor_dibujo.jpg"), 3);

                        res.add(emocion);
                        res.add(tristeza);
                        res.add(felicidad);
                        res.add(miedo);
                        res.add(asco);
                        res.add(enfado);
                        res.add(sorpresa);
                        res.add(amor);
                        break;

                    case "Profesiones":
                        CommonWord profesion = new CommonWord(0, "Profesión", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fprofesi%C3%B3n_dibujo.jpg?alt=media&token=3ddad120-4994-4eb1-a6d7-d17b2149a7a3", "gs://comtietea.appspot.com/images/default/profesión_dibujo.jpg"), 10);

                        CommonWord profesora = new CommonWord(1, "Profesora", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fprofesora_dibujo.jpg?alt=media&token=18c00d93-1960-46c9-9d8c-f50d96fbae39", "gs://comtietea.appspot.com/images/default/profesora_dibujo.jpg"), 9);

                        CommonWord medico = new CommonWord(2, "Médico", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fmedico_dibujo.jpg?alt=media&token=50a49659-18cb-41cf-84f2-da6e1d1bcee7", "gs://comtietea.appspot.com/images/default/medico_dibujo.jpg"), 9);

                        CommonWord policia = new CommonWord(3, "Policía", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fpolicia_dibujo.png?alt=media&token=c04098ec-15cd-4caa-b01f-9af325553a0a", "gs://comtietea.appspot.com/images/default/policia_dibujo.png"), 9);

                        CommonWord bombero = new CommonWord(4, "Bombero", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fbombero_dibujo.jpg?alt=media&token=9c4917d5-84ab-4849-83f7-cfed413f481d", "gs://comtietea.appspot.com/images/default/bombero_dibujo.jpg"), 8);

                        CommonWord camarero = new CommonWord(5, "Camarero", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcamarero_dibujo.jpg?alt=media&token=d5fed9aa-ed6f-44db-9180-b4bd99460a06", "gs://comtietea.appspot.com/images/default/camarero_dibujo.jpg"), 7);

                        CommonWord electricista = new CommonWord(6, "Electricista", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Felectricista_dibujo.jpg?alt=media&token=507937af-6e8b-48da-8a7d-43b228e952ff", "gs://comtietea.appspot.com/images/default/electricista_dibujo.jpg"), 6);

                        CommonWord enfermero = new CommonWord(7, "Enfermero", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fenefermero_dibujo.png?alt=media&token=3ffedf71-db5d-4253-932e-ef93bcdcf635", "gs://comtietea.appspot.com/images/default/enefermero_dibujo.png"), 5);

                        CommonWord psicologo = new CommonWord(8, "Psicólogo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fpsicologo_dibujo.png?alt=media&token=eaaf06c3-fda5-49fc-829e-18a9616e1beb", "gs://comtietea.appspot.com/images/default/psicologo_dibujo.png"), 4);

                        CommonWord abogado = new CommonWord(9, "Abogado", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fabodago_dibujo.jpg?alt=media&token=4dab65d8-b892-4234-8ea3-81ad66464c88", "gs://comtietea.appspot.com/images/default/abodago_dibujo.jpg"), 3);

                        CommonWord cocinero = new CommonWord(10, "Cocinero", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcocinero_dibujo.jpg?alt=media&token=8cf3dc85-5e3b-45d6-9062-40aec6cdd35b", "gs://comtietea.appspot.com/images/default/cocinero_dibujo.jpg"), 2);

                        res.add(profesion);
                        res.add(profesora);
                        res.add(medico);
                        res.add(policia);
                        res.add(bombero);
                        res.add(camarero);
                        res.add(electricista);
                        res.add(enfermero);
                        res.add(psicologo);
                        res.add(abogado);
                        res.add(cocinero);
                        break;

                    case "Comida":
                        CommonWord comida = new CommonWord(0, "Comida", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcomida_dibujo.png?alt=media&token=dbc6051c-438f-4d60-a476-50baf7297129", "gs://comtietea.appspot.com/images/default/comida_dibujo.png"), 10);

                        CommonWord verdura = new CommonWord(1, "Verdura", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fverdura_dibujo.jpg?alt=media&token=a5b8dba7-0ec6-49a3-9e1d-5998696379a6", "gs://comtietea.appspot.com/images/default/verdura_dibujo.jpg"), 9);

                        CommonWord tomate = new CommonWord(2, "Tomate", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Ftomate_dibujo.jpg?alt=media&token=579e9104-0f44-4983-8c14-7935427e6792", "gs://comtietea.appspot.com/images/default/tomate_dibujo.jpg"), 8);

                        CommonWord lechuga = new CommonWord(3, "Lechuga", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Flechuga_dibujo.png?alt=media&token=21c7479b-3bad-48e5-b4bd-cae75f4bf060", "gs://comtietea.appspot.com/images/default/lechuga_dibujo.png"), 8);

                        CommonWord cebolla = new CommonWord(4, "Cebolla", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcebolla_dibujo.png?alt=media&token=c19d314f-68e4-46ef-a2bb-e7de26db81af", "gs://comtietea.appspot.com/images/default/cebolla_dibujo.png"), 8);

                        CommonWord patatas = new CommonWord(5, "Patatas", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fpatata_dibujo.jpg?alt=media&token=ca0aa72c-38b2-4f93-9ba8-4209859d2940", "gs://comtietea.appspot.com/images/default/patata_dibujo.jpg"), 8);

                        CommonWord fruta = new CommonWord(6, "Fruta", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Ffruta_dibujo.jpg?alt=media&token=e7c0a08d-fd8d-4f64-9d99-28e6013d295e", "gs://comtietea.appspot.com/images/default/fruta_dibujo.jpg"), 7);

                        CommonWord manzana = new CommonWord(7, "Manzana", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fmanzana_dibujo.png?alt=media&token=09546c9c-21d5-410f-94f8-e7fa7b0eb610", "gs://comtietea.appspot.com/images/default/manzana_dibujo.png"), 6);

                        CommonWord pera = new CommonWord(8, "Pera", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fpera_dibujo.png?alt=media&token=9cff2ae3-2a73-4562-a1a2-1797d827f016", "gs://comtietea.appspot.com/images/default/pera_dibujo.png"), 6);

                        CommonWord platano = new CommonWord(9, "Plátano", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fplatano_dibujo.png?alt=media&token=47e8ba2e-ca0c-4106-abd0-57c813a66938", "gs://comtietea.appspot.com/images/default/platano_dibujo.png"), 6);

                        CommonWord fresa = new CommonWord(10, "Fresa", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Ffresa_dibujo.jpg?alt=media&token=9d2a0818-e099-48aa-81ff-38732506be9a", "gs://comtietea.appspot.com/images/default/fresa_dibujo.jpg"), 6);

                        CommonWord melon = new CommonWord(11, "Melón", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fmelon_dibujo.png?alt=media&token=122b257d-b560-473e-b2f1-4ea882f0ad4f", "gs://comtietea.appspot.com/images/default/melon_dibujo.png"), 6);

                        CommonWord sandia = new CommonWord(12, "Sandía", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fsand%C3%ADa_dibujo.jpg?alt=media&token=b5e0f26a-3d65-45ef-918c-96d5e350cae7", "gs://comtietea.appspot.com/images/default/sandía_dibujo.jpg"), 6);

                        CommonWord cereza = new CommonWord(13, "Cereza", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcereza_dibujo.jpg?alt=media&token=cae6c051-257b-442c-8544-d6e3a02acef9", "gs://comtietea.appspot.com/images/default/cereza_dibujo.jpg"), 6);

                        CommonWord pina = new CommonWord(14, "Piña", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fpi%C3%B1a_dibujo.png?alt=media&token=dc9e1057-9448-46a2-9069-761a95038b4f", "gs://comtietea.appspot.com/images/default/piña_dibujo.png"), 6);

                        CommonWord pescado = new CommonWord(15, "Pescado", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fpescado_dibujo.jpg?alt=media&token=8d129843-c781-4e91-a4e1-3143fcf5dfd6", "gs://comtietea.appspot.com/images/default/pescado_dibujo.jpg"), 5);

                        CommonWord carne = new CommonWord(16, "Carne", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcarne_dibujo.jpg?alt=media&token=d5789ec1-fd49-4564-a64e-2af79326dcc6", "gs://comtietea.appspot.com/images/default/carne_dibujo.jpg"), 4);

                        CommonWord pollo = new CommonWord(17, "Pollo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fpollo_dibujo.jpg?alt=media&token=41d3a61d-af1e-425e-95a7-d544e4066757", "gs://comtietea.appspot.com/images/default/pollo_dibujo.jpg"), 3);

                        CommonWord filete = new CommonWord(18, "Filete", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Ffilete_dibujo.jpg?alt=media&token=3815e923-aade-4945-b173-694adba32e77", "gs://comtietea.appspot.com/images/default/filete_dibujo.jpg"), 3);

                        CommonWord hamburguesa = new CommonWord(19, "Hamburguesa", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fhamburguesa_dibujo.png?alt=media&token=8fb21197-6f47-4289-865f-2a74c1191641", "gs://comtietea.appspot.com/images/default/hamburguesa_dibujo.png"), 3);

                        CommonWord salchicha = new CommonWord(20, "Salchicha", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fsalchicha_dibujo.png?alt=media&token=7f116fd4-afb7-4bea-bb3b-48bb46eea38e", "gs://comtietea.appspot.com/images/default/salchicha_dibujo.png"), 3);

                        CommonWord bebida = new CommonWord(21, "Bebida", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fbebida_dibujo.jpg?alt=media&token=98a6f1c6-04cc-42b3-b105-d340867e7055", "gs://comtietea.appspot.com/images/default/bebida_dibujo.jpg"), 2);

                        CommonWord agua = new CommonWord(22, "Agua", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fagua_dibujo.jpg?alt=media&token=4a8ba7a2-017b-4a83-8255-f06bb0292914", "gs://comtietea.appspot.com/images/default/agua_dibujo.jpg"), 2);

                        CommonWord cocacola = new CommonWord(23, "Coca-Cola", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcocacola_dibujo.png?alt=media&token=e787668e-cc24-49af-b597-a55518a95ac3", "gs://comtietea.appspot.com/images/default/cocacola_dibujo.png"), 2);

                        CommonWord refresco = new CommonWord(24, "Refresco", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Frefresco_dibujo.jpg?alt=media&token=ea0b57a4-64d8-4bf2-b0cf-ec52de9f75e7", "gs://comtietea.appspot.com/images/default/refresco_dibujo.jpg"), 2);

                        CommonWord zumo = new CommonWord(25, "Zumo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fzumo_dibujo.png?alt=media&token=7c87431b-d571-4cd3-bc07-a75257067fba", "gs://comtietea.appspot.com/images/default/zumo_dibujo.png"), 2);

                        CommonWord leche = new CommonWord(26, "Leche", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fleche_dibujo.jpg?alt=media&token=b10c858d-167e-4c2d-a631-3e5ae36c37fb", "gs://comtietea.appspot.com/images/default/leche_dibujo.jpg"), 2);

                        CommonWord batido = new CommonWord(27, "Batido", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fbatido_dibujo.png?alt=media&token=380ab781-376d-4968-aff6-e77aed4eb62a", "gs://comtietea.appspot.com/images/default/batido_dibujo.png"), 2);

                        CommonWord colacao = new CommonWord(28, "Cola-Cao", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcolacao_dibujo.png?alt=media&token=6198c7eb-e0dd-48ed-98f9-ebb5e70af190", "gs://comtietea.appspot.com/images/default/colacao_dibujo.png"), 2);

                        CommonWord cafe = new CommonWord(29, "Café", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcafe_dibujo.png?alt=media&token=2a0b2e93-c58b-44f6-b7c3-08d6648efb85", "gs://comtietea.appspot.com/images/default/cafe_dibujo.png"), 2);

                        CommonWord cerveza = new CommonWord(30, "Cerveza", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcerveza_dibujo.png?alt=media&token=5f668496-b9b3-422f-9b91-85fba8656b10", "gs://comtietea.appspot.com/images/default/cerveza_dibujo.png"), 2);

                        CommonWord vino = new CommonWord(31, "Vino", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fvino_dibujo.jpg?alt=media&token=5292826f-2247-40b0-9a34-f5cb1443f866", "gs://comtietea.appspot.com/images/default/vino_dibujo.jpg"), 2);

                        CommonWord pizza = new CommonWord(32, "Pizza", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fpizza_dibujo.png?alt=media&token=46a7dfea-56d1-412c-b936-57f4722b5a03", "gs://comtietea.appspot.com/images/default/pizza_dibujo.png"), 1);

                        CommonWord bocadillo = new CommonWord(33, "Bocadillo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fbocadillo_dibujo.jpg?alt=media&token=d5fd4727-7340-40e4-b487-8657c2a1d721", "gs://comtietea.appspot.com/images/default/bocadillo_dibujo.jpg"), 1);

                        CommonWord jamon = new CommonWord(34, "Jamón", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fjam%C3%B3n_dibujo.png?alt=media&token=664e1ce1-e062-43e0-bc1b-8e3cc8d16afc", "gs://comtietea.appspot.com/images/default/jamón_dibujo.png"), 1);

                        CommonWord embutido = new CommonWord(35, "Embutido", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fembutido_dibujo.jpg?alt=media&token=563f3c8d-3255-402f-9863-a0eb8579e4b0", "gs://comtietea.appspot.com/images/default/embutido_dibujo.jpg"), 1);

                        CommonWord tortilla = new CommonWord(36, "Tortilla", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Ftortilla_dibujo.png?alt=media&token=10175c35-49ed-428c-adb4-41c767ab1a42", "gs://comtietea.appspot.com/images/default/tortilla_dibujo.png"), 1);

                        CommonWord pasta = new CommonWord(37, "Pasta", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fpasta_dibujo.png?alt=media&token=2336ff83-3a7f-46e9-bc84-08881ad396cb", "gs://comtietea.appspot.com/images/default/pasta_dibujo.png"), 1);

                        CommonWord queso = new CommonWord(38, "Queso", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fqueso_dibujo.png?alt=media&token=85967359-92d0-48e0-ac87-c7544fa14edc", "gs://comtietea.appspot.com/images/default/queso_dibujo.png"), 1);

                        CommonWord cereales = new CommonWord(39, "Cereales", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcereales_dibujo.jpg?alt=media&token=f8e3334b-4dc2-4c10-b263-339960ca4366", "gs://comtietea.appspot.com/images/default/cereales_dibujo.jpg"), 1);

                        CommonWord sopa = new CommonWord(40, "Sopa", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fsopa_dibujo.png?alt=media&token=f54a0498-9d32-4bcb-a304-14853eebae5f", "gs://comtietea.appspot.com/images/default/sopa_dibujo.png"), 1);

                        CommonWord arroz = new CommonWord(41, "Arroz", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Farroz_dibujo.png?alt=media&token=eb8d7ade-3aec-48ab-822f-73a0607ac337", "gs://comtietea.appspot.com/images/default/arroz_dibujo.png"), 1);

                        CommonWord lentejas = new CommonWord(42, "Lentejas", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Flentejas_dibujo.png?alt=media&token=f83b3e97-1895-428e-977a-5140ca502343", "gs://comtietea.appspot.com/images/default/lentejas_dibujo.png"), 1);

                        CommonWord chocolate = new CommonWord(43, "Chocolate", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fchocolate_dibujo.png?alt=media&token=e6592235-55b7-4c38-941a-894d9041c1c5", "gs://comtietea.appspot.com/images/default/chocolate_dibujo.png"), 1);

                        CommonWord huevosFritos = new CommonWord(44, "Huevos fritos", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fhuevo_frito_dibujo.png?alt=media&token=96a93a7c-cf48-427b-8d2f-9e87a38ec050", "gs://comtietea.appspot.com/images/default/huevo_frito_dibujo.png"), 1);

                        CommonWord paella = new CommonWord(45, "Paella", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fpaella_dibujo.png?alt=media&token=7a2c6d23-734f-415d-bb1c-bfa65dd91fc6", "gs://comtietea.appspot.com/images/default/paella_dibujo.png"), 1);

                        res.add(comida);
                        res.add(verdura);
                        res.add(tomate);
                        res.add(lechuga);
                        res.add(cebolla);
                        res.add(patatas);
                        res.add(fruta);
                        res.add(manzana);
                        res.add(pera);
                        res.add(platano);
                        res.add(fresa);
                        res.add(melon);
                        res.add(sandia);
                        res.add(cereza);
                        res.add(pina);
                        res.add(pescado);
                        res.add(carne);
                        res.add(pollo);
                        res.add(filete);
                        res.add(hamburguesa);
                        res.add(salchicha);
                        res.add(bebida);
                        res.add(agua);
                        res.add(cocacola);
                        res.add(refresco);
                        res.add(zumo);
                        res.add(leche);
                        res.add(batido);
                        res.add(colacao);
                        res.add(cafe);
                        res.add(cerveza);
                        res.add(vino);
                        res.add(pizza);
                        res.add(bocadillo);
                        res.add(jamon);
                        res.add(embutido);
                        res.add(tortilla);
                        res.add(pasta);
                        res.add(queso);
                        res.add(cereales);
                        res.add(sopa);
                        res.add(arroz);
                        res.add(lentejas);
                        res.add(chocolate);
                        res.add(huevosFritos);
                        res.add(paella);
                        break;

                    case "Aseo":
                        CommonWord aseo = new CommonWord(0, "Aseo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Faseo_dibujo.jpg?alt=media&token=20eac5ed-3962-49f3-bb71-1b4ee1360cde", "gs://comtietea.appspot.com/images/default/aseo_dibujo.jpg"), 10);

                        CommonWord ducha = new CommonWord(1, "Ducha", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fducha_dibujo.png?alt=media&token=65662d13-3276-4782-a110-d5c4aa2a8d3e", "gs://comtietea.appspot.com/images/default/ducha_dibujo.png"), 9);

                        CommonWord banera = new CommonWord(2, "Bañera", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fba%C3%B1era_dibujo.png?alt=media&token=f7f481b3-a146-45c2-8159-6d9e993e8ac6", "gs://comtietea.appspot.com/images/default/bañera_dibujo.png"), 9);

                        CommonWord wc = new CommonWord(3, "WC", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fwc_dibujo.png?alt=media&token=42703acb-4d73-4225-9a00-fb97d9ae3919", "gs://comtietea.appspot.com/images/default/wc_dibujo.png"), 8);

                        CommonWord peine = new CommonWord(4, "Peine", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fpeine_dibujo.png?alt=media&token=7c8f4715-d8eb-462d-8834-f51ed7be6bf9", "gs://comtietea.appspot.com/images/default/peine_dibujo.png"), 7);

                        CommonWord cepilloDientes = new CommonWord(5, "Cepillo de dientes", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcepillo_de_dientes_dibujo.png?alt=media&token=6e0e7114-1d17-46e4-8359-9851638fbf69", "gs://comtietea.appspot.com/images/default/cepillo_de_dientes_dibujo.png"), 7);

                        CommonWord toalla = new CommonWord(6, "Toalla", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Ftoalla_dibujo.jpg?alt=media&token=859cad39-1d91-4baf-907b-61a0e7410a80", "gs://comtietea.appspot.com/images/default/toalla_dibujo.jpg"), 6);

                        CommonWord papelHigienico = new CommonWord(7, "Papel higiénico", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fpapel_higi%C3%A9nico_dibujo.png?alt=media&token=47c40f1d-299d-47a4-81a2-b6ac5c6c9258", "gs://comtietea.appspot.com/images/default/papel_higiénico_dibujo.png"), 5);

                        CommonWord toallitas = new CommonWord(8, "Toallitas", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Ftoallitas_dibujo.png?alt=media&token=38622798-6986-4ee5-97e3-5721bb61ade5", "gs://comtietea.appspot.com/images/default/toallitas_dibujo.png"), 5);

                        CommonWord maquillaje = new CommonWord(9, "Maquillaje", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fmaquillaje_dibujo.png?alt=media&token=e92145cb-667f-4a97-9311-fac5bcba27f5", "gs://comtietea.appspot.com/images/default/maquillaje_dibujo.png"), 4);

                        CommonWord ducharse = new CommonWord(10, "Ducharse", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fducharse_dibujo.png?alt=media&token=12fbc1d5-355b-415c-a71a-86b656e838d2", "gs://comtietea.appspot.com/images/default/ducharse_dibujo.png"), 3);

                        CommonWord cepillarseDientes = new CommonWord(11, "Cepillarse los dientes", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcepillarse_dientes_dibujo.png?alt=media&token=5e7caa3e-6709-43ba-86e9-a5132b1b0a26", "gs://comtietea.appspot.com/images/default/cepillarse_dientes_dibujo.png"), 3);

                        res.add(aseo);
                        res.add(ducha);
                        res.add(banera);
                        res.add(wc);
                        res.add(peine);
                        res.add(cepilloDientes);
                        res.add(toalla);
                        res.add(papelHigienico);
                        res.add(toallitas);
                        res.add(maquillaje);
                        res.add(ducharse);
                        res.add(cepillarseDientes);
                        break;

                    case "Estaciones y Tiempo":
                        CommonWord estaciones = new CommonWord(0, "Estaciones", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Festaciones_dibujo.PNG?alt=media&token=74f53d8e-827c-4b31-a9da-bc8f5e6604f9", "gs://comtietea.appspot.com/images/default/estaciones_dibujo.PNG"), 10);

                        CommonWord primavera = new CommonWord(1, "Primavera", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fprimavera_dibujo.PNG?alt=media&token=aa8da158-ea49-4db8-ae93-11627ef89ea4", "gs://comtietea.appspot.com/images/default/primavera_dibujo.PNG"), 9);

                        CommonWord verano = new CommonWord(2, "Verano", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fverano_dibujo.png?alt=media&token=58640875-1606-4d9b-b5ca-41e8b3f197c6", "gs://comtietea.appspot.com/images/default/verano_dibujo.png"), 9);

                        CommonWord otono = new CommonWord(3, "Otoño", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Foto%C3%B1o_dibujo.png?alt=media&token=97ef5e92-82db-43b9-90d9-955b3a879be6", "gs://comtietea.appspot.com/images/default/otoño_dibujo.png"), 9);

                        CommonWord invierno = new CommonWord(4, "Invierno", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Finvierno_dibujo.png?alt=media&token=07b477a0-63d2-403e-93d8-8100f151b9be", "gs://comtietea.appspot.com/images/default/invierno_dibujo.png"), 9);

                        CommonWord tiempo = new CommonWord(5, "Tiempo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Ftiempo_foto.png?alt=media&token=b1db22fd-8c0e-4ef9-923f-f7c838d0903c", "gs://comtietea.appspot.com/images/default/tiempo_foto.png"), 8);

                        CommonWord frio = new CommonWord(6, "Frío", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Ffrio_dibujo.png?alt=media&token=11ff8f83-d5fa-49a6-9f2d-d5a3d39be0cb", "gs://comtietea.appspot.com/images/default/frio_dibujo.png"), 7);

                        CommonWord calor = new CommonWord(7, "Calor", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcalor_dibujo.png?alt=media&token=40b0e661-f460-4a76-90e0-21a783558332", "gs://comtietea.appspot.com/images/default/calor_dibujo.png"), 7);

                        CommonWord sol = new CommonWord(8, "Sol", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fsol_dibujo.png?alt=media&token=ddf0d34c-7425-4e88-909d-c26a4447a148", "gs://comtietea.appspot.com/images/default/sol_dibujo.png"), 6);

                        CommonWord nube = new CommonWord(9, "Nube", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fnube_dibujo.png?alt=media&token=836701cf-1080-4d37-92a0-7f60830ce3fa", "gs://comtietea.appspot.com/images/default/nube_dibujo.png"), 5);

                        CommonWord lluvia = new CommonWord(10, "Lluvia", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Flluvia_dibujo.png?alt=media&token=8857cfa6-cdd6-47a4-bea7-96db075fac05", "gs://comtietea.appspot.com/images/default/lluvia_dibujo.png"), 4);

                        CommonWord tormenta = new CommonWord(11, "Tormenta", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Ftormenta_dibujo.png?alt=media&token=92569db7-2e04-4f95-af22-5ca4aa5d748b", "gs://comtietea.appspot.com/images/default/tormenta_dibujo.png"), 3);

                        CommonWord nieve = new CommonWord(12, "Nieve", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fnieve_dibujo.png?alt=media&token=a86e8947-7838-44e2-90a8-aeb170b86958", "gs://comtietea.appspot.com/images/default/nieve_dibujo.png"), 2);

                        res.add(estaciones);
                        res.add(primavera);
                        res.add(verano);
                        res.add(otono);
                        res.add(invierno);
                        res.add(tiempo);
                        res.add(frio);
                        res.add(calor);
                        res.add(sol);
                        res.add(nube);
                        res.add(lluvia);
                        res.add(tormenta);
                        res.add(nieve);
                        break;

                    case "Ropa":
                        CommonWord ropa = new CommonWord(0, "Ropa", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fropa_dibujo.png?alt=media&token=83cd1677-d21a-4a40-8511-070319383a1f", "gs://comtietea.appspot.com/images/default/ropa_dibujo.png"), 10);

                        CommonWord camiseta = new CommonWord(1, "Camiseta", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcamiseta_dibujo.jpg?alt=media&token=26bd7f0c-18c4-4f90-a7f0-b731b85a2490", "gs://comtietea.appspot.com/images/default/camiseta_dibujo.jpg"), 9);

                        CommonWord camisa = new CommonWord(2, "Camisa", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcamisa_dibujo.jpg?alt=media&token=8093ae88-060f-41b0-b4b8-81d83bd1c21e", "gs://comtietea.appspot.com/images/default/camisa_dibujo.jpg"), 9);

                        CommonWord pantalon = new CommonWord(3, "Pantalón", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fpantalon_dibujo.PNG?alt=media&token=d20c920b-9eb2-4cfd-bf7a-b11f1b19621f", "gs://comtietea.appspot.com/images/default/pantalon_dibujo.PNG"), 9);

                        CommonWord falda = new CommonWord(4, "Falda", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Ffalda_dibujo.jpg?alt=media&token=01c6cef0-a7b3-4e7c-9825-d15a66a1e828", "gs://comtietea.appspot.com/images/default/falda_dibujo.jpg"), 8);

                        CommonWord vestido = new CommonWord(5, "Vestido", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fvestido_dibujo.jpg?alt=media&token=99cd89db-1981-4a8d-9043-2d93549b1f9e", "gs://comtietea.appspot.com/images/default/vestido_dibujo.jpg"), 8);

                        CommonWord sudadera = new CommonWord(6, "Sudadera", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fsudadera_dibujo.jpg?alt=media&token=35543707-cb83-4219-9408-1355c15b1ba3", "gs://comtietea.appspot.com/images/default/sudadera_dibujo.jpg"), 8);

                        CommonWord jersey = new CommonWord(7, "Jersey", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fjersey_dibujo.jpg?alt=media&token=15f27b88-198f-4658-9d40-4b70c7840ef8", "gs://comtietea.appspot.com/images/default/jersey_dibujo.jpg"), 7);

                        CommonWord chaqueta = new CommonWord(8, "Chaqueta", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fchaqueta_dibujo.png?alt=media&token=1966aabf-da78-4489-b122-71d53081cf98", "gs://comtietea.appspot.com/images/default/chaqueta_dibujo.png"), 7);

                        CommonWord abrigo = new CommonWord(9, "Abrigo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fabrigo_dibujo.PNG?alt=media&token=d5c38d26-d6ae-45a3-a64d-7f9ac520661d", "gs://comtietea.appspot.com/images/default/abrigo_dibujo.PNG"), 7);

                        CommonWord chandal = new CommonWord(10, "Chándal", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fchandal_dibujo.PNG?alt=media&token=f498920d-e31a-4467-aa7d-af0eccd2d3ba", "gs://comtietea.appspot.com/images/default/chandal_dibujo.PNG"), 6);

                        CommonWord pijama = new CommonWord(11, "Pijama", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fpjama_dibujo.jpg?alt=media&token=6e86110b-a910-4098-b399-4b0cc9970f05", "gs://comtietea.appspot.com/images/default/pjama_dibujo.jpg"), 6);

                        CommonWord bragas = new CommonWord(12, "Bragas", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fbragas_dibujo.jpg?alt=media&token=baaf18b9-ea9c-4833-aec5-33baff31d8e3", "gs://comtietea.appspot.com/images/default/bragas_dibujo.jpg"), 5);

                        CommonWord sujetador = new CommonWord(13, "Sujetador", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fsujetador_dibujo.PNG?alt=media&token=d2f1fe7e-fb32-4174-8204-e094dd279bcb", "gs://comtietea.appspot.com/images/default/sujetador_dibujo.PNG"), 5);

                        CommonWord calzoncillos = new CommonWord(14, "Calzoncillos", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcalzoncillos_dibujo.PNG?alt=media&token=c5377e94-1ef4-41ab-9619-61d552f28d34", "gs://comtietea.appspot.com/images/default/calzoncillos_dibujo.PNG"), 5);

                        CommonWord calcetines = new CommonWord(15, "Calcetines", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcalcetines_dibujo.png?alt=media&token=9389fb65-83b4-4c7b-ab5e-aa9b209c9c4b", "gs://comtietea.appspot.com/images/default/calcetines_dibujo.png"), 4);

                        CommonWord zapatos = new CommonWord(16, "Zapatos", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fzapatos_dibujo.jpg?alt=media&token=75bbee0b-2dd2-42df-8f8b-60e0151655da", "gs://comtietea.appspot.com/images/default/zapatos_dibujo.jpg"), 4);

                        CommonWord chanclas = new CommonWord(17, "Chanclas", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fchanclas_dibujo.png?alt=media&token=1c8229a5-8113-48b6-a8c3-a1ec10b155ad", "gs://comtietea.appspot.com/images/default/chanclas_dibujo.png"), 3);

                        CommonWord zapatosTacon = new CommonWord(18, "Zapatos de tacón", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fzapato_tacon_dibujo.png?alt=media&token=9f28f21c-676a-4f84-bce6-e361f76c2a46", "gs://comtietea.appspot.com/images/default/zapato_tacon_dibujo.png"), 3);

                        CommonWord zapatillas = new CommonWord(19, "Zapatillas", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fzapatillas_dibujo.PNG?alt=media&token=2cb12840-21a0-450d-a278-6b636b71199f", "gs://comtietea.appspot.com/images/default/zapatillas_dibujo.PNG"), 3);

                        CommonWord deportes = new CommonWord(20, "Deportes", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fdeportes_dibujo.png?alt=media&token=33b2a2bd-2984-4d28-8529-26028d096a8b", "gs://comtietea.appspot.com/images/default/deportes_dibujo.png"), 2);

                        CommonWord sandalias = new CommonWord(21, "Sandalias", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fsandalia_dibujo.png?alt=media&token=21589ef6-f9c9-46fd-91d2-00120d78edcd", "gs://comtietea.appspot.com/images/default/sandalia_dibujo.png"), 2);

                        CommonWord bufanda = new CommonWord(22, "Bufanda", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fbufanda_dibujo.jpg?alt=media&token=e97c334c-f059-489d-86c2-9f7a2736e1cd", "gs://comtietea.appspot.com/images/default/bufanda_dibujo.jpg"), 1);

                        CommonWord gorro = new CommonWord(23, "Gorro", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fgorro_dibujo.jpg?alt=media&token=fa20c2a8-b42b-4e3a-9243-b581b6499ff1", "gs://comtietea.appspot.com/images/default/gorro_dibujo.jpg"), 1);

                        CommonWord guantes = new CommonWord(24, "Guantes", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fguantes_dibujo.png?alt=media&token=e8c7c72b-1494-4ae9-a7ac-0e466abe36b1", "gs://comtietea.appspot.com/images/default/guantes_dibujo.png"), 1);

                        res.add(ropa);
                        res.add(camiseta);
                        res.add(camisa);
                        res.add(pantalon);
                        res.add(falda);
                        res.add(vestido);
                        res.add(sudadera);
                        res.add(jersey);
                        res.add(chaqueta);
                        res.add(abrigo);
                        res.add(chandal);
                        res.add(pijama);
                        res.add(bragas);
                        res.add(sujetador);
                        res.add(calzoncillos);
                        res.add(calcetines);
                        res.add(zapatos);
                        res.add(chanclas);
                        res.add(zapatosTacon);
                        res.add(zapatillas);
                        res.add(deportes);
                        res.add(sandalias);
                        res.add(bufanda);
                        res.add(gorro);
                        res.add(guantes);
                        break;

                    case "Salud y Cuerpo Humano":
                        CommonWord salud = new CommonWord(0, "Salud", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fsalud_dibujo.jpg?alt=media&token=ecbf2350-dc97-4d54-a648-d3696ad87d7a", "gs://comtietea.appspot.com/images/default/salud_dibujo.jpg"), 10);

                        CommonWord sano = new CommonWord(1, "Sano", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fsano_dibujo.jpg?alt=media&token=f42b2f86-72bd-47f0-bc6e-efb18bb81312", "gs://comtietea.appspot.com/images/default/sano_dibujo.jpg"), 9);

                        CommonWord enfermo = new CommonWord(2, "Enfermo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fenefermo_dibujo.png?alt=media&token=150e4477-a855-4582-b9f2-117c60e1304d", "gs://comtietea.appspot.com/images/default/enefermo_dibujo.png"), 9);

                        CommonWord dolor = new CommonWord(3, "Dolor", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fdolor_dibujo.jpg?alt=media&token=ace87c4c-057b-44c1-bdf0-b325212231d8", "gs://comtietea.appspot.com/images/default/dolor_dibujo.jpg"), 8);

                        CommonWord resfriado = new CommonWord(4, "Resfriado", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fresfriado_dibujo.jpg?alt=media&token=c4fda10d-ee31-4bb8-af29-eea06882a0c8", "gs://comtietea.appspot.com/images/default/resfriado_dibujo.jpg"), 8);

                        CommonWord fiebre = new CommonWord(5, "Fiebre", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Ffiebre_dibujo.png?alt=media&token=f4fb751e-9fee-449d-b6dd-e7eacd04ee96", "gs://comtietea.appspot.com/images/default/fiebre_dibujo.png"), 8);

                        CommonWord vomito = new CommonWord(6, "Vómito", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fvomito_dibujo.jpg?alt=media&token=e7b31ec6-6e34-4f3a-9c85-26df0f579d16", "gs://comtietea.appspot.com/images/default/vomito_dibujo.jpg"), 8);

                        CommonWord sangre = new CommonWord(7, "Sangre", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fsangre_dibujo.png?alt=media&token=d89b0d1d-b04c-4c4b-8554-a8b523c59ec2", "gs://comtietea.appspot.com/images/default/sangre_dibujo.png"), 8);

                        CommonWord hospital = new CommonWord(8, "Hospital", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fhospital_dibujo.png?alt=media&token=01e6b93b-c8de-4049-85ce-203152ce8b5d", "gs://comtietea.appspot.com/images/default/hospital_dibujo.png"), 7);

                        CommonWord ambulancia = new CommonWord(9, "Ambulancia", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fambulancia_dibujo.png?alt=media&token=0b1085ad-0937-48c8-b1b7-a2d817c3a86e", "gs://comtietea.appspot.com/images/default/ambulancia_dibujo.png"), 7);

                        CommonWord medicamento = new CommonWord(10, "Medicamento", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fmedicamento_dibujo.png?alt=media&token=49c55cdb-2fb2-4350-b77e-0c2dd3383e0b", "gs://comtietea.appspot.com/images/default/medicamento_dibujo.png"), 6);

                        CommonWord pastilla = new CommonWord(11, "Pastilla", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fpastilla_dibujo.png?alt=media&token=8b645382-f87f-4f0c-9975-02b589c7c1b8", "gs://comtietea.appspot.com/images/default/pastilla_dibujo.png"), 6);

                        CommonWord jarabe = new CommonWord(12, "Jarabe", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fjarabe_dibujo.jpg?alt=media&token=ec129e5b-4b30-4db9-b379-c0ab6bb9d640", "gs://comtietea.appspot.com/images/default/jarabe_dibujo.jpg"), 6);

                        CommonWord venda = new CommonWord(13, "Venda", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fvenda_dibujo.png?alt=media&token=a5bbe120-736c-4cb2-9674-06034d3d6edb", "gs://comtietea.appspot.com/images/default/venda_dibujo.png"), 5);

                        CommonWord cuerpo = new CommonWord(14, "Cuerpo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcuerpo_dibujo.jpg?alt=media&token=e4149617-1d75-4f57-9031-7dcbe170a706", "gs://comtietea.appspot.com/images/default/cuerpo_dibujo.jpg"), 4);

                        CommonWord cabeza = new CommonWord(15, "Cabeza", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcabeza_dibujo.png?alt=media&token=17af8fe2-bb7a-4601-a6f4-5f659a744754", "gs://comtietea.appspot.com/images/default/cabeza_dibujo.png"), 3);

                        CommonWord cuello = new CommonWord(16, "Cuello", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcuello_dibujo.PNG?alt=media&token=1b96cd4d-b63b-4a0c-9dd3-4a90ca955336", "gs://comtietea.appspot.com/images/default/cuello_dibujo.PNG"), 3);

                        CommonWord boca = new CommonWord(17, "Boca", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fboca_dibujo.png?alt=media&token=36e35110-f5b8-4ae0-b46f-65e55c7cb8e0", "gs://comtietea.appspot.com/images/default/boca_dibujo.png"), 3);

                        CommonWord nariz = new CommonWord(18, "Nariz", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fnariz_dibujo.jpg?alt=media&token=6fb6e12f-8350-4816-a8cc-09509b4a48c3", "gs://comtietea.appspot.com/images/default/nariz_dibujo.jpg"), 3);

                        CommonWord ojo = new CommonWord(19, "Ojo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fojo_dibujo.png?alt=media&token=c4ec6c3f-e35f-4805-b7a2-0ce7caddad75", "gs://comtietea.appspot.com/images/default/ojo_dibujo.png"), 3);

                        CommonWord oreja = new CommonWord(20, "Oreja", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Foreja_dibujo.png?alt=media&token=ab9c690b-5ff9-48fa-877b-44320dbe9d98", "gs://comtietea.appspot.com/images/default/oreja_dibujo.png"), 3);

                        CommonWord brazo = new CommonWord(21, "Brazo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fbrazo_dibujo.png?alt=media&token=d529b76e-1098-47d4-8d08-2ac096891587", "gs://comtietea.appspot.com/images/default/brazo_dibujo.png"), 2);

                        CommonWord codo = new CommonWord(22, "Codo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcodo_dibujo.PNG?alt=media&token=46b567b7-ad89-4d21-902d-e05f90983a13", "gs://comtietea.appspot.com/images/default/codo_dibujo.PNG"), 2);

                        CommonWord mano = new CommonWord(23, "Mano", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fmano_dibujo.png?alt=media&token=17b6633e-9b7f-4422-bd38-fb6fa1a6f55d", "gs://comtietea.appspot.com/images/default/mano_dibujo.png"), 2);

                        CommonWord muneca = new CommonWord(24, "Muñeca", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fmu%C3%B1eca_dibujo.jpg?alt=media&token=a168eb6b-41d7-4e35-9e83-ae51e213de38", "gs://comtietea.appspot.com/images/default/muñeca_dibujo.jpg"), 2);

                        CommonWord dedo = new CommonWord(25, "Dedo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fdedo_dibujo.png?alt=media&token=78aa94b0-431a-454d-b755-4b314a5c970a", "gs://comtietea.appspot.com/images/default/dedo_dibujo.png"), 2);

                        CommonWord piernas = new CommonWord(26, "Piernas", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fpiernas_dibujo.png?alt=media&token=47db19a8-7f4d-4bcd-a4eb-766958d5bd2f", "gs://comtietea.appspot.com/images/default/piernas_dibujo.png"), 2);

                        CommonWord rodilla = new CommonWord(27, "Rodilla", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Frodilla_dibujo.png?alt=media&token=4ab4f673-4553-4a09-b780-c44486bc3334", "gs://comtietea.appspot.com/images/default/rodilla_dibujo.png"), 2);

                        CommonWord pie = new CommonWord(28, "Pie", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fpie_dibujo.png?alt=media&token=628c8c35-b9fd-4851-aa89-2aa4d3d2cfeb", "gs://comtietea.appspot.com/images/default/pie_dibujo.png"), 2);

                        CommonWord tobillo = new CommonWord(29, "Tobillo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Ftobillo_dibujo.png?alt=media&token=253e335f-89e1-4b31-9620-c411f1a6f7f0", "gs://comtietea.appspot.com/images/default/tobillo_dibujo.png"), 2);

                        CommonWord hombro = new CommonWord(30, "Hombro", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fhombro_dibujo.jpg?alt=media&token=96a64e1a-caca-4816-a302-158516222f7c", "gs://comtietea.appspot.com/images/default/hombro_dibujo.jpg"), 2);

                        CommonWord espalda = new CommonWord(31, "Espalda", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fespalda_dibujo.png?alt=media&token=dd7de071-8705-4063-9eef-1b6f5a0eedd2", "gs://comtietea.appspot.com/images/default/espalda_dibujo.png"), 2);

                        CommonWord barriga = new CommonWord(32, "Barriga", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fbarriga_dibujo.png?alt=media&token=4e460a30-c0ac-42da-a1fd-4922c70dcd8c", "gs://comtietea.appspot.com/images/default/barriga_dibujo.png"), 2);

                        res.add(salud);
                        res.add(sano);
                        res.add(enfermo);
                        res.add(dolor);
                        res.add(resfriado);
                        res.add(fiebre);
                        res.add(vomito);
                        res.add(sangre);
                        res.add(hospital);
                        res.add(ambulancia);
                        res.add(medicamento);
                        res.add(pastilla);
                        res.add(jarabe);
                        res.add(venda);
                        res.add(cuerpo);
                        res.add(cabeza);
                        res.add(cuello);
                        res.add(boca);
                        res.add(nariz);
                        res.add(ojo);
                        res.add(oreja);
                        res.add(brazo);
                        res.add(codo);
                        res.add(mano);
                        res.add(muneca);
                        res.add(dedo);
                        res.add(piernas);
                        res.add(rodilla);
                        res.add(pie);
                        res.add(tobillo);
                        res.add(hombro);
                        res.add(espalda);
                        res.add(barriga);
                        break;

                    case "Transportes":
                        CommonWord transporte = new CommonWord(0, "Transporte", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Ftransporte_dibujo.jpg?alt=media&token=18e1ec0b-3426-4b7d-ad2f-a30b91db56cf", "gs://comtietea.appspot.com/images/default/transporte_dibujo.jpg"), 10);

                        CommonWord coche = new CommonWord(1, "Coche", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcoche_dibujo.jpg?alt=media&token=cef27575-e53f-4e5e-9b1c-758a2f395634", "gs://comtietea.appspot.com/images/default/coche_dibujo.jpg"), 9);

                        CommonWord moto = new CommonWord(2, "Moto", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fmoto_dibujo.png?alt=media&token=ceb4c392-48cd-4659-be28-96cf1f8d55ab", "gs://comtietea.appspot.com/images/default/moto_dibujo.png"), 9);

                        CommonWord camion = new CommonWord(3, "Camión", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcami%C3%B3n_dibuji.png?alt=media&token=4584e78b-908c-4029-8a4d-2516fa6d8671", "gs://comtietea.appspot.com/images/default/camión_dibuji.png"), 8);

                        CommonWord furgoneta = new CommonWord(4, "Furgoneta", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Ffurgoneta_dibujo.png?alt=media&token=e415e00d-2c9d-4845-9725-8d50a0bb392c", "gs://comtietea.appspot.com/images/default/furgoneta_dibujo.png"), 8);

                        CommonWord avion = new CommonWord(5, "Avión", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Favi%C3%B3n_dibujo.png?alt=media&token=12b24296-877f-4db9-8058-f09297fa6ada", "gs://comtietea.appspot.com/images/default/avión_dibujo.png"), 7);

                        CommonWord barco = new CommonWord(6, "Barco", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fbarco_dibujo.png?alt=media&token=9a839015-b899-4c37-a636-2081a693751e", "gs://comtietea.appspot.com/images/default/barco_dibujo.png"), 6);

                        CommonWord bicicleta = new CommonWord(7, "Bicicleta", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fbici_dibujo.jpg?alt=media&token=1369570b-a0b3-4b66-9d34-e022811e3bb8", "gs://comtietea.appspot.com/images/default/bici_dibujo.jpg"), 5);

                        CommonWord tractor = new CommonWord(8, "Tractor", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Ftractor_dibujo.png?alt=media&token=94e33585-aea4-43cd-bfa8-de8316f782d0", "gs://comtietea.appspot.com/images/default/tractor_dibujo.png"), 4);

                        CommonWord helicoptero = new CommonWord(9, "Helicóptero", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fhelicoptero_dibujo.png?alt=media&token=77c5388c-3c8a-4196-97e8-92b8201d3455", "gs://comtietea.appspot.com/images/default/helicoptero_dibujo.png"), 3);

                        CommonWord autobus = new CommonWord(10, "Autobús", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fautobus_dibujo.png?alt=media&token=44775a05-d81f-4904-890b-cbda9958f463", "gs://comtietea.appspot.com/images/default/autobus_dibujo.png"), 2);

                        CommonWord tren = new CommonWord(11, "Tren", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Ftren_dibujo.jpg?alt=media&token=445fa128-0931-4efc-b8c4-45cbca6dcbba", "gs://comtietea.appspot.com/images/default/tren_dibujo.jpg"), 1);

                        res.add(transporte);
                        res.add(coche);
                        res.add(moto);
                        res.add(camion);
                        res.add(furgoneta);
                        res.add(avion);
                        res.add(barco);
                        res.add(bicicleta);
                        res.add(tractor);
                        res.add(helicoptero);
                        res.add(autobus);
                        res.add(tren);
                        break;
                }
                break;

            case "Imagenes":
                switch (campoSemantico) {
                    case "Colegio":
                        CommonWord colegio = new CommonWord(0, "Colegio", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcolegio_foto.jpg?alt=media&token=9cb13dd6-13a1-4072-a430-52b4eb1e5365", "gs://comtietea.appspot.com/images/default/colegio_foto.jpg"), 10);

                        CommonWord profesor = new CommonWord(1, "Profesor", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fprofesora_foto.jpg?alt=media&token=108b7f87-b728-489c-8523-e56a541b1f83", "gs://comtietea.appspot.com/images/default/profesora_foto.jpg"), 9);

                        CommonWord companero = new CommonWord(2, "Compañero", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcompa%C3%B1ero_foto.jpg?alt=media&token=fe0f7f1e-d034-4b5a-85ef-23e1096dba09", "gs://comtietea.appspot.com/images/default/compañero_foto.jpg"), 9);

                        CommonWord director = new CommonWord(3, "Director", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fdirector_foto.jpg?alt=media&token=087c70f2-a58d-4f32-ad69-abece24b86f6", "gs://comtietea.appspot.com/images/default/director_foto.jpg"), 9);

                        CommonWord recreo = new CommonWord(4, "Recreo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Frecreo_foto.jpg?alt=media&token=bcd33d10-ff14-4109-839a-8232beccac92", "gs://comtietea.appspot.com/images/default/recreo_foto.jpg"), 8);

                        CommonWord gimnasio = new CommonWord(5, "Gimnasio", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fgimnasio_foto.jpg?alt=media&token=03f509e6-24e0-4fe2-b9d5-5bf72ece2f7c", "gs://comtietea.appspot.com/images/default/gimnasio_foto.jpg"), 8);

                        CommonWord lengua = new CommonWord(6, "Lengua", new FirebaseImage(a), 7);

                        CommonWord mates = new CommonWord(7, "Matemáticas", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fmatem%C3%A1ticas_foto.jpg?alt=media&token=062c1afb-3fb7-4fa3-95e2-20d1b619a58c", "gs://comtietea.appspot.com/images/default/matemáticas_foto.jpg"), 7);

                        CommonWord biologia = new CommonWord(8, "Biología", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fbiolog%C3%ADa_foto.jpg?alt=media&token=ec362bc4-ba24-451b-b678-01e7bfdc2d3a", "gs://comtietea.appspot.com/images/default/biología_foto.jpg"), 7);

                        CommonWord musica = new CommonWord(9, "Música", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fmusica_foto.jpg?alt=media&token=153d691b-146d-4c0c-8b03-b11563ff116a", "gs://comtietea.appspot.com/images/default/musica_foto.jpg"), 7);

                        CommonWord ingles = new CommonWord(10, "Inglés", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fingl%C3%A9s_foto.jpg?alt=media&token=9b94117c-2348-4104-a9c4-576e28d444b9", "gs://comtietea.appspot.com/images/default/inglés_foto.jpg"), 7);

                        CommonWord educacionFisica = new CommonWord(11, "Educación física", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fe.fisica_foto.jpg?alt=media&token=f49c13c7-3a5b-408d-bbc1-335142f28a47", "gs://comtietea.appspot.com/images/default/e.fisica_foto.jpg"), 7);

                        CommonWord pizarra = new CommonWord(12, "Pizarra", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fpizarra_foto.jpg?alt=media&token=3dec0ad0-9a5a-45f8-be0a-4ee1de9bb609", "gs://comtietea.appspot.com/images/default/pizarra_foto.jpg"), 6);

                        CommonWord mesa = new CommonWord(13, "Mesa", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fmesa_foto.jpg?alt=media&token=3d01f17d-925f-413b-b803-6c38d9c3810e", "gs://comtietea.appspot.com/images/default/mesa_foto.jpg"), 6);

                        CommonWord silla = new CommonWord(14, "Silla", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fsilla_foto.jpg?alt=media&token=bf9e2315-4e0a-468d-acd8-296fa6e9986f", "gs://comtietea.appspot.com/images/default/silla_foto.jpg"), 6);

                        CommonWord libro = new CommonWord(15, "Libro", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Flibro_foto.jpg?alt=media&token=476c8458-24ea-43be-a2f8-efe3a8480f5f", "gs://comtietea.appspot.com/images/default/libro_foto.jpg"), 5);

                        CommonWord cuaderno = new CommonWord(16, "Cuaderno", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcuaderno_foto.jpg?alt=media&token=fe594c87-2a70-4a66-9a38-6ad55e1bf0df", "gs://comtietea.appspot.com/images/default/cuaderno_foto.jpg"), 5);

                        CommonWord diccionario = new CommonWord(17, "Diccionario", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fdiccionario_foto.jpg?alt=media&token=167b36cf-1c2e-4bed-8137-af2692561862", "gs://comtietea.appspot.com/images/default/diccionario_foto.jpg"), 5);

                        CommonWord estuche = new CommonWord(18, "Estuche", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Festuche_foto.jpg?alt=media&token=419f14d7-2683-4c70-97e9-78d7899b734b", "gs://comtietea.appspot.com/images/default/estuche_foto.jpg"), 4);

                        CommonWord mochila = new CommonWord(19, "Mochila", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fmochila_foto.jpg?alt=media&token=a4352824-a306-4d64-b9b4-a1d788c41f48", "gs://comtietea.appspot.com/images/default/mochila_foto.jpg"), 4);

                        CommonWord boli = new CommonWord(20, "Bolígrafo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fboli_foto.jpg?alt=media&token=cb5c3a3f-8b95-484f-87b0-889da2360518", "gs://comtietea.appspot.com/images/default/boli_foto.jpg"), 3);

                        CommonWord lapiz = new CommonWord(21, "Lápiz", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Flapiz_foto.jpg?alt=media&token=7323a398-a15d-4741-a8bd-11dc96ec4384", "gs://comtietea.appspot.com/images/default/lapiz_foto.jpg"), 3);

                        CommonWord colores = new CommonWord(22, "Colores", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcolores_foto.jpg?alt=media&token=2401f766-5ffb-4479-8c70-8771f5f5098c", "gs://comtietea.appspot.com/images/default/colores_foto.jpg"), 2);

                        CommonWord goma = new CommonWord(23, "Goma", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fgoma_foto.jpg?alt=media&token=ef5639a0-4b9c-4a61-ad54-bfc3b3c08ef4", "gs://comtietea.appspot.com/images/default/goma_foto.jpg"), 2);

                        CommonWord sacapuntas = new CommonWord(24, "Sacapuntas", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fsacapuntas_foto.jpg?alt=media&token=2f027877-bcd4-4def-84fb-a9dd2615d2f9", "gs://comtietea.appspot.com/images/default/sacapuntas_foto.jpg"), 2);

                        CommonWord rotuladores = new CommonWord(25, "Rotuladores", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Frotuladores_foto.jpg?alt=media&token=a6fcbed6-3205-491b-a090-59228ba9c4ba", "gs://comtietea.appspot.com/images/default/rotuladores_foto.jpg"), 2);

                        CommonWord clase = new CommonWord(26, "Clase", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fclase_foto.jpg?alt=media&token=d5514b51-7063-408b-ba1b-f7b1f133c6e1", "gs://comtietea.appspot.com/images/default/clase_foto.jpg"), 1);

                        res.add(colegio);
                        res.add(profesor);
                        res.add(companero);
                        res.add(director);
                        res.add(recreo);
                        res.add(gimnasio);
                        res.add(lengua);
                        res.add(mates);
                        res.add(biologia);
                        res.add(musica);
                        res.add(ingles);
                        res.add(educacionFisica);
                        res.add(pizarra);
                        res.add(mesa);
                        res.add(silla);
                        res.add(libro);
                        res.add(cuaderno);
                        res.add(diccionario);
                        res.add(estuche);
                        res.add(mochila);
                        res.add(boli);
                        res.add(lapiz);
                        res.add(colores);
                        res.add(goma);
                        res.add(sacapuntas);
                        res.add(rotuladores);
                        res.add(clase);
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

                    case "Acciones":
                        CommonWord accion = new CommonWord(0, "Acción", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Facci%C3%B3n_foto.jpg?alt=media&token=9ddffbfa-6c46-4eca-8cbd-fce6f8a0f050", "gs://comtietea.appspot.com/images/default/acción_foto.jpg"), 10);

                        CommonWord beber = new CommonWord(1, "Beber", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fbeber_foto.jpg?alt=media&token=6eca3971-98f4-4e0c-995d-65b6eea7b288", "gs://comtietea.appspot.com/images/default/beber_foto.jpg"), 9);

                        CommonWord comer = new CommonWord(2, "Comer", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcomer_foto.jpg?alt=media&token=5cb6eeba-0435-4e82-bb4b-945b3239825c", "gs://comtietea.appspot.com/images/default/comer_foto.jpg"), 9);

                        CommonWord despertar = new CommonWord(3, "Despertar", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fdespertar_foto.jpg?alt=media&token=631a540c-3d82-4533-ab73-5a3902b67eb2", "gs://comtietea.appspot.com/images/default/despertar_foto.jpg"), 9);

                        CommonWord levantar = new CommonWord(4, "Levantar", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Flevantar_foto.jpg?alt=media&token=8983e915-9fa1-401f-a5c2-c3f9a3dd4f3a", "gs://comtietea.appspot.com/images/default/levantar_foto.jpg"), 9);

                        CommonWord dormir = new CommonWord(5, "Dormir", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fdormir_foto.jpg?alt=media&token=05ff5c9c-c4d8-44e0-90a9-6a3f0d147556", "gs://comtietea.appspot.com/images/default/dormir_foto.jpg"), 9);

                        CommonWord jugar = new CommonWord(6, "Jugar", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fjugar_foto.jpg?alt=media&token=c577febc-93d5-4587-8b84-524bf70d038d", "gs://comtietea.appspot.com/images/default/jugar_foto.jpg"), 8);

                        CommonWord trabajar = new CommonWord(7, "Trabajar", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Ftrabajar_foto.jpg?alt=media&token=f7652eb6-1b41-4432-8b54-0e156d42ce47", "gs://comtietea.appspot.com/images/default/trabajar_foto.jpg"), 8);

                        CommonWord estudiar = new CommonWord(8, "Estudiar", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Festudiar_foto.jpg?alt=media&token=a761f536-bda6-4cb0-8751-72d810b8f0c6", "gs://comtietea.appspot.com/images/default/estudiar_foto.jpg"), 8);

                        CommonWord sentar = new CommonWord(9, "Sentar", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fsentar_foto.jpg?alt=media&token=38a6f83e-3c40-40c5-a822-eb9665875c91", "gs://comtietea.appspot.com/images/default/sentar_foto.jpg"), 7);

                        CommonWord pedir = new CommonWord(10, "Pedir", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fpedir_foto.jpg?alt=media&token=b473f45c-0354-476a-bec6-781ff296696f", "gs://comtietea.appspot.com/images/default/pedir_foto.jpg"), 7);

                        CommonWord ver = new CommonWord(11, "Ver", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fver_foto.jpg?alt=media&token=e123677a-3dd6-493a-8880-a9b82d493703", "gs://comtietea.appspot.com/images/default/ver_foto.jpg"), 6);

                        CommonWord escuchar = new CommonWord(12, "Escuchar", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fescuchar_foto.jpg?alt=media&token=28d966ef-a72c-4d27-8521-442b55978295", "gs://comtietea.appspot.com/images/default/escuchar_foto.jpg"), 6);

                        CommonWord hablar = new CommonWord(13, "Hablar", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fhablar_foto.jpg?alt=media&token=87e6d19c-3317-4171-a638-95aeff8fa17e", "gs://comtietea.appspot.com/images/default/hablar_foto.jpg"), 6);

                        CommonWord oler = new CommonWord(14, "Oler", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Foler_foto.jpg?alt=media&token=0a7bbe95-9e21-4d0d-a752-1af82d44729d", "gs://comtietea.appspot.com/images/default/oler_foto.jpg"), 6);

                        CommonWord leer = new CommonWord(15, "Leer", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fleer_foto.jpg?alt=media&token=fb696c03-d9e6-4197-8bf7-078682c614b4", "gs://comtietea.appspot.com/images/default/leer_foto.jpg"), 5);

                        CommonWord escribir = new CommonWord(16, "Escribir", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fescribir_foto.jpg?alt=media&token=cc958a85-cf22-4b1d-8b76-1ad3bbcde4c9", "gs://comtietea.appspot.com/images/default/escribir_foto.jpg"), 5);

                        CommonWord dibujar = new CommonWord(17, "Dibujar", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fdibujar_foto.jpg?alt=media&token=316695ce-e506-4340-9455-aaf1f830e302", "gs://comtietea.appspot.com/images/default/dibujar_foto.jpg"), 5);

                        CommonWord pintar = new CommonWord(18, "Pintar", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fpintar_foto.jpg?alt=media&token=aa167b74-db14-42fa-815b-5b02596752f5", "gs://comtietea.appspot.com/images/default/pintar_foto.jpg"), 5);

                        CommonWord morir = new CommonWord(19, "Morir", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fmorir_foto.JPG?alt=media&token=ec848cbc-ba2f-4fca-89a9-4f926d6d713c", "gs://comtietea.appspot.com/images/default/morir_foto.JPG"), 4);

                        CommonWord nacer = new CommonWord(20, "Nacer", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fnacer_foto.jpg?alt=media&token=51a49460-fefc-45bc-a39c-97eb7a057838", "gs://comtietea.appspot.com/images/default/nacer_foto.jpg"), 4);

                        CommonWord correr = new CommonWord(21, "Correr", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcorrer_foto.jpg?alt=media&token=e6b42b23-e10c-460e-9830-52e1a4066d8a", "gs://comtietea.appspot.com/images/default/correr_foto.jpg"), 4);

                        CommonWord ensenar = new CommonWord(22, "Enseñar", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fense%C3%B1ar_foto.jpg?alt=media&token=af9d96ef-2c44-4f87-a3f2-1ffc571055d4", "gs://comtietea.appspot.com/images/default/enseñar_foto.jpg"), 4);

                        CommonWord dudar = new CommonWord(23, "Dudar", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fdudar_foto.jpg?alt=media&token=7aa182b0-43e0-4b07-bcf8-5d721b9b7376", "gs://comtietea.appspot.com/images/default/dudar_foto.jpg"), 4);

                        CommonWord peinar = new CommonWord(24, "Peinar", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fpeinar_foto.jpg?alt=media&token=645c463b-767b-4ae9-acae-8fe921ceed8d", "gs://comtietea.appspot.com/images/default/peinar_foto.jpg"), 3);

                        CommonWord secar = new CommonWord(25, "Secar", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fsecar_foto.jpg?alt=media&token=6ea4c952-3b9a-4eb3-ab03-cb57b8a941fd", "gs://comtietea.appspot.com/images/default/secar_foto.jpg"), 3);

                        CommonWord cepillarDientes = new CommonWord(26, "Cepillarse los dientes", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcepillarse_dientes_foto.jpg?alt=media&token=9e32a39c-cc44-4ea0-ac61-edfd3abc3fca", "gs://comtietea.appspot.com/images/default/cepillarse_dientes_foto.jpg"), 3);

                        CommonWord lavar = new CommonWord(27, "Lavar", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Flavar_foto.jpg?alt=media&token=8c666629-afed-4c0e-a633-fe597e53a0a3", "gs://comtietea.appspot.com/images/default/lavar_foto.jpg"), 3);

                        CommonWord limpiar = new CommonWord(28, "Limpiar", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Flimpiar_foto.jpg?alt=media&token=4c009fea-1017-4113-a322-6fabdfe196ae", "gs://comtietea.appspot.com/images/default/limpiar_foto.jpg"), 3);

                        CommonWord ir = new CommonWord(29, "Ir", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fir_foto.jpg?alt=media&token=a4535d5f-f3f1-4011-909b-4559ca270737", "gs://comtietea.appspot.com/images/default/ir_foto.jpg"), 2);

                        CommonWord pensar = new CommonWord(30, "Pensar", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fpensar_foto.jpg?alt=media&token=d489f199-eb94-4069-af4f-b6d21f870804", "gs://comtietea.appspot.com/images/default/pensar_foto.jpg"), 2);

                        CommonWord abrazar = new CommonWord(31, "Abrazar", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fabrazar_foto.jpg?alt=media&token=c31c7b7c-8b94-4e80-97e3-491127cff863", "gs://comtietea.appspot.com/images/default/abrazar_foto.jpg"), 1);

                        CommonWord besar = new CommonWord(32, "Besar", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fbesar_foto.jpg?alt=media&token=aee48146-7913-458b-806e-33b11ad3bbf6", "gs://comtietea.appspot.com/images/default/besar_foto.jpg"), 1);

                        CommonWord querer = new CommonWord(33, "Querer", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fquerer_foto.jpg?alt=media&token=6e2ccb01-8a61-450d-b1b8-5f63e5754f1b", "gs://comtietea.appspot.com/images/default/querer_foto.jpg"), 1);

                        res.add(accion);
                        res.add(beber);
                        res.add(comer);
                        res.add(despertar);
                        res.add(levantar);
                        res.add(dormir);
                        res.add(jugar);
                        res.add(trabajar);
                        res.add(estudiar);
                        res.add(sentar);
                        res.add(pedir);
                        res.add(ver);
                        res.add(escuchar);
                        res.add(hablar);
                        res.add(oler);
                        res.add(leer);
                        res.add(escribir);
                        res.add(dibujar);
                        res.add(pintar);
                        res.add(morir);
                        res.add(nacer);
                        res.add(correr);
                        res.add(ensenar);
                        res.add(dudar);
                        res.add(peinar);
                        res.add(secar);
                        res.add(cepillarDientes);
                        res.add(lavar);
                        res.add(limpiar);
                        res.add(ir);
                        res.add(pensar);
                        res.add(abrazar);
                        res.add(besar);
                        res.add(querer);
                        break;

                    case "Casa":
                        CommonWord casa = new CommonWord(0, "Casa", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcasa_foto.jpg?alt=media&token=09e4b609-19dd-406e-a0a2-56c478bf184e", "gs://comtietea.appspot.com/images/default/casa_foto.jpg"), 10);

                        CommonWord cocina = new CommonWord(1, "Cocina", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcocina_foto.jpg?alt=media&token=753ba696-6a0b-458b-9ce0-b5dc405629b3", "gs://comtietea.appspot.com/images/default/cocina_foto.jpg"), 9);

                        CommonWord bano = new CommonWord(2, "Baño", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fba%C3%B1o_foto.jpg?alt=media&token=592975e1-f5c7-40b3-bc7c-6414274a2ad0", "gs://comtietea.appspot.com/images/default/baño_foto.jpg"), 8);

                        CommonWord dormitorio = new CommonWord(3, "Dormitorio", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fdormitorio_foto.jpg?alt=media&token=e515e8cc-0c4f-4a61-98ac-e493f624bfdf", "gs://comtietea.appspot.com/images/default/dormitorio_foto.jpg"), 7);

                        CommonWord salon = new CommonWord(4, "Salón", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fsal%C3%B3n_foto.jpg?alt=media&token=fc506c78-2480-453f-91e2-794cd844bb63", "gs://comtietea.appspot.com/images/default/salón_foto.jpg"), 6);

                        CommonWord comedor = new CommonWord(5, "Comedor", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcomedor_foto.jpg?alt=media&token=3a543719-b2e2-4619-90ed-6d9ea821401b", "gs://comtietea.appspot.com/images/default/comedor_foto.jpg"), 5);

                        CommonWord jardin = new CommonWord(6, "Jardín", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fjard%C3%ADn_foto.jpg?alt=media&token=29798fdc-e96a-464d-a795-c3f3bb6317b5", "gs://comtietea.appspot.com/images/default/jardín_foto.jpg"), 4);

                        CommonWord balcon = new CommonWord(7, "Balcón", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fbalc%C3%B3n_foto.jpg?alt=media&token=1b250b2a-20ee-4942-8738-00320ce558ab", "gs://comtietea.appspot.com/images/default/balcón_foto.jpg"), 3);

                        res.add(casa);
                        res.add(cocina);
                        res.add(bano);
                        res.add(dormitorio);
                        res.add(salon);
                        res.add(comedor);
                        res.add(jardin);
                        res.add(balcon);
                        break;

                    case "Ocio":
                        CommonWord ocio = new CommonWord(0, "Ocio", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Focio_foto.jpg?alt=media&token=9f8fdc3b-6705-42e2-a29c-7a55ab1f77b5", "gs://comtietea.appspot.com/images/default/ocio_foto.jpg"), 10);

                        CommonWord videojuego = new CommonWord(1, "Videojuego", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fvideojuego_foto.jpg?alt=media&token=ed8ffe94-b528-41dc-9002-c8d1cf5a5037", "gs://comtietea.appspot.com/images/default/videojuego_foto.jpg"), 9);

                        CommonWord juegoMesa = new CommonWord(2, "Juego de mesa", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fjuego_de_mesa_foto.jpg?alt=media&token=e20e4002-1c27-4839-8ad3-85f0d9892b18", "gs://comtietea.appspot.com/images/default/juego_de_mesa_foto.jpg"), 9);

                        CommonWord juguete = new CommonWord(3, "Juguete", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fjuguete_foto.jpg?alt=media&token=71d99fcf-95f2-43a7-8a0b-63f593b6e266", "gs://comtietea.appspot.com/images/default/juguete_foto.jpg"), 8);

                        CommonWord pelota = new CommonWord(4, "Pelota", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fpelota_foto.jpg?alt=media&token=00b62e64-26b8-464b-bf2e-997bfea12eef", "gs://comtietea.appspot.com/images/default/pelota_foto.jpg"), 8);

                        CommonWord cine = new CommonWord(5, "Cine", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcine_foto.jpg?alt=media&token=992563bc-7ba0-48fe-bfe3-51c5fbaa38a6", "gs://comtietea.appspot.com/images/default/cine_foto.jpg"), 7);

                        CommonWord escucharMusica = new CommonWord(6, "Escuchar música", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fescuchar_musica_foto.jpg?alt=media&token=e2af2a28-2021-4893-beda-0a4aff697836", "gs://comtietea.appspot.com/images/default/escuchar_musica_foto.jpg"), 7);

                        CommonWord baile = new CommonWord(7, "Baile", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fbaile_foto.png?alt=media&token=c47a9dd4-ff9b-4359-947e-1b283e74e9b8", "gs://comtietea.appspot.com/images/default/baile_foto.png"), 7);

                        CommonWord teatro = new CommonWord(8, "Teatro", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fteatro_foto.jpg?alt=media&token=1b34df54-2fba-4e67-89fc-f99aa5ae7d8e", "gs://comtietea.appspot.com/images/default/teatro_foto.jpg"), 7);

                        CommonWord lectura = new CommonWord(9, "Lectura", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Flectura_foto.jpg?alt=media&token=c13824c1-1762-40b8-8fe6-a42d4e55a024", "gs://comtietea.appspot.com/images/default/lectura_foto.jpg"), 6);

                        CommonWord viajar = new CommonWord(10, "Viajar", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fviajar_foto.jpg?alt=media&token=4819090e-3c5c-4f5a-bf83-2357b2fb9b3a", "gs://comtietea.appspot.com/images/default/viajar_foto.jpg"), 6);

                        CommonWord deporte = new CommonWord(11, "Deporte", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fdeporte_foto.jpg?alt=media&token=e785a68b-1666-44c8-990d-21aee5c157ee", "gs://comtietea.appspot.com/images/default/deporte_foto.jpg"), 5);

                        CommonWord futbol = new CommonWord(12, "Fútbol", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Ffutbol_foto.jpg?alt=media&token=5e56aa69-0ee4-43b9-80c4-3f098e412c37", "gs://comtietea.appspot.com/images/default/futbol_foto.jpg"), 4);

                        CommonWord baloncesto = new CommonWord(13, "Baloncesto", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fbaloncesto_foto.jpg?alt=media&token=8a3644a0-ca8d-4dc3-b847-07e409fb3c1c", "gs://comtietea.appspot.com/images/default/baloncesto_foto.jpg"), 4);

                        CommonWord tenis = new CommonWord(14, "Tenis", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Ftenis_foto.jpg?alt=media&token=f63fcc5c-4b0e-4890-9478-f3aa083d70fc", "gs://comtietea.appspot.com/images/default/tenis_foto.jpg"), 4);

                        CommonWord gimnasia = new CommonWord(15, "Gimnasia", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fgimnasia_foto.jpg?alt=media&token=fdefacc2-c0e1-4bbb-addb-4945721cd50a", "gs://comtietea.appspot.com/images/default/gimnasia_foto.jpg"), 3);

                        CommonWord padel = new CommonWord(16, "Pádel", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fpadel_foto.jpg?alt=media&token=dcda83eb-80a6-4591-b5c6-0ec431c785dc", "gs://comtietea.appspot.com/images/default/padel_foto.jpg"), 3);

                        CommonWord ciclismo = new CommonWord(17, "Ciclismo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fciclismo_foto.jpg?alt=media&token=2823fc3a-d3bc-4ebd-ae5f-11c5cc2579d6", "gs://comtietea.appspot.com/images/default/ciclismo_foto.jpg"), 3);

                        CommonWord natacion = new CommonWord(18, "Natación", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fnataci%C3%B3n_foto.jpg?alt=media&token=8ea277b6-82f7-46a3-8f75-bf26297dd400", "gs://comtietea.appspot.com/images/default/natación_foto.jpg"), 3);

                        CommonWord parque = new CommonWord(19, "Parque", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fparque_foto.jpg?alt=media&token=8c218c6a-289b-453b-afe5-12ec82ec4641", "gs://comtietea.appspot.com/images/default/parque_foto.jpg"), 2);

                        res.add(ocio);
                        res.add(videojuego);
                        res.add(juegoMesa);
                        res.add(juguete);
                        res.add(pelota);
                        res.add(cine);
                        res.add(escucharMusica);
                        res.add(baile);
                        res.add(teatro);
                        res.add(lectura);
                        res.add(viajar);
                        res.add(deporte);
                        res.add(futbol);
                        res.add(baloncesto);
                        res.add(tenis);
                        res.add(gimnasia);
                        res.add(padel);
                        res.add(ciclismo);
                        res.add(natacion);
                        res.add(parque);
                        break;

                    case "Animales":
                        CommonWord animal = new CommonWord(0, "Animal", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fanimales_foto.PNG?alt=media&token=b2a00091-15ba-4fad-b067-0621ec5018ad", "gs://comtietea.appspot.com/images/default/animales_foto.PNG"), 10);

                        CommonWord perro = new CommonWord(1, "Perro", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fperro_foto.jpg?alt=media&token=ada6b277-95d8-43e1-b1c3-4ac0d895374b", "gs://comtietea.appspot.com/images/default/perro_foto.jpg"), 9);

                        CommonWord gato = new CommonWord(2, "Gato", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fgato_foto.jpg?alt=media&token=e09ef439-8d9e-41b1-97f7-58e8e972390d", "gs://comtietea.appspot.com/images/default/gato_foto.jpg"), 9);

                        CommonWord conejo = new CommonWord(3, "Conejo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fconejo_foto.jpg?alt=media&token=bc64adaa-9434-4d50-8733-b97dd3782e7b", "gs://comtietea.appspot.com/images/default/conejo_foto.jpg"), 8);

                        CommonWord pajaro = new CommonWord(4, "Pájaro", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fpajaro_foto.jpg?alt=media&token=a1296dd5-318d-4846-9134-15ed9c2bd22c", "gs://comtietea.appspot.com/images/default/pajaro_foto.jpg"), 8);

                        CommonWord pez = new CommonWord(5, "Pez", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fpez_foto.jpg?alt=media&token=3bdbfa45-7e80-4ff1-b842-25cfbe805a59", "gs://comtietea.appspot.com/images/default/pez_foto.jpg"), 8);

                        CommonWord hamster = new CommonWord(6, "Hamster", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fhamster_foto.jpg?alt=media&token=23f6b6e6-6bef-4733-8775-8f86acc516a0", "gs://comtietea.appspot.com/images/default/hamster_foto.jpg"), 8);

                        CommonWord leon = new CommonWord(7, "León", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fle%C3%B3n_foto.jpg?alt=media&token=0e4e313c-b250-48d6-b99a-44a65ce12f57", "gs://comtietea.appspot.com/images/default/león_foto.jpg"), 7);

                        CommonWord tigre = new CommonWord(8, "Tigre", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Ftigre_foto.jpg?alt=media&token=e172b5fe-2faf-4d73-acb2-6d4b245a6eab", "gs://comtietea.appspot.com/images/default/tigre_foto.jpg"), 7);

                        CommonWord jirafa = new CommonWord(9, "Jirafa", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fjirafa_foto.jpg?alt=media&token=53aa2d47-5a7b-4e42-9507-688200a35f23", "gs://comtietea.appspot.com/images/default/jirafa_foto.jpg"), 7);

                        CommonWord cebra = new CommonWord(10, "Cebra", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcebra_foto.jpg?alt=media&token=7f41b3de-742d-42f7-8c6c-10c6fdee2cf0", "gs://comtietea.appspot.com/images/default/cebra_foto.jpg"), 6);

                        CommonWord oso = new CommonWord(11, "Oso", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Foso_foto.jpg?alt=media&token=88e4ef69-781e-46e5-a0e7-51e8fce4e1b4", "gs://comtietea.appspot.com/images/default/oso_foto.jpg"), 6);

                        CommonWord serpiente = new CommonWord(12, "Serpiente", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fserpiente_foto.jpg?alt=media&token=61eb81c3-b613-4262-88a2-1787e648ed73", "gs://comtietea.appspot.com/images/default/serpiente_foto.jpg"), 6);

                        CommonWord mariposa = new CommonWord(13, "Mariposa", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fmariposa_foto.jpg?alt=media&token=ccbaa4f4-96e8-4742-83dd-84813cea91a5", "gs://comtietea.appspot.com/images/default/mariposa_foto.jpg"), 5);

                        CommonWord mosquito = new CommonWord(14, "Mosquito", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fmosquito_foto.jpg?alt=media&token=57c9424f-177d-4a14-a3ca-ab41a1dfa06e", "gs://comtietea.appspot.com/images/default/mosquito_foto.jpg"), 5);

                        CommonWord arana = new CommonWord(15, "Araña", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fara%C3%B1a_foto.jpg?alt=media&token=9d625bd8-f857-4dc7-9ccf-c7c0bd3916b6", "gs://comtietea.appspot.com/images/default/araña_foto.jpg"), 5);

                        CommonWord tiburon = new CommonWord(16, "Tiburón", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Ftiburon_foto.jpg?alt=media&token=1b6fd97a-de14-4a94-9ebe-1df05ab4dd0a", "gs://comtietea.appspot.com/images/default/tiburon_foto.jpg"), 4);

                        CommonWord ballena = new CommonWord(17, "Ballena", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fballena_foto.jpg?alt=media&token=a8825f29-b71e-4d17-acec-4d860c035551", "gs://comtietea.appspot.com/images/default/ballena_foto.jpg"), 4);

                        CommonWord delfin = new CommonWord(18, "Delfín", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fdelfin_foto.png?alt=media&token=93b13b42-7295-4d04-ac3a-747ebf2da09e", "gs://comtietea.appspot.com/images/default/delfin_foto.png"), 4);

                        CommonWord caballo = new CommonWord(19, "Caballo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcaballo_foto.jpg?alt=media&token=ac58f0e6-76f4-414d-b47f-496ac5e3197d", "gs://comtietea.appspot.com/images/default/caballo_foto.jpg"), 3);

                        CommonWord cerdo = new CommonWord (20, "Cerdo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcerdo_foto.jpg?alt=media&token=f4caa2af-c0b8-455c-a6ee-5daba1e2c5f0", "gs://comtietea.appspot.com/images/default/cerdo_foto.jpg"), 3);

                        CommonWord gallina = new CommonWord(21, "Gallina", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fgallina_foto.png?alt=media&token=99a7e8ed-1d60-4490-9d7d-6c803606fb0d", "gs://comtietea.appspot.com/images/default/gallina_foto.png"), 3);

                        CommonWord oveja = new CommonWord(22, "Oveja", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Foveja_foto.jpg?alt=media&token=8c548ccf-9087-4076-9e22-b4c8ac019a94", "gs://comtietea.appspot.com/images/default/oveja_foto.jpg"), 3);

                        CommonWord vaca = new CommonWord(23, "Vaca", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fvaca_foto.jpg?alt=media&token=9f5940fb-2f3d-44ca-bc80-f3d7f8c1dcc1", "gs://comtietea.appspot.com/images/default/vaca_foto.jpg"), 3);

                        CommonWord elefante = new CommonWord(24, "Elefante", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Felefante_foto.jpg?alt=media&token=07345e30-2d54-4863-bb05-7b0ea5b76630", "gs://comtietea.appspot.com/images/default/elefante_foto.jpg"), 2);

                        CommonWord mono = new CommonWord(25, "Mono", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fmono_foto.jpg?alt=media&token=36951db9-5f1a-493a-b8ac-b0517fedc0b1", "gs://comtietea.appspot.com/images/default/mono_foto.jpg"), 2);

                        res.add(animal);
                        res.add(perro);
                        res.add(gato);
                        res.add(conejo);
                        res.add(pajaro);
                        res.add(pez);
                        res.add(hamster);
                        res.add(leon);
                        res.add(tigre);
                        res.add(jirafa);
                        res.add(cebra);
                        res.add(oso);
                        res.add(serpiente);
                        res.add(mariposa);
                        res.add(mosquito);
                        res.add(arana);
                        res.add(tiburon);
                        res.add(ballena);
                        res.add(delfin);
                        res.add(caballo);
                        res.add(cerdo);
                        res.add(gallina);
                        res.add(oveja);
                        res.add(vaca);
                        res.add(elefante);
                        res.add(mono);
                        break;

                    case "Colores":
                        CommonWord color = new CommonWord(0, "Color", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcolor_foto.jpg?alt=media&token=f8b48ec4-f686-4d84-b805-68a14cb0ac5b", "gs://comtietea.appspot.com/images/default/color_foto.jpg"), 10);

                        CommonWord negro = new CommonWord(1, "Negro", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fnegro.PNG?alt=media&token=1a51e037-6eb5-4516-996b-d34a322d200e", "gs://comtietea.appspot.com/images/default/negro.PNG"), 9);

                        CommonWord blanco = new CommonWord(2, "Blanco", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fblanco.PNG?alt=media&token=93486d07-3935-44f3-bfc2-c948078183ae", "gs://comtietea.appspot.com/images/default/blanco.PNG"), 9);

                        CommonWord azul = new CommonWord(3, "Azul", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fazul.PNG?alt=media&token=30ffbce4-8587-4742-92a4-dae9d927ab39", "gs://comtietea.appspot.com/images/default/azul.PNG"), 8);

                        CommonWord verde = new CommonWord(4, "Verde", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fverde.PNG?alt=media&token=a24733f3-b560-4767-aee7-19ffdf2c8ecc", "gs://comtietea.appspot.com/images/default/verde.PNG"), 8);

                        CommonWord rosa = new CommonWord(5, "Rosa", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Frosa.PNG?alt=media&token=a40a0b8a-e5d2-48af-9b45-653b40d4409a", "gs://comtietea.appspot.com/images/default/rosa.PNG"), 7);

                        CommonWord rojo = new CommonWord(6, "Rojo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Frojo.PNG?alt=media&token=f2be6ee5-dcb0-4df7-bcf6-6889fe345fdb", "gs://comtietea.appspot.com/images/default/rojo.PNG"), 7);

                        CommonWord amarillo = new CommonWord(7, "Amarillo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Famarillo.PNG?alt=media&token=88f6388c-ce7d-445d-a300-de15219edaf8", "gs://comtietea.appspot.com/images/default/amarillo.PNG"), 6);

                        CommonWord naranja = new CommonWord(8, "Naranja", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fnaranja.PNG?alt=media&token=03c9e9fd-cab4-4080-9550-62f7668c608b", "gs://comtietea.appspot.com/images/default/naranja.PNG"), 5);

                        CommonWord marron = new CommonWord(9, "Marrón", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fmarr%C3%B3n.PNG?alt=media&token=e757ed4c-16de-4376-b2d2-d6f6c959fee2", "gs://comtietea.appspot.com/images/default/marrón.PNG"), 4);

                        CommonWord morado = new CommonWord(10, "Morado", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fmorado.PNG?alt=media&token=bf63525c-09e9-4a72-867c-01ec6d4b7ef4", "gs://comtietea.appspot.com/images/default/morado.PNG"), 3);

                        CommonWord gris = new CommonWord(11, "Gris", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fgris.PNG?alt=media&token=c7836319-e4a7-410f-8157-52419c2eb4c0", "gs://comtietea.appspot.com/images/default/gris.PNG"), 2);

                        res.add(color);
                        res.add(negro);
                        res.add(blanco);
                        res.add(azul);
                        res.add(verde);
                        res.add(rosa);
                        res.add(rojo);
                        res.add(amarillo);
                        res.add(naranja);
                        res.add(marron);
                        res.add(morado);
                        res.add(gris);
                        break;

                    case "Adjetivos":
                        CommonWord adjetivo = new CommonWord(0, "Adjetivo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fadjetivo_foto.jpg?alt=media&token=dd868878-be30-4aca-b49f-4b889477ffbc", "gs://comtietea.appspot.com/images/default/adjetivo_foto.jpg"), 10);

                        CommonWord grande = new CommonWord(1, "Grande", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fgrande_foto.PNG?alt=media&token=648c9c49-3d4e-4cca-a907-02034e1fcec5", "gs://comtietea.appspot.com/images/default/grande_foto.PNG"), 9);

                        CommonWord pequeno = new CommonWord(2, "Pequeño", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fpeque%C3%B1o_foto.PNG?alt=media&token=66b0144c-f687-42c2-a1b6-b1ee6cd96587", "gs://comtietea.appspot.com/images/default/pequeño_foto.PNG"), 9);

                        CommonWord alto = new CommonWord(3, "Alto", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Falto_foto.PNG?alt=media&token=550a2857-6cea-4b1f-a50d-fed728370d88", "gs://comtietea.appspot.com/images/default/alto_foto.PNG"), 8);

                        CommonWord bajo = new CommonWord(4, "Bajo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fbajo_foto.PNG?alt=media&token=6b579177-ded4-44fe-b8c0-3ea3275cc485", "gs://comtietea.appspot.com/images/default/bajo_foto.PNG"), 8);

                        CommonWord largo = new CommonWord(5, "Largo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Flargo_foto.jpg?alt=media&token=2a758ac1-d3bb-413c-ab21-e2ebc4c6fa66", "gs://comtietea.appspot.com/images/default/largo_foto.jpg"), 7);

                        CommonWord corto = new CommonWord(6, "Corto", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcorto_foto.jpg?alt=media&token=b0b458d4-7aee-4352-9bd7-22983cbd3f55", "gs://comtietea.appspot.com/images/default/corto_foto.jpg"), 7);

                        CommonWord gordo = new CommonWord(7, "Gordo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fgordo_foto.jpg?alt=media&token=339f9acc-2233-454a-aa81-416a8e4cd437", "gs://comtietea.appspot.com/images/default/gordo_foto.jpg"), 6);

                        CommonWord delgado = new CommonWord(8, "Delgado", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fdelgado_foto.jpg?alt=media&token=3a2f970d-dee3-44b8-bc31-6ed3a5a2adc4", "gs://comtietea.appspot.com/images/default/delgado_foto.jpg"), 6);

                        CommonWord pesado = new CommonWord(9, "Pesado", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fpesado_foto.PNG?alt=media&token=f8d1a020-329a-4f82-a6ea-e39edc494aec", "gs://comtietea.appspot.com/images/default/pesado_foto.PNG"), 5);

                        CommonWord ligero = new CommonWord(10, "Ligero", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fligero_foto.jpg?alt=media&token=3366802a-1a09-4b71-bad7-2602ad9637c8", "gs://comtietea.appspot.com/images/default/ligero_foto.jpg"), 5);

                        CommonWord claro = new CommonWord(11, "Claro", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fclaro_foto.jpg?alt=media&token=32c4a8d7-5781-4be2-80bc-522ff999a909", "gs://comtietea.appspot.com/images/default/claro_foto.jpg"), 4);

                        CommonWord oscuro = new CommonWord(12, "Oscuro", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Foscuro_foto.jpg?alt=media&token=caac8b37-3bc6-4e4e-87bc-14dcc45bf743", "gs://comtietea.appspot.com/images/default/oscuro_foto.jpg"), 4);

                        CommonWord viejo = new CommonWord(13, "Viejo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fviejo_foto.jpg?alt=media&token=10c44742-0475-40ce-8584-437aa715b9e4", "gs://comtietea.appspot.com/images/default/viejo_foto.jpg"), 3);

                        CommonWord nuevo = new CommonWord(14, "Nuevo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fnuevo_foto.jpg?alt=media&token=09449325-6252-469c-88d4-30da6e46da73", "gs://comtietea.appspot.com/images/default/nuevo_foto.jpg"), 3);

                        CommonWord joven = new CommonWord(15, "Joven", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fjoven_foto.jpg?alt=media&token=dcee0abd-f0ce-4124-b527-b96eadc2cd21", "gs://comtietea.appspot.com/images/default/joven_foto.jpg"), 3);

                        CommonWord antiguo = new CommonWord(16, "Antiguo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fantiguo_foto.jpg?alt=media&token=9e6056f2-0049-4640-af97-fe5985b68676", "gs://comtietea.appspot.com/images/default/antiguo_foto.jpg"), 3);

                        CommonWord liso = new CommonWord(17, "Liso", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fliso_foto.jpg?alt=media&token=4a886630-c9bf-4c5c-a89a-49261b04b131", "gs://comtietea.appspot.com/images/default/liso_foto.jpg"), 2);

                        CommonWord rugoso = new CommonWord(18, "Rugoso", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Farrugado_foto.jpg?alt=media&token=cf4d281c-07fd-4665-b738-e8dbc142c3a8", "gs://comtietea.appspot.com/images/default/arrugado_foto.jpg"), 2);

                        CommonWord feo = new CommonWord(19, "Feo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Ffeo_foto.jpg?alt=media&token=e4b309e8-cf88-4be0-a7a8-791a36374c46", "gs://comtietea.appspot.com/images/default/feo_foto.jpg"), 1);

                        CommonWord guapo = new CommonWord (20, "Guapo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fguapa_foto.jpg?alt=media&token=da273a20-efae-4e92-905f-15fd03a680d9", "gs://comtietea.appspot.com/images/default/guapa_foto.jpg"), 1);

                        res.add(adjetivo);
                        res.add(grande);
                        res.add(pequeno);
                        res.add(alto);
                        res.add(bajo);
                        res.add(largo);
                        res.add(corto);
                        res.add(gordo);
                        res.add(delgado);
                        res.add(pesado);
                        res.add(ligero);
                        res.add(claro);
                        res.add(oscuro);
                        res.add(viejo);
                        res.add(nuevo);
                        res.add(joven);
                        res.add(antiguo);
                        res.add(liso);
                        res.add(rugoso);
                        res.add(feo);
                        res.add(guapo);
                        break;

                    case "Emociones":
                        CommonWord emocion = new CommonWord(0, "Emoción", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Femociones_foto.jpg?alt=media&token=100bf43c-dc99-4548-ad46-484aee559575", "gs://comtietea.appspot.com/images/default/emociones_foto.jpg"), 10);

                        CommonWord tristeza = new CommonWord(1, "Tristeza", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Ftristeza_foto.jpg?alt=media&token=26455a01-38df-44ec-bea9-4d712a5ff41d", "gs://comtietea.appspot.com/images/default/tristeza_foto.jpg"), 9);

                        CommonWord felicidad = new CommonWord(2, "Felicidad", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Ffeliz_foto.jpg?alt=media&token=0ba5472b-7212-4791-9ee8-e30694678c32", "gs://comtietea.appspot.com/images/default/feliz_foto.jpg"), 8);

                        CommonWord miedo = new CommonWord(3, "Miedo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fmiedo_foto.jpg?alt=media&token=4c5f6c58-45ab-4ad4-bd9c-bea8d497e311", "gs://comtietea.appspot.com/images/default/miedo_foto.jpg"), 7);

                        CommonWord asco = new CommonWord(4, "Asco", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fasco_foto.jpg?alt=media&token=07957fc5-9f50-4618-b35c-c058495944c6", "gs://comtietea.appspot.com/images/default/asco_foto.jpg"), 6);

                        CommonWord enfado = new CommonWord(5, "Enfado", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fenfado_foto.jpg?alt=media&token=9c5c0c1e-3b85-4979-b6d2-9c14ea937924", "gs://comtietea.appspot.com/images/default/enfado_foto.jpg"), 5);

                        CommonWord sorpresa = new CommonWord(6, "Sorpresa", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fsorpresa_foto.jpg?alt=media&token=fd92f525-3433-4f15-97a5-2457519c8dc8", "gs://comtietea.appspot.com/images/default/sorpresa_foto.jpg"), 4);

                        CommonWord amor = new CommonWord(7, "Amor", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Famor_foto.jpg?alt=media&token=35672da2-cd57-455b-8238-3f7c8d7a5d95", "gs://comtietea.appspot.com/images/default/amor_foto.jpg"), 3);

                        res.add(emocion);
                        res.add(tristeza);
                        res.add(felicidad);
                        res.add(miedo);
                        res.add(asco);
                        res.add(enfado);
                        res.add(sorpresa);
                        res.add(amor);
                        break;

                    case "Profesiones":
                        CommonWord profesion = new CommonWord(0, "Profesión", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fprofesi%C3%B3n_foto.jpg?alt=media&token=d6731d11-2abe-4387-8640-442a0e764747", "gs://comtietea.appspot.com/images/default/profesión_foto.jpg"), 10);

                        CommonWord profesora = new CommonWord(1, "Profesora", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fprofesora_foto.jpg?alt=media&token=108b7f87-b728-489c-8523-e56a541b1f83", "gs://comtietea.appspot.com/images/default/profesora_foto.jpg"), 9);

                        CommonWord medico = new CommonWord(2, "Médico", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fmedico_foto.jpg?alt=media&token=fb8aeaba-d763-4244-b66f-53df9c6e4651", "gs://comtietea.appspot.com/images/default/medico_foto.jpg"), 9);

                        CommonWord policia = new CommonWord(3, "Policía", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fpolicia_foto.jpg?alt=media&token=e9ad0800-493f-43dc-8c15-a1ef3e8f82b2", "gs://comtietea.appspot.com/images/default/policia_foto.jpg"), 9);

                        CommonWord bombero = new CommonWord(4, "Bombero", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fbombero_foto.jpg?alt=media&token=f8e93b40-42bf-4995-8f95-c519c59a9b56", "gs://comtietea.appspot.com/images/default/bombero_foto.jpg"), 8);

                        CommonWord camarero = new CommonWord(5, "Camarero", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcamarero_foto.jpg?alt=media&token=21031a5d-b3f1-4b33-a5a9-890e7ab666c5", "gs://comtietea.appspot.com/images/default/camarero_foto.jpg"), 7);

                        CommonWord electricista = new CommonWord(6, "Electricista", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Felectricista_foto.jpg?alt=media&token=af5c0799-1c9f-47ca-85a6-407bf5fafae4", "gs://comtietea.appspot.com/images/default/electricista_foto.jpg"), 6);

                        CommonWord enfermero = new CommonWord(7, "Enfermero", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fenfermo_foto.jpg?alt=media&token=22b881d2-036f-4871-8bd8-327f629bcbf0", "gs://comtietea.appspot.com/images/default/enfermo_foto.jpg"), 5);

                        CommonWord psicologo = new CommonWord(8, "Psicólogo", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fpsicologo_foto.jpg?alt=media&token=20642e53-b6fe-41d2-92f9-5db16db88d5c", "gs://comtietea.appspot.com/images/default/psicologo_foto.jpg"), 4);

                        CommonWord abogado = new CommonWord(9, "Abogado", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fabogado_foto.jpg?alt=media&token=2e96962a-172a-40aa-9024-5acff8f895f0", "gs://comtietea.appspot.com/images/default/abogado_foto.jpg"), 3);

                        CommonWord cocinero = new CommonWord(10, "Cocinero", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcocinero_foto.jpg?alt=media&token=8cdcd67d-50c0-4936-9a39-e39ee6ff4a63", "gs://comtietea.appspot.com/images/default/cocinero_foto.jpg"), 2);

                        res.add(profesion);
                        res.add(profesora);
                        res.add(medico);
                        res.add(policia);
                        res.add(bombero);
                        res.add(camarero);
                        res.add(electricista);
                        res.add(enfermero);
                        res.add(psicologo);
                        res.add(abogado);
                        res.add(cocinero);
                        break;

                    case "Comida":
                        CommonWord comida = new CommonWord(0, "Comida", new FirebaseImage(), 10);

                        CommonWord verdura = new CommonWord(1, "Verdura", new FirebaseImage(), 9);

                        CommonWord tomate = new CommonWord(2, "Tomate", new FirebaseImage(), 8);

                        CommonWord lechuga = new CommonWord(3, "Lechuga", new FirebaseImage(), 8);

                        CommonWord cebolla = new CommonWord(4, "Cebolla", new FirebaseImage(), 8);

                        CommonWord patatas = new CommonWord(5, "Patatas", new FirebaseImage(), 8);

                        CommonWord fruta = new CommonWord(6, "Fruta", new FirebaseImage(), 7);

                        CommonWord manzana = new CommonWord(7, "Manzana", new FirebaseImage(), 6);

                        CommonWord pera = new CommonWord(8, "Pera", new FirebaseImage(), 6);

                        CommonWord platano = new CommonWord(9, "Plátano", new FirebaseImage(), 6);

                        CommonWord fresa = new CommonWord(10, "Fresa", new FirebaseImage(), 6);

                        CommonWord melon = new CommonWord(11, "Melón", new FirebaseImage(), 6);

                        CommonWord sandia = new CommonWord(12, "Sandía", new FirebaseImage(), 6);

                        CommonWord cereza = new CommonWord(13, "Cereza", new FirebaseImage(), 6);

                        CommonWord pina = new CommonWord(14, "Piña", new FirebaseImage(), 6);

                        CommonWord pescado = new CommonWord(15, "Pescado", new FirebaseImage(), 5);

                        CommonWord carne = new CommonWord(16, "Carne", new FirebaseImage(), 4);

                        CommonWord pollo = new CommonWord(17, "Pollo", new FirebaseImage(), 3);

                        CommonWord filete = new CommonWord(18, "Filete", new FirebaseImage(), 3);

                        CommonWord hamburguesa = new CommonWord(19, "Hamburguesa", new FirebaseImage(), 3);

                        CommonWord salchicha = new CommonWord(20, "Salchicha", new FirebaseImage(), 3);

                        CommonWord bebida = new CommonWord(21, "Bebida", new FirebaseImage(), 2);

                        CommonWord agua = new CommonWord(22, "Agua", new FirebaseImage(), 2);

                        CommonWord cocacola = new CommonWord(23, "Coca-Cola", new FirebaseImage(), 2);

                        CommonWord refresco = new CommonWord(24, "Refresco", new FirebaseImage(), 2);

                        CommonWord zumo = new CommonWord(25, "Zumo", new FirebaseImage(), 2);

                        CommonWord leche = new CommonWord(26, "Leche", new FirebaseImage(), 2);

                        CommonWord batido = new CommonWord(27, "Batido", new FirebaseImage(), 2);

                        CommonWord colacao = new CommonWord(28, "Cola-Cao", new FirebaseImage(), 2);

                        CommonWord cafe = new CommonWord(29, "Café", new FirebaseImage(), 2);

                        CommonWord cerveza = new CommonWord(30, "Cerveza", new FirebaseImage(), 2);

                        CommonWord vino = new CommonWord(31, "Vino", new FirebaseImage(), 2);

                        CommonWord pizza = new CommonWord(32, "Pizza", new FirebaseImage(), 1);

                        CommonWord bocadillo = new CommonWord(33, "Bocadillo", new FirebaseImage(), 1);

                        CommonWord jamon = new CommonWord(34, "Jamón", new FirebaseImage(), 1);

                        CommonWord embutido = new CommonWord(35, "Embutido", new FirebaseImage(), 1);

                        CommonWord tortilla = new CommonWord(36, "Tortilla", new FirebaseImage(), 1);

                        CommonWord pasta = new CommonWord(37, "Pasta", new FirebaseImage(), 1);

                        CommonWord queso = new CommonWord(38, "Queso", new FirebaseImage(), 1);

                        CommonWord cereales = new CommonWord(39, "Cereales", new FirebaseImage(), 1);

                        CommonWord sopa = new CommonWord(40, "Sopa", new FirebaseImage(), 1);

                        CommonWord arroz = new CommonWord(41, "Arroz", new FirebaseImage(), 1);

                        CommonWord lentejas = new CommonWord(42, "Lentejas", new FirebaseImage(), 1);

                        CommonWord chocolate = new CommonWord(43, "Chocolate", new FirebaseImage(), 1);

                        CommonWord huevosFritos = new CommonWord(44, "Huevos fritos", new FirebaseImage(), 1);

                        CommonWord paella = new CommonWord(45, "Paella", new FirebaseImage(), 1);

                        res.add(comida);
                        res.add(verdura);
                        res.add(tomate);
                        res.add(lechuga);
                        res.add(cebolla);
                        res.add(patatas);
                        res.add(fruta);
                        res.add(manzana);
                        res.add(pera);
                        res.add(platano);
                        res.add(fresa);
                        res.add(melon);
                        res.add(sandia);
                        res.add(cereza);
                        res.add(pina);
                        res.add(pescado);
                        res.add(carne);
                        res.add(pollo);
                        res.add(filete);
                        res.add(hamburguesa);
                        res.add(salchicha);
                        res.add(bebida);
                        res.add(agua);
                        res.add(cocacola);
                        res.add(refresco);
                        res.add(zumo);
                        res.add(leche);
                        res.add(batido);
                        res.add(colacao);
                        res.add(cafe);
                        res.add(cerveza);
                        res.add(vino);
                        res.add(pizza);
                        res.add(bocadillo);
                        res.add(jamon);
                        res.add(embutido);
                        res.add(tortilla);
                        res.add(pasta);
                        res.add(queso);
                        res.add(cereales);
                        res.add(sopa);
                        res.add(arroz);
                        res.add(lentejas);
                        res.add(chocolate);
                        res.add(huevosFritos);
                        res.add(paella);
                        break;

                    case "Aseo":
                        CommonWord aseo = new CommonWord(0, "Aseo", new FirebaseImage(), 10);

                        CommonWord ducha = new CommonWord(1, "Ducha", new FirebaseImage(), 9);

                        CommonWord banera = new CommonWord(2, "Bañera", new FirebaseImage(), 9);

                        CommonWord wc = new CommonWord(3, "WC", new FirebaseImage(), 8);

                        CommonWord peine = new CommonWord(4, "Peine", new FirebaseImage(), 7);

                        CommonWord cepilloDientes = new CommonWord(5, "Cepillo de dientes", new FirebaseImage(), 7);

                        CommonWord toalla = new CommonWord(6, "Toalla", new FirebaseImage(), 6);

                        CommonWord papelHigienico = new CommonWord(7, "Papel higiénico", new FirebaseImage(), 5);

                        CommonWord toallitas = new CommonWord(8, "Toallitas", new FirebaseImage(), 5);

                        CommonWord maquillaje = new CommonWord(9, "Maquillaje", new FirebaseImage(), 4);

                        CommonWord ducharse = new CommonWord(10, "Ducharse", new FirebaseImage(), 3);

                        CommonWord cepillarseDientes = new CommonWord(11, "Cepillarse los dientes", new FirebaseImage(), 3);

                        res.add(aseo);
                        res.add(ducha);
                        res.add(banera);
                        res.add(wc);
                        res.add(peine);
                        res.add(cepilloDientes);
                        res.add(toalla);
                        res.add(papelHigienico);
                        res.add(toallitas);
                        res.add(maquillaje);
                        res.add(ducharse);
                        res.add(cepillarseDientes);
                        break;

                    case "Estaciones y Tiempo":
                        CommonWord estaciones = new CommonWord(0, "Estaciones", new FirebaseImage(), 10);

                        CommonWord primavera = new CommonWord(1, "Primavera", new FirebaseImage(), 9);

                        CommonWord verano = new CommonWord(2, "Verano", new FirebaseImage(), 9);

                        CommonWord otono = new CommonWord(3, "Otoño", new FirebaseImage(), 9);

                        CommonWord invierno = new CommonWord(4, "Invierno", new FirebaseImage(), 9);

                        CommonWord tiempo = new CommonWord(5, "Tiempo", new FirebaseImage(), 8);

                        CommonWord frio = new CommonWord(6, "Frío", new FirebaseImage(), 7);

                        CommonWord calor = new CommonWord(7, "Calor", new FirebaseImage(), 7);

                        CommonWord sol = new CommonWord(8, "Sol", new FirebaseImage(), 6);

                        CommonWord nube = new CommonWord(9, "Nube", new FirebaseImage(), 5);

                        CommonWord lluvia = new CommonWord(10, "Lluvia", new FirebaseImage(), 4);

                        CommonWord tormenta = new CommonWord(11, "Tormenta", new FirebaseImage(), 3);

                        CommonWord nieve = new CommonWord(12, "Nieve", new FirebaseImage(), 2);

                        res.add(estaciones);
                        res.add(primavera);
                        res.add(verano);
                        res.add(otono);
                        res.add(invierno);
                        res.add(tiempo);
                        res.add(frio);
                        res.add(calor);
                        res.add(sol);
                        res.add(nube);
                        res.add(lluvia);
                        res.add(tormenta);
                        res.add(nieve);
                        break;

                    case "Ropa":
                        CommonWord ropa = new CommonWord(0, "Ropa", new FirebaseImage(), 10);

                        CommonWord camiseta = new CommonWord(1, "Camiseta", new FirebaseImage(), 9);

                        CommonWord camisa = new CommonWord(2, "Camisa", new FirebaseImage(), 9);

                        CommonWord pantalon = new CommonWord(3, "Pantalón", new FirebaseImage(), 9);

                        CommonWord falda = new CommonWord(4, "Falda", new FirebaseImage(), 8);

                        CommonWord vestido = new CommonWord(5, "Vestido", new FirebaseImage(), 8);

                        CommonWord sudadera = new CommonWord(6, "Sudadera", new FirebaseImage(), 8);

                        CommonWord jersey = new CommonWord(7, "Jersey", new FirebaseImage(), 7);

                        CommonWord chaqueta = new CommonWord(8, "Chaqueta", new FirebaseImage(), 7);

                        CommonWord abrigo = new CommonWord(9, "Abrigo", new FirebaseImage(), 7);

                        CommonWord chandal = new CommonWord(10, "Chándal", new FirebaseImage(), 6);

                        CommonWord pijama = new CommonWord(11, "Pijama", new FirebaseImage(), 6);

                        CommonWord bragas = new CommonWord(12, "Bragas", new FirebaseImage(), 5);

                        CommonWord sujetador = new CommonWord(13, "Sujetador", new FirebaseImage(), 5);

                        CommonWord calzoncillos = new CommonWord(14, "Calzoncillos", new FirebaseImage(), 5);

                        CommonWord calcetines = new CommonWord(15, "Calcetines", new FirebaseImage(), 4);

                        CommonWord zapatos = new CommonWord(16, "Zapatos", new FirebaseImage(), 4);

                        CommonWord chanclas = new CommonWord(17, "Chanclas", new FirebaseImage(), 3);

                        CommonWord zapatosTacon = new CommonWord(18, "Zapatos de tacón", new FirebaseImage(), 3);

                        CommonWord zapatillas = new CommonWord(19, "Zapatillas", new FirebaseImage(), 3);

                        CommonWord deportes = new CommonWord(20, "Deportes", new FirebaseImage(), 2);

                        CommonWord sandalias = new CommonWord(21, "Sandalias", new FirebaseImage(), 2);

                        CommonWord bufanda = new CommonWord(22, "Bufanda", new FirebaseImage(), 1);

                        CommonWord gorro = new CommonWord(23, "Gorro", new FirebaseImage(), 1);

                        CommonWord guantes = new CommonWord(24, "Guantes", new FirebaseImage(), 1);

                        res.add(ropa);
                        res.add(camiseta);
                        res.add(camisa);
                        res.add(pantalon);
                        res.add(falda);
                        res.add(vestido);
                        res.add(sudadera);
                        res.add(jersey);
                        res.add(chaqueta);
                        res.add(abrigo);
                        res.add(chandal);
                        res.add(pijama);
                        res.add(bragas);
                        res.add(sujetador);
                        res.add(calzoncillos);
                        res.add(calcetines);
                        res.add(zapatos);
                        res.add(chanclas);
                        res.add(zapatosTacon);
                        res.add(zapatillas);
                        res.add(deportes);
                        res.add(sandalias);
                        res.add(bufanda);
                        res.add(gorro);
                        res.add(guantes);
                        break;

                    case "Salud y Cuerpo Humano":
                        CommonWord salud = new CommonWord(0, "Salud", new FirebaseImage(), 10);

                        CommonWord sano = new CommonWord(1, "Sano", new FirebaseImage(), 9);

                        CommonWord enfermo = new CommonWord(2, "Enfermo", new FirebaseImage(), 9);

                        CommonWord dolor = new CommonWord(3, "Dolor", new FirebaseImage(), 8);

                        CommonWord resfriado = new CommonWord(4, "Resfriado", new FirebaseImage(), 8);

                        CommonWord fiebre = new CommonWord(5, "Fiebre", new FirebaseImage(), 8);

                        CommonWord vomito = new CommonWord(6, "Vómito", new FirebaseImage(), 8);

                        CommonWord sangre = new CommonWord(7, "Sangre", new FirebaseImage(), 8);

                        CommonWord hospital = new CommonWord(8, "Hospital", new FirebaseImage(), 7);

                        CommonWord ambulancia = new CommonWord(9, "Ambulancia", new FirebaseImage(), 7);

                        CommonWord medicamento = new CommonWord(10, "Medicamento", new FirebaseImage(), 6);

                        CommonWord pastilla = new CommonWord(11, "Pastilla", new FirebaseImage(), 6);

                        CommonWord jarabe = new CommonWord(12, "Jarabe", new FirebaseImage(), 6);

                        CommonWord venda = new CommonWord(13, "Venda", new FirebaseImage(), 5);

                        CommonWord cuerpo = new CommonWord(14, "Cuerpo", new FirebaseImage(), 4);

                        CommonWord cabeza = new CommonWord(15, "Cabeza", new FirebaseImage(), 3);

                        CommonWord cuello = new CommonWord(16, "Cuello", new FirebaseImage(), 3);

                        CommonWord boca = new CommonWord(17, "Boca", new FirebaseImage(), 3);

                        CommonWord nariz = new CommonWord(18, "Nariz", new FirebaseImage(), 3);

                        CommonWord ojo = new CommonWord(19, "Ojo", new FirebaseImage(), 3);

                        CommonWord oreja = new CommonWord(20, "Oreja", new FirebaseImage(), 3);

                        CommonWord brazo = new CommonWord(21, "Brazo", new FirebaseImage(), 2);

                        CommonWord codo = new CommonWord(22, "Codo", new FirebaseImage(), 2);

                        CommonWord mano = new CommonWord(23, "Mano", new FirebaseImage(), 2);

                        CommonWord muneca = new CommonWord(24, "Muñeca", new FirebaseImage(), 2);

                        CommonWord dedo = new CommonWord(25, "Dedo", new FirebaseImage(), 2);

                        CommonWord piernas = new CommonWord(26, "Piernas", new FirebaseImage(), 2);

                        CommonWord rodilla = new CommonWord(27, "Rodilla", new FirebaseImage(), 2);

                        CommonWord pie = new CommonWord(28, "Pie", new FirebaseImage(), 2);

                        CommonWord tobillo = new CommonWord(29, "Tobillo", new FirebaseImage(), 2);

                        CommonWord hombro = new CommonWord(30, "Hombro", new FirebaseImage(), 2);

                        CommonWord espalda = new CommonWord(31, "Espalda", new FirebaseImage(), 2);

                        CommonWord barriga = new CommonWord(32, "Barriga", new FirebaseImage(), 2);

                        res.add(salud);
                        res.add(sano);
                        res.add(enfermo);
                        res.add(dolor);
                        res.add(resfriado);
                        res.add(fiebre);
                        res.add(vomito);
                        res.add(sangre);
                        res.add(hospital);
                        res.add(ambulancia);
                        res.add(medicamento);
                        res.add(pastilla);
                        res.add(jarabe);
                        res.add(venda);
                        res.add(cuerpo);
                        res.add(cabeza);
                        res.add(cuello);
                        res.add(boca);
                        res.add(nariz);
                        res.add(ojo);
                        res.add(oreja);
                        res.add(brazo);
                        res.add(codo);
                        res.add(mano);
                        res.add(muneca);
                        res.add(dedo);
                        res.add(piernas);
                        res.add(rodilla);
                        res.add(pie);
                        res.add(tobillo);
                        res.add(hombro);
                        res.add(espalda);
                        res.add(barriga);
                        break;

                    case "Transportes":
                        CommonWord transporte = new CommonWord(0, "Transporte", new FirebaseImage(), 10);

                        CommonWord coche = new CommonWord(1, "Coche", new FirebaseImage(), 9);

                        CommonWord moto = new CommonWord(2, "Moto", new FirebaseImage(), 9);

                        CommonWord camion = new CommonWord(3, "Camión", new FirebaseImage(), 8);

                        CommonWord furgoneta = new CommonWord(4, "Furgoneta", new FirebaseImage(), 8);

                        CommonWord avion = new CommonWord(5, "Avión", new FirebaseImage(), 7);

                        CommonWord barco = new CommonWord(6, "Barco", new FirebaseImage(), 6);

                        CommonWord bicicleta = new CommonWord(7, "Bicicleta", new FirebaseImage(), 5);

                        CommonWord tractor = new CommonWord(8, "Tractor", new FirebaseImage(), 4);

                        CommonWord helicoptero = new CommonWord(9, "Helicóptero", new FirebaseImage(), 3);

                        CommonWord autobus = new CommonWord(10, "Autobús", new FirebaseImage(), 2);

                        CommonWord tren = new CommonWord(11, "Tren", new FirebaseImage(), 1);

                        res.add(transporte);
                        res.add(coche);
                        res.add(moto);
                        res.add(camion);
                        res.add(furgoneta);
                        res.add(avion);
                        res.add(barco);
                        res.add(bicicleta);
                        res.add(tractor);
                        res.add(helicoptero);
                        res.add(autobus);
                        res.add(tren);
                        break;*/
                }
                break;
        }

        return res;
    }
}
