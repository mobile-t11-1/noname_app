package com.example.noname;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Register extends AppCompatActivity implements View.OnClickListener{

    private FirebaseAuth mAuth;

    private EditText editTextUsername, editTextEmail, editTextPassword, editTextRepeatedPassword;
    private Button registerBtn;
    private ImageButton gobackBtn;
    private ProgressBar progressBar;

    private FirebaseFirestore mydb = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        editTextUsername = (EditText) findViewById(R.id.username_reg);
        editTextEmail = (EditText) findViewById(R.id.textEmailAddress_reg);
        editTextPassword = (EditText) findViewById(R.id.textPassword_reg);
        editTextRepeatedPassword = (EditText) findViewById(R.id.textPassword_reg_repeat);

        progressBar = (ProgressBar) findViewById(R.id.register_progressBar);

        registerBtn = (Button) findViewById(R.id.register_btn);
        registerBtn.setOnClickListener(this);

        gobackBtn = (ImageButton) findViewById(R.id.goback_btn);
        gobackBtn.setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.register_btn:
                registerUser();
                break;
            case R.id.goback_btn:
                startActivity(new Intent(Register.this, Login.class));
                break;
        }
    }

    private void registerUser() {
        String username = editTextUsername.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String passwordRepeat = editTextRepeatedPassword.getText().toString().trim();

        if(username.isEmpty()){
            editTextUsername.setError("Username is required!");
            editTextUsername.requestFocus();
            return;
        }

        if(email.isEmpty()){
            editTextEmail.setError("Email is required!");
            editTextEmail.requestFocus();
            return;
        }

        if(password.isEmpty()){
            editTextPassword.setError("Password is required!");
            editTextPassword.requestFocus();
            return;
        }

        if(password.length() < 7){
            editTextPassword.setError("Password should be longer than 6 chars");
            editTextPassword.requestFocus();
            return;
        }

        if(passwordRepeat.isEmpty()){
            editTextRepeatedPassword.setError("Comfirm your password!");
            editTextRepeatedPassword.requestFocus();
            return;
        }


        if(!passwordRepeat.equals(password)){
            editTextRepeatedPassword.setError("Password failed to match!");
            editTextRepeatedPassword.requestFocus();
            return;
        }

        // check email format
        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            editTextEmail.setError("Please provide valid email!");
            editTextEmail.requestFocus();
            return;
        }
        // set progress bar visible
        progressBar.setVisibility(View.VISIBLE);

        // create User object based on the provided information
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        if(task.isSuccessful()){
//                            User user = new User(username, email);
//
//                            // pass this user instance to firebase
//                            // 1. Get a unique UID for this user
//                            // 2. Check if the data has been successfully passed into database
//                            FirebaseDatabase.getInstance().getReference("Users")
//                                    .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
//                                    .setValue(user).addOnCompleteListener(new OnCompleteListener<Void>() {
//                                @Override
//                                public void onComplete(@NonNull Task<Void> task) {
//                                    // if the data is in database
//                                    if(task.isSuccessful()){
//                                        Toast.makeText(Register.this, "User has been registered successfully!", Toast.LENGTH_LONG).show();
//                                        progressBar.setVisibility(View.GONE);
//
//                                        startActivity(new Intent(Register.this, Login.class));
//                                    }else{
//                                        Toast.makeText(Register.this, "Failed to register!1", Toast.LENGTH_LONG).show();
//                                        progressBar.setVisibility(View.GONE);
//                                    }
//
//                                }
//                            });

                            Map<String, Object> newUser = new HashMap<>();
                            newUser.put("User Name", username);
                            newUser.put("Email", email);
                            newUser.put("User ID", FirebaseAuth.getInstance().getCurrentUser().getUid());

                            // add the user into Cloud Firestore
                            mydb.collection("user").document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                    .set(newUser)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void unused) {
                                            Toast.makeText(Register.this, "User has been registered successfully!", Toast.LENGTH_LONG).show();
                                            progressBar.setVisibility(View.GONE);

                                            // once registered, jump to Login page
                                            startActivity(new Intent(Register.this, Login.class));
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Toast.makeText(Register.this, "Failed to register!1", Toast.LENGTH_LONG).show();
                                            progressBar.setVisibility(View.GONE);
                                        }
                                    });




                        }

                        else {
                            Toast.makeText(Register.this, "Failed to register!2", Toast.LENGTH_LONG).show();
                            progressBar.setVisibility(View.GONE);
                        }

                    }
                });

    }
}