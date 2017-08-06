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
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private List<SymbolicCode> cargaCodigosSimbolicos() {
        List<SymbolicCode> res = new ArrayList<SymbolicCode>();

        List<SemanticField> camposSemanticosPalabras = cargaCamposSemanticos("Palabras");
        SymbolicCode codigoSimbolicoPalabra = new SymbolicCode(0, "Palabras", camposSemanticosPalabras);

        List<SemanticField> camposSemanticosDibujos = cargaCamposSemanticos("Dibujos");
        SymbolicCode codigoSimbolicoDibujo = new SymbolicCode(1, "Dibujos", camposSemanticosDibujos);

        List<SemanticField> camposSemanticosImagenes = cargaCamposSemanticos("Imagenes");
        SymbolicCode codigoSimbolicoImagen = new SymbolicCode(2, "Imagenes", camposSemanticosImagenes);

        res.add(codigoSimbolicoPalabra);
        res.add(codigoSimbolicoDibujo);
        res.add(codigoSimbolicoImagen);

        return res;
    }

    private List<SemanticField> cargaCamposSemanticos(String codigoSimbolico) {
        List<SemanticField> res = new ArrayList<SemanticField>();

        List<CommonWord> palabrasColegio;
        SemanticField colegio = null;
        List<CommonWord> palabrasFamilia;
        SemanticField familia = null;

        int colorColegio = Color.argb(255, 255, 255, 0);
        int colorFamilia = Color.argb(255, 180, 4, 0);

        switch (codigoSimbolico) {
            case "Palabras":
                palabrasColegio = cargaPalabrasHabituales(codigoSimbolico, "Colegio");

                colegio = new SemanticField(0, "Colegio", null, 8, colorColegio, palabrasColegio);
                palabrasFamilia = cargaPalabrasHabituales(codigoSimbolico, "Familia");
                familia = new SemanticField(1, "Familia", null, 8, colorFamilia, palabrasFamilia);
                break;
            case "Dibujos":
                palabrasColegio = cargaPalabrasHabituales(codigoSimbolico, "Colegio");
                colegio = new SemanticField(0, "Colegio", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcolegio_dibujo.jpg?alt=media&token=dce6f6bd-ffb2-499b-b316-d6bdb33889a7", "gs://comtietea.appspot.com/images/default/colegio_dibujo.jpg"), 8, colorColegio, palabrasColegio);

                palabrasFamilia = cargaPalabrasHabituales(codigoSimbolico, "Familia");
                familia = new SemanticField(1, "Familia", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Ffamilia_dibujo.png?alt=media&token=deba80eb-a62e-4b18-ad8e-a2f0c7de8b0d", "gs://comtietea.appspot.com/images/default/familia_dibujo.png"), 8, colorFamilia, palabrasFamilia);
                break;
            case "Imagenes":
                palabrasColegio = cargaPalabrasHabituales(codigoSimbolico, "Colegio");
                colegio = new SemanticField(0, "Colegio", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcolegio_foto.jpg?alt=media&token=9f92d73d-a040-4716-8bdd-c67bd37bc27d", "gs://comtietea.appspot.com/images/default/colegio_foto.png"), 8, colorColegio, palabrasColegio);

                palabrasFamilia = cargaPalabrasHabituales(codigoSimbolico, "Familia");
                familia = new SemanticField(1, "Familia", new FirebaseImage("https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Ffamilia_foto.jpg?alt=media&token=b2b4ca19-611e-48ae-8718-8d206c1f0c13", "gs://comtietea.appspot.com/images/default/familia_foto.png"), 8, colorFamilia, palabrasFamilia);
                break;
        }

        res.add(colegio);
        res.add(familia);

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
