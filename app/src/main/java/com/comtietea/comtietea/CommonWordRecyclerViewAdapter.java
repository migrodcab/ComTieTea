package com.comtietea.comtietea;

import android.content.Context;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.comtietea.comtietea.Domain.CommonWord;

import java.util.ArrayList;

public class CommonWordRecyclerViewAdapter extends RecyclerView.Adapter<CommonWordRecyclerViewAdapter.ViewHolder> {

    ArrayList<CommonWord> palabrasHabituales;
    Context mContext;
    protected ItemListener mListener;
    String type;
    int color;

    public CommonWordRecyclerViewAdapter(Context context, ArrayList<CommonWord> values, ItemListener itemListener, String type, int color) {

        palabrasHabituales = values;
        mContext = context;
        mListener=itemListener;
        this.type = type;
        this.color = color;
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
            relativeLayout.setBackgroundColor(color);

            if(type.equals("Palabras")) {
                if (textView.getText().toString().length() >= 13) {
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
                } else if (textView.getText().toString().length() >= 9) {
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
                } else if (textView.getText().toString().length() >= 5) {
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
                } else {
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 45);
                }

                imageView.setVisibility(View.GONE);

                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) textView.getLayoutParams();
                layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    layoutParams.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                }
                textView.setLayoutParams(layoutParams);
            } else {
                Glide.with(mContext).load(palabraHabitual.getImagen().getImagenURL()).into(imageView);

                if (textView.getText().toString().length() >= 13) {
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                } else if (textView.getText().toString().length() >= 10) {
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
                }
            }
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
