package com.omnitech.liftonia;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.PendingResult;
import com.google.maps.model.DirectionsLeg;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.DirectionsStep;
import com.google.maps.model.EncodedPolyline;
import com.google.maps.model.TravelMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressLint({"ClickableViewAccessibility", "MissingPermission"})
public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, NavigationView.OnNavigationItemSelectedListener {

    //saves the custoer ID if user is active as cust<-SearchRide
    public static final String CUSTUID = "CUSTUID";
    //saves boolean value according to customer booking status
    public static final String CUSTBOOKED = "CUSTBOOKED";
    //customer's destination latlng separated with , <-SearchRide
    public static final String CUST_DEST = "CUST_DEST";
    //customer's src latlng separated with , <-SearchRide
    public static final String CUST_SRC_LAT = "CUST_SRC_LAT";
    public static final String CUST_SRC_LNG = "CUST_SRC_LNG";
    //save the captain that the user has booked a ride with
    public static final String CAPID4CUST = "CAPID4CUST";
    //saves the captain ID if the user is active as cap<-ConfigureRide
    public static final String CAPUID = "CAPUID";
    public static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    public static final int REQUEST_CODE = 101;
    //save custRef name of cap's table
    private static final String CUST_FIELD_NAME_IN_BOOKING = "CUST_FIELD_NAME_IN_BOOKING";
    private static final String BROADCAST_ACTION = "android.location.PROVIDERS_CHANGED";
    //permission variables
    private static final int ENABLE_GPS_CODE = 158;
    private static final int ENABLE_WIFI_CODE = 159;
    static boolean locationStatus;
    static boolean permissionStatus;
    List<LatLng> path = new ArrayList();

    boolean mapsExecuted = false;
    boolean terrainView = false;
    boolean isAlreadyCap = false;
    boolean isAlreadyCust = false;

    String CapBookingID;
    String CustBookingID;

    //UI variables
    DrawerLayout drawer;
    Button terrainBtn, CapBtn, CustBtn, cancelBtn, getMyPositionBtn, refreshMarkers;
    NavigationView nav;
    ProgressBar progressBar;

    //Location variables
    InternetConnector_Receiver receiver;
    LocationManager locationManager;
    LocationRequest mLocationRequest;
    LocationCallback mLocationCallback;
    FusedLocationProviderClient fusedLocationProviderClient;
    Location currloc;
    GoogleMap mGoogleMap;
    SupportMapFragment mapFragment;


    int seats;
    Marker CaptainMarker, destMarker, CustMarker1;
    boolean mapsLoaded = false;
    boolean isCustBooked = false;
    String[] source;
    String[] destination;
    LatLng Source;
    LatLng Destination;
    Integer markerPointer;
    LatLng latLng = null;
    ArrayList<Marker> CapMarkers = new ArrayList<>();
    Marker[] markers = new Marker[4];
    Polyline polyline;

    //Google LocationAPI
    private GeoApiContext geoApiContext = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

        CapBtn = findViewById(R.id.capbtn);
        CustBtn = findViewById(R.id.custbtn);
        refreshMarkers = findViewById(R.id.refreshMarkers);
        cancelBtn = findViewById(R.id.cancelBtn);
        drawer = findViewById(R.id.drawer_layout);
        progressBar = findViewById(R.id.progressBar);
        nav = findViewById(R.id.nav_view);
        Toolbar toolbar = findViewById(R.id.maps_toolbar);

        //create broadcast for location change status listener
        receiver = InternetConnector_Receiver.getInstance();
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (geoApiContext == null) {
            geoApiContext = new GeoApiContext.Builder()
                    .apiKey(getString(R.string.map_key)).build();
        }

        //Set actionbar at the top of application
        setSupportActionBar(toolbar);

        //toggle object on action bar
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(MapsActivity.this, drawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        //causes toggle to rotate
        drawer.addDrawerListener(toggle);
        //change color of toggle bars
        toggle.getDrawerArrowDrawable().setColor(getColor(R.color.white));
        //creates toggle button on action bar
        toggle.syncState();
        nav.setNavigationItemSelectedListener(this);
        progressBar.getIndeterminateDrawable().setColorFilter(0xFFFF0000, android.graphics.PorterDuff.Mode.MULTIPLY);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        //Check if user is a customer or a captain already
        final SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(MapsActivity.this);
        CapBookingID = sharedPreferences.getString(CAPUID, "");
        CustBookingID = sharedPreferences.getString(CUSTUID, "");
        isCustBooked = sharedPreferences.getBoolean(CUSTBOOKED, false);

        if (!CapBookingID.isEmpty()) {
            isAlreadyCap = true;
        } else if (!CustBookingID.isEmpty()) {
            isAlreadyCust = true;
        }

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Disable UI
                cancelRide();
            }
        });

        //Check current status of gps
        locationStatus = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!permissionStatus) {
            getLocationPermissions();
        }
        if (permissionStatus) {
            if (!locationStatus) {
                EnableGPS();
            } else {
                fetchLastLocation();
            }
        }
    }

    private void cancelRide() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo cellularInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if ((networkInfo != null && networkInfo.isConnected() || cellularInfo != null && cellularInfo.isConnected())) {
            if (isAlreadyCap) {
                sharedPreferences.edit().remove(CAPUID).apply();
                //also delete any customer that is already registered with this captain
                FirebaseFirestore.getInstance().collection("Bookings").document(CapBookingID).addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            Map<String, Object> allCol = documentSnapshot.getData();
                            for (Map.Entry<String, Object> entry : allCol.entrySet()) {
                                if (entry.getKey().contains("CustomerRef")) {
                                    if (!documentSnapshot.getString(entry.getKey()).equals("empty")) {
                                        String custID = documentSnapshot.getString(entry.getKey());
                                        FirebaseFirestore.getInstance().collection("Customers").document(custID).delete();
                                    }
                                }
                            }
                        }
                    }
                });
                FirebaseFirestore.getInstance().collection("Bookings").document(CapBookingID).delete();
                fusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
                isAlreadyCap = false;
                startActivity(new Intent(MapsActivity.this, MapsActivity.class));
            } else if (isAlreadyCust) {
                if (isCustBooked) {
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    db.collection(("Bookings")).document(sharedPreferences.getString(CAPID4CUST, ""))
                            .update(sharedPreferences.getString(CUST_FIELD_NAME_IN_BOOKING, ""), "empty",
                                    "Seats", seats + 1);
                    db.collection("Customers").document(CustBookingID).delete();
                    sharedPreferences.edit().remove(CUSTBOOKED).apply();
                    sharedPreferences.edit().remove(CUSTUID).apply();
                    sharedPreferences.edit().remove(CUST_DEST).apply();
                    sharedPreferences.edit().remove(CUST_FIELD_NAME_IN_BOOKING).apply();
                    sharedPreferences.edit().remove(CUST_SRC_LAT).apply();
                    sharedPreferences.edit().remove(CUST_SRC_LNG).apply();
                    startActivity(new Intent(MapsActivity.this, MapsActivity.class));
                } else {
                    FirebaseFirestore.getInstance().collection("Customers").document(CustBookingID).delete();
                    fusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
                    sharedPreferences.edit().remove(CUSTUID).apply();
                    startActivity(new Intent(MapsActivity.this, MapsActivity.class));
                }
            }
        } else {
            EnableWifi();
        }
    }

    private void getLocationPermissions() {
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
        if (ActivityCompat.checkSelfPermission(this.getApplicationContext(),
                FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            permissionStatus = true;
        } else {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE);
        }
    }

    private void fetchLastLocation() {
        progressBar.setVisibility(View.VISIBLE);
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(final LocationResult locationResult) {
                fusedLocationProviderClient.getLastLocation()
                        .addOnSuccessListener(new OnSuccessListener<Location>() {
                            @Override
                            public void onSuccess(Location location) {
                                progressBar.setVisibility(View.GONE);
                                currloc = location;
                                if (!mapsLoaded) {
                                    mapFragment.getMapAsync(MapsActivity.this);
                                    mapsLoaded = true;
                                }
                                ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
                                NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                                NetworkInfo cellularInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
                                if ((networkInfo != null && networkInfo.isConnected() || cellularInfo != null && cellularInfo.isConnected())) {
                                    if (CaptainMarker != null && mGoogleMap != null && isAlreadyCap) {
                                        //update cap's own marker
                                        LatLng CaptainUpdatedLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                                        CaptainMarker.setPosition(CaptainUpdatedLatLng);
                                        //create array to refer document IDs in the Customers table
                                        ArrayList<String> Customers = new ArrayList<>();
                                        //create marker array to
                                        // 1.instantiate each time location updates from DB
                                        // 2.delete before instatiating to refresh to new coordinates

                                        DocumentReference ref = FirebaseFirestore.getInstance().
                                                collection("Bookings").document(CapBookingID);
                                        ref.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                if (task.isSuccessful()) {
                                                    DocumentSnapshot doc = task.getResult();
                                                    if (doc != null && doc.exists()) {
                                                        if (!doc.getString("CustomerRef1").equals("empty")) {
                                                            Customers.add(doc.getString("CustomerRef1"));
                                                        }
                                                        if (doc.contains("CustomerRef2")) {
                                                            if (!doc.getString("CustomerRef2").equals("empty"))
                                                                Customers.add(doc.getString("CustomerRef2"));
                                                        }
                                                        if (doc.contains("CustomerRef3")) {
                                                            if (!doc.getString("CustomerRef3").equals("empty"))
                                                                Customers.add(doc.getString("CustomerRef3"));
                                                        }
                                                        if (doc.contains("CustomerRef4")) {
                                                            if (!doc.getString("CustomerRef4").equals("empty"))
                                                                Customers.add(doc.getString("CustomerRef4"));
                                                        }
                                                        for (Marker marker : markers) {
                                                            if (marker != null)
                                                                marker.remove();
                                                        }
                                                        markerPointer = 0;
                                                        for (String customer : Customers) {
                                                            DocumentReference ref1 = FirebaseFirestore.getInstance().
                                                                    collection("Customers").document(customer);
                                                            ref1.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                                    DocumentSnapshot doc = task.getResult();

                                                                    String[] custLatLng = doc.getString("CustLatLng").split(",");
                                                                    LatLng CustLatLng = new LatLng(Double.parseDouble(custLatLng[0]), Double.parseDouble(custLatLng[1]));

                                                                    MarkerOptions markerOptions = new MarkerOptions().position(CustLatLng)
                                                                            .title(doc.getString("Name")).snippet(doc.getString("CustPhNo"))
                                                                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_accessibility_green_500_36dp)).draggable(false);

                                                                    markers[markerPointer++] = mGoogleMap.addMarker(markerOptions);
                                                                }
                                                            });
                                                        }
                                                    }

                                                    FirebaseFirestore.getInstance().collection("Bookings").document(CapBookingID)
                                                            .update("CapLatLng", location.getLatitude() + "," + location.getLongitude());
                                                }
                                            }
                                        });

                                    } else if (mGoogleMap != null && isAlreadyCust && CustMarker1 != null) {
                                        //customer is already booked in Bookings table
                                        if (isCustBooked) {
                                            refreshMarkers.setVisibility(View.GONE);
                                            //keep updating the position of this customers marker
                                            LatLng CustUpdatedLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                                            CustMarker1.setPosition(CustUpdatedLatLng);

                                            FirebaseFirestore.getInstance().collection("Customers").document(CustBookingID)
                                                    .update("CustLatLng", location.getLatitude() + "," + location.getLongitude());

                                            SharedPreferences sharedPreferences = PreferenceManager
                                                    .getDefaultSharedPreferences(MapsActivity.this);
                                            String DocID = sharedPreferences.getString(CAPID4CUST, "");

                                            DocumentReference Cap4thisCust = FirebaseFirestore.getInstance().collection("Bookings").document(DocID);
                                            Cap4thisCust.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                @Override
                                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                    if (task.isSuccessful()) {
                                                        DocumentSnapshot documentSnapshot = task.getResult();
                                                        if (documentSnapshot != null && documentSnapshot.exists()) {
                                                            String[] capLatLng = documentSnapshot.getString("CapLatLng").split(",");
                                                            double captLat = Double.parseDouble(capLatLng[0]);
                                                            double capLng = Double.parseDouble(capLatLng[1]);
                                                            LatLng CapLatLng = new LatLng(captLat, capLng);
                                                            seats = Integer.parseInt(String.valueOf(documentSnapshot.get("Seats")));
                                                            String StrTime = documentSnapshot.getString("StartTime");
                                                            String CapPhNo = documentSnapshot.getString("CapPhNo");
                                                            String CapName = documentSnapshot.getString("CapName");
                                                            String CapSID = documentSnapshot.getString("CapSID");

                                                            MarkerOptions markerOptions = new MarkerOptions().position(CapLatLng)
                                                                    .title(CapName).snippet(String.format("Phone: %s\nSID: %s\n Start Time: %s\nSeats Available: %s \n%s", CapPhNo, CapSID, StrTime, seats, DocID))
                                                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.stearing)).draggable(false);
                                                            if (CaptainMarker == null)
                                                                CaptainMarker = mGoogleMap.addMarker(markerOptions);
                                                            else {
                                                                CaptainMarker.setPosition(CapLatLng);
                                                            }
                                                        } else {
                                                            isCustBooked = false;
                                                            startActivity(new Intent(MapsActivity.this, MapsActivity.class));
                                                        }
                                                    }
                                                }
                                            });
                                        }
                                    }
                                } else {
                                    EnableWifi();
                                }
                            }
                        });
            }
        };
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        mLocationRequest.setInterval(100);
        mLocationRequest.setFastestInterval(100);
        mLocationRequest.setSmallestDisplacement(500);
        fusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
    }

    private void EnableGPS() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        progressBar.setVisibility(View.VISIBLE);
        Snackbar.make(drawer, "Please turn on Locations", Snackbar.LENGTH_INDEFINITE)
                .setAction("ENABLE", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), ENABLE_GPS_CODE);
                    }
                })
                .setActionTextColor(getResources().getColor(android.R.color.holo_red_light))
                .show();
    }

    private void EnableWifi() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        boolean netState = networkInfo != null && networkInfo.isConnected();
        if (!netState) {
            progressBar.setVisibility(View.VISIBLE);
            Snackbar.make(drawer, "Please connect to the internet", Snackbar.LENGTH_INDEFINITE)
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
    @Override
    public void onMapReady(final GoogleMap googleMap) {
        //set UI params
        mGoogleMap = googleMap;
        Animate(mGoogleMap);
        mGoogleMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(MapsActivity.this));
        mGoogleMap.getUiSettings().setMyLocationButtonEnabled(false);
        mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        //setting ride to KIET
        latLng = new LatLng(currloc.getLatitude(), currloc.getLongitude());
        if (!isAlreadyCap && !isAlreadyCust) {
            mGoogleMap.setMyLocationEnabled(true);
            CapBtn.setVisibility(View.VISIBLE);
            CustBtn.setVisibility(View.VISIBLE);
            //set listeners for buttons
            CapBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(MapsActivity.this, ConfigureRide.class));
                }
            });
            CustBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(MapsActivity.this, SearchRide.class));
                }
            });
        } else if (isAlreadyCap) {
            cancelBtn.setVisibility(View.VISIBLE);
            CapBtn.setVisibility(View.GONE);
            CustBtn.setVisibility(View.GONE);
            ArrayList<String> Customers = new ArrayList<>();

            DocumentReference ref = FirebaseFirestore.getInstance().
                    collection("Bookings").document(CapBookingID);
            ref.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot doc = task.getResult();
                        if (!doc.getString("CustomerRef1").equals("empty")) {
                            Customers.add(doc.getString("CustomerRef1"));
                        }
                        if (doc.contains("CustomerRef2")) {
                            if (!doc.getString("CustomerRef2").equals("empty"))
                                Customers.add(doc.getString("CustomerRef2"));
                        }
                        if (doc.contains("CustomerRef3")) {
                            if (!doc.getString("CustomerRef3").equals("empty"))
                                Customers.add(doc.getString("CustomerRef3"));
                        }
                        if (doc.contains("CustomerRef4")) {
                            if (!doc.getString("CustomerRef4").equals("empty"))
                                Customers.add(doc.getString("CustomerRef4"));
                        }
                        try {
                            source = (doc != null ? (doc.getString("Source")).split(",") : null);
                            Source = source != null ? (new LatLng(Double.parseDouble(source[0]), Double.parseDouble(source[1]))) : null;
                            destination = (doc != null ? (doc.getString("Destination")).split(",") : null);
                            Destination = destination != null ? (new LatLng(Double.parseDouble(destination[0]), Double.parseDouble(destination[1]))) : null;

                        } catch (Exception e) {
                            Toast.makeText(MapsActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        if (Source != null) {
                            MarkerOptions markerOptions1 = new MarkerOptions().position(Destination)
                                    .title("Destination").snippet("")
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_location_on_red_400_48dp)).draggable(false);

                            MarkerOptions markerOptions2 = new MarkerOptions().position(latLng)
                                    .title("My location").snippet("")
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.helmet)).draggable(false);
                            destMarker = mGoogleMap.addMarker(markerOptions1);
                            CaptainMarker = mGoogleMap.addMarker(markerOptions2);
                            calculateDirections(destMarker, CaptainMarker);
                        }
                        for (Marker marker : markers) {
                            if (marker != null)
                                marker.remove();
                        }
                        markerPointer = 0;
                        for (String customer : Customers) {
                            DocumentReference ref1 = FirebaseFirestore.getInstance().
                                    collection("Customers").document(customer);
                            ref1.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                    DocumentSnapshot doc = task.getResult();
                                    String[] custLatLng = doc.getString("CustLatLng").split(",");
                                    LatLng CustLatLng = new LatLng(Double.parseDouble(custLatLng[0]), Double.parseDouble(custLatLng[1]));

                                    MarkerOptions markerOptions = new MarkerOptions().position(CustLatLng)
                                            .title(doc.getString("Name")).snippet(doc.getString("CustPhNo"))
                                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_accessibility_green_500_36dp)).draggable(false);

                                    markers[markerPointer++] = mGoogleMap.addMarker(markerOptions);
                                }
                            });
                        }
                    }
                }
            });

        } else if (isAlreadyCust) {
            cancelBtn.setVisibility(View.VISIBLE);
            CapBtn.setVisibility(View.GONE);
            CustBtn.setVisibility(View.GONE);

            DocumentReference ref = FirebaseFirestore.getInstance().
                    collection("Customers").document(CustBookingID);
            ref.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot doc = task.getResult();
                        try {
                            source = (doc != null ? (doc.getString("CustLatLng")).split(",") : null);
                            Source = source != null ? (new LatLng(Double.parseDouble(source[0]), Double.parseDouble(source[1]))) : null;
                            destination = (doc != null ? (doc.getString("CustDestination")).split(",") : null);
                            Destination = destination != null ? (new LatLng(Double.parseDouble(destination[0]), Double.parseDouble(destination[1]))) : null;

                        } catch (Exception e) {
                            Toast.makeText(MapsActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        if (Source != null) {
                            MarkerOptions markerOptions = new MarkerOptions().position(Source)
                                    .title("Your location").snippet("")
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_accessibility_green_500_36dp)).draggable(false);

                            MarkerOptions markerOptions1 = new MarkerOptions().position(Destination)
                                    .title("Destination " + destination[0] + "," + destination[1]).snippet("")
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_location_on_red_400_48dp)).draggable(false);
                            CustMarker1 = mGoogleMap.addMarker(markerOptions);
                            destMarker = mGoogleMap.addMarker(markerOptions1);
                        }
                    }
                }
            });

            //some markers found to facilitate customers
            if (!isCustBooked) {
                SearchForCaptains();
                if (CapMarkers != null) {
                    refreshMarkers.setVisibility(View.VISIBLE);
                    refreshMarkers.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            refrehMarkers(v);
                        }
                    });

                    mGoogleMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
                        @Override
                        public void onInfoWindowClick(Marker marker) {
                            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
                            NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                            NetworkInfo cellularInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
                            if (networkInfo != null && networkInfo.isConnected() || cellularInfo != null && cellularInfo.isConnected()) {
                                if (!marker.getTitle().equals("Your location")) {
                                    //acquire cap info from marker snippet
                                    String[] title = marker.getSnippet().split("\n");
                                    //search in booking table for the captain with the same ID as acquired from title info
                                    DocumentReference capRef = FirebaseFirestore.getInstance().collection("Bookings").document(title[5]);
                                    capRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                            //close info window on click
                                            marker.hideInfoWindow();
                                            //set isBooked to true
                                            isCustBooked = true;
                                            //set isBooked to true in sharedprefs
                                            SharedPreferences sharedPreferences = PreferenceManager
                                                    .getDefaultSharedPreferences(MapsActivity.this);
                                            SharedPreferences.Editor editor = sharedPreferences.edit();
                                            editor.putBoolean(CUSTBOOKED, true);
                                            editor.putString(CAPID4CUST, title[5]);
                                            editor.apply();

                                            //create list to store fields of customerRefs in Bookings table
                                            DocumentSnapshot documentSnapshot = task.getResult();
                                            ArrayList<String> fieldNames = new ArrayList<>();
                                            Map<String, Object> allCol = documentSnapshot.getData();
                                            for (Map.Entry<String, Object> entry : allCol.entrySet()) {
                                                if (entry.getKey().contains("CustomerRef")) {
                                                    if (documentSnapshot.getString(entry.getKey()).equals("empty")) {
                                                        capRef.update(entry.getKey(), CustBookingID);
                                                        sharedPreferences.edit().putString(CUST_FIELD_NAME_IN_BOOKING, entry.getKey()).apply();
                                                        Toast.makeText(MapsActivity.this, "Booked Successfully", Toast.LENGTH_SHORT).show();
                                                        capRef.update("Seats", seats - 1);
                                                        startActivity(new Intent(MapsActivity.this, MapsActivity.class));
                                                        break;
                                                    }
                                                }
                                            }
                                        }
                                    });
                                }
                            } else {
                                EnableWifi();
                            }
                        }
                    });
                }
                //No markers found to facilitate customer
                if (CapMarkers == null) {
                    Snackbar.make(drawer, "Sorry, Couldn't find anyone at the moment.", Snackbar.LENGTH_INDEFINITE)
                            .setAction("Try again", new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    startActivity(new Intent(MapsActivity.this, SearchRide.class));
                                }
                            })
                            .setActionTextColor(getResources().getColor(android.R.color.holo_red_light))
                            .show();
                }


                //hide other markers when one marker is clicked
                mGoogleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(Marker marker) {
                        marker.showInfoWindow();
                        for (Marker cMarker : CapMarkers) {
                            if (!cMarker.getId().equals(marker.getId()))
                                cMarker.setVisible(false);
                        }
                        calculateDirections(destMarker, marker);
                        return false;
                    }
                });
                //display markers again when clicked on map fragment
                mGoogleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(LatLng latLng) {
                        if (!isCustBooked) {
                            for (Marker cMarker : CapMarkers) {
                                cMarker.setVisible(true);
                            }
                        }
                    }
                });
            }
            if (isCustBooked) {
                refreshMarkers.setVisibility(View.GONE);

                SharedPreferences sharedPreferences = PreferenceManager
                        .getDefaultSharedPreferences(MapsActivity.this);
                String DocID = sharedPreferences.getString(CAPID4CUST, "");

                DocumentReference Cap4thisCust = FirebaseFirestore.getInstance().collection("Bookings").document(DocID);
                Cap4thisCust.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot documentSnapshot = task.getResult();
                            if (documentSnapshot != null && documentSnapshot.exists()) {
                                String[] capLatLng = documentSnapshot.getString("CapLatLng").split(",");
                                double captLat = Double.parseDouble(capLatLng[0]);
                                double capLng = Double.parseDouble(capLatLng[1]);
                                LatLng CapLatLng = new LatLng(captLat, capLng);
                                seats = Integer.parseInt(String.valueOf(documentSnapshot.get("Seats")));
                                String StrTime = documentSnapshot.getString("StartTime");
                                String CapPhNo = documentSnapshot.getString("CapPhNo");
                                String CapName = documentSnapshot.getString("CapName");
                                String CapSID = documentSnapshot.getString("CapSID");

                                MarkerOptions markerOptions = new MarkerOptions().position(CapLatLng)
                                        .title(CapName).snippet(String.format("Phone: %s\nSID: %s\n Start Time: %s\nSeats Available: %s \n%s", CapPhNo, CapSID, StrTime, seats, DocID))
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.stearing)).draggable(false);
                                CaptainMarker = mGoogleMap.addMarker(markerOptions);
                                if (CaptainMarker == null)
                                    CaptainMarker = mGoogleMap.addMarker(markerOptions);
                                else {
                                    CaptainMarker.setPosition(CapLatLng);
                                }
                            } else {
                                cancelRide();
                            }
                        }
                    }
                });
            }
        }
        terrainBtn = findViewById(R.id.button3);
        terrainBtn.setVisibility(View.VISIBLE);
        terrainBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!terrainView) {
                    mGoogleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                    terrainBtn.setForeground(getResources().getDrawable(R.drawable.mapstrview));
                    terrainView = true;
                } else if (terrainView) {
                    mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                    terrainBtn.setForeground(getResources().getDrawable(R.drawable.mapsatview));
                    terrainView = false;
                }
            }
        });
    }

    private void Animate(final GoogleMap googleMap) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                googleMap.setMaxZoomPreference(17);
                googleMap.setMinZoomPreference(12);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                progressBar.setVisibility(View.GONE);
            }
        };
        final Handler handler = new Handler();
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currloc.getLatitude(), currloc.getLongitude()), 17), new GoogleMap.CancelableCallback() {
            @Override
            public void onFinish() {
                handler.postDelayed(runnable, 1000);
            }

            @Override
            public void onCancel() {

            }
        });

    }

    private void SearchForCaptains() {
        for (Marker marker : CapMarkers) {
            marker.remove();
        }
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(MapsActivity.this);
        //get the whole Bookings table
        final CollectionReference bookings = FirebaseFirestore.getInstance().
                collection("Bookings");

        //1. Check if CustDestination == CapDestination
        Query allDest = bookings.whereEqualTo("Destination", sharedPreferences.getString(CUST_DEST, ""));

        //Acquire Current LatLng of thisCustomer
        final Double CustLat = Double.parseDouble(sharedPreferences.getString(CUST_SRC_LAT, ""));
        final Double CustLng = Double.parseDouble(sharedPreferences.getString(CUST_SRC_LNG, ""));

        allDest.get().addOnCompleteListener(task1 -> {
            if (task1.isSuccessful()) {
                for (QueryDocumentSnapshot doc1 : task1.getResult()) {
                    //Acquire info of each cap with same Destination
                    String[] capLatLng = doc1.getString("CapLatLng").split(",");
                    double captLat = Double.parseDouble(capLatLng[0]);
                    double capLng = Double.parseDouble(capLatLng[1]);
                    seats = Integer.parseInt(String.valueOf(doc1.get("Seats")));
                    String StrTime = doc1.getString("StartTime");
                    String ID = doc1.getId();
                    String CapPhNo = doc1.getString("CapPhNo");
                    String CapName = doc1.getString("CapName");
                    String CapSID = doc1.getString("CapSID");

                    if (captLat > CustLat) {
                        if (capLng > CustLng) {
                            if (captLat - CustLat < 0.060000 || capLng - CustLng < 0.060000) {
                                if (seats > 0) {
                                    AddMarkers(CapName, CapSID, CapPhNo, seats, StrTime, ID, new LatLng(captLat, capLng));
                                }
                            }
                        } else if (capLng < CustLng) {
                            if (captLat - CustLat < 0.060000 || CustLng - capLng < 0.060000) {
                                if (seats > 0) {
                                    AddMarkers(CapName, CapSID, CapPhNo, seats, StrTime, ID, new LatLng(captLat, capLng));
                                }
                            }
                        }
                    } else if (captLat < CustLat) {
                        if (capLng > CustLng) {
                            if (CustLat - captLat < 0.060000 || capLng - CustLng < 0.060000) {
                                if (seats > 0) {
                                    AddMarkers(CapName, CapSID, CapPhNo, seats, StrTime, ID, new LatLng(captLat, capLng));
                                }
                            }

                        } else if (capLng < CustLng) {
                            if (captLat - CustLat < 0.060000 || CustLng - capLng < 0.060000) {
                                if (seats > 0) {
                                    AddMarkers(CapName, CapSID, CapPhNo, seats, StrTime, ID, new LatLng(captLat, capLng));
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    private void AddMarkers(String Name, String SID, String PhNo, int seats, String StartTime, String ID, LatLng latLng) {
        MarkerOptions markerOptions = new MarkerOptions().position(latLng)
                .title("Tap to book").snippet(String.format("Name: %s\nSID: %s\nPhone: %s\nLeaves at: %s\nSeats Available: %s\n%s", Name, SID, PhNo, StartTime, seats, ID))
                .alpha(0.9f)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.helmet)).draggable(false);
        CapMarkers.add(mGoogleMap.addMarker(markerOptions));
    }

    private void calculateDirections(Marker DesMakrer, Marker SrcMarker) {
        Log.d("calcDir", "calculateDirections: calculating directions.");

        DirectionsApiRequest directions = new DirectionsApiRequest(geoApiContext);
        directions.mode(TravelMode.DRIVING);
        directions.destination(new com.google.maps.model.LatLng(
                        DesMakrer.getPosition().latitude,
                        DesMakrer.getPosition().longitude
                )
        );
        if (isAlreadyCap) {
            directions.origin(
                    new com.google.maps.model.LatLng(
                            CaptainMarker.getPosition().latitude,
                            CaptainMarker.getPosition().longitude
                    )
            );
        } else if (isAlreadyCust) {
            directions.origin(
                    new com.google.maps.model.LatLng(
                            SrcMarker.getPosition().latitude,
                            SrcMarker.getPosition().longitude
                    )
            );
        }
        directions.alternatives(false);
        Log.d("calcDir", "calculateDirections: destination: " + destination.toString());
        directions.setCallback(new PendingResult.Callback<DirectionsResult>() {
            @Override
            public void onResult(DirectionsResult result) {
                if (result.routes != null && result.routes.length > 0 && result.routes[0].legs != null) {
                    DirectionsRoute route = result.routes[0];
                    for (int i = 0; i < route.legs.length; i++) {
                        DirectionsLeg leg = route.legs[i];
                        if (leg.steps != null) {
                            for (int j = 0; j < leg.steps.length; j++) {
                                DirectionsStep step = leg.steps[j];
                                if (step.steps != null && step.steps.length > 0) {
                                    EncodedPolyline points = step.polyline;
                                    if (points != null) {
                                        //Decode polyline and add points to list of route coordinates
                                        List<com.google.maps.model.LatLng> coords = points.decodePath();
                                        for (com.google.maps.model.LatLng coord : coords) {
                                            path.add(new LatLng(coord.lat, coord.lng));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (path.size() > 0) {
                    PolylineOptions opts = new PolylineOptions().addAll(path).color(Color.BLUE).width(5);
                    mGoogleMap.addPolyline(opts);
                }
                Log.d("calcDir", "onResult: duration: " + result.routes[0].legs[0].duration);
                Log.d("calcDir", "onResult: distance: " + result.routes[0].legs[0].distance);
                Log.d("calcDir", "onResult: geocodedWayPoints: " + result.geocodedWaypoints[0].toString());
            }

            @Override
            public void onFailure(Throwable e) {
                Log.e("calcDir", "onFailure: " + e.getMessage());

            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void EnableGPSBtn() {
        getMyPositionBtn = findViewById(R.id.getCurrent);
        getMyPositionBtn.setVisibility(View.VISIBLE);
        getMyPositionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Animate(mGoogleMap);
            }
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.nav_history:
                drawer.closeDrawer(GravityCompat.START);
                History();
                break;
            case R.id.nav_settings:
                if (locationStatus) {
                    drawer.closeDrawer(GravityCompat.START);
                    Toast.makeText(this, "Already Enabled", Toast.LENGTH_SHORT).show();
                } else {
                    drawer.closeDrawer(GravityCompat.START);
                    startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), ENABLE_GPS_CODE);
                }
                break;
            case R.id.nav_logout:
                drawer.closeDrawer(GravityCompat.START);
                Logout();
                break;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0) {
                for (int i = 0; i < grantResults.length; i++) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        getLocationPermissions();
                        break;
                    }
                }
                permissionStatus = true;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ENABLE_GPS_CODE) {
            locationStatus = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            if (locationStatus) {
                fetchLastLocation();
            } else
                EnableGPS();
        } else if (requestCode == ENABLE_WIFI_CODE) {
            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (networkInfo.isConnected() && networkInfo != null) {
                startActivity(new Intent(MapsActivity.this, MapsActivity.class));
            }
        }
    }

    public void History() {
        MyAppState.activityPaused();// On Pause notify the Application
        startActivity(new Intent(MapsActivity.this, History.class));
    }

    public void Logout() {
        MyAppState.activityPaused();// On Pause notify the Application
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle("Log out");
        alertDialog.setMessage("Are you sure? All booked rides will be deleted");
        alertDialog.setIcon(android.R.drawable.ic_dialog_alert);
        alertDialog.setCancelable(false);
        // Specifying a listener allows you to take an action before dismissing the dialog.
        // The dialog is automatically dismissed when a dialog button is clicked.
        alertDialog.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                FirebaseAuth.getInstance().signOut();
                SharedPreferences sharedPreferences = PreferenceManager
                        .getDefaultSharedPreferences(MapsActivity.this);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.clear();
                editor.apply();
                if (isAlreadyCap)
                    FirebaseFirestore.getInstance().collection("Bookings").document(CapBookingID).delete();
                else if (isAlreadyCust)
                    FirebaseFirestore.getInstance().collection("Customers").document(CustBookingID).delete();
                startActivity(new Intent(MapsActivity.this, MainActivity.class));
            }
        });
        // A null listener allows the button to dismiss the dialog and take no further action.
        alertDialog.setNegativeButton(android.R.string.no, null);
        Button cancel = alertDialog.show().getButton(DialogInterface.BUTTON_NEGATIVE);
        cancel.setFocusable(true);
        cancel.setTextColor(getResources().getColor(R.color.conv_tomaterial_theme_taskbar));
        cancel.setFocusableInTouchMode(true);
        cancel.requestFocus();
    }

    private void refrehMarkers(View view) {
        if (!isCustBooked) {
            SearchForCaptains();
        }
    }

    @Override
    public void onBackPressed() {
        boolean isDrawerClosed = true;
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (isDrawerClosed) {
            final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
            alertDialog.setTitle("Exit Application");
            alertDialog.setMessage("Are you sure you want to exit Liftonia?");
            alertDialog.setIcon(android.R.drawable.ic_dialog_alert);
            alertDialog.setCancelable(false);
            // Specifying a listener allows you to take an action before dismissing the dialog.
            // The dialog is automatically dismissed when a dialog button is clicked.
            alertDialog.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    MapsActivity.this.finishAffinity();
                }
            });
            // A null listener allows the button to dismiss the dialog and take no further action.
            alertDialog.setNegativeButton(android.R.string.no, null);
            Button cancel = alertDialog.show().getButton(DialogInterface.BUTTON_NEGATIVE);
            cancel.setFocusable(true);
            cancel.setTextColor(getResources().getColor(R.color.conv_tomaterial_theme_taskbar));
            cancel.setFocusableInTouchMode(true);
            cancel.requestFocus();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        MyAppState.activityResumed();
        registerReceiver(receiver, new IntentFilter(BROADCAST_ACTION));
        locationStatus = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!permissionStatus) {
            getLocationPermissions();
        } else {
            if (!locationStatus) {
                EnableGPS();
            } else {
                fetchLastLocation();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        fusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
        MyAppState.activityPaused();// On Pause notify the Application
        if (receiver != null)
            unregisterReceiver(receiver);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mLocationCallback != null && fusedLocationProviderClient != null) {
            fusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
        }
        MyAppState.activityPaused();// On Pause notify the Application
    }
}
