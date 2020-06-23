package com.omnitech.liftonia;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.icu.text.SimpleDateFormat;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

public class SearchRide extends AppCompatActivity {

    public static final String phNo = "phNo";
    public static final String CUST_DEST = "CUST_DEST";
    public static final String CUST_SRC_LAT = "CUST_SRC_LAT";
    public static final String CUST_SRC_LNG = "CUST_SRC_LNG";
    private static final String NAME = "NAME";
    private static final String SID = "SID";
    private static final String CUSTUID = "CUSTUID";
    private static final int ENABLE_GPS_CODE = 158;
    private static final int ENABLE_WIFI_CODE = 159;
    Button strtSearch;
    TextView setTime;
    String[] time;
    LocationRequest mLocationRequest;
    LocationCallback mLocationCallback;
    FusedLocationProviderClient fusedLocationProviderClient;
    Location currloc;
    LocationManager locationManager;
    boolean locationStatus = false;
    boolean isInKIET = false;
    Spinner spinner;
    LatLng destination;
    ProgressBar progressBar;
    boolean netState = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_ride);

        strtSearch = findViewById(R.id.searchbtn);
        setTime = findViewById(R.id.setTime2);
        spinner = findViewById(R.id.spinner2);
        progressBar = findViewById(R.id.progressBar3);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationStatus = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!locationStatus)
            EnableGPS();
        else
            fetchLastLocation();

        progressBar.getIndeterminateDrawable().setColorFilter(0xFFFF0000, android.graphics.PorterDuff.Mode.MULTIPLY);

        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        setTime.setShowSoftInputOnFocus(false);
        setTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setStartTime(setTime);
            }
        });
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        destination = new LatLng(24.794795, 67.136185);
                        break;
                    case 1:
                        destination = new LatLng(24.861874, 67.073479);
                        break;
                    case 2:
                        destination = new LatLng(24.929144, 67.040552);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                destination = null;
            }
        });
        strtSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                netState = networkInfo != null && networkInfo.isConnected();
                if (currloc != null && netState) {
                    saveToDB();
                } else {
                    EnableWifi(netState);
                }
            }
        });
    }

    private void EnableWifi(boolean netState) {
        if (!netState) {
            progressBar.setVisibility(View.VISIBLE);
            Snackbar.make(findViewById(R.id.layout_activity_search), "Please connect to the internet", Snackbar.LENGTH_INDEFINITE)
                    .setAction("ENABLE", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            startActivityForResult(new Intent(Settings.ACTION_WIFI_SETTINGS), ENABLE_WIFI_CODE);
                        }
                    })
                    .setActionTextColor(getResources().getColor(android.R.color.holo_red_light))
                    .show();
        }
    }

    private void setStartTime(final TextView time) {
        final View dialogView = View.inflate(this, R.layout.datetime_picker, null);
        final AlertDialog alertDialog = new AlertDialog.Builder(this).create();

        dialogView.findViewById(R.id.date_time_set).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                DatePicker datePicker = (DatePicker) dialogView.findViewById(R.id.date_picker);
                TimePicker timePicker = (TimePicker) dialogView.findViewById(R.id.time_picker);

                Calendar calendar = new GregorianCalendar(datePicker.getYear(),
                        datePicker.getMonth(),
                        datePicker.getDayOfMonth(),
                        timePicker.getCurrentHour(),
                        timePicker.getCurrentMinute());

                SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm a");

                // Create a calendar object that will convert the date and time value in milliseconds to date.
                calendar.setTimeInMillis(calendar.getTimeInMillis());
                time.setText((String.valueOf(formatter.format(calendar.getTime()))));
                alertDialog.dismiss();
            }});
        dialogView.findViewById(R.id.button3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.dismiss();
            }});
        alertDialog.setView(dialogView);
        alertDialog.show();
    }

    private void saveToDB() {
        progressBar.setVisibility(View.VISIBLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        String time = setTime.getText().toString();
        if (time.equals("00:00") ||  destination == null) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Incomplete Information", Toast.LENGTH_SHORT).show();
        } else {
            String period = time;
            //create UUID for Booking table
            String ID = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
            Random random = new Random();
            String suffix = String.valueOf(random.nextInt(999));
            SharedPreferences sharedPreferences = PreferenceManager
                    .getDefaultSharedPreferences(SearchRide.this);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(CUSTUID, ID + suffix);
            editor.apply();

            //retrieve variables from sharedpreferences into variables
            String CustName = sharedPreferences.getString(NAME, "");
            String CustPhNo = sharedPreferences.getString(phNo, "");
            String CustSID = sharedPreferences.getString(SID, "");

            //create maps to set in DB table
            final HashMap ride = new HashMap();
            ride.put("CustName", CustName);
            ride.put("CustSID", CustSID);
            ride.put("CustPhNo", CustPhNo);
            ride.put("CustTime", time);
            ride.put("CustLatLng", currloc.getLatitude() + "," + currloc.getLongitude());
            ride.put("CustDestination", destination.latitude + "," + destination.longitude);
            sharedPreferences.edit().putString(CUST_DEST, destination.latitude + "," + destination.longitude).apply();
            sharedPreferences.edit().putString(CUST_SRC_LAT, String.valueOf(currloc.getLatitude())).apply();
            sharedPreferences.edit().putString(CUST_SRC_LNG, String.valueOf(currloc.getLongitude())).apply();


            //Instantiate Firebase;
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("Customers").document(ID + suffix).set(ride).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    progressBar.setVisibility(View.GONE);
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                    startActivity(new Intent(SearchRide.this, MapsActivity.class));
                }
            });
        }
    }

    private void fetchLastLocation() {
        if (currloc == null) {
            progressBar.setVisibility(View.VISIBLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            mLocationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(final LocationResult locationResult) {
                    for (Location location : locationResult.getLocations()) {
                        if (location != null) {
                            LocationServices.getFusedLocationProviderClient(SearchRide.this).getLastLocation()
                                    .addOnSuccessListener(new OnSuccessListener<Location>() {
                                        @Override
                                        public void onSuccess(Location location) {
                                            if (location != null) {
                                                progressBar.setVisibility(View.GONE);
                                                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                                                currloc = location;
                                            }
                                            LatLng latLng = new LatLng(currloc.getLatitude(), currloc.getLongitude());
                                            //setting ride to HOME from MAIN
                                            if (latLng.latitude > 24.794795) {
                                                if (latLng.longitude > 67.136185) {
                                                    if (latLng.latitude - 24.794795 < 0.004000 && latLng.longitude - 67.136185 < 0.004000) {
                                                        Toast.makeText(SearchRide.this, "In Main Campus", Toast.LENGTH_SHORT).show();
                                                        isInKIET = true;
                                                    }
                                                } else if (latLng.longitude < 67.136185) {
                                                    if (latLng.latitude - 24.794795 < 0.004000 && 67.136185 - latLng.longitude < 0.004000) {
                                                        Toast.makeText(SearchRide.this, "In Main Campus", Toast.LENGTH_SHORT).show();
                                                        isInKIET = true;
                                                    }
                                                }
                                            }
                                            if (latLng.latitude < 24.794795) {
                                                if (latLng.longitude > 67.136185) {
                                                    if (24.794795 - latLng.latitude < 0.004000 && latLng.longitude - 67.136185 < 0.004000) {
                                                        Toast.makeText(SearchRide.this, "In Main Campus", Toast.LENGTH_SHORT).show();
                                                        isInKIET = true;
                                                    }
                                                } else if (latLng.longitude < 67.136185) {
                                                    if (24.794795 - latLng.latitude < 0.004000 && latLng.longitude - 67.136185 < 0.004000) {
                                                        Toast.makeText(SearchRide.this, "In Main Campus", Toast.LENGTH_SHORT).show();
                                                        isInKIET = true;
                                                    }
                                                }
                                            }
                                            //setting ride to HOME from CITY
                                            else if (latLng.latitude > 24.861874) {
                                                if (latLng.longitude > 67.073479) {
                                                    if (latLng.latitude - 24.861874 < 0.004000 && latLng.longitude - 67.073479 < 0.004000) {
                                                        Toast.makeText(SearchRide.this, "In City Campus", Toast.LENGTH_SHORT).show();
                                                        isInKIET = true;
                                                    }
                                                } else if (latLng.longitude < 67.073479) {
                                                    if (latLng.latitude - 24.861874 < 0.004000 && 67.073479 - latLng.longitude < 0.004000) {
                                                        Toast.makeText(SearchRide.this, "In City Campus", Toast.LENGTH_SHORT).show();
                                                        isInKIET = true;
                                                    }
                                                }
                                            }
                                            if (latLng.latitude < 24.861874) {
                                                if (latLng.longitude > 67.073479) {
                                                    if (24.861874 - latLng.latitude < 0.004000 && latLng.longitude - 67.073479 < 0.004000) {
                                                        Toast.makeText(SearchRide.this, "In City Campus", Toast.LENGTH_SHORT).show();
                                                        isInKIET = true;
                                                    }
                                                } else if (latLng.longitude < 67.073479) {
                                                    if (24.861874 - latLng.latitude < 0.004000 && latLng.longitude - 67.073479 < 0.004000) {
                                                        Toast.makeText(SearchRide.this, "In City Campus", Toast.LENGTH_SHORT).show();
                                                        isInKIET = true;
                                                    }
                                                }
                                            }
                                            //setting ride to HOME from NORTH
                                            if (latLng.latitude > 24.929144) {
                                                if (latLng.longitude > 67.040552) {
                                                    if (latLng.latitude - 24.929144 < 0.004000 && latLng.longitude - 67.040552 < 0.004000) {
                                                        Toast.makeText(SearchRide.this, "In North Campus", Toast.LENGTH_SHORT).show();
                                                        isInKIET = true;
                                                    }
                                                } else if (latLng.longitude < 67.136185) {
                                                    if (latLng.latitude - 24.929144 < 0.004000 && 67.040552 - latLng.longitude < 0.004000) {
                                                        Toast.makeText(SearchRide.this, "In North Campus", Toast.LENGTH_SHORT).show();
                                                        isInKIET = true;
                                                    }
                                                }
                                            } else if (latLng.latitude < 24.929144) {
                                                if (latLng.longitude > 67.040552) {
                                                    if (24.929144 - latLng.latitude < 0.004000 && latLng.longitude - 67.040552 < 0.004000) {
                                                        Toast.makeText(SearchRide.this, "In North Campus", Toast.LENGTH_SHORT).show();
                                                        isInKIET = true;
                                                    }
                                                } else if (latLng.longitude < 67.136185) {
                                                    if (24.929144 - latLng.latitude < 0.004000 && latLng.longitude - 67.040552 < 0.004000) {
                                                        Toast.makeText(SearchRide.this, "In North Campus", Toast.LENGTH_SHORT).show();
                                                        isInKIET = true;
                                                    }
                                                }
                                            }
                                            if (!isInKIET) {
                                                spinner.setVisibility(View.VISIBLE);
                                            }
                                        }
                                    });
                            break;
                        }
                    }
                }
            };
            mLocationRequest = LocationRequest.create();
            mLocationRequest.setNumUpdates(1);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            mLocationRequest.setInterval(2000);
            mLocationRequest.setFastestInterval(1000);
            fusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
        }
    }

    private void EnableGPS() {
        progressBar.setVisibility(View.VISIBLE);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        Snackbar.make(findViewById(R.id.layout_activity_search), "Please turn on Locations", Snackbar.LENGTH_INDEFINITE)
                .setAction("ENABLE", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), ENABLE_GPS_CODE);
                    }
                })
                .setActionTextColor(getResources().getColor(android.R.color.holo_red_light))
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ENABLE_GPS_CODE) {
            locationStatus = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            if (!locationStatus) {
                EnableGPS();
            }
        } else if (requestCode == ENABLE_WIFI_CODE) {
            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (networkInfo.isConnected() && networkInfo != null) {
                startActivity(new Intent(SearchRide.this, SearchRide.class));
            }
        }

    }
}
