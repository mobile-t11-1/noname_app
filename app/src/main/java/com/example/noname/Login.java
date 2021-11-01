package com.example.noname;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class Login extends AppCompatActivity implements View.OnClickListener{

    private FirebaseAuth mAuth;

    private TextView register;
    private TextView resetPassword;

    private EditText editTextEmail, editTextPassword;
    private Button loginBtn;
    private ProgressBar progressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        register = (TextView) findViewById(R.id.register);
        register.setOnClickListener(this);

        resetPassword = (TextView) findViewById(R.id.forget_pw);
        resetPassword.setOnClickListener(this);

        editTextEmail = (EditText) findViewById(R.id.textEmailAddress);
        editTextPassword = (EditText) findViewById(R.id.textPassword);
        progressBar = (ProgressBar) findViewById(R.id.login_progressBar);

        loginBtn = (Button) findViewById(R.id.login_btn);
        loginBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.register:
                startActivity(new Intent(Login.this, Register.class));
                break;
            case R.id.login_btn:
                userLogin();
                break;
            case R.id.forget_pw:
                startActivity(new Intent(Login.this, ResetPassword.class));
                break;

        }
    }

    private void userLogin(){
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if(email.isEmpty()){
            editTextEmail.setError("Email is required!");
            editTextEmail.requestFocus();
            return;
        }

        // check email format
        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            editTextEmail.setError("Please provide valid email!");
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

        progressBar.setVisibility(View.VISIBLE);

        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {

                if(task.isSuccessful()){
                    // redirect to logged in profile page
                    startActivity(new Intent(Login.this, MainActivity.class));
                    progressBar.setVisibility(View.GONE);
                }
                else {
                    Toast.makeText(Login.this, "Sign in Failed! Please check your email/password", Toast.LENGTH_LONG).show();
                    progressBar.setVisibility(View.GONE);
                }
            }
        });

    }

}