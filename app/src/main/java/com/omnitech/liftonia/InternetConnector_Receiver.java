package com.omnitech.liftonia;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.util.Log;

import com.google.common.collect.Maps;

public class InternetConnector_Receiver extends BroadcastReceiver {
    private static InternetConnector_Receiver receiver = null;
    public InternetConnector_Receiver() {
    }
    public static InternetConnector_Receiver getInstance(){
        if(receiver == null){
            receiver = new InternetConnector_Receiver();
        }
        return receiver;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean isVisible = MyAppState.isActivityVisible();
        if (intent.getAction().matches("android.location.PROVIDERS_CHANGED") && isVisible) {
            //If Action is Location
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            //Check if GPS is turned ON or OFF
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                context.startActivity(new Intent(context, MapsActivity.class));
            }
        }
    }
}
