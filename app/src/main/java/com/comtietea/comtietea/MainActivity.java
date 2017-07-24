package com.comtietea.comtietea;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

    private GoogleApiClient googleApiClient;

    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener firebaseAuthListener;

    private String uid;
    private String uidAux;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();

        googleApiClient =  new GoogleApiClient.Builder(this).enableAutoManage(this, this).addApi(Auth.GOOGLE_SIGN_IN_API, googleSignInOptions).build();

        FirebaseDatabase db = FirebaseDatabase.getInstance();
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
                                List<SymbolicCode> codigosSimbolicos = cargaCodigosSimbolicos();
                                User u = new User(user.getDisplayName(), user.getEmail(), user.getUid(), codigosSimbolicos);
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

        List<CommonWord> palabrasColegio = cargaPalabrasHabituales(codigoSimbolico, "Colegio");
        SemanticField colegio = new SemanticField("Colegio", 8, palabrasColegio);

        List<CommonWord> palabrasFamilia = cargaPalabrasHabituales(codigoSimbolico, "Familia");
        SemanticField familia = new SemanticField("Familia", 8, palabrasFamilia);

        res.add(colegio);
        res.add(familia);

        return res;
    }

    private List<CommonWord> cargaPalabrasHabituales(String codigoSimbolico, String campoSemantico) {
        List<CommonWord> res = new ArrayList<CommonWord>();

        switch (campoSemantico) {
            case "Colegio":
                CommonWord boli = new CommonWord("Boligrafo", 5);
                CommonWord lapiz = new CommonWord("Lapiz", 4);

                res.add(boli);
                res.add(lapiz);
                break;

            case "Familia":
                CommonWord papa = new CommonWord("Papa", 10);
                CommonWord primo = new CommonWord("Primo", 3);

                res.add(papa);
                res.add(primo);
                break;
        }

        return res;
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
}
