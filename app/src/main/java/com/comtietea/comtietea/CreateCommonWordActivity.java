package com.comtietea.comtietea;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
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
import java.util.Random;

public class CreateCommonWordActivity extends AppCompatActivity {

    private StorageReference storageReference;
    private DatabaseReference dbRef;

    private String uid;
    private String tipo;
    private String action;
    private String codSimId;
    private String camSemId;
    private String color;
    private String nombreCampoSemantico;
    private String palHabId;

    private EditText name;
    private ImageButton img;
    private Spinner spinner;
    private Uri imgUri;
    private TextView textView;

    private CommonWord palabraHabitual;

    public static final int REQUEST_CODE = 1995;
    private CreateCommonWordActivity createCommonWordActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create);

        createCommonWordActivity = this;

        img = (ImageButton) findViewById(R.id.imageView);
        name = (EditText) findViewById(R.id.editText);
        spinner = (Spinner) findViewById(R.id.spinner);
        textView = (TextView) findViewById(R.id.textView3);

        Bundle bundle = getIntent().getExtras();

        tipo = bundle.getString("type");
        uid = bundle.getString("uid");
        action = bundle.getString("action");
        codSimId = bundle.getString("codSimId");
        camSemId = bundle.getString("camSemId");
        color = bundle.getString("color");
        nombreCampoSemantico = bundle.getString("nombreCampoSemantico");
        palHabId = bundle.getString("palHabId");

        storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(FirebaseReferences.FIREBASE_STORAGE_REFERENCE);

        name.setText("");

        if (action.equals("editar")) {
            dbRef = FirebaseDatabase.getInstance().getReference(
                    FirebaseReferences.USER_REFERENCE + "/" + uid + "/" + FirebaseReferences.SYMBOLIC_CODE_REFERENCE + "/" + codSimId +
                            "/" + FirebaseReferences.SEMANTIC_FIELD_REFERENCE + "/" + camSemId + "/" + FirebaseReferences.COMMON_WORD_REFERENCE +
            "/" + palHabId);

            dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    palabraHabitual = dataSnapshot.getValue(CommonWord.class);

                    name.setText(palabraHabitual.getNombre());
                    spinner.setSelection(palabraHabitual.getRelevancia() - 1);
                    if (!tipo.equals("Palabras")) {
                        Glide.with(createCommonWordActivity).load(palabraHabitual.getImagen().getImagenURL()).into(img);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        } else {
            dbRef = FirebaseDatabase.getInstance().getReference(
                    FirebaseReferences.USER_REFERENCE + "/" + uid + "/" + FirebaseReferences.SYMBOLIC_CODE_REFERENCE + "/" + codSimId +
                            "/" + FirebaseReferences.SEMANTIC_FIELD_REFERENCE + "/" + camSemId);
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
        if (!action.equals("editar")) {
            if (imgUri != null && !tipo.equals("Palabras") && !name.getText().toString().equals("")) {
                final ProgressDialog dialog = new ProgressDialog(this);
                dialog.setTitle("Subiendo imagen");
                dialog.show();

                final String path = "images/" + uid + "/" + tipo + "/" + nombreCampoSemantico + "/" + name.getText().toString() + "." + getImageExt(imgUri);
                StorageReference ref = storageReference.child(path);
                ref.putFile(imgUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        dialog.dismiss();

                        Toast.makeText(getApplicationContext(), "Imagen subida", Toast.LENGTH_SHORT).show();

                        final CommonWord palabraHabitual = new CommonWord(100, name.getText().toString(), new FirebaseImage(taskSnapshot.getDownloadUrl().toString(), path), new Integer(spinner.getSelectedItem().toString()));

                        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                String uploadId = "" + dataSnapshot.child(FirebaseReferences.COMMON_WORD_REFERENCE).getChildrenCount();
                                palabraHabitual.setId(new Integer(uploadId));
                                dataSnapshot.child(FirebaseReferences.COMMON_WORD_REFERENCE).getRef().child(uploadId).setValue(palabraHabitual);

                                Toast.makeText(getApplicationContext(), "La palabra habitual ha sido creada correctamente.", Toast.LENGTH_SHORT).show();

                                Intent i = new Intent(createCommonWordActivity, CommonWordActivity.class);
                                i.putExtra("type", tipo);
                                i.putExtra("uid", uid);
                                i.putExtra("codSimId", codSimId);
                                i.putExtra("camSemId", camSemId);
                                i.putExtra("color", color);
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
            } else if (imgUri == null && tipo.equals("Palabras") && !name.getText().toString().equals("")) {
                final CommonWord palabraHabitual = new CommonWord(100, name.getText().toString(), null, new Integer(spinner.getSelectedItem().toString()));

                dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        String uploadId = "" + dataSnapshot.child(FirebaseReferences.COMMON_WORD_REFERENCE).getChildrenCount();
                        palabraHabitual.setId(new Integer(uploadId));
                        dataSnapshot.child(FirebaseReferences.COMMON_WORD_REFERENCE).getRef().child(uploadId).setValue(palabraHabitual);

                        Toast.makeText(getApplicationContext(), "La palabra habitual ha sido creada correctamente.", Toast.LENGTH_SHORT).show();

                        Intent i = new Intent(createCommonWordActivity, CommonWordActivity.class);
                        i.putExtra("type", tipo);
                        i.putExtra("uid", uid);
                        i.putExtra("codSimId", codSimId);
                        i.putExtra("camSemId", camSemId);
                        i.putExtra("color", color);
                        startActivity(i);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            } else {
                Toast.makeText(getApplicationContext(), "Por favor, rellene todos los campos.", Toast.LENGTH_SHORT).show();
            }
        } else {
            if (imgUri != null && !tipo.equals("Palabras") && !name.getText().toString().equals("")) {
                final ProgressDialog dialog = new ProgressDialog(this);
                dialog.setTitle("Subiendo imagen");
                dialog.show();

                if (palabraHabitual.getImagen().getImagenRuta().contains(uid)) {
                    StorageReference sf = storageReference.child(palabraHabitual.getImagen().getImagenRuta());
                    sf.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            final String path = "images/" + uid + "/" + tipo + "/" + nombreCampoSemantico + "/" + name.getText().toString() + "." + getImageExt(imgUri);
                            StorageReference sfAux = storageReference.child(path);
                            sfAux.putFile(imgUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                    dialog.dismiss();

                                    Toast.makeText(getApplicationContext(), "Imagen subida", Toast.LENGTH_SHORT).show();

                                    palabraHabitual.setNombre(name.getText().toString());
                                    palabraHabitual.setRelevancia(new Integer(spinner.getSelectedItem().toString()));
                                    palabraHabitual.setImagen(new FirebaseImage(taskSnapshot.getDownloadUrl().toString(), path));

                                    dbRef.setValue(palabraHabitual);

                                    Toast.makeText(getApplicationContext(), "La palabra habitual ha sido editada correctamente.", Toast.LENGTH_SHORT).show();

                                    Intent i = new Intent(createCommonWordActivity, CommonWordDetailActivity.class);
                                    i.putExtra("type", tipo);
                                    i.putExtra("uid", uid);
                                    i.putExtra("codSimId", codSimId);
                                    i.putExtra("camSemId", camSemId);
                                    i.putExtra("color", "" + color);
                                    i.putExtra("nombreCampoSemantico", nombreCampoSemantico);
                                    i.putExtra("palHabId", ""+palabraHabitual.getId());
                                    startActivity(i);
                                }
                            });
                        }
                    });
                } else {
                    final String path = "images/" + uid + "/" + tipo + "/" + nombreCampoSemantico + "/" + name.getText().toString() + "." + getImageExt(imgUri);
                    StorageReference ref = storageReference.child(path);
                    ref.putFile(imgUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            dialog.dismiss();

                            Toast.makeText(getApplicationContext(), "Imagen subida", Toast.LENGTH_SHORT).show();

                            palabraHabitual.setNombre(name.getText().toString());
                            palabraHabitual.setRelevancia(new Integer(spinner.getSelectedItem().toString()));
                            palabraHabitual.setImagen(new FirebaseImage(taskSnapshot.getDownloadUrl().toString(), path));

                            dbRef.setValue(palabraHabitual);

                            Toast.makeText(getApplicationContext(), "La palabra habitual ha sido editada correctamente.", Toast.LENGTH_SHORT).show();

                            Intent i = new Intent(createCommonWordActivity, CommonWordDetailActivity.class);
                            i.putExtra("type", tipo);
                            i.putExtra("uid", uid);
                            i.putExtra("codSimId", codSimId);
                            i.putExtra("camSemId", camSemId);
                            i.putExtra("color", "" + color);
                            i.putExtra("nombreCampoSemantico", nombreCampoSemantico);
                            i.putExtra("palHabId", ""+palabraHabitual.getId());
                            startActivity(i);
                        }
                    });
                }
            } else if (imgUri == null && !name.getText().toString().equals("")) {
                palabraHabitual.setNombre(name.getText().toString());
                palabraHabitual.setRelevancia(new Integer(spinner.getSelectedItem().toString()));
                dbRef.setValue(palabraHabitual);

                Toast.makeText(getApplicationContext(), "La palabra habitual ha sido editada correctamente.", Toast.LENGTH_SHORT).show();

                Intent i = new Intent(createCommonWordActivity, CommonWordDetailActivity.class);
                i.putExtra("type", tipo);
                i.putExtra("uid", uid);
                i.putExtra("codSimId", codSimId);
                i.putExtra("camSemId", camSemId);
                i.putExtra("color", "" + color);
                i.putExtra("nombreCampoSemantico", nombreCampoSemantico);
                i.putExtra("palHabId", ""+palabraHabitual.getId());
                startActivity(i);
            } else {
                Toast.makeText(getApplicationContext(), "Por favor, rellene todos los campos.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void botonCancelar(View v) {
        if (action.equals("crear")) {
            Intent i = new Intent(createCommonWordActivity, CommonWordActivity.class);
            i.putExtra("type", tipo);
            i.putExtra("uid", uid);
            i.putExtra("codSimId", codSimId);
            i.putExtra("camSemId", camSemId);
            i.putExtra("color", color);
            startActivity(i);
        } else if (action.equals("editar")) {
            Intent i = new Intent(this, CommonWordDetailActivity.class);
            i.putExtra("type", tipo);
            i.putExtra("uid", uid);
            i.putExtra("codSimId", codSimId);
            i.putExtra("camSemId", camSemId);
            i.putExtra("color", "" + color);
            i.putExtra("nombreCampoSemantico", nombreCampoSemantico);
            i.putExtra("palHabId", ""+palabraHabitual.getId());
            startActivity(i);
        }
    }
}
