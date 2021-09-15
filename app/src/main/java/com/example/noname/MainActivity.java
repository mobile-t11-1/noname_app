package com.example.noname;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;

import android.os.Bundle;
import android.view.MenuItem;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // navigation bar
        BottomNavigationView bottomNavigationView = findViewById(R.id.nav_bar);
        bottomNavigationView.setOnNavigationItemSelectedListener(navListener);

    }


    private BottomNavigationView.OnNavigationItemSelectedListener navListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    Fragment selectedFragment = null;

                    switch (item.getItemId()){
                        case R.id.shopLstFrag:
                            selectedFragment = new ShopLstFrag();
                            break;
                        case R.id.mapFrag:
                            selectedFragment = new MapFrag();
                            break;
                        case R.id.profileFrag:
                            selectedFragment = new ProfileFrag();
                            break;

                    }

                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container,selectedFragment).commit();

                    return true;

                }
            };
}