package com.omnitech.liftonia;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.InputType;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {
    public static final int REQUEST_CODE = 101;
    public static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    public static final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    public static final String phNo = "phNo";
    private static final String NAME = "NAME";
    private static final String EMAIL = "EMAIL";
    private static final String SID = "SID";
    private static final int ERROR_DIALOGUE_REQUEST = 102;

    ImageView visibilityIcon;
    Toolbar toolbar;
    ConstraintLayout coordinatorLayout;
    private TextView forgot_pass, Register;
    private EditText Email;
    private EditText Password;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private ProgressDialog progressDialog;
    private boolean mLocationPermissionGranted = false;
    private boolean netState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_NoActionBarBG);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        coordinatorLayout = findViewById(R.id.constraint_layout);
        visibilityIcon = findViewById(R.id.imageView4);
        forgot_pass = findViewById(R.id.textView5);
        toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        SpannableString content = new SpannableString("forgot password?");
        Email = findViewById(R.id.editText4);
        Password = findViewById((R.id.editText6));
        firebaseAuth = FirebaseAuth.getInstance();
        Register = findViewById(R.id.textView8);

        //setting show/hide password
        visibilityIcon.setBackgroundResource(R.drawable.ic_visibility_black_24dp);
        visibilityIcon.setVisibility(View.GONE);

        //setting forgot password label
        content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
        forgot_pass.setText(content);
        forgot_pass.setVisibility(View.GONE);
        Register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, Register.class));
            }
        });

        //Show/Hide visibilityIcon on text change
        showHideVisibilityIcon();

        //get location permissions
        getLocationPermissions();

        //check if the user is authenticated or not
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {

            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser mfirebaseUser = firebaseAuth.getCurrentUser();
                if (mfirebaseUser != null) {
                    if (isServicesOK() && mLocationPermissionGranted) {
                        Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                        startActivity(intent);
                    } else if (!mLocationPermissionGranted) {
                        Toast.makeText(getApplicationContext(), "Please Enable Location Permission First", Toast.LENGTH_SHORT).show();
                        getLocationPermissions();
                    }
                }
            }
        };


    }



    private void showHideVisibilityIcon() {
        Password.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (Password.getText().length() > 0) {
                    visibilityIcon.setVisibility(View.VISIBLE);
                } else
                    visibilityIcon.setVisibility(View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void getLocationPermissions() {
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE);
        }
    }

    public void showHidePassword(View view) {
        if ("visible".equals(visibilityIcon.getTag().toString())) {
            visibilityIcon.setBackgroundResource(R.drawable.ic_visibility_off_black_24dp);
            visibilityIcon.setTag("Invisible");
            Password.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            Password.setSelection(Password.length());
        } else if ("Invisible".equals(visibilityIcon.getTag().toString())) {
            visibilityIcon.setBackgroundResource(R.drawable.ic_visibility_black_24dp);
            visibilityIcon.setTag("visible");
            Password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            Password.setSelection(Password.length());
        }
    }

    public void SignIn(final View view) {
        final String email = Email.getText().toString().trim();
        String pass = Password.getText().toString().trim();

        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        netState = networkInfo != null && networkInfo.isConnected();

        if (!netState) {
            EnableWifi(netState);
        }
        else if(!mLocationPermissionGranted){
            getLocationPermissions();
        }
        else if (TextUtils.isEmpty(email)) {
            Email.setError("Please Enter Email ID");
            Email.requestFocus();
        }
        else if (!ValidateEmail(Email.getText().toString().trim())) {
            Email.setError("Please enter valid email address");
            Email.requestFocus();
        }
        else if (TextUtils.isEmpty(pass)) {
            Password.setError("Please Enter Password");
            Password.requestFocus();
        }
        else if (TextUtils.isEmpty(pass) && TextUtils.isEmpty(email)) {
            Email.setError("Please enter valid email address");
            Email.requestFocus();
            Password.setError("Please Enter Password");
            Password.requestFocus();
        }
        else if (!TextUtils.isEmpty(email) && !TextUtils.isEmpty(pass) && netState && mLocationPermissionGranted && isServicesOK()) {
            if (ValidateEmail(Email.getText().toString().trim())) {
                progressDialog = new ProgressDialog(this);
                progressDialog.setMessage("Please wait...");
                progressDialog.setCanceledOnTouchOutside(false);
                progressDialog.show();
                firebaseAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(MainActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (!task.isSuccessful()) {
                            progressDialog.dismiss();
                            Toast.makeText(getApplicationContext(), task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            forgot_pass.setVisibility(View.VISIBLE);
                        } else {
                            FirebaseFirestore db;
                            db = FirebaseFirestore.getInstance();
                            DocumentReference docRef = db.collection("Users").document(firebaseAuth.getUid());
                            docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                    if (task.isSuccessful()) {
                                        DocumentSnapshot doc = task.getResult();
                                        if (doc.exists()) {
                                            SharedPreferences sharedPreferences = PreferenceManager
                                                    .getDefaultSharedPreferences(MainActivity.this);
                                            SharedPreferences.Editor editor = sharedPreferences.edit();
                                            editor.putString(NAME, doc.getString("First Name") +" "+doc.getString("Last Name"));
                                            editor.putString(phNo, doc.getString("Phone No"));
                                            editor.putString(EMAIL, doc.getString("Email"));
                                            editor.putString(SID, doc.getString("SID"));
                                            editor.apply();
                                            Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                                            progressDialog.dismiss();
                                            startActivity(intent);
                                        }
                                    }
                                }
                            });
                        }
                    }
                });
            }
        }
    }

    private void EnableWifi(boolean netState) {
        if (!netState) {
            Snackbar.make(coordinatorLayout, "Please connect to the internet", Snackbar.LENGTH_INDEFINITE)
                    .setAction("CLOSE", view -> {

                    })
                    .setActionTextColor(getResources().getColor(android.R.color.holo_red_light))
                    .show();
        }
        else if (!mLocationPermissionGranted) {
            getLocationPermissions();
        }
    }

    public boolean ValidateEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public boolean isServicesOK() {
        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MainActivity.this);
        if (available == ConnectionResult.SUCCESS) {
            //everything is fine and user can make map request
            return true;
        } else if (GoogleApiAvailability.getInstance().isUserResolvableError(available)) {
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(MainActivity.this, available, ERROR_DIALOGUE_REQUEST);
            dialog.show();
        } else {
            Toast.makeText(this, "you cant make map request", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(isServicesOK()) {
            firebaseAuth.addAuthStateListener(mAuthStateListener);
        }
    }



    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        FirebaseAuth.getInstance().signOut();
        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0) {
                for (int grantResult : grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                }
                mLocationPermissionGranted = true;
            }
        }
    }

    public void forgotPassword(View view) {
        if (!ValidateEmail(Email.getText().toString().trim())) {
            Email.setError("Please enter valid email address");
            Email.requestFocus();
            return;
        }
        FirebaseAuth.getInstance().sendPasswordResetEmail(Email.getText().toString().trim())
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            progressDialog.setMessage("Sending Email...");
                            progressDialog.setCanceledOnTouchOutside(false);
                            progressDialog.show();
                            progressDialog.setCancelable(false);
                            progressDialog.setCanceledOnTouchOutside(false);
                            Runnable progressRunnable = new Runnable() {

                                @Override
                                public void run() {
                                    progressDialog.dismiss();
                                }
                            };

                            Handler pdCanceller = new Handler();
                            pdCanceller.postDelayed(progressRunnable, 3000);
                            Snackbar.make(coordinatorLayout, "Password recovery email sent", Snackbar.LENGTH_INDEFINITE)
                                    .setAction("CLOSE", new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {

                                        }
                                    })
                                    .setActionTextColor(getResources().getColor(android.R.color.holo_red_light))
                                    .show();
                        } else if (!task.isSuccessful()) {
                            Toast.makeText(MainActivity.this, "Email not sent, make sure you are connected to internet", Toast.LENGTH_LONG).show();
                        }
                    }
                });

    }

    @Override
    public void onBackPressed() {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle("Exit Application");
        alertDialog.setMessage("Are you sure you want to exit Liftify?");
        alertDialog.setIcon(android.R.drawable.ic_dialog_alert);
        alertDialog.setCancelable(false);
        // Specifying a listener allows you to take an action before dismissing the dialog.
        // The dialog is automatically dismissed when a dialog button is clicked.
        alertDialog.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                MainActivity.this.finishAffinity();
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
