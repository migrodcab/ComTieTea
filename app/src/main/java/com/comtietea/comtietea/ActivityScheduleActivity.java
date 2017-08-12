package com.comtietea.comtietea;

import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import com.comtietea.comtietea.Domain.ActivitySchedule;
import com.comtietea.comtietea.Domain.CalendarObject;
import com.comtietea.comtietea.Domain.CommonWord;
import com.comtietea.comtietea.Domain.FirebaseReferences;
import com.comtietea.comtietea.Domain.SemanticField;
import com.comtietea.comtietea.Domain.SymbolicCode;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;

public class ActivityScheduleActivity extends AppCompatActivity implements ActivityScheduleRecyclerViewAdapter.ItemListener {
    private String type;
    private String uid;
    private String codSimId;
    private String calObjId;
    private String fecha;

    private ActivityScheduleActivity activityScheduleActivity;

    RecyclerView recyclerView;
    ArrayList<ActivitySchedule> actividades = new ArrayList<ActivitySchedule>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activity_schedule);

        activityScheduleActivity = this;

        Bundle bundle = getIntent().getExtras();

        type = bundle.getString("type");
        uid = bundle.getString("uid");
        codSimId = bundle.getString("codSimId");
        calObjId = bundle.getString("calObjId");
        fecha = bundle.getString("fecha");

        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);

        final ActivityScheduleRecyclerViewAdapter adapter = new ActivityScheduleRecyclerViewAdapter(this, actividades, this, uid, codSimId);
        recyclerView.setAdapter(adapter);

        GridLayoutManager manager = new GridLayoutManager(this, 1, GridLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(manager);

        FirebaseDatabase.getInstance().getReference(FirebaseReferences.USER_REFERENCE + "/" + uid + "/" + FirebaseReferences.SYMBOLIC_CODE_REFERENCE + "/" + codSimId +
                "/" + FirebaseReferences.CALENDAR_OBJECT_REFERENCE + "/" + calObjId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        actividades.clear();
                        CalendarObject calendarObject = dataSnapshot.getValue(CalendarObject.class);
                        if (calendarObject.getActividades() != null) {
                            for (final ActivitySchedule activitySchedule : calendarObject.getActividades()) {
                                if (activitySchedule != null && activitySchedule.getId() != -1) {
                                    FirebaseDatabase.getInstance().getReference(FirebaseReferences.USER_REFERENCE + "/" + uid + "/" + FirebaseReferences.SYMBOLIC_CODE_REFERENCE + "/" + codSimId +
                                            "/" + FirebaseReferences.SEMANTIC_FIELD_REFERENCE + "/" + activitySchedule.getCamSemId()).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            SemanticField sf = dataSnapshot.getValue(SemanticField.class);
                                            if(sf != null && sf.getId() != -1 && sf.getId() == activitySchedule.getCamSemId()) {
                                                FirebaseDatabase.getInstance().getReference(FirebaseReferences.USER_REFERENCE + "/" + uid + "/" + FirebaseReferences.SYMBOLIC_CODE_REFERENCE + "/" + codSimId +
                                                        "/" + FirebaseReferences.SEMANTIC_FIELD_REFERENCE + "/" + sf.getId() + "/" + FirebaseReferences.COMMON_WORD_REFERENCE + "/" +
                                                        activitySchedule.getPalHabId()).addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                                        CommonWord cw = dataSnapshot.getValue(CommonWord.class);
                                                        if (cw != null && cw.getId() != -1) {
                                                            actividades.add(activitySchedule);

                                                            if (actividades.size() >= 2) {
                                                                Collections.sort(actividades);
                                                                Collections.reverse(actividades);
                                                            }

                                                            adapter.notifyDataSetChanged();
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(DatabaseError databaseError) {

                                                    }
                                                });
                                            }
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {

                                        }
                                    });
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(activityScheduleActivity, CreateActivityScheduleActivity.class);
                i.putExtra("type", type);
                i.putExtra("uid", uid);
                i.putExtra("codSimId", codSimId);
                i.putExtra("calObjId", calObjId);
                i.putExtra("action", "crear");
                i.putExtra("fecha", fecha);
                startActivity(i);
            }
        });

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onItemClick(ActivitySchedule activitySchedule) {
        Intent i = new Intent(this, CommonWordDetailActivity.class);
        i.putExtra("type", type);
        i.putExtra("uid", uid);
        i.putExtra("codSimId", codSimId);
        i.putExtra("camSemId", ""+activitySchedule.getCamSemId());
        i.putExtra("color", ""+activitySchedule.getColor());
        i.putExtra("nombreCampoSemantico", "");
        i.putExtra("palHabId", "" + activitySchedule.getPalHabId());
        i.putExtra("anterior", "agenda");
        i.putExtra("calObjId", calObjId);
        i.putExtra("actSchId", ""+activitySchedule.getId());
        startActivity(i);
    }

    @Override
    public boolean onSupportNavigateUp() {
        Intent i = new Intent(this, CalendarActivity.class);
        i.putExtra("type", type);
        i.putExtra("uid", uid);
        i.putExtra("codSimId", codSimId);
        startActivity(i);
        return false;
    }
}