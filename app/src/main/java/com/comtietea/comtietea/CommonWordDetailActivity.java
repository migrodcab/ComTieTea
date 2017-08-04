package com.comtietea.comtietea;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
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

public class CommonWordDetailActivity extends AppCompatActivity {
    private String uid;
    private String type;
    private String codSimId;
    private String camSemId;
    private String color;
    private String nombreCampoSemantico;
    private String palHabId;

    private ImageView img;
    private TextView name;
    private RelativeLayout relativeLayout;

    private CommonWordDetailActivity commonWordDetailActivity;
    private DatabaseReference dbRef;

    private CommonWord palabraHabitual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_common_word_detail);

        img = (ImageView) findViewById(R.id.imageView);
        name = (TextView) findViewById(R.id.textView);
        relativeLayout = (RelativeLayout) findViewById(R.id.relativeLayout);

        commonWordDetailActivity = this;

        Bundle bundle = getIntent().getExtras();

        type = bundle.getString("type");
        uid = bundle.getString("uid");
        codSimId = bundle.getString("codSimId");
        camSemId = bundle.getString("camSemId");
        color = bundle.getString("color");
        nombreCampoSemantico = bundle.getString("nombreCampoSemantico");
        palHabId = bundle.getString("palHabId");

        dbRef = FirebaseDatabase.getInstance().getReference(FirebaseReferences.USER_REFERENCE + "/" + uid + "/" +
                FirebaseReferences.SYMBOLIC_CODE_REFERENCE + "/" + codSimId + "/" + FirebaseReferences.SEMANTIC_FIELD_REFERENCE +
                "/" + camSemId + "/" + FirebaseReferences.COMMON_WORD_REFERENCE + "/" + palHabId);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                palabraHabitual = dataSnapshot.getValue(CommonWord.class);

                name.setText(palabraHabitual.getNombre());
                Glide.with(commonWordDetailActivity).load(palabraHabitual.getImagen().getImagenURL()).into(img);
                relativeLayout.setBackgroundColor(new Integer(color));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
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
                deleteCommonWord();
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void deleteCommonWord(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("¿Desea borrar esta palabra habitual?").setCancelable(false)
                .setPositiveButton("Si", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (palabraHabitual.getImagen().getImagenRuta().contains(uid)) {
                            StorageReference sf = FirebaseStorage.getInstance().getReferenceFromUrl(FirebaseReferences.FIREBASE_STORAGE_REFERENCE).child(palabraHabitual.getImagen().getImagenRuta());
                            sf.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    dbRef.setValue(null);

                                    Intent i = new Intent(commonWordDetailActivity, CommonWordActivity.class);
                                    i.putExtra("type", type);
                                    i.putExtra("uid", uid);
                                    i.putExtra("codSimId", codSimId);
                                    i.putExtra("camSemId", camSemId);
                                    i.putExtra("color", color);
                                    startActivity(i);
                                }
                            });
                        } else {
                            dbRef.setValue(null);

                            Intent i = new Intent(commonWordDetailActivity, CommonWordActivity.class);
                            i.putExtra("type", type);
                            i.putExtra("uid", uid);
                            i.putExtra("codSimId", codSimId);
                            i.putExtra("camSemId", camSemId);
                            i.putExtra("color", color);
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
}
