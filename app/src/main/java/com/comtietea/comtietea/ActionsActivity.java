package com.comtietea.comtietea;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;

import com.bumptech.glide.Glide;

public class ActionsActivity extends AppCompatActivity {
    private String type;
    private String uid;
    private String codSimId;

    private ImageButton comunicacion, agenda;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_actions);

        Bundle bundle = getIntent().getExtras();

        type = bundle.getString("type");
        uid = bundle.getString("uid");
        codSimId = bundle.getString("codSimId");

        comunicacion = (ImageButton) findViewById(R.id.comunicacion);
        agenda = (ImageButton) findViewById(R.id.agenda);

        if(type.equals("Palabras")) {
            comunicacion.setImageResource(R.drawable.comunicacion_palabras);
            agenda.setImageResource(R.drawable.agenda_palabras);
        } else if(type.equals("Dibujos")) {
            comunicacion.setImageResource(R.drawable.comunicacion_dibujos);
            agenda.setImageResource(R.drawable.agenda_dibujos);
        } else if(type.equals("Imagenes")) {
            comunicacion.setImageResource(R.drawable.comunicacion_imagenes);
            agenda.setImageResource(R.drawable.agenda_imagenes);
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public void comunicacionButton(View view) {
        Intent i = new Intent(this, SemanticFieldActivity.class);
        i.putExtra("type", type);
        i.putExtra("uid", uid);
        i.putExtra("codSimId", codSimId);
        startActivity(i);
    }

    public void agendaButton(View view) {
        Intent i = new Intent(this, CalendarActivity.class);
        i.putExtra("type", type);
        i.putExtra("uid", uid);
        i.putExtra("codSimId", codSimId);
        startActivity(i);
    }

    @Override
    public boolean onSupportNavigateUp() {
        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_actions_profile, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent(this, ProfileInfoActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
