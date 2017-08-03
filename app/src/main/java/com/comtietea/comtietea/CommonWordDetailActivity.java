package com.comtietea.comtietea;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class CommonWordDetailActivity extends AppCompatActivity {
    private String uid;
    private String type;
    private String codSimId;
    private String camSemId;
    private String color;
    private String nombreCampoSemantico;
    private String palHabId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_common_word_detail);

        Bundle bundle = getIntent().getExtras();

        type = bundle.getString("type");
        uid = bundle.getString("uid");
        codSimId = bundle.getString("codSimId");
        camSemId = bundle.getString("camSemId");
        color = bundle.getString("color");
        nombreCampoSemantico = bundle.getString("nombreCampoSemantico");
        palHabId = bundle.getString("palHabId");
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
                return true;
            case R.id.action_delete:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }
}
