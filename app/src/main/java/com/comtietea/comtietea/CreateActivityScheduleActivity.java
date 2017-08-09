package com.comtietea.comtietea;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;

import com.comtietea.comtietea.Domain.CommonWord;
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

    private DatabaseReference dbRef;

    private String uid;
    private String tipo;
    private String action;
    private String codSimId;

    private TextView hora;

    private List<CommonWord> palabrasHabituales = new ArrayList<CommonWord>();
    private List<String> palabras = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_activity_schedule);

        hora = (TextView) findViewById(R.id.hora);

        Bundle bundle = getIntent().getExtras();

        tipo = bundle.getString("type");
        uid = bundle.getString("uid");
        action = bundle.getString("action");
        codSimId = bundle.getString("codSimId");

        dbRef = FirebaseDatabase.getInstance().getReference(
                FirebaseReferences.USER_REFERENCE + "/" + uid + "/" + FirebaseReferences.SYMBOLIC_CODE_REFERENCE + "/" + codSimId);

        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                SymbolicCode codigo = dataSnapshot.getValue(SymbolicCode.class);
                if(codigo != null && codigo.getCamposSemanticos() != null) {
                    for (SemanticField campoSemantico : codigo.getCamposSemanticos()) {
                        if(campoSemantico != null && campoSemantico.getPalabrasHabituales() != null) {
                            for (CommonWord palabraHabitual : campoSemantico.getPalabrasHabituales()) {
                                palabrasHabituales.add(palabraHabitual);
                                palabras.add(palabraHabitual.getNombre());
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

        AutoCompleteTextView autoComplete = (AutoCompleteTextView) findViewById(R.id.autoComplete);
        autoComplete.setThreshold(3);
        autoComplete.setAdapter(adapter);
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
}
