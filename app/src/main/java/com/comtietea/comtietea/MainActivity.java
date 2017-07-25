package com.comtietea.comtietea;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.comtietea.comtietea.Domain.CommonWord;
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
                                dbRef.push().setValue(u);

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

    public void logOut(View view) {
        firebaseAuth.signOut();

        Auth.GoogleSignInApi.signOut(googleApiClient).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status) {
                if (status.isSuccess()) {
                    goToLoginActivity();
                } else {
                    Toast.makeText(getApplicationContext(), "No se pudo cerrar sesión", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void revoke(View view) {
        firebaseAuth.signOut();

        Auth.GoogleSignInApi.revokeAccess(googleApiClient).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status) {
                if (status.isSuccess()) {
                    goToLoginActivity();
                } else {
                    Toast.makeText(getApplicationContext(), "No se pudo cerrar sesión", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void palabrasButton(View view) {
        Intent i = new Intent(this, SemanticFieldActivity.class);
        i.putExtra("type", "Palabras");
        i.putExtra("uid", uid);
        startActivity(i);
    }

    public void dibujosButton(View view) {
        Intent i = new Intent(this, SemanticFieldActivity.class);
        i.putExtra("type", "Dibujos");
        i.putExtra("uid", uid);
        startActivity(i);
    }

    public void imagenesButton(View view) {
        Intent i = new Intent(this, SemanticFieldActivity.class);
        i.putExtra("type", "Imagenes");
        i.putExtra("uid", uid);
        startActivity(i);
    }

    public void fotoButton(View view) {
        try {
            StorageReference boliRef = storageRef.child("images/boli_foto.jpg");
            InputStream stream = this.getAssets().open("boli_foto.jpg");//new FileInputStream(new File("file:///android_asset/boli_foto.jpg"));

            UploadTask uploadTask = boliRef.putStream(stream);
            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Log.i("FOTO", taskSnapshot.getDownloadUrl().toString());
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }





    private List<SymbolicCode> cargaCodigosSimbolicos() {
        List<SymbolicCode> res = new ArrayList<SymbolicCode>();

        List<SemanticField> camposSemanticosPalabras = cargaCamposSemanticos("Palabras");
        SymbolicCode codigoSimbolicoPalabra = new SymbolicCode("Palabras", camposSemanticosPalabras);

        List<SemanticField> camposSemanticosDibujos = cargaCamposSemanticos("Dibujos");
        SymbolicCode codigoSimbolicoDibujo = new SymbolicCode("Dibujos", camposSemanticosDibujos);

        List<SemanticField> camposSemanticosImagenes = cargaCamposSemanticos("Imagenes");
        SymbolicCode codigoSimbolicoImagen = new SymbolicCode("Imagenes", camposSemanticosImagenes);

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

        switch (codigoSimbolico) {
            case "Palabras":
                palabrasColegio = cargaPalabrasHabituales(codigoSimbolico, "Colegio");
                colegio = new SemanticField("Colegio", "", 8, palabrasColegio);

                palabrasFamilia = cargaPalabrasHabituales(codigoSimbolico, "Familia");
                familia = new SemanticField("Familia", "", 8, palabrasFamilia);
                break;
            case "Dibujos":
                palabrasColegio = cargaPalabrasHabituales(codigoSimbolico, "Colegio");
                colegio = new SemanticField("Colegio", "https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcolegio_dibujo.jpg?alt=media&token=dce6f6bd-ffb2-499b-b316-d6bdb33889a7", 8, palabrasColegio);

                palabrasFamilia = cargaPalabrasHabituales(codigoSimbolico, "Familia");
                familia = new SemanticField("Familia", "https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Ffamilia_dibujo.png?alt=media&token=cbfe05d9-671f-411c-a342-7ab18138b94b", 8, palabrasFamilia);
                break;
            case "Imagenes":
                palabrasColegio = cargaPalabrasHabituales(codigoSimbolico, "Colegio");
                colegio = new SemanticField("Colegio", "https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fcolegio_foto.jpg?alt=media&token=9f92d73d-a040-4716-8bdd-c67bd37bc27d", 8, palabrasColegio);

                palabrasFamilia = cargaPalabrasHabituales(codigoSimbolico, "Familia");
                familia = new SemanticField("Familia", "https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Ffamilia_foto.jpg?alt=media&token=b2b4ca19-611e-48ae-8718-8d206c1f0c13", 8, palabrasFamilia);
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
                        CommonWord boli = new CommonWord("Boligrafo", "", 5);
                        CommonWord lapiz = new CommonWord("Lapiz", "", 4);

                        res.add(boli);
                        res.add(lapiz);
                        break;

                    case "Familia":
                        CommonWord papa = new CommonWord("Papa", "", 10);
                        CommonWord primo = new CommonWord("Primo", "", 3);

                        res.add(papa);
                        res.add(primo);
                        break;
                }
                break;

            case "Dibujos":
                switch (campoSemantico) {
                    case "Colegio":
                        CommonWord boli = new CommonWord("Boligrafo", "https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fboli_dibujo.png?alt=media&token=303830d6-689e-4207-a50f-74dc795c34df", 5);
                        CommonWord lapiz = new CommonWord("Lapiz", "https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Flapiz_dibujo.png?alt=media&token=0283555f-c65c-434f-bda4-5d3e89768ef2", 4);

                        res.add(boli);
                        res.add(lapiz);
                        break;

                    case "Familia":
                        CommonWord papa = new CommonWord("Papa", "https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fpadre_dibujo.png?alt=media&token=0d5bc5a1-775b-4200-8e2c-194623e62eb6", 10);
                        CommonWord primo = new CommonWord("Primo", "https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fprimo_dibujo.jpg?alt=media&token=864e1ae7-d38e-4888-820c-e61ff6888f3f", 3);

                        res.add(papa);
                        res.add(primo);
                        break;
                }
                break;

            case "Imagenes":
                switch (campoSemantico) {
                    case "Colegio":
                        CommonWord boli = new CommonWord("Boligrafo", "https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fboli_foto.jpg?alt=media&token=7a74ef05-6294-4853-a3ae-30df3e2ac0e9", 5);
                        CommonWord lapiz = new CommonWord("Lapiz", "https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Flapiz_foto.jpg?alt=media&token=7323a398-a15d-4741-a8bd-11dc96ec4384", 4);

                        res.add(boli);
                        res.add(lapiz);
                        break;

                    case "Familia":
                        CommonWord papa = new CommonWord("Papa", "https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fpadre_foto.jpg?alt=media&token=82b2a9b6-2783-4a2f-b53b-2c471d4d6b3d", 10);
                        CommonWord primo = new CommonWord("Primo", "https://firebasestorage.googleapis.com/v0/b/comtietea.appspot.com/o/images%2Fdefault%2Fprimo_foto.jpg?alt=media&token=7b566fcd-06c1-4605-99b3-992ba2f6ddc0", 3);

                        res.add(papa);
                        res.add(primo);
                        break;
                }
                break;
        }

        return res;
    }
}
