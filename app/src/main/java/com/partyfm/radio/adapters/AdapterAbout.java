package com.partyfm.radio.adapters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.partyfm.radio.R;

public class AdapterAbout extends ArrayAdapter<String> {

    private final Activity context;
    private final String[] titleId;
    private final String[] subtitleId;
    private final Integer[] imageId;

    public AdapterAbout(Activity context, String[] titleId, String[] subtitleId, Integer[] imageId) {
        super(context, R.layout.lsv_item_about, titleId);
        this.context = context;
        this.titleId = titleId;
        this.subtitleId = subtitleId;
        this.imageId = imageId;
    }

    @NonNull
    @Override
    public View getView(int position, View view, @NonNull ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        @SuppressLint({"ViewHolder", "InflateParams"}) View rowView = inflater.inflate(R.layout.lsv_item_about, null, true);

        TextView title = rowView.findViewById(R.id.title);
        TextView subtitle = rowView.findViewById(R.id.subtitle);
        ImageView imageView = rowView.findViewById(R.id.image);

        title.setText(titleId[position]);
        subtitle.setText(subtitleId[position]);
        imageView.setImageResource(imageId[position]);

        return rowView;
    }
}