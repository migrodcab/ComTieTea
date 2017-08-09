package com.comtietea.comtietea;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.comtietea.comtietea.Domain.CommonWord;
import com.comtietea.comtietea.Domain.FirebaseReferences;
import com.comtietea.comtietea.Domain.SemanticField;
import com.comtietea.comtietea.Domain.SymbolicCode;
import com.comtietea.comtietea.Domain.User;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

/**
 * Created by HP on 23/07/2017.
 */
public class CommonWordActivity extends AppCompatActivity implements CommonWordRecyclerViewAdapter.ItemListener {
    private String type;
    private String uid;
    private int color;
    private String codSimId;
    private String camSemId;
    private String nombreCampoSemantico;

    private CommonWordActivity commonWordActivity;

    private DatabaseReference dbRef;

    RecyclerView recyclerView;
    ArrayList<CommonWord> palabrasHabituales = new ArrayList<CommonWord>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycler_view);

        commonWordActivity = this;

        Bundle bundle = getIntent().getExtras();

        type = bundle.getString("type");
        uid = bundle.getString("uid");
        color = new Integer(bundle.getString("color"));
        codSimId = bundle.getString("codSimId");
        camSemId = bundle.getString("camSemId");

        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);

        final CommonWordRecyclerViewAdapter adapter = new CommonWordRecyclerViewAdapter(this, palabrasHabituales, this, type, color);
        recyclerView.setAdapter(adapter);

        GridLayoutManager manager = new GridLayoutManager(this, 2, GridLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(manager);

        dbRef = FirebaseDatabase.getInstance().getReference(FirebaseReferences.USER_REFERENCE + "/" + uid + "/" +
                FirebaseReferences.SYMBOLIC_CODE_REFERENCE + "/" + codSimId + "/" + FirebaseReferences.SEMANTIC_FIELD_REFERENCE + "/" + camSemId);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                palabrasHabituales.clear();
                SemanticField camposSemantico = dataSnapshot.getValue(SemanticField.class);
                nombreCampoSemantico = camposSemantico.getNombre();
                if (camposSemantico.getPalabrasHabituales() != null) {
                    for (CommonWord palabraHabitual : camposSemantico.getPalabrasHabituales()) {
                        if (palabraHabitual != null && palabraHabitual.getId() != -1) {
                            palabrasHabituales.add(palabraHabitual);
                        }
                    }
                    if (palabrasHabituales.size() >= 2) {
                        Collections.sort(palabrasHabituales);
                        Collections.reverse(palabrasHabituales);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(commonWordActivity, CreateCommonWordActivity.class);
                i.putExtra("type", type);
                i.putExtra("uid", uid);
                i.putExtra("codSimId", codSimId);
                i.putExtra("camSemId", camSemId);
                i.putExtra("action", "crear");
                i.putExtra("color", "" + color);
                i.putExtra("nombreCampoSemantico", nombreCampoSemantico);
                startActivity(i);
            }
        });

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onItemClick(CommonWord palabraHabitual) {
        Intent i = new Intent(this, CommonWordDetailActivity.class);
        i.putExtra("type", type);
        i.putExtra("uid", uid);
        i.putExtra("codSimId", codSimId);
        i.putExtra("camSemId", camSemId);
        i.putExtra("color", "" + color);
        i.putExtra("nombreCampoSemantico", nombreCampoSemantico);
        i.putExtra("palHabId", "" + palabraHabitual.getId());
        startActivity(i);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_actions, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_edit:
                Intent i = new Intent(this, CreateSemanticFieldActivity.class);
                i.putExtra("type", type);
                i.putExtra("uid", uid);
                i.putExtra("codSimId", codSimId);
                i.putExtra("camSemId", camSemId);
                i.putExtra("color", "" + color);
                i.putExtra("action", "editar");
                startActivity(i);
                return true;
            case R.id.action_delete:
                deleteSemanticField();
                return true;
            case R.id.action_settings:
                Intent intent = new Intent(this, ProfileInfoActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void deleteSemanticField() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("¿Desea borrar este campo semántico?").setCancelable(false)
                .setPositiveButton("Si", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final ProgressDialog dialogAux = new ProgressDialog(commonWordActivity);
                        dialogAux.setTitle("Borrando campo semántico");
                        dialogAux.show();

                        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                SemanticField campoSemantico = dataSnapshot.getValue(SemanticField.class);

                                for (DataSnapshot data : dataSnapshot.child(FirebaseReferences.COMMON_WORD_REFERENCE).getChildren()) {
                                    CommonWord palabraHabitual = data.getValue(CommonWord.class);
                                    if (palabraHabitual.getImagen() != null && palabraHabitual.getImagen().getImagenRuta().contains(uid)) {
                                        FirebaseStorage.getInstance().getReferenceFromUrl(FirebaseReferences.FIREBASE_STORAGE_REFERENCE)
                                                .child(palabraHabitual.getImagen().getImagenRuta()).delete();
                                    }
                                }

                                if (campoSemantico.getImagen() != null && campoSemantico.getImagen().getImagenRuta().contains(uid)) {
                                    FirebaseStorage.getInstance().getReferenceFromUrl(FirebaseReferences.FIREBASE_STORAGE_REFERENCE)
                                            .child(campoSemantico.getImagen().getImagenRuta()).delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            dbRef.setValue(new SemanticField(-1, null, null, 0, 0, null));
                                            dialogAux.dismiss();

                                            Intent i = new Intent(commonWordActivity, SemanticFieldActivity.class);
                                            i.putExtra("type", type);
                                            i.putExtra("uid", uid);
                                            i.putExtra("codSimId", codSimId);
                                            Toast.makeText(getApplicationContext(), "El campo semántico ha sido borrado correctamente.", Toast.LENGTH_SHORT).show();
                                            startActivity(i);
                                        }
                                    });
                                } else {
                                    dbRef.setValue(new SemanticField(-1, null, null, 0, 0, null));
                                    dialogAux.dismiss();

                                    Intent i = new Intent(commonWordActivity, SemanticFieldActivity.class);
                                    i.putExtra("type", type);
                                    i.putExtra("uid", uid);
                                    i.putExtra("codSimId", codSimId);
                                    Toast.makeText(getApplicationContext(), "El campo semántico ha sido borrado correctamente.", Toast.LENGTH_SHORT).show();
                                    startActivity(i);
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.setTitle("Confirmar");
        dialog.show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        Intent i = new Intent(this, SemanticFieldActivity.class);
        i.putExtra("type", type);
        i.putExtra("uid", uid);
        i.putExtra("codSimId", codSimId);
        startActivity(i);
        return false;
    }
}
