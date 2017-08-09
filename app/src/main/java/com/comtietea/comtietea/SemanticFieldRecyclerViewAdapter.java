package com.comtietea.comtietea;

import android.content.Context;
import android.os.Build;
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

public class SemanticFieldRecyclerViewAdapter extends RecyclerView.Adapter<SemanticFieldRecyclerViewAdapter.ViewHolder> {

    ArrayList<SemanticField> camposSemanticos;
    Context mContext;
    protected ItemListener mListener;
    String type;

    public SemanticFieldRecyclerViewAdapter(Context context, ArrayList<SemanticField> values, ItemListener itemListener, String type) {

        camposSemanticos = values;
        mContext = context;
        mListener=itemListener;
        this.type = type;
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
            relativeLayout.setBackgroundColor(campoSemantico.getColor());

            if(type.equals("Palabras")) {
                if (textView.getText().toString().length() > 7) {
                    //textView.setTextSize(textView.getTextSize()*1.125f);
                } else {
                    //textView.setTextSize(textView.getTextSize()*1.25f);
                }

                imageView.setVisibility(View.GONE);

                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) textView.getLayoutParams();
                layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    layoutParams.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                }
                textView.setLayoutParams(layoutParams);
            } else {
                Glide.with(mContext).load(campoSemantico.getImagen().getImagenURL()).into(imageView);
            }
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
