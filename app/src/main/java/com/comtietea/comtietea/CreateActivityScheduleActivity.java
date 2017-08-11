package com.comtietea.comtietea;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.comtietea.comtietea.Domain.ActivitySchedule;
import com.comtietea.comtietea.Domain.CommonWord;
import com.comtietea.comtietea.Domain.FirebaseImage;
import com.comtietea.comtietea.Domain.FirebaseReferences;
import com.comtietea.comtietea.Domain.SemanticField;
import com.comtietea.comtietea.Domain.SymbolicCode;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CreateActivityScheduleActivity extends AppCompatActivity {

    private TextView hora;
    private Spinner spinner1;
    private Spinner spinner2;
    private Spinner spinner3;
    private TextView textView4;
    private AutoCompleteTextView autoComplete;

    private DatabaseReference dbRef;

    private String uid;
    private String tipo;
    private String action;
    private String codSimId;
    private String calObjId;

    private List<String> palabras = new ArrayList<String>();
    private List<SemanticField> camposSemanticos = new ArrayList<>();

    private CreateActivityScheduleActivity createActivityScheduleActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_activity_schedule);

        hora = (TextView) findViewById(R.id.hora);
        spinner1 = (Spinner) findViewById(R.id.spinner1);
        spinner2 = (Spinner) findViewById(R.id.spinner2);
        spinner3 = (Spinner) findViewById(R.id.spinner3);
        textView4 = (TextView) findViewById(R.id.textView4);
        autoComplete = (AutoCompleteTextView) findViewById(R.id.autoComplete);

        createActivityScheduleActivity = this;

        Bundle bundle = getIntent().getExtras();

        tipo = bundle.getString("type");
        uid = bundle.getString("uid");
        action = bundle.getString("action");
        codSimId = bundle.getString("codSimId");
        calObjId = bundle.getString("calObjId");

        dbRef = FirebaseDatabase.getInstance().getReference(
                FirebaseReferences.USER_REFERENCE + "/" + uid + "/" + FirebaseReferences.SYMBOLIC_CODE_REFERENCE + "/" + codSimId);

        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                SymbolicCode codigo = dataSnapshot.getValue(SymbolicCode.class);
                if (codigo != null && codigo.getCamposSemanticos() != null) {
                    for (SemanticField campoSemantico : codigo.getCamposSemanticos()) {
                        if (campoSemantico != null && campoSemantico.getPalabrasHabituales() != null) {
                            camposSemanticos.add(campoSemantico);
                            for (CommonWord palabraHabitual : campoSemantico.getPalabrasHabituales()) {
                                palabras.add(palabraHabitual.getNombre() + " - " + campoSemantico.getNombre());
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line, palabras);

        autoComplete.setThreshold(3);
        autoComplete.setAdapter(adapter);

        spinner2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (((String) parent.getItemAtPosition(position)).equals("No")) {
                    textView4.setVisibility(View.GONE);
                    spinner3.setVisibility(View.GONE);
                } else {
                    textView4.setVisibility(View.VISIBLE);
                    spinner3.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    public void botonHora(View v) {
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minutes = c.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                hora.setText(hourOfDay + ":" + minute);
            }
        }, hour, minutes, true);
        timePickerDialog.show();

    }

    public void botonGuardar(View v) {
        String antelacion;
        int camSemId, palHabId, color;

        if (spinner2.getSelectedItem().toString().equals("Si")) {
            antelacion = spinner3.getSelectedItem().toString();
        } else {
            antelacion = null;
        }

        camSemId = encuentraPalabraHabitual(autoComplete.getText().toString()).get(0);
        color = encuentraPalabraHabitual(autoComplete.getText().toString()).get(1);
        palHabId = encuentraPalabraHabitual(autoComplete.getText().toString()).get(2);

        final ActivitySchedule activitySchedule = new ActivitySchedule(100, hora.getText().toString(), spinner1.getSelectedItem().toString(), spinner2.getSelectedItem().toString(), antelacion, camSemId, palHabId, color);
        dbRef = FirebaseDatabase.getInstance().getReference(
                FirebaseReferences.USER_REFERENCE + "/" + uid + "/" + FirebaseReferences.SYMBOLIC_CODE_REFERENCE + "/" + codSimId + "/" +
        FirebaseReferences.CALENDAR_OBJECT_REFERENCE + "/" + calObjId);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String uploadId = "" + dataSnapshot.child(FirebaseReferences.ACTIVITY_SCHEDULE_REFERENCE).getChildrenCount();
                activitySchedule.setId(new Integer(uploadId));
                dataSnapshot.child(FirebaseReferences.ACTIVITY_SCHEDULE_REFERENCE).getRef().child(uploadId).setValue(activitySchedule);

                Toast.makeText(getApplicationContext(), "La actividad ha sido creada correctamente.", Toast.LENGTH_SHORT).show();

                Intent i = new Intent(createActivityScheduleActivity, ActivityScheduleActivity.class);
                i.putExtra("type", tipo);
                i.putExtra("uid", uid);
                i.putExtra("codSimId", codSimId);
                i.putExtra("calObjId", calObjId);
                startActivity(i);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void botonCancelar(View v) {
        if (action.equals("crear")) {
            Intent i = new Intent(this, ActivityScheduleActivity.class);
            i.putExtra("type", tipo);
            i.putExtra("uid", uid);
            i.putExtra("codSimId", codSimId);
            startActivity(i);
        } else if (action.equals("editar")) {
            /*Intent i = new Intent(this, CommonWordActivity.class);
            i.putExtra("type", tipo);
            i.putExtra("uid", uid);
            i.putExtra("codSimId", codSimId);
            i.putExtra("camSemId", ""+campoSemantico.getId());
            i.putExtra("color", ""+campoSemantico.getColor());
            startActivity(i);*/
        }
    }

    private List<Integer> encuentraPalabraHabitual(String palHab) {
        List<Integer> res = new ArrayList<>();
        String nombreCampo, nombrePalabra;

        Log.i("Hola1", palHab);
        String[] nombres = palHab.trim().split(" - ");
        Log.i("Hola2", nombres.toString());
        nombrePalabra = nombres[0];
        nombreCampo = nombres[1];

        for(SemanticField campoSemantico : camposSemanticos) {
            if(nombreCampo.equals(campoSemantico.getNombre())) {
                for(CommonWord palabraHabitual : campoSemantico.getPalabrasHabituales()) {
                    if(palabraHabitual != null && nombrePalabra.equals(palabraHabitual.getNombre())) {
                        res.add(campoSemantico.getId());
                        res.add(campoSemantico.getColor());
                        res.add(palabraHabitual.getId());

                        break;
                    }
                }
                break;
            }
        }

        return res;
    }
}
