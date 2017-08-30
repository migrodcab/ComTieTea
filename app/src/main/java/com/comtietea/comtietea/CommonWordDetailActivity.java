package com.comtietea.comtietea;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.comtietea.comtietea.Domain.ActivitySchedule;
import com.comtietea.comtietea.Domain.CommonWord;
import com.comtietea.comtietea.Domain.FirebaseReferences;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Locale;

public class CommonWordDetailActivity extends AppCompatActivity {
    private String uid;
    private String type;
    private String codSimId;
    private String camSemId;
    private String color;
    private String nombreCampoSemantico;
    private String palHabId;
    private String anterior;
    private String calObjId;
    private String actSchId;
    private String fecha;
    private String codigoAlarma;
    private String hora;

    private ImageView img;
    private TextView name;
    private RelativeLayout relativeLayout;
    private ImageButton imageButton;
    private Button button;

    private CommonWordDetailActivity commonWordDetailActivity;
    private DatabaseReference dbRef;

    private CommonWord palabraHabitual;

    private TextToSpeech textToSpeech;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_common_word_detail);

        img = (ImageView) findViewById(R.id.imageView);
        name = (TextView) findViewById(R.id.textView);
        relativeLayout = (RelativeLayout) findViewById(R.id.relativeLayout);
        imageButton = (ImageButton) findViewById(R.id.imageButton);
        button = (Button) findViewById(R.id.aceptar);

        commonWordDetailActivity = this;

        Bundle bundle = getIntent().getExtras();

        type = bundle.getString("type");
        uid = bundle.getString("uid");
        codSimId = bundle.getString("codSimId");
        camSemId = bundle.getString("camSemId");
        color = bundle.getString("color");
        nombreCampoSemantico = bundle.getString("nombreCampoSemantico");
        palHabId = bundle.getString("palHabId");
        anterior = bundle.getString("anterior");
        calObjId = bundle.getString("calObjId");
        actSchId = bundle.getString("actSchId");
        fecha = bundle.getString("fecha");
        codigoAlarma = bundle.getString("codigoAlarma");
        hora = bundle.getString("hora");

        dbRef = FirebaseDatabase.getInstance().getReference(FirebaseReferences.USER_REFERENCE + "/" + uid + "/" +
                FirebaseReferences.SYMBOLIC_CODE_REFERENCE + "/" + codSimId + "/" + FirebaseReferences.SEMANTIC_FIELD_REFERENCE +
                "/" + camSemId + "/" + FirebaseReferences.COMMON_WORD_REFERENCE + "/" + palHabId);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                palabraHabitual = dataSnapshot.getValue(CommonWord.class);

                name.setText(palabraHabitual.getNombre());
                if (!type.equals("Palabras")) {
                    Glide.with(commonWordDetailActivity).load(palabraHabitual.getImagen().getImagenURL()).into(img);
                } else {
                    img.setVisibility(View.GONE);
                    CardView cardView = (CardView) findViewById(R.id.cardView);
                    RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) cardView.getLayoutParams();
                    layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
                    cardView.setLayoutParams(layoutParams);
                }
                relativeLayout.setBackgroundColor(new Integer(color));

                textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {
                        if (status != TextToSpeech.ERROR) {
                            Locale locSpanish = new Locale("spa", "ESP");
                            textToSpeech.setLanguage(locSpanish);
                        }
                    }
                });

                if (name.getText().toString().length() > 7) {
                    name.setTextSize(50f);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        if(anterior.equals("comunicacion")) {
            setTitle(nombreCampoSemantico);
        } else if(anterior.equals("agenda")) {
            setTitle(hora);
        }


        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

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
                if (anterior.equals("comunicacion")) {
                    Intent i = new Intent(this, CreateCommonWordActivity.class);
                    i.putExtra("type", type);
                    i.putExtra("uid", uid);
                    i.putExtra("codSimId", codSimId);
                    i.putExtra("camSemId", camSemId);
                    i.putExtra("color", color);
                    i.putExtra("nombreCampoSemantico", nombreCampoSemantico);
                    i.putExtra("palHabId", palHabId);
                    i.putExtra("action", "editar");
                    startActivity(i);
                } else if (anterior.equals("agenda")) {
                    Intent i = new Intent(this, CreateActivityScheduleActivity.class);
                    i.putExtra("type", type);
                    i.putExtra("uid", uid);
                    i.putExtra("codSimId", codSimId);
                    i.putExtra("calObjId", calObjId);
                    i.putExtra("actSchId", actSchId);
                    i.putExtra("action", "editar");
                    i.putExtra("fecha", fecha);
                    startActivity(i);
                }
                return true;
            case R.id.action_delete:
                if (anterior.equals("comunicacion")) {
                    deleteCommonWord();
                } else if (anterior.equals("agenda")) {
                    deleteActivitySchedule();
                }
                return true;
            case R.id.action_settings:
                Intent intent = new Intent(this, ProfileInfoActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_info:
                Intent intent2 = new Intent(this, AcercaDeActivity.class);
                startActivity(intent2);
                return super.onOptionsItemSelected(item);
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void deleteCommonWord() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("¿Desea borrar esta palabra habitual?").setCancelable(false)
                .setPositiveButton("Si", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final ProgressDialog dialogAux = new ProgressDialog(commonWordDetailActivity);
                        dialogAux.setTitle("Borrando palabra habitual");
                        dialogAux.show();

                        if (palabraHabitual.getImagen() != null && palabraHabitual.getImagen().getImagenRuta().contains(uid)) {
                            StorageReference sf = FirebaseStorage.getInstance().getReferenceFromUrl(FirebaseReferences.FIREBASE_STORAGE_REFERENCE).child(palabraHabitual.getImagen().getImagenRuta());
                            sf.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    dbRef.setValue(new CommonWord(-1, null, null, -1));
                                    dialogAux.dismiss();

                                    Intent i = new Intent(commonWordDetailActivity, CommonWordActivity.class);
                                    i.putExtra("type", type);
                                    i.putExtra("uid", uid);
                                    i.putExtra("codSimId", codSimId);
                                    i.putExtra("camSemId", camSemId);
                                    i.putExtra("color", color);
                                    Toast.makeText(getApplicationContext(), "La palabra habitual ha sido borrada correctamente.", Toast.LENGTH_SHORT).show();
                                    startActivity(i);

                                }
                            });
                        } else {
                            dbRef.setValue(new CommonWord(-1, null, null, -1));
                            dialogAux.dismiss();

                            Intent i = new Intent(commonWordDetailActivity, CommonWordActivity.class);
                            i.putExtra("type", type);
                            i.putExtra("uid", uid);
                            i.putExtra("codSimId", codSimId);
                            i.putExtra("camSemId", camSemId);
                            i.putExtra("color", color);
                            Toast.makeText(getApplicationContext(), "La palabra habitual ha sido borrada correctamente.", Toast.LENGTH_SHORT).show();
                            startActivity(i);

                        }
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

    private void deleteActivitySchedule() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("¿Desea borrar esta actividad?").setCancelable(false)
                .setPositiveButton("Si", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final ProgressDialog dialogAux = new ProgressDialog(commonWordDetailActivity);
                        dialogAux.setTitle("Borrando actividad");
                        dialogAux.show();

                        dbRef = FirebaseDatabase.getInstance().getReference(
                                FirebaseReferences.USER_REFERENCE + "/" + uid + "/" + FirebaseReferences.SYMBOLIC_CODE_REFERENCE + "/" + codSimId + "/" +
                                        FirebaseReferences.CALENDAR_OBJECT_REFERENCE + "/" + calObjId + "/" + FirebaseReferences.ACTIVITY_SCHEDULE_REFERENCE +
                                        "/" + actSchId);

                        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                ActivitySchedule activitySchedule = dataSnapshot.getValue(ActivitySchedule.class);

                                if (activitySchedule.getAviso().equals("Si")) {
                                    borrarNotificacion(Integer.parseInt(actSchId));
                                }

                                if (activitySchedule.getAlarma().equals("Si")) {
                                    Log.i("CODIGO", codigoAlarma);
                                    borrarAlarma(new Integer(codigoAlarma));
                                }

                                dbRef.setValue(new ActivitySchedule(-1, null, null, null, null, null, -1, -1, -1, null, -1));
                                dialogAux.dismiss();

                                Intent i = new Intent(commonWordDetailActivity, ActivityScheduleActivity.class);
                                i.putExtra("type", type);
                                i.putExtra("uid", uid);
                                i.putExtra("codSimId", codSimId);
                                i.putExtra("calObjId", calObjId);
                                i.putExtra("fecha", fecha);
                                Toast.makeText(getApplicationContext(), "La actividad ha sido borrada correctamente.", Toast.LENGTH_SHORT).show();
                                startActivity(i);
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

    public void suenaPalabra(View view) {
        textToSpeech.speak(name.getText().toString(), TextToSpeech.QUEUE_FLUSH, null);
    }

    public void onPause() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onPause();
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (anterior.equals("comunicacion")) {
            Intent i = new Intent(this, CommonWordActivity.class);
            i.putExtra("type", type);
            i.putExtra("uid", uid);
            i.putExtra("codSimId", codSimId);
            i.putExtra("camSemId", camSemId);
            i.putExtra("color", color);
            startActivity(i);
        } else if (anterior.equals("agenda")) {
            Intent i = new Intent(this, ActivityScheduleActivity.class);
            i.putExtra("type", type);
            i.putExtra("uid", uid);
            i.putExtra("codSimId", codSimId);
            i.putExtra("calObjId", calObjId);
            i.putExtra("fecha", fecha);
            startActivity(i);
        }
        return false;
    }

    public void aceptarAlarma(View view) {
        dbRef = FirebaseDatabase.getInstance().getReference(
                FirebaseReferences.USER_REFERENCE + "/" + uid + "/" + FirebaseReferences.SYMBOLIC_CODE_REFERENCE + "/" + codSimId + "/" +
                        FirebaseReferences.CALENDAR_OBJECT_REFERENCE + "/" + calObjId + "/" + FirebaseReferences.ACTIVITY_SCHEDULE_REFERENCE +
                        "/" + actSchId);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ActivitySchedule activitySchedule = dataSnapshot.getValue(ActivitySchedule.class);

                if (activitySchedule.getAlarma().equals("Si")) {
                    borrarAlarma(Integer.parseInt(codigoAlarma));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void borrarNotificacion(int id) {
        Intent intent = new Intent(this, NotificationClass.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), id, intent, PendingIntent.FLAG_NO_CREATE);
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
