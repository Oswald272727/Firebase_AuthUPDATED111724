package com.example.firebaseauth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class Login extends AppCompatActivity {

    EditText emailEditText, passwordEditText;
    Button loginButton, forgotPasswordButton;
    ProgressBar progressBar;
    TextView registerNowTextView;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Bind views
        emailEditText = findViewById(R.id.email);
        passwordEditText = findViewById(R.id.password);
        loginButton = findViewById(R.id.btn_login);
        forgotPasswordButton = findViewById(R.id.btn_forgot_password);
        progressBar = findViewById(R.id.progressBar);
        registerNowTextView = findViewById(R.id.registerNow);

        // Login button click listener
        loginButton.setOnClickListener(view -> loginUser());

        // Forgot Password button click listener
        forgotPasswordButton.setOnClickListener(view -> showForgotPasswordDialog());

        // Register Now text click listener
        registerNowTextView.setOnClickListener(view -> {
            Intent intent = new Intent(Login.this, Register.class);
            startActivity(intent);
        });
    }

    private void loginUser() {
        String input = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Input validation
        if (TextUtils.isEmpty(input)) {
            emailEditText.setError("Enter your email or username");
            emailEditText.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Enter your password");
            passwordEditText.requestFocus();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        // Check if input is an email or username
        if (Patterns.EMAIL_ADDRESS.matcher(input).matches()) {
            // Input is an email
            authenticateWithEmail(input, password);
        } else {
            // Input is a username
            validateUsername(input, password);
        }
    }

    // Method to authenticate with email
    private void authenticateWithEmail(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null && user.isEmailVerified()) {
                            Toast.makeText(Login.this, "Login successful", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(Login.this, MainActivity.class));
                            finish();
                        } else {
                            Toast.makeText(Login.this, "Please verify your email", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(Login.this, "Login failed. Check your credentials", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Method to validate username
    private void validateUsername(String username, String password) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users")
                .whereEqualTo("username", username)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        // Username exists, get associated email
                        String email = task.getResult().getDocuments().get(0).getString("email");
                        if (email != null) {
                            // Authenticate using the associated email
                            authenticateWithEmail(email, password);
                        } else {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(Login.this, "Username not linked to any email", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(Login.this, "Username not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(Login.this, "Error validating username", Toast.LENGTH_SHORT).show();
                });
    }

    private void showForgotPasswordDialog() {
        // Create an alert dialog to ask for the email address
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Forgot Password");

        // Create an input field for the email
        final EditText input = new EditText(this);
        input.setHint("Enter your email");
        builder.setView(input);

        // Set dialog buttons
        builder.setPositiveButton("Send", (dialog, which) -> {
            String email = input.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                Toast.makeText(Login.this, "Enter a valid email", Toast.LENGTH_SHORT).show();
                return;
            }
            sendPasswordResetEmail(email);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        // Show the dialog
        builder.show();
    }

    private void sendPasswordResetEmail(String email) {
        progressBar.setVisibility(View.VISIBLE);
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Toast.makeText(Login.this, "Reset email sent successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(Login.this, "Error sending reset email", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
