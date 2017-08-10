package com.comtietea.comtietea;

import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.comtietea.comtietea.Domain.ActivitySchedule;
import com.comtietea.comtietea.Domain.CalendarObject;
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
import java.util.Date;

public class CalendarActivity extends AppCompatActivity implements CalendarObjectRecyclerViewAdapter.ItemListener {
    private String type;
    private String uid;
    private String codSimId;

    private String fechaActual;

    RecyclerView recyclerView;
    ArrayList<CalendarObject> calendario = new ArrayList<CalendarObject>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar_recycler_view);

        Bundle bundle = getIntent().getExtras();

        type = bundle.getString("type");
        uid = bundle.getString("uid");
        codSimId = bundle.getString("codSimId");

        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);

        final CalendarObjectRecyclerViewAdapter adapter = new CalendarObjectRecyclerViewAdapter(this, calendario, this);
        recyclerView.setAdapter(adapter);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        final Calendar calendar = Calendar.getInstance();

        fechaActual = calculaFecha(calendar);

        FirebaseDatabase.getInstance().getReference(FirebaseReferences.USER_REFERENCE + "/" + uid + "/" + FirebaseReferences.SYMBOLIC_CODE_REFERENCE + "/" + codSimId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        calendario.clear();
                        SymbolicCode codigo = dataSnapshot.getValue(SymbolicCode.class);
                        if (codigo.getCalendario() != null) {
                            for (CalendarObject calendarObject : codigo.getCalendario()) {
                                if (calendarObject != null && calendarObject.getFecha().compareTo(fechaActual) >= 0) {
                                    calendario.add(calendarObject);
                                }
                            }
                        }
                        if (calendario.size() == 0) {
                            int id;
                            String diaSemana = calculaDiaSemana(calendar.get(Calendar.DAY_OF_WEEK) - 1);
                            String mes = calculaMes(calendar.get(Calendar.MONTH) + 1);

                            if (codigo.getCalendario() != null) {
                                id = codigo.getCalendario().size();
                            } else {
                                id = 0;
                            }

                            CalendarObject co = new CalendarObject(id, fechaActual, diaSemana, mes, null);

                            dataSnapshot.child(FirebaseReferences.CALENDAR_OBJECT_REFERENCE).getRef().child(""+id).setValue(co);

                        } else if (calendario.size() > 0 && calendario.size() < 7) {
                            String fechaMasAlta = calendario.get(calendario.size() - 1).getFecha();
                            int year = Integer.parseInt(fechaMasAlta.substring(0, 4));
                            int month = Integer.parseInt(fechaMasAlta.substring(5, 7)) - 1;
                            int day = Integer.parseInt(fechaMasAlta.substring(8, 10));


                            Calendar calendarAux = Calendar.getInstance();
                            calendarAux.set(year, month, day);
                            calendarAux.add(Calendar.DATE, 1);

                            int id;
                            String diaSemana = calculaDiaSemana(calendarAux.get(Calendar.DAY_OF_WEEK) - 1);
                            String mes = calculaMes(calendarAux.get(Calendar.MONTH) + 1);

                            if (codigo.getCalendario() != null) {
                                id = codigo.getCalendario().size();
                            } else {
                                id = 0;
                            }

                            CalendarObject co = new CalendarObject(id, calculaFecha(calendarAux), diaSemana, mes, null);

                            dataSnapshot.child(FirebaseReferences.CALENDAR_OBJECT_REFERENCE).getRef().child(""+id).setValue(co);

                        } else if (calendario.size() >= 7) {
                            Collections.sort(calendario);
                            Collections.reverse(calendario);
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
                FirebaseDatabase.getInstance().getReference(FirebaseReferences.USER_REFERENCE + "/" + uid + "/" + FirebaseReferences.SYMBOLIC_CODE_REFERENCE + "/" + codSimId)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                calendario.clear();
                                SymbolicCode codigo = dataSnapshot.getValue(SymbolicCode.class);

                                String fechaMasAlta = codigo.getCalendario().get(codigo.getCalendario().size() - 1).getFecha();
                                int year = Integer.parseInt(fechaMasAlta.substring(0, 4));
                                int month = Integer.parseInt(fechaMasAlta.substring(5, 7)) - 1;
                                int day = Integer.parseInt(fechaMasAlta.substring(8, 10));


                                Calendar calendarAux = Calendar.getInstance();
                                calendarAux.set(year, month, day);
                                calendarAux.add(Calendar.DATE, 1);

                                int id;
                                String diaSemana = calculaDiaSemana(calendarAux.get(Calendar.DAY_OF_WEEK) - 1);
                                String mes = calculaMes(calendarAux.get(Calendar.MONTH) + 1);

                                if (codigo.getCalendario() != null) {
                                    id = codigo.getCalendario().size();
                                } else {
                                    id = 0;
                                }

                                CalendarObject co = new CalendarObject(id, calculaFecha(calendarAux), diaSemana, mes, null);

                                dataSnapshot.child(FirebaseReferences.CALENDAR_OBJECT_REFERENCE).getRef().child(""+id).setValue(co);
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
            }
        });

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onItemClick(CalendarObject calendarObject) {
        Intent i = new Intent(this, ActivityScheduleActivity.class);
        i.putExtra("type", type);
        i.putExtra("uid", uid);
        i.putExtra("codSimId", codSimId);
        i.putExtra("calActId", ""+calendarObject.getId());
        startActivity(i);
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

    private String calculaDiaSemana(int day) {
        String res = null;

        switch (day) {
            case 0:
                res = "DOMINGO";
                break;
            case 1:
                res = "LUNES";
                break;
            case 2:
                res = "MARTES";
                break;
            case 3:
                res = "MIÉRCOLES";
                break;
            case 4:
                res = "JUEVES";
                break;
            case 5:
                res = "VIERNES";
                break;
            case 6:
                res = "SÁBADO";
                break;
        }

        return res;
    }

    private String calculaMes(int day) {
        String res = null;

        switch (day) {
            case 1:
                res = "Enero";
                break;
            case 2:
                res = "Febrero";
                break;
            case 3:
                res = "Marzo";
                break;
            case 4:
                res = "Abril";
                break;
            case 5:
                res = "Mayo";
                break;
            case 6:
                res = "Junio";
                break;
            case 7:
                res = "Julio";
                break;
            case 8:
                res = "Agosto";
                break;
            case 9:
                res = "Septiembre";
                break;
            case 10:
                res = "Octubre";
                break;
            case 11:
                res = "Noviembre";
                break;
            case 12:
                res = "Diciembre";
                break;
        }

        return res;
    }

    private String calculaFecha(Calendar calendar) {
        String year = "" + calendar.get(Calendar.YEAR);
        String month = "" + (calendar.get(Calendar.MONTH) + 1);
        String day = "" + calendar.get(Calendar.DATE);

        if (month.length() == 1)
            month = "0" + month;

        if (day.length() == 1)
            day = "0" + day;

        return year + "-" + month + "-" + day;
    }
}
