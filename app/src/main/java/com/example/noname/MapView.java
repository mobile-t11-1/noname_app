package com.example.noname;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.SearchView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MapView extends AppCompatActivity {

    GoogleMap mMap;
    SupportMapFragment supportMapFragment;
    SearchView searchView;
    FloatingActionButton locatedFAB;
    FusedLocationProviderClient mLocationClient;

    PlacesClient placesClient;
    List<AutocompletePrediction> predictionList;


    private final float DEFAULT_ZOOM = 15;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_view);
        supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.google_map);
        mLocationClient = LocationServices.getFusedLocationProviderClient(this);

        Places.initialize(MapView.this, "AIzaSyCzs3j4u8RQDFL3Hjrc_yE_Zt0Uk72diEw");
        placesClient = Places.createClient(this);
        final AutocompleteSessionToken token = AutocompleteSessionToken.newInstance();

        checkPermission();
        supportMapFragment.getMapAsync(new OnMapReadyCallback() {
            @SuppressLint("MissingPermission")
            @Override
            public void onMapReady(@NonNull GoogleMap googleMap) {
                mMap = googleMap;
            }
        });
        searchView = findViewById(R.id.sv_location);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                String location = searchView.getQuery().toString();
                List<Address> addressesList = null;

                if(location != null || !location.equals("")){
                    Geocoder geocoder = new Geocoder(MapView.this);
                    try {
                        addressesList = geocoder.getFromLocationName(location,1);
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                    Address address = addressesList.get(0);
                    LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                    mMap.addMarker(new MarkerOptions().position(latLng).title(location));
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM));

                    supportMapFragment.getMapAsync(new OnMapReadyCallback() {
                        @Override
                        public void onMapReady(@NonNull GoogleMap googleMap) { mMap = googleMap; }
                    });
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                FindAutocompletePredictionsRequest predictionsRequest = FindAutocompletePredictionsRequest.builder()
                        .setTypeFilter(TypeFilter.ADDRESS)
                        .setSessionToken(token)
                        .setQuery(s.toString())
                        .build();
                placesClient.findAutocompletePredictions(predictionsRequest)
                        .addOnCompleteListener(new OnCompleteListener<FindAutocompletePredictionsResponse>() {
                            @Override
                            public void onComplete(@NonNull Task<FindAutocompletePredictionsResponse> task) {
                                if (task.isSuccessful()) {
                                    FindAutocompletePredictionsResponse predictionsResponse = task.getResult();
                                    if (predictionsResponse != null) {
                                        predictionList = predictionsResponse.getAutocompletePredictions();
                                        List<String> suggestionsList = new ArrayList<>();
                                        for (int i = 0; i < predictionList.size(); i++) {
                                            AutocompletePrediction prediction = predictionList.get(i);
                                            suggestionsList.add(prediction.getFullText(null).toString());
                                        }

                                    }
                                } else {
                                    System.out.println("Something Wrong when getting suggestion");
                                }
                            }
                        });
                return false;
            }
        });

        locatedFAB = findViewById(R.id.locate_btn);
        locatedFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gotoMyLocation();
            }
        });
    }

    private void checkPermission() {
        Dexter.withContext(this).withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                        Toast.makeText(MapView.this, "Permission Granted", Toast.LENGTH_LONG).show();
                        getCurrLoc();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                        permissionToken.continuePermissionRequest();
                    }
                }).check();
    }


    private void getCurrLoc() {
        @SuppressLint("MissingPermission") Task<Location> task = mLocationClient.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    supportMapFragment.getMapAsync(new OnMapReadyCallback() {
                        @SuppressLint("MissingPermission")
                        @Override
                        public void onMapReady(@NonNull GoogleMap googleMap) {
                            mMap = googleMap;
                            mMap.setMyLocationEnabled(true);
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);
                            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                            System.out.println(latLng.latitude + "," + latLng.longitude);
                             //Create marker options
                            MarkerOptions options = new MarkerOptions().position(latLng)
                                        .title("I am there");
                            //Zoom map
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM));
                            // Add marker on map
//                            mMap.addMarker(options);
                        }
                    });
                }
            }
        });
    }

    private void gotoMyLocation() {
        @SuppressLint("MissingPermission") Task<Location> task = mLocationClient.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if(location != null){
                    supportMapFragment.getMapAsync(new OnMapReadyCallback() {
                        @SuppressLint("MissingPermission")
                        @Override
                        public void onMapReady(@NonNull GoogleMap googleMap) {
                            mMap = googleMap;
                            mMap.setMyLocationEnabled(true);
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);
                            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                            System.out.println(latLng.latitude + "," + latLng.longitude);
                            //Create marker options
                            MarkerOptions options = new MarkerOptions().position(latLng)
                                    .title("I am there");
                            //Zoom map
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM));
                            // Add marker on map
                            //mMap.addMarker(options);
                        }
                    });
                }
            }
        });
    }
}