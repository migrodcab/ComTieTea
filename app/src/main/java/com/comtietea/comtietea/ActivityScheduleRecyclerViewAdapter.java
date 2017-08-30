package com.comtietea.comtietea;

import android.content.Context;
import android.graphics.Color;
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
import java.util.Calendar;

public class ActivityScheduleRecyclerViewAdapter extends RecyclerView.Adapter<ActivityScheduleRecyclerViewAdapter.ViewHolder> {

    ArrayList<ActivitySchedule> actividades;
    Context mContext;
    protected ItemListener mListener;
    String uid;
    String codSimId;
    String fecha;

    public ActivityScheduleRecyclerViewAdapter(Context context, ArrayList<ActivitySchedule> values, ItemListener itemListener, String uid, String codSimId, String fecha) {

        actividades = values;
        mContext = context;
        mListener=itemListener;
        this.uid = uid;
        this.codSimId = codSimId;
        this.fecha = fecha;
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public TextView nombre;
        public TextView hora;
        public ImageView img;
        public RelativeLayout relativeLayout, relativeLayoutHora;
        ActivitySchedule actividad;

        public ViewHolder(View v) {

            super(v);

            v.setOnClickListener(this);
            hora = (TextView) v.findViewById(R.id.hora);
            nombre = (TextView) v.findViewById(R.id.nombre);
            img = (ImageView) v.findViewById(R.id.imageView);
            relativeLayout = (RelativeLayout) v.findViewById(R.id.relativeLayout2);
            relativeLayoutHora = (RelativeLayout) v.findViewById(R.id.relativeLayoutHora);

        }

        public void setData(final ActivitySchedule actividad) {
            this.actividad = actividad;
            int id = -10;
            Calendar c = Calendar.getInstance();
            String horaAux = "";
            String fechaAux;

            fechaAux = "" + c.get(Calendar.YEAR);
            if(c.get(Calendar.MONTH) < 9) {
                fechaAux = fechaAux + "-0" + (c.get(Calendar.MONTH) + 1);
            } else {
                fechaAux = fechaAux + "-" + (c.get(Calendar.MONTH) + 1);
            }
            if (c.get(Calendar.DATE) < 10) {
                fechaAux = fechaAux + "-0" + c.get(Calendar.DATE);
            } else {
                fechaAux = fechaAux + "-" + c.get(Calendar.DATE);
            }

            if(fechaAux.equals(fecha)) {
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

                for (ActivitySchedule actSch : actividades) {
                    if(horaAux.compareTo(actSch.getHora()) >= 0) {
                        id = actSch.getId();
                    } else {
                        break;
                    }
                }

                if (id == actividad.getId()) {
                    relativeLayoutHora.setBackgroundColor(Color.parseColor("#5EB2FC"));
                }
            }

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