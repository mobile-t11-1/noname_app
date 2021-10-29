package com.example.noname;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Welcome extends AppCompatActivity implements View.OnClickListener{

    private Button welcomeBtn;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        welcomeBtn = (Button) findViewById(R.id.welcome_btn);
        welcomeBtn.setOnClickListener(this);
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public void onClick(View view) {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if(firebaseUser != null){
            startActivity(new Intent(Welcome.this, MainActivity.class));
        }else {
            startActivity(new Intent(Welcome.this, Login.class));
        }
    }
}