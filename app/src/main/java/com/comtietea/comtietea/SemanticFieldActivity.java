package com.comtietea.comtietea;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.comtietea.comtietea.Domain.FirebaseReferences;
import com.comtietea.comtietea.Domain.SemanticField;
import com.comtietea.comtietea.Domain.SymbolicCode;
import com.comtietea.comtietea.Domain.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;


public class SemanticFieldActivity extends AppCompatActivity implements SemanticFieldRecyclerViewAdapter.ItemListener {
    private String type;
    private String uid;
    private String codSimId;

    private SemanticFieldActivity semanticFieldActivity;

    RecyclerView recyclerView;
    ArrayList<SemanticField> camposSemanticos = new ArrayList<SemanticField>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycler_view);

        Bundle bundle = getIntent().getExtras();

        type = bundle.getString("type");
        uid = bundle.getString("uid");
        codSimId = bundle.getString("codSimId");

        semanticFieldActivity = this;

        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);

        final SemanticFieldRecyclerViewAdapter adapter = new SemanticFieldRecyclerViewAdapter(this, camposSemanticos, this, type);
        recyclerView.setAdapter(adapter);

        GridLayoutManager manager = new GridLayoutManager(this, 2, GridLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(manager);

        FirebaseDatabase.getInstance().getReference(FirebaseReferences.USER_REFERENCE + "/" + uid + "/" + FirebaseReferences.SYMBOLIC_CODE_REFERENCE + "/" + codSimId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        camposSemanticos.clear();
                        SymbolicCode codigo = dataSnapshot.getValue(SymbolicCode.class);
                        camposSemanticos.addAll(codigo.getCamposSemanticos());
                        Collections.sort(camposSemanticos);
                        Collections.reverse(camposSemanticos);
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
                Intent i = new Intent(semanticFieldActivity, CreateSemanticFieldActivity.class);
                i.putExtra("type", type);
                i.putExtra("uid", uid);
                i.putExtra("codSimId", codSimId);
                startActivity(i);
            }
        });
    }

    @Override
    public void onItemClick(SemanticField campoSemantico) {
        Intent i = new Intent(this, CommonWordActivity.class);
        i.putExtra("type", type);
        i.putExtra("uid", uid);
        i.putExtra("codSimId", codSimId);
        i.putExtra("camSemId", ""+campoSemantico.getId());
        i.putExtra("color", ""+campoSemantico.getColor());
        startActivity(i);
    }
}
