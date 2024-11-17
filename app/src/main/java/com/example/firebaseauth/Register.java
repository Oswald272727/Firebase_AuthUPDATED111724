package com.example.firebaseauth;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Register extends AppCompatActivity {

    TextInputEditText editTextEmail, editTextPassword, editTextStudentId;
    TextInputEditText editTextFirstname, editTextLastname, editTextUsername;
    Button buttonReg;
    FirebaseAuth mAuth;
    ProgressBar progressBar;
    TextView textView;
    FirebaseFirestore db;
    private boolean isRegistering = false;

    @Override
    public void onStart() {
        super.onStart();
        if (!isRegistering) {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                finish();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        editTextEmail = findViewById(R.id.email);
        editTextPassword = findViewById(R.id.password);
        editTextStudentId = findViewById(R.id.student_id);
        editTextFirstname = findViewById(R.id.firstname);
        editTextLastname = findViewById(R.id.lastname);
        editTextUsername = findViewById(R.id.username);
        buttonReg = findViewById(R.id.btn_register);
        progressBar = findViewById(R.id.progressBar);
        textView = findViewById(R.id.loginNow);

        textView.setOnClickListener(view -> {
            startActivity(new Intent(getApplicationContext(), Login.class));
            finish();
        });

        buttonReg.setOnClickListener(view -> {
            progressBar.setVisibility(View.VISIBLE);
            String email = editTextEmail.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();
            String studentId = editTextStudentId.getText().toString().trim();
            String firstname = editTextFirstname.getText().toString().trim();
            String lastname = editTextLastname.getText().toString().trim();
            String username = editTextUsername.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                Toast.makeText(Register.this, "Enter email", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                return;
            }

            if (TextUtils.isEmpty(password)) {
                Toast.makeText(Register.this, "Enter password", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                return;
            }

            if (TextUtils.isEmpty(studentId)) {
                Toast.makeText(Register.this, "Enter student ID", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                return;
            }

            if (TextUtils.isEmpty(firstname)) {
                Toast.makeText(Register.this, "Enter first name", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                return;
            }

            if (TextUtils.isEmpty(lastname)) {
                Toast.makeText(Register.this, "Enter last name", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                return;
            }

            if (TextUtils.isEmpty(username)) {
                Toast.makeText(Register.this, "Enter username", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                return;
            }

            doRegister(email, password, studentId, firstname, lastname, username);
        });
    }

    private void doRegister(String email, String password, String studentId, String firstname, String lastname, String username) {
        isRegistering = true;
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserToFirestore(user, studentId, firstname, lastname, username);
                            sendVerificationEmail();
                            mAuth.signOut();
                        }
                        Toast.makeText(Register.this, "Account created. Please verify your email.", Toast.LENGTH_SHORT).show();
                    } else {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(Register.this, "Registration failed.", Toast.LENGTH_SHORT).show();
                    }
                    isRegistering = false;
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    if (e instanceof FirebaseAuthUserCollisionException) {
                        editTextEmail.setError("Email already registered");
                        editTextEmail.requestFocus();
                    } else {
                        Toast.makeText(Register.this, "Oops! Something went wrong.", Toast.LENGTH_SHORT).show();
                    }
                    isRegistering = false;
                });
    }

    private void sendVerificationEmail() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.sendEmailVerification()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(Register.this, "Verification email sent.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(Register.this, "Failed to send verification email.", Toast.LENGTH_SHORT).show();
                        }
                        progressBar.setVisibility(View.GONE);
                    });
        }
    }

    private void saveUserToFirestore(FirebaseUser user, String studentId, String firstname, String lastname, String username) {
        String userId = user.getUid();
        String email = user.getEmail();

        // Prepare user data
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", email);
        userData.put("studentId", studentId);
        userData.put("firstname", firstname);
        userData.put("lastname", lastname);
        userData.put("username", username);
        userData.put("registeredAt", FieldValue.serverTimestamp()); // Adds the server's current timestamp

        // Save data to Firestore
        db.collection("users").document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User profile saved to Firestore"))
                .addOnFailureListener(e -> Log.w(TAG, "Error saving user profile", e));
    }
}
