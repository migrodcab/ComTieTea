package com.comtietea.comtietea;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.util.TypedValue;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.comtietea.comtietea.Domain.ActivitySchedule;
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
    private String actSchId;
    private String fecha;

    private List<String> palabras = new ArrayList<String>();
    private List<SemanticField> camposSemanticos = new ArrayList<>();

    private CreateActivityScheduleActivity createActivityScheduleActivity;

    private ActivitySchedule activitySchedule;

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

        setTitle("Añadir");

        LinearLayout linearLayout1 = (LinearLayout) findViewById(R.id.linearLayout);
        LinearLayout linearLayout2 = (LinearLayout) findViewById(R.id.bottom_bar2);
        RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.relativeLayout1);
        relativeLayout.setVisibility(View.GONE);

        createActivityScheduleActivity = this;

        Bundle bundle = getIntent().getExtras();

        tipo = bundle.getString("type");
        uid = bundle.getString("uid");
        action = bundle.getString("action");
        codSimId = bundle.getString("codSimId");
        calObjId = bundle.getString("calObjId");
        actSchId = bundle.getString("actSchId");
        fecha = bundle.getString("fecha");

        if (action.equals("crear")) {
            Calendar c = Calendar.getInstance();
            String horaAux;

            if (c.get(Calendar.HOUR_OF_DAY) < 10) {
                horaAux = "0" + c.get(Calendar.HOUR_OF_DAY);
            } else {
                horaAux = "" + c.get(Calendar.HOUR_OF_DAY);
            }
            if (c.get(Calendar.MINUTE) < 10) {
                horaAux = horaAux + ":0" + c.get(Calendar.MINUTE);
            } else {
                horaAux = horaAux + ":" + c.get(Calendar.MINUTE);
            }

            hora.setText(horaAux);
        }

        dbRef = FirebaseDatabase.getInstance().getReference(
                FirebaseReferences.USER_REFERENCE + "/" + uid + "/" + FirebaseReferences.SYMBOLIC_CODE_REFERENCE + "/" + codSimId);

        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                palabras.clear();
                camposSemanticos.clear();
                SymbolicCode codigo = dataSnapshot.getValue(SymbolicCode.class);
                if (codigo != null && codigo.getCamposSemanticos() != null) {
                    for (SemanticField campoSemantico : codigo.getCamposSemanticos()) {
                        if (campoSemantico != null && campoSemantico.getId() != -1 && campoSemantico.getPalabrasHabituales() != null) {
                            camposSemanticos.add(campoSemantico);
                            for (CommonWord palabraHabitual : campoSemantico.getPalabrasHabituales()) {
                                if (palabraHabitual != null && palabraHabitual.getId() != -1)
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

        if (action.equals("editar")) {

            setTitle("Editar");

            dbRef = FirebaseDatabase.getInstance().getReference(
                    FirebaseReferences.USER_REFERENCE + "/" + uid + "/" + FirebaseReferences.SYMBOLIC_CODE_REFERENCE + "/" + codSimId + "/" +
                            FirebaseReferences.CALENDAR_OBJECT_REFERENCE + "/" + calObjId + "/" + FirebaseReferences.ACTIVITY_SCHEDULE_REFERENCE +
                            "/" + actSchId);
            dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    activitySchedule = dataSnapshot.getValue(ActivitySchedule.class);

                    hora.setText(activitySchedule.getHora());
                    autoComplete.setText(activitySchedule.getNombre());
                    if (activitySchedule.getAlarma().equals("Si")) {
                        spinner1.setSelection(0);
                    } else {
                        spinner1.setSelection(1);
                    }
                    if (activitySchedule.getAviso().equals("Si")) {
                        spinner2.setSelection(0);

                        switch (activitySchedule.getAntelacion()) {
                            case "5 Minutos":
                                spinner3.setSelection(0);
                                break;
                            case "10 Minutos":
                                spinner3.setSelection(1);
                                break;
                            case "15 Minutos":
                                spinner3.setSelection(2);
                                break;
                            case "30 Minutos":
                                spinner3.setSelection(3);
                                break;
                            case "1 Hora":
                                spinner3.setSelection(4);
                                break;
                        }

                    } else {
                        spinner2.setSelection(1);
                        spinner3.setVisibility(View.GONE);
                    }

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        } else if (action.equals("alarma")) {
            linearLayout1.setVisibility(View.GONE);
            linearLayout2.setVisibility(View.GONE);
            relativeLayout.setVisibility(View.VISIBLE);

            dbRef = FirebaseDatabase.getInstance().getReference(
                    FirebaseReferences.USER_REFERENCE + "/" + uid + "/" + FirebaseReferences.SYMBOLIC_CODE_REFERENCE + "/" + codSimId + "/" +
                            FirebaseReferences.CALENDAR_OBJECT_REFERENCE + "/" + calObjId + "/" + FirebaseReferences.ACTIVITY_SCHEDULE_REFERENCE +
                            "/" + actSchId);
            dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    ImageView imageView = (ImageView) findViewById(R.id.imageView);
                    TextView text = (TextView) findViewById(R.id.textViewDetail);
                    Button button = (Button) findViewById(R.id.aceptar);

                    Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                    if (alarmUri == null) {
                        alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    }
                    final Ringtone ringtone = RingtoneManager.getRingtone(createActivityScheduleActivity, alarmUri);
                    ringtone.play();

                    final ActivitySchedule actSch = dataSnapshot.getValue(ActivitySchedule.class);

                    setTitle(actSch.getHora());

                    String[] datos = actSch.getNombre().trim().split(" - ");

                    text.setText(datos[0]);

                    if (text.getText().toString().length() >= 15) {
                        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
                    } else if (text.getText().toString().length() >= 10) {
                        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 40);
                    } else if (text.getText().toString().length() >= 7) {
                        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 50);
                    }

                    if (!tipo.equals("Palabras")) {
                        Glide.with(createActivityScheduleActivity).load(actSch.getUrl()).into(imageView);
                    } else {
                        imageView.setVisibility(View.GONE);
                        CardView cardView = (CardView) findViewById(R.id.cardView);
                        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) cardView.getLayoutParams();
                        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
                        cardView.setLayoutParams(layoutParams);
                    }
                    ((RelativeLayout) findViewById(R.id.relativeLayout2)).setBackgroundColor(new Integer(actSch.getColor()));

                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            borrarAlarma((int) actSch.getAlarmCode());
                            ringtone.stop();

                            Intent i = new Intent(createActivityScheduleActivity, ActivityScheduleActivity.class);
                            i.putExtra("type", tipo);
                            i.putExtra("uid", uid);
                            i.putExtra("codSimId", codSimId);
                            i.putExtra("calObjId", calObjId);
                            i.putExtra("fecha", fecha);
                            startActivity(i);
                        }
                    });
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }

    public void botonHora(View v) {
        int hour, minutes;

        String[] horaArray = hora.getText().toString().split(":");
        hour = Integer.parseInt(horaArray[0]);
        minutes = Integer.parseInt(horaArray[1]);


        TimePickerDialog timePickerDialog = new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                String hourAux, minuteAux;

                hourAux = "" + hourOfDay;
                minuteAux = "" + minute;

                if (hourAux.length() == 1)
                    hourAux = "0" + hourAux;
                if (minuteAux.length() == 1)
                    minuteAux = "0" + minuteAux;

                hora.setText(hourAux + ":" + minuteAux);
            }
        }, hour, minutes, true);
        timePickerDialog.show();

    }

    public void botonGuardar(View v) {
        String antelacion;
        int camSemId, palHabId, color;
        String url;
        List<String> datos;
        Calendar c = Calendar.getInstance();
        String horaAux = "";
        String fechaAux;
        Boolean esFuturo = true;

        fechaAux = "" + c.get(Calendar.YEAR);
        if (c.get(Calendar.MONTH) < 9) {
            fechaAux = fechaAux + "-0" + (c.get(Calendar.MONTH) + 1);
        } else {
            fechaAux = fechaAux + "-" + (c.get(Calendar.MONTH) + 1);
        }
        if (c.get(Calendar.DATE) < 10) {
            fechaAux = fechaAux + "-0" + c.get(Calendar.DATE);
        } else {
            fechaAux = fechaAux + "-" + c.get(Calendar.DATE);
        }

        if (fechaAux.compareTo(fecha) == 0) {
            if (c.get(Calendar.HOUR_OF_DAY) < 10) {
                horaAux = "0" + c.get(Calendar.HOUR_OF_DAY);
            } else {
                horaAux = "" + c.get(Calendar.HOUR_OF_DAY);
            }
            if (c.get(Calendar.MINUTE) < 10) {
                horaAux = horaAux + ":0" + c.get(Calendar.MINUTE);
            } else {
                horaAux = horaAux + ":" + c.get(Calendar.MINUTE);
            }

            if (horaAux.compareTo(hora.getText().toString()) >= 0) {
                esFuturo = false;
            }
        }


        if (spinner2.getSelectedItem().toString().equals("Si")) {
            antelacion = spinner3.getSelectedItem().toString();
        } else {
            antelacion = null;
        }

        if (esFuturo) {
            if (!autoComplete.getText().equals("")) {
                datos = encuentraPalabraHabitual(autoComplete.getText().toString());
                if (datos != null) {

                    camSemId = Integer.parseInt(datos.get(0));
                    color = Integer.parseInt(datos.get(1));
                    palHabId = Integer.parseInt(datos.get(2));
                    url = datos.get(3);

                    if (action.equals("crear")) {
                        activitySchedule = new ActivitySchedule(100, autoComplete.getText().toString(), hora.getText().toString(), spinner1.getSelectedItem().toString(), spinner2.getSelectedItem().toString(), antelacion, camSemId, palHabId, color, url, new Integer(("" + System.currentTimeMillis()).substring(0, 10)));
                        dbRef = FirebaseDatabase.getInstance().getReference(
                                FirebaseReferences.USER_REFERENCE + "/" + uid + "/" + FirebaseReferences.SYMBOLIC_CODE_REFERENCE + "/" + codSimId + "/" +
                                        FirebaseReferences.CALENDAR_OBJECT_REFERENCE + "/" + calObjId);
                        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                String uploadId = "" + dataSnapshot.child(FirebaseReferences.ACTIVITY_SCHEDULE_REFERENCE).getChildrenCount();
                                activitySchedule.setId(new Integer(uploadId));
                                dataSnapshot.child(FirebaseReferences.ACTIVITY_SCHEDULE_REFERENCE).getRef().child(uploadId).setValue(activitySchedule);

                                if (activitySchedule.getAviso().equals("Si")) {
                                    int antelacion = 0;
                                    switch (activitySchedule.getAntelacion()) {
                                        case "5 Minutos":
                                            antelacion = 5;
                                            break;
                                        case "10 Minutos":
                                            antelacion = 10;
                                            break;
                                        case "15 Minutos":
                                            antelacion = 15;
                                            break;
                                        case "30 Minutos":
                                            antelacion = 30;
                                            break;
                                        case "1 Hora":
                                            antelacion = 60;
                                            break;
                                    }
                                    establecerNotificacion(fecha, activitySchedule.getHora(), antelacion, activitySchedule.getId(), activitySchedule.getUrl(), activitySchedule.getNombre());
                                }

                                if (activitySchedule.getAlarma().equals("Si")) {
                                    establecerAlarma(fecha, activitySchedule.getHora(), (int) activitySchedule.getAlarmCode());
                                }

                                Toast.makeText(getApplicationContext(), "La actividad ha sido creada correctamente.", Toast.LENGTH_SHORT).show();

                                Intent i = new Intent(createActivityScheduleActivity, ActivityScheduleActivity.class);
                                i.putExtra("type", tipo);
                                i.putExtra("uid", uid);
                                i.putExtra("codSimId", codSimId);
                                i.putExtra("calObjId", calObjId);
                                i.putExtra("fecha", fecha);
                                startActivity(i);
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                    } else if (action.equals("editar")) {
                        String avisoAux = activitySchedule.getAviso();
                        String alarmaAux = activitySchedule.getAlarma();

                        activitySchedule.setHora(hora.getText().toString());
                        activitySchedule.setNombre(autoComplete.getText().toString());
                        activitySchedule.setAlarma(spinner1.getSelectedItem().toString());
                        activitySchedule.setAviso(spinner2.getSelectedItem().toString());
                        activitySchedule.setAntelacion(antelacion);
                        activitySchedule.setCamSemId(camSemId);
                        activitySchedule.setColor(color);
                        activitySchedule.setPalHabId(palHabId);
                        activitySchedule.setUrl(url);

                        dbRef.setValue(activitySchedule);
                        Toast.makeText(getApplicationContext(), "La actividad ha sido editada correctamente.", Toast.LENGTH_SHORT).show();

                        if (activitySchedule.getAviso().equals("Si")) {
                            int antelacionAux = 0;
                            switch (activitySchedule.getAntelacion()) {
                                case "5 Minutos":
                                    antelacionAux = 5;
                                    break;
                                case "10 Minutos":
                                    antelacionAux = 10;
                                    break;
                                case "15 Minutos":
                                    antelacionAux = 15;
                                    break;
                                case "30 Minutos":
                                    antelacionAux = 30;
                                    break;
                                case "1 Hora":
                                    antelacionAux = 60;
                                    break;
                            }

                            establecerNotificacion(fecha, activitySchedule.getHora(), antelacionAux, activitySchedule.getId(), activitySchedule.getUrl(), activitySchedule.getNombre());
                        } else if (activitySchedule.getAviso().equals("No") && avisoAux.equals("Si")) {
                            borrarNotificacion(activitySchedule.getId());
                        }

                        if (activitySchedule.getAlarma().equals("Si")) {
                            establecerAlarma(fecha, activitySchedule.getHora(), (int) activitySchedule.getAlarmCode());
                        } else if (activitySchedule.getAlarma().equals("No") && alarmaAux.equals("Si")) {
                            borrarAlarma((int) activitySchedule.getAlarmCode());
                        }

                        Intent i = new Intent(this, CommonWordDetailActivity.class);
                        i.putExtra("type", tipo);
                        i.putExtra("uid", uid);
                        i.putExtra("codSimId", codSimId);
                        i.putExtra("camSemId", "" + activitySchedule.getCamSemId());
                        i.putExtra("color", "" + activitySchedule.getColor());
                        i.putExtra("nombreCampoSemantico", "");
                        i.putExtra("palHabId", "" + activitySchedule.getPalHabId());
                        i.putExtra("anterior", "agenda");
                        i.putExtra("calObjId", calObjId);
                        i.putExtra("actSchId", "" + activitySchedule.getId());
                        i.putExtra("fecha", fecha);
                        i.putExtra("codigoAlarma", "" + activitySchedule.getAlarmCode());
                        i.putExtra("hora", activitySchedule.getHora());
                        startActivity(i);
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Por favor, indique una tarea ya existente.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getApplicationContext(), "Por favor, rellene todos los campos.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getApplicationContext(), "No puede programar una tarea para el pasado.", Toast.LENGTH_SHORT).show();
        }
    }

    public void botonCancelar(View v) {
        if (action.equals("crear")) {
            Intent i = new Intent(createActivityScheduleActivity, ActivityScheduleActivity.class);
            i.putExtra("type", tipo);
            i.putExtra("uid", uid);
            i.putExtra("codSimId", codSimId);
            i.putExtra("calObjId", calObjId);
            i.putExtra("fecha", fecha);
            startActivity(i);
        } else if (action.equals("editar")) {
            Intent i = new Intent(this, CommonWordDetailActivity.class);
            i.putExtra("type", tipo);
            i.putExtra("uid", uid);
            i.putExtra("codSimId", codSimId);
            i.putExtra("camSemId", "" + activitySchedule.getCamSemId());
            i.putExtra("color", "" + activitySchedule.getColor());
            i.putExtra("nombreCampoSemantico", "");
            i.putExtra("palHabId", "" + activitySchedule.getPalHabId());
            i.putExtra("anterior", "agenda");
            i.putExtra("calObjId", calObjId);
            i.putExtra("actSchId", "" + activitySchedule.getId());
            i.putExtra("fecha", fecha);
            i.putExtra("codigoAlarma", "" + activitySchedule.getAlarmCode());
            i.putExtra("hora", activitySchedule.getHora());
            startActivity(i);
        }
    }

    private List<String> encuentraPalabraHabitual(String palHab) {
        List<String> res = new ArrayList<>();
        String nombreCampo, nombrePalabra;

        if (!palabras.contains(palHab)) {
            return null;
        }

        String[] nombres = palHab.trim().split(" - ");
        nombrePalabra = nombres[0];
        nombreCampo = nombres[1];

        for (SemanticField campoSemantico : camposSemanticos) {
            if (nombreCampo.equals(campoSemantico.getNombre())) {
                for (CommonWord palabraHabitual : campoSemantico.getPalabrasHabituales()) {
                    if (palabraHabitual != null && nombrePalabra.equals(palabraHabitual.getNombre())) {
                        res.add("" + campoSemantico.getId());
                        res.add("" + campoSemantico.getColor());
                        res.add("" + palabraHabitual.getId());
                        if (palabraHabitual.getImagen() != null) {
                            res.add(palabraHabitual.getImagen().getImagenURL());
                        } else {
                            res.add("");
                        }

                        break;
                    }
                }
                break;
            }
        }

        return res;
    }

    private void establecerNotificacion(String fecha, String momento, int antelacion, int id, String url, String nombre) {
        AlarmManager alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        Calendar momentoNotificacion = Calendar.getInstance();

        int year = Integer.parseInt(fecha.substring(0, 4));
        int month = Integer.parseInt(fecha.substring(5, 7)) - 1;
        int day = Integer.parseInt(fecha.substring(8, 10));

        int hour = Integer.parseInt(momento.substring(0, 2));
        int minute = Integer.parseInt(momento.substring(3, 5));

        momentoNotificacion.set(year, month, day, hour, minute);
        momentoNotificacion.add(Calendar.MINUTE, -antelacion);

        String[] nombres = nombre.trim().split(" - ");
        String nombrePalabra = nombres[0];

        Intent intent = new Intent(this, NotificationClass.class);
        intent.putExtra("nombre", nombrePalabra);
        intent.putExtra("url", url);
        intent.putExtra("id", "" + id);

        intent.putExtra("type", tipo);
        intent.putExtra("uid", uid);
        intent.putExtra("codSimId", codSimId);
        intent.putExtra("calObjId", calObjId);
        intent.putExtra("actSchId", "" + activitySchedule.getId());
        intent.putExtra("fecha", fecha);
        intent.putExtra("camSemId", "" + activitySchedule.getCamSemId());
        intent.putExtra("color", "" + activitySchedule.getColor());
        intent.putExtra("palHabId", "" + activitySchedule.getPalHabId());
        intent.putExtra("hora", activitySchedule.getHora());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, id, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        alarmManager.set(AlarmManager.RTC_WAKEUP, momentoNotificacion.getTimeInMillis(), pendingIntent);
    }

    private void establecerAlarma(String fecha, String momento, int id) {
        AlarmManager alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        Calendar momentoAlarma = Calendar.getInstance();

        int year = Integer.parseInt(fecha.substring(0, 4));
        int month = Integer.parseInt(fecha.substring(5, 7)) - 1;
        int day = Integer.parseInt(fecha.substring(8, 10));

        int hour = Integer.parseInt(momento.substring(0, 2));
        int minute = Integer.parseInt(momento.substring(3, 5));

        momentoAlarma.set(year, month, day, hour, minute);

        Intent intent = new Intent(this, AlarmClass.class);

        intent.putExtra("type", tipo);
        intent.putExtra("uid", uid);
        intent.putExtra("codSimId", codSimId);
        intent.putExtra("calObjId", calObjId);
        intent.putExtra("actSchId", "" + activitySchedule.getId());
        intent.putExtra("fecha", fecha);
        intent.putExtra("camSemId", "" + activitySchedule.getCamSemId());
        intent.putExtra("color", "" + activitySchedule.getColor());
        intent.putExtra("palHabId", "" + activitySchedule.getPalHabId());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, id, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        alarmManager.set(AlarmManager.RTC_WAKEUP, momentoAlarma.getTimeInMillis(), pendingIntent);
    }

    private void borrarNotificacion(int id) {
        Intent intent = new Intent(this, NotificationClass.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, id, intent, PendingIntent.FLAG_NO_CREATE);
        pendingIntent.cancel();
        AlarmManager alarmManager = (AlarmManager) getSystemService(getApplicationContext().ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
    }

    private void borrarAlarma(int id) {
        Intent intent = new Intent(this, AlarmClass.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        pendingIntent.cancel();
        AlarmManager alarmManager = (AlarmManager) getSystemService(getApplicationContext().ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
    }
}
