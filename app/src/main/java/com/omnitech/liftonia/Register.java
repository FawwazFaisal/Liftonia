package com.omnitech.liftonia;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;

public class Register extends AppCompatActivity {

    FirebaseFirestore db;
    Toolbar toolbar;
    private EditText Email, Password, confirmPassword, Fname, Lname, Phno, SID;
    private ProgressDialog progressDialog;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        firebaseAuth = FirebaseAuth.getInstance();
        Fname = findViewById(R.id.editText);
        Lname = findViewById(R.id.editText2);
        Phno = findViewById(R.id.editText8);
        Email = findViewById(R.id.editText3);
        Password = findViewById((R.id.editText5));
        confirmPassword = findViewById((R.id.editText7));
        SID = findViewById(R.id.editText10);
        toolbar = findViewById(R.id.register_toolbar);
        setSupportActionBar(toolbar);
    }

    public void Register(View view) {
        final String email = Email.getText().toString().trim();
        String pass = Password.getText().toString().trim();
        String conPass = confirmPassword.getText().toString().trim();
        final String fname = Fname.getText().toString().trim();
        final String lname = Lname.getText().toString().trim();
        final String phno = Phno.getText().toString().trim();
        final String sid = SID.getText().toString().trim();
        db = FirebaseFirestore.getInstance();
        if (TextUtils.isEmpty(fname)) {
            Fname.setError("Please Enter First name");
            Fname.requestFocus();
        }
        if (TextUtils.isEmpty(lname)) {
            Lname.setError("Please Enter last name");
            Lname.requestFocus();
        }
        if (TextUtils.isEmpty(email)) {
            Email.setError("Please Enter Email ID");
            Email.requestFocus();
        }
        if (TextUtils.isEmpty(pass)) {
            Password.setError("Please Enter Password");
        }
        if (TextUtils.isEmpty(conPass)) {
            confirmPassword.setError("Please Confirm Password");
        }
        if (pass.length() < 8) {
            Password.setError("password should be 8 digits");
        }
        if (TextUtils.isEmpty(phno)) {
            Phno.setError("Please Enter Phone number");
            Phno.requestFocus();
        }
        if (phno.length() != 11 || !phno.startsWith("03")) {
            Phno.setError("Please Correct Phone number");
            Phno.requestFocus();
        }
        if (TextUtils.isEmpty(sid)) {
            SID.setError("Please Enter Student ID");
            SID.requestFocus();
        }
        if (pass.matches("[^0-9]+") || pass.matches("[^a-z]+")) {
            Password.setError("must contain alphanumeric");
            return;
        }
        if (!TextUtils.equals(conPass, pass)) {
            confirmPassword.setError("Incorrect password entered");
            confirmPassword.requestFocus();
        } else if(!email.isEmpty() & !pass.isEmpty()) {
            CollectionReference users = db.collection("Users");
            Query allPhoneNo = users.whereEqualTo("Phone No", Phno.getText().toString().trim());
            progressDialog = new ProgressDialog(Register.this);
            progressDialog.setMessage("Registering");
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();
            allPhoneNo.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        if (task.getResult().size()>0) {
                            for (QueryDocumentSnapshot doc : task.getResult()) {
                                Toast.makeText(Register.this, doc.get("Phone No") + " already exists", Toast.LENGTH_SHORT).show();
                                progressDialog.dismiss();
                                break;
                            }
                        } else{
                            firebaseAuth.createUserWithEmailAndPassword(email, pass)
                                    .addOnCompleteListener(Register.this, new OnCompleteListener<AuthResult>() {
                                        @Override
                                        public void onComplete(@NonNull Task<AuthResult> task) {
                                            if (task.isSuccessful()) {
                                                HashMap user = new HashMap();
                                                user.put("First Name", Fname.getText().toString());
                                                user.put("Last Name", Lname.getText().toString());
                                                user.put("Email", Email.getText().toString());
                                                user.put("Phone No", Phno.getText().toString());
                                                user.put("SID", SID.getText().toString());
                                                CollectionReference ref = db.collection("Users");
                                                String Uid = firebaseAuth.getUid();
                                                DocumentReference d = ref.document(Uid);
                                                d.set(user)
                                                        .addOnFailureListener(new OnFailureListener() {
                                                            @Override
                                                            public void onFailure(@NonNull Exception e) {
                                                                Toast.makeText(getApplicationContext(), "Error" + e, Toast.LENGTH_LONG).show();
                                                            }
                                                        });
                                                progressDialog.dismiss();
                                                Toast.makeText(getApplicationContext(), "Registered", Toast.LENGTH_SHORT).show();

                                                Logout();
                                            } else {
                                                progressDialog.dismiss();
                                                Toast.makeText(getApplicationContext(), task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                        }
                    }
                }
            });
        }
    }

    public void Logout() {
        FirebaseAuth.getInstance().signOut();
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(Register.this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.commit();
        startActivity(new Intent(Register.this, MainActivity.class));
        Register.this.finish();
    }
}
