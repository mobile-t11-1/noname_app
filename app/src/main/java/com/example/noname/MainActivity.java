package com.example.noname;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.os.Bundle;
import android.view.MenuItem;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private ShopLstFrag listFrag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // navigation bar
        BottomNavigationView bottomNavigationView = findViewById(R.id.nav_bar);
        bottomNavigationView.setOnNavigationItemSelectedListener(navListener);
        bottomNavigationView.getMenu().findItem(R.id.profileFrag).setChecked(true);


        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new ProfileFrag()).commit();

    }

    private BottomNavigationView.OnNavigationItemSelectedListener navListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    Fragment selectedFragment = null;

                    switch (item.getItemId()){
                        case R.id.profileFrag:
                            selectedFragment = new ProfileFrag();
                            break;
                        case R.id.shopLstFrag:
                            if(listFrag == null){
                                listFrag = new ShopLstFrag();
                            }
                            selectedFragment = listFrag;
                            break;
                        case R.id.mapInfoFrag:
                            selectedFragment = new MapInfoFr();
                            break;
                        case R.id.pomoFrag:
                            selectedFragment = new PomoFrag();
                            break;
                        case R.id.leaderBFrag:
                            selectedFragment = new LeaderBFrag();
                            break;

                    }

                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container,selectedFragment).commit();

                    return true;

                }
            };
}