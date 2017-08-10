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
import com.comtietea.comtietea.Domain.FirebaseReferences;
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
    private String calActId;

    RecyclerView recyclerView;
    ArrayList<ActivitySchedule> actividades = new ArrayList<ActivitySchedule>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activity_schedule);

        Bundle bundle = getIntent().getExtras();

        type = bundle.getString("type");
        uid = bundle.getString("uid");
        codSimId = bundle.getString("codSimId");
        calActId = bundle.getString("calActId");

        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);

        final ActivityScheduleRecyclerViewAdapter adapter = new ActivityScheduleRecyclerViewAdapter(this, actividades, this, uid, codSimId);
        recyclerView.setAdapter(adapter);

        GridLayoutManager manager = new GridLayoutManager(this, 1, GridLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(manager);

        FirebaseDatabase.getInstance().getReference(FirebaseReferences.USER_REFERENCE + "/" + uid + "/" + FirebaseReferences.SYMBOLIC_CODE_REFERENCE + "/" + codSimId +
                "/" + FirebaseReferences.CALENDAR_OBJECT_REFERENCE + "/" + calActId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        actividades.clear();
                        CalendarObject calendarObject = dataSnapshot.getValue(CalendarObject.class);
                        if (calendarObject.getActividades() != null) {
                            for (ActivitySchedule activitySchedule : calendarObject.getActividades()) {
                                if (activitySchedule != null) {
                                    actividades.add(activitySchedule);
                                }
                            }
                        }

                        if (actividades.size() >= 2) {
                            Collections.sort(actividades);
                            Collections.reverse(actividades);
                        }

                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onItemClick(ActivitySchedule activitySchedule) {
        /*Intent i = new Intent(this, CreateActivityScheduleActivity.class);
        i.putExtra("type", type);
        i.putExtra("uid", uid);
        i.putExtra("codSimId", codSimId);
        startActivity(i);*/
    }

    @Override
    public boolean onSupportNavigateUp() {
        Intent i = new Intent(this, ActionsActivity.class);
        i.putExtra("type", type);
        i.putExtra("uid", uid);
        i.putExtra("codSimId", codSimId);
        startActivity(i);
        return false;
    }
}