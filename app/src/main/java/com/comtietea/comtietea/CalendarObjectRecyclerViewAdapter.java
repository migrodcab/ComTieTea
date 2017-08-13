package com.comtietea.comtietea;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.comtietea.comtietea.Domain.CalendarObject;
import com.comtietea.comtietea.Domain.SemanticField;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class CalendarObjectRecyclerViewAdapter extends RecyclerView.Adapter<CalendarObjectRecyclerViewAdapter.ViewHolder> {

    ArrayList<CalendarObject> calendario;
    Context mContext;
    protected ItemListener mListener;

    public CalendarObjectRecyclerViewAdapter(Context context, ArrayList<CalendarObject> values, ItemListener itemListener) {

        calendario = values;
        mContext = context;
        mListener=itemListener;
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public TextView diaSemana;
        public TextView diaMes;
        public RelativeLayout relativeLayout;
        CalendarObject diaCalendario;

        public ViewHolder(View v) {

            super(v);

            v.setOnClickListener(this);
            diaMes = (TextView) v.findViewById(R.id.diaMes);
            diaSemana = (TextView) v.findViewById(R.id.diaSemana);
            relativeLayout = (RelativeLayout) v.findViewById(R.id.relativeLayout);

        }

        public void setData(CalendarObject diaCalendario) {
            this.diaCalendario = diaCalendario;
            Calendar c = Calendar.getInstance();
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
            
            if(fechaAux.equals(diaCalendario.getFecha())) {
                relativeLayout.setBackgroundColor(Color.parseColor("#5EB2FC"));
            }

            diaSemana.setText(diaCalendario.getDiaSemana());
            diaMes.setText(diaCalendario.getFecha().substring(8,10) + " de " + diaCalendario.getMes());
        }

        @Override
        public void onClick(View view) {
            if (mListener != null) {
                mListener.onItemClick(diaCalendario);
            }
        }
    }

    @Override
    public CalendarObjectRecyclerViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(mContext).inflate(R.layout.recycler_view_calendar_item, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder Vholder, int position) {
        Vholder.setData(calendario.get(position));

    }

    @Override
    public int getItemCount() {

        return calendario.size();
    }

    public interface ItemListener {
        void onItemClick(CalendarObject calendarObject);
    }
}
