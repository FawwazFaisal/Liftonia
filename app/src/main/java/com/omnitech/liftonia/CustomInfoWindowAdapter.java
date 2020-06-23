package com.omnitech.liftonia;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

public class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {
    private final View mWindow;
    private final Context mContext;

    public CustomInfoWindowAdapter(Context context) {
        mContext = context;
        mWindow = LayoutInflater.from(mContext).inflate(R.layout.custom_info_window,null);
    }

    private void renderWindowText(Marker marker, View view){
        String title = marker.getTitle();
        TextView titleView = view.findViewById(R.id.title);
        if(!title.isEmpty()){
            titleView.setText(title);
        }

        String snippet = marker.getSnippet();
        TextView snippetView = view.findViewById(R.id.snippet);
        if(!snippet.isEmpty()){
            snippetView.setText(snippet);
        }
        else if(snippet.isEmpty()){
            snippetView.setText("");
        }
    }


    @Override
    public View getInfoWindow(Marker marker) {
        renderWindowText(marker, mWindow);
        return mWindow;
    }

    @Override
    public View getInfoContents(Marker marker) {
        renderWindowText(marker, mWindow);
        return mWindow;
    }
}
