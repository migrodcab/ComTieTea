package com.comtietea.comtietea;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.comtietea.comtietea.Domain.CommonWord;
import com.comtietea.comtietea.Domain.FirebaseImage;
import com.comtietea.comtietea.Domain.FirebaseReferences;
import com.comtietea.comtietea.Domain.SemanticField;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class CreateSemanticFieldActivity extends AppCompatActivity {

    private StorageReference storageReference;
    private DatabaseReference dbRef;

    private String uid;
    private String tipo;
    private String action;
    private String codSimId;
    private String camSemId;

    private EditText name;
    private ImageButton img;
    private Spinner spinner;
    private Uri imgUri;
    private TextView textView;

    public static final int REQUEST_CODE = 1995;
    private CreateSemanticFieldActivity createSemanticFieldActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create);

        createSemanticFieldActivity = this;

        img = (ImageButton) findViewById(R.id.imageView);
        name = (EditText) findViewById(R.id.editText);
        spinner = (Spinner) findViewById(R.id.spinner);
        textView = (TextView) findViewById(R.id.textView3);

        Bundle bundle = getIntent().getExtras();

        tipo = bundle.getString("type");
        uid = bundle.getString("uid");
        action = bundle.getString("action");
        codSimId = bundle.getString("codSimId");
        camSemId= bundle.getString("camSemId");

        storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(FirebaseReferences.FIREBASE_STORAGE_REFERENCE);

        name.setText("");

        if (action.equals("editar")) {
            dbRef = FirebaseDatabase.getInstance().getReference(
                    FirebaseReferences.USER_REFERENCE + "/" + uid + "/" + FirebaseReferences.SYMBOLIC_CODE_REFERENCE + "/" + codSimId +
                    "/" + FirebaseReferences.SEMANTIC_FIELD_REFERENCE + "/" + camSemId);

            dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    SemanticField campoSemantico = dataSnapshot.getValue(SemanticField.class);

                    name.setText(campoSemantico.getNombre());
                    spinner.setSelection(campoSemantico.getRelevancia() - 1);
                    if(!tipo.equals("Palabras")) {
                        Glide.with(createSemanticFieldActivity).load(campoSemantico.getImagen().getImagenURL()).into(img);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        } else {
            dbRef = FirebaseDatabase.getInstance().getReference(
                    FirebaseReferences.USER_REFERENCE + "/" + uid + "/" + FirebaseReferences.SYMBOLIC_CODE_REFERENCE + "/" + codSimId);
        }

        if (tipo.equals("Palabras")) {
            img.setVisibility(View.GONE);
            textView.setVisibility(View.GONE);
        }
    }

    public void cargaImagen(View v) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Elija la imagen"), REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imgUri = data.getData();

            try {
                Bitmap bm = MediaStore.Images.Media.getBitmap(getContentResolver(), imgUri);
                img.setImageBitmap(bm);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String getImageExt(Uri uri) {
        ContentResolver contentResolver = getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
    }

    public void botonGuardar(View v) {
        if (imgUri != null && !tipo.equals("Palabras") && !name.getText().toString().equals("")) {
            final ProgressDialog dialog = new ProgressDialog(this);
            dialog.setTitle("Subiendo imagen");
            dialog.show();

            final String path = "images/" + uid + "/" + name.getText().toString() + "/" + name.getText().toString() + "." + getImageExt(imgUri);
            StorageReference ref = storageReference.child(path);
            ref.putFile(imgUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    dialog.dismiss();

                    Toast.makeText(getApplicationContext(), "Imagen subida", Toast.LENGTH_SHORT).show();

                    Random rnd = new Random();
                    int color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
                    final SemanticField campoSemantico = new SemanticField(100, name.getText().toString(), new FirebaseImage(taskSnapshot.getDownloadUrl().toString(), path), new Integer(spinner.getSelectedItem().toString()), color, new ArrayList<CommonWord>());

                    dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            String uploadId = "" + dataSnapshot.child(FirebaseReferences.SEMANTIC_FIELD_REFERENCE).getChildrenCount();
                            campoSemantico.setId(new Integer(uploadId));
                            dataSnapshot.child(FirebaseReferences.SEMANTIC_FIELD_REFERENCE).getRef().child(uploadId).setValue(campoSemantico);

                            Toast.makeText(getApplicationContext(), "El campo semántico ha sido creado correctamente.", Toast.LENGTH_SHORT).show();

                            Intent i = new Intent(createSemanticFieldActivity, SemanticFieldActivity.class);
                            i.putExtra("type", tipo);
                            i.putExtra("uid", uid);
                            i.putExtra("codSimId", codSimId);
                            startActivity(i);
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }
            })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            dialog.dismiss();
                            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {

                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            double progress = (100 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                            dialog.setMessage("En proceso " + (int) progress + "%");
                        }
                    });
        } else if(imgUri == null && tipo.equals("Palabras") && !name.getText().toString().equals("")) {
            Random rnd = new Random();
            int color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
            final SemanticField campoSemantico = new SemanticField(100, name.getText().toString(), null, new Integer(spinner.getSelectedItem().toString()), color, new ArrayList<CommonWord>());

            dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    String uploadId = "" + dataSnapshot.child(FirebaseReferences.SEMANTIC_FIELD_REFERENCE).getChildrenCount();
                    campoSemantico.setId(new Integer(uploadId));
                    dataSnapshot.child(FirebaseReferences.SEMANTIC_FIELD_REFERENCE).getRef().child(uploadId).setValue(campoSemantico);

                    Toast.makeText(getApplicationContext(), "El campo semántico ha sido creado correctamente.", Toast.LENGTH_SHORT).show();

                    Intent i = new Intent(createSemanticFieldActivity, SemanticFieldActivity.class);
                    i.putExtra("type", tipo);
                    i.putExtra("uid", uid);
                    i.putExtra("codSimId", codSimId);
                    startActivity(i);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        } else {
            Toast.makeText(getApplicationContext(), "Por favor, rellene todos los campos.", Toast.LENGTH_SHORT).show();
        }
    }

    public void botonCancelar(View v) {
        Intent i = new Intent(this, SemanticFieldActivity.class);
        i.putExtra("type", tipo);
        i.putExtra("uid", uid);
        i.putExtra("codSimId", codSimId);
        startActivity(i);
    }
}
