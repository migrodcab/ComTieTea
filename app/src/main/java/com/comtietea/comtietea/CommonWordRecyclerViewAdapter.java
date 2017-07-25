package com.comtietea.comtietea;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.comtietea.comtietea.Domain.CommonWord;
import com.comtietea.comtietea.Domain.SemanticField;

import java.util.ArrayList;

/**
 * Created by HP on 23/07/2017.
 */
public class CommonWordRecyclerViewAdapter extends RecyclerView.Adapter<CommonWordRecyclerViewAdapter.ViewHolder> {

    ArrayList<CommonWord> palabrasHabituales;
    Context mContext;
    protected ItemListener mListener;

    public CommonWordRecyclerViewAdapter(Context context, ArrayList<CommonWord> values, ItemListener itemListener) {

        palabrasHabituales = values;
        mContext = context;
        mListener=itemListener;
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public TextView textView;
        public ImageView imageView;
        public RelativeLayout relativeLayout;
        CommonWord palabraHabitual;

        public ViewHolder(View v) {

            super(v);

            v.setOnClickListener(this);
            textView = (TextView) v.findViewById(R.id.textView);
            imageView = (ImageView) v.findViewById(R.id.imageView);
            relativeLayout = (RelativeLayout) v.findViewById(R.id.relativeLayout);

        }

        public void setData(CommonWord palabraHabitual) {
            this.palabraHabitual = palabraHabitual;

            textView.setText(palabraHabitual.getNombre());
            Glide.with(mContext).load(palabraHabitual.getImagenURL()).into(imageView);
            relativeLayout.setBackgroundColor(Color.parseColor("#09A9FF"));

        }


        @Override
        public void onClick(View view) {
            if (mListener != null) {
                mListener.onItemClick(palabraHabitual);
            }
        }
    }

    @Override
    public CommonWordRecyclerViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(mContext).inflate(R.layout.recycler_view_item, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder Vholder, int position) {
        Vholder.setData(palabrasHabituales.get(position));

    }

    @Override
    public int getItemCount() {

        return palabrasHabituales.size();
    }

    public interface ItemListener {
        void onItemClick(CommonWord palabraHabitual);
    }
}
