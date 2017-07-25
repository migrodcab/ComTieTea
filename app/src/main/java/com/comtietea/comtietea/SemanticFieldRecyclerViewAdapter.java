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
import com.comtietea.comtietea.Domain.SemanticField;

import java.util.ArrayList;

/**
 * Created by HP on 23/07/2017.
 */
public class SemanticFieldRecyclerViewAdapter extends RecyclerView.Adapter<SemanticFieldRecyclerViewAdapter.ViewHolder> {

    ArrayList<SemanticField> camposSemanticos;
    Context mContext;
    protected ItemListener mListener;

    public SemanticFieldRecyclerViewAdapter(Context context, ArrayList<SemanticField> values, ItemListener itemListener) {

        camposSemanticos = values;
        mContext = context;
        mListener=itemListener;
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public TextView textView;
        public ImageView imageView;
        public RelativeLayout relativeLayout;
        SemanticField campoSemantico;

        public ViewHolder(View v) {

            super(v);

            v.setOnClickListener(this);
            textView = (TextView) v.findViewById(R.id.textView);
            imageView = (ImageView) v.findViewById(R.id.imageView);
            relativeLayout = (RelativeLayout) v.findViewById(R.id.relativeLayout);

        }

        public void setData(SemanticField campoSemantico) {
            this.campoSemantico = campoSemantico;

            textView.setText(campoSemantico.getNombre());
            Glide.with(mContext).load(campoSemantico.getImagenURL()).into(imageView);
            relativeLayout.setBackgroundColor(Color.parseColor("#09A9FF"));

        }


        @Override
        public void onClick(View view) {
            if (mListener != null) {
                mListener.onItemClick(campoSemantico);
            }
        }
    }

    @Override
    public SemanticFieldRecyclerViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(mContext).inflate(R.layout.recycler_view_item, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder Vholder, int position) {
        Vholder.setData(camposSemanticos.get(position));

    }

    @Override
    public int getItemCount() {

        return camposSemanticos.size();
    }

    public interface ItemListener {
        void onItemClick(SemanticField campoSemantico);
    }
}
