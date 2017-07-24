package com.comtietea.comtietea;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.comtietea.comtietea.Domain.CommonWord;
import com.comtietea.comtietea.Domain.FirebaseReferences;
import com.comtietea.comtietea.Domain.SemanticField;
import com.comtietea.comtietea.Domain.SymbolicCode;
import com.comtietea.comtietea.Domain.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by HP on 23/07/2017.
 */
public class SemanticFieldActivity extends AppCompatActivity implements SemanticFieldRecyclerViewAdapter.ItemListener {
    private String type;
    private String uid;

    RecyclerView recyclerView;
    ArrayList<SemanticField> camposSemanticos = new ArrayList<SemanticField>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycler_view);

        Bundle bundle = getIntent().getExtras();

        type = bundle.getString("type");
        uid = bundle.getString("uid");

        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);

        final SemanticFieldRecyclerViewAdapter adapter = new SemanticFieldRecyclerViewAdapter(this, camposSemanticos, this);
        recyclerView.setAdapter(adapter);

        GridLayoutManager manager = new GridLayoutManager(this, 2, GridLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(manager);

        FirebaseDatabase.getInstance().getReference(FirebaseReferences.USER_REFERENCE).orderByChild("uid")
                .equalTo(uid).limitToFirst(1).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        camposSemanticos.clear();
                        for(DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            User user = snapshot.getValue(User.class);
                            for (SymbolicCode codigo : user.getCodigosSimbolicos()) {
                                if(codigo.getTipo().equals(type)) {
                                    camposSemanticos.addAll(codigo.getCamposSemanticos());
                                } else {
                                    continue;
                                }
                            }
                        }
                        adapter.notifyDataSetChanged();
                        /*Iterator<DataSnapshot> items = dataSnapshot.getChildren().iterator();
                        camposSemanticos.clear();
                        while (items.hasNext()) {
                            DataSnapshot item = items.next();
                            Iterator<DataSnapshot> codigos = item.child(FirebaseReferences.SYMBOLIC_CODE_REFERENCE).getChildren().iterator();
                            while (codigos.hasNext()) {
                                DataSnapshot codigo = codigos.next();
                                if(codigo.child("tipo").getValue().toString().equals(type)) {
                                    Iterator<DataSnapshot> campos = codigo.child(FirebaseReferences.SEMANTIC_FIELD_REFERENCE).getChildren().iterator();
                                    while (campos.hasNext()) {
                                        DataSnapshot campo = campos.next();
                                        SemanticField campoSemantico = new SemanticField(campo.child("nombre").getValue().toString(), new Integer(campo.child("relevancia").getValue().toString()), new ArrayList<CommonWord>());

                                        camposSemanticos.add(campoSemantico);
                                    }
                                } else {
                                    continue;
                                }
                            }
                        }*/
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

    @Override
    public void onItemClick(SemanticField campoSemantico) {

        Log.i("Hola", campoSemantico.getNombre());
        //Toast.makeText(getApplicationContext(), item.text + " is clicked", Toast.LENGTH_SHORT).show();

    }
}
