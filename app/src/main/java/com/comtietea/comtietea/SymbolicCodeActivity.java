package com.comtietea.comtietea;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.comtietea.comtietea.Domain.FirebaseReferences;
import com.comtietea.comtietea.Domain.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SymbolicCodeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_symbolic_code);

        //FirebaseDatabase db = FirebaseDatabase.getInstance();
        //DatabaseReference dbRef = db.getReference(FirebaseReferences.COMTIETEA_REFERENCE);
        /*dbRef.child(FirebaseReferences.USER_REFERENCE).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                Log.i("USER", dataSnapshot.toString());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });*/
    }
}
