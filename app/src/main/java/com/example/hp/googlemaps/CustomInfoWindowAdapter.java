package com.example.hp.googlemaps;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

/**
 * Created by hp on 31-Jul-18.
 */

public class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

    private final View window;
    private Context context;

    public CustomInfoWindowAdapter(Context context) {
        this.context = context;
        this.window = LayoutInflater.from(context).inflate(R.layout.custom_info_window, null);
    }

    private void writeWindowText(Marker marker, View view) {

        String title = marker.getTitle();
        TextView textView_title = view.findViewById(R.id.customInfoWindow_title);

        if (!title.equals(""))
            textView_title.setText(title);

        String snippet = marker.getSnippet();
        TextView textView_snippet = view.findViewById(R.id.customInfoWindow_snippet);

        if (!snippet.equals(""))
            textView_snippet.setText(snippet);

    }

    @Override
    public View getInfoWindow(Marker marker) {
        writeWindowText(marker, window);
        return window;
    }

    @Override
    public View getInfoContents(Marker marker) {
        writeWindowText(marker, window);
        return window;
    }
}
