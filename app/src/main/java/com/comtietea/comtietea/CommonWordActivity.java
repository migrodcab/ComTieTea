package com.comtietea.comtietea;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

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
import java.util.Collections;

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

        FirebaseDatabase.getInstance().getReference(FirebaseReferences.USER_REFERENCE + "/" + uid + "/" +
                FirebaseReferences.SYMBOLIC_CODE_REFERENCE + "/" + codSimId + "/" + FirebaseReferences.SEMANTIC_FIELD_REFERENCE + "/" + camSemId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        palabrasHabituales.clear();
                        SemanticField camposSemantico = dataSnapshot.getValue(SemanticField.class);
                        nombreCampoSemantico = camposSemantico.getNombre();
                        if(camposSemantico.getPalabrasHabituales() != null) {
                            palabrasHabituales.addAll(camposSemantico.getPalabrasHabituales());
                            Collections.sort(palabrasHabituales);
                            Collections.reverse(palabrasHabituales);
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
    }

    @Override
    public void onItemClick(CommonWord palabraHabitual) {
        //Intent i = new Intent(this, );
        Log.i("Hola", palabraHabitual.getNombre());
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
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }
}
