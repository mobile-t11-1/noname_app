package com.example.noname;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class Welcome extends AppCompatActivity implements View.OnClickListener{

    private Button welcomeBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        welcomeBtn = (Button) findViewById(R.id.welcome_btn);
        welcomeBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        startActivity(new Intent(Welcome.this, Login.class));
    }
}