package com.comtietea.comtietea;

import android.content.Context;
import android.os.Build;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.comtietea.comtietea.Domain.ActivitySchedule;
import com.comtietea.comtietea.Domain.CalendarObject;
import com.comtietea.comtietea.Domain.CommonWord;
import com.comtietea.comtietea.Domain.FirebaseReferences;
import com.comtietea.comtietea.Domain.SemanticField;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class ActivityScheduleRecyclerViewAdapter extends RecyclerView.Adapter<ActivityScheduleRecyclerViewAdapter.ViewHolder> {

    ArrayList<ActivitySchedule> actividades;
    Context mContext;
    protected ItemListener mListener;
    String uid;
    String codSimId;

    public ActivityScheduleRecyclerViewAdapter(Context context, ArrayList<ActivitySchedule> values, ItemListener itemListener, String uid, String codSimId) {

        actividades = values;
        mContext = context;
        mListener=itemListener;
        this.uid = uid;
        this.codSimId = codSimId;
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public TextView nombre;
        public TextView hora;
        public ImageView img;
        public RelativeLayout relativeLayout;
        ActivitySchedule actividad;

        public ViewHolder(View v) {

            super(v);

            v.setOnClickListener(this);
            hora = (TextView) v.findViewById(R.id.hora);
            nombre = (TextView) v.findViewById(R.id.nombre);
            img = (ImageView) v.findViewById(R.id.imageView);
            relativeLayout = (RelativeLayout) v.findViewById(R.id.relativeLayout2);

        }

        public void setData(final ActivitySchedule actividad) {
            this.actividad = actividad;

            FirebaseDatabase.getInstance().getReference(FirebaseReferences.USER_REFERENCE + "/" + uid + "/" +
                    FirebaseReferences.SYMBOLIC_CODE_REFERENCE + "/" + codSimId + "/" + FirebaseReferences.SEMANTIC_FIELD_REFERENCE +
                    "/" + actividad.getCamSemId() + "/" + FirebaseReferences.COMMON_WORD_REFERENCE + "/" + actividad.getPalHabId()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    CommonWord palabraHabitual = dataSnapshot.getValue(CommonWord.class);

                    nombre.setText(palabraHabitual.getNombre());

                    if(palabraHabitual.getImagen() == null) {
                        if (nombre.getText().toString().length() > 7) {
                            //textView.setTextSize(textView.getTextSize()*1.125f);
                        } else {
                            //textView.setTextSize(textView.getTextSize()*1.25f);
                        }

                        img.setVisibility(View.GONE);

                        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) nombre.getLayoutParams();
                        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                            layoutParams.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                        }
                        nombre.setLayoutParams(layoutParams);
                    } else {
                        Glide.with(mContext).load(palabraHabitual.getImagen().getImagenURL()).into(img);
                    }

                    relativeLayout.setBackgroundColor(actividad.getColor());
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

            hora.setText(actividad.getHora());
            nombre.setText(actividad.getCamSemId() + " - " + actividad.getPalHabId());
        }


        @Override
        public void onClick(View view) {
            if (mListener != null) {
                mListener.onItemClick(actividad);
            }
        }
    }

    @Override
    public ActivityScheduleRecyclerViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(mContext).inflate(R.layout.recycler_view_activity_schedule, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder Vholder, int position) {
        Vholder.setData(actividades.get(position));

    }

    @Override
    public int getItemCount() {

        return actividades.size();
    }

    public interface ItemListener {
        void onItemClick(ActivitySchedule actividad);
    }
}