package com.example.noname;

import android.annotation.SuppressLint;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

/**
 * Map fragment view for discover function
 */
public class MapFrag extends Fragment{

    private GoogleMap mMap;
    FloatingActionButton locatedFAB;
    Button libFindBtn;
    Button cafeFindBtn;
    Button parkFindBtn;
    //private FusedLocationProviderClient mLocationClient;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private final float DEFAULT_ZOOM = 15;
    //SupportMapFragment supportMapFragment;
    private Location mLastKnownLocation;
    private LocationCallback locationCallback;
    Integer radius = 5000;


    @SuppressLint("MissingPermission")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        SupportMapFragment supportMapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.google_map);

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getActivity());

        supportMapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull GoogleMap googleMap) {
                mMap = googleMap;
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);

                //check if gps is enabled or not and then request user to enable it
                LocationRequest locationRequest = LocationRequest.create();
                locationRequest.setInterval(10000);
                locationRequest.setFastestInterval(5000);
                locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

                LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);

                SettingsClient settingsClient = LocationServices.getSettingsClient(getActivity());

                Task<LocationSettingsResponse> task = settingsClient.checkLocationSettings(builder.build());
                task.addOnSuccessListener(getActivity(), new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        getDeviceLocation();
                    }
                });

                mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(@NonNull LatLng latLng) {
                        MarkerOptions markerOptions = new MarkerOptions();
                        markerOptions.position(latLng);

                        markerOptions.title(latLng.latitude + ":" + latLng.longitude);

                        mMap.clear();
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM));

                        mMap.addMarker(markerOptions);
                    }
                });

            }
        });

        locatedFAB = (FloatingActionButton) view.findViewById(R.id.locate_btn);
        locatedFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getDeviceLocation();
            }
        });

        libFindBtn = (Button) view.findViewById(R.id.lib_search_btn);
        libFindBtn.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("UseCompatLoadingForDrawables")
            @Override
            public void onClick(View view) {
                libFindBtn.setBackground(getResources().getDrawable(R.drawable.pressed_btn_style));
                cafeFindBtn.setBackground(getResources().getDrawable(R.drawable.normal_btn_style));
                parkFindBtn.setBackground(getResources().getDrawable(R.drawable.normal_btn_style));
                findPlace("library");
            }
        });

        cafeFindBtn = (Button) view.findViewById(R.id.cafe_search_btn);
        cafeFindBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cafeFindBtn.setBackground(getResources().getDrawable(R.drawable.pressed_btn_style));
                libFindBtn.setBackground(getResources().getDrawable(R.drawable.normal_btn_style));
                parkFindBtn.setBackground(getResources().getDrawable(R.drawable.normal_btn_style));
                findPlace("cafe");
            }
        });

        parkFindBtn = (Button) view.findViewById(R.id.park_search_btn);
        parkFindBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                parkFindBtn.setBackground(getResources().getDrawable(R.drawable.pressed_btn_style));
                libFindBtn.setBackground(getResources().getDrawable(R.drawable.normal_btn_style));
                cafeFindBtn.setBackground(getResources().getDrawable(R.drawable.normal_btn_style));
                findPlace("park");
            }
        });

        return view;
    }

    /**
     * The function finds the nearby places around the current user's location
     * @param placeName the kind of place want to find
     */
    private void findPlace(String placeName){
        if (mLastKnownLocation == null){
            openDialog();
        }else {
            // Init url
            String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                    "?location=" + mLastKnownLocation.getLatitude() + "," + mLastKnownLocation.getLongitude() +
                    "&radius=" + radius + // nearby radius
                    "&type=" + placeName +
                    "&sensor=true" + //Sensor
                    "&key=" + getResources().getString(R.string.google_map_key);
            // Execute Place task
            System.out.println(url);
            new PlaceTask().execute(url);
        }
    }



    @SuppressLint("MissingPermission")
    private void getDeviceLocation() {
        mFusedLocationProviderClient.getLastLocation()
                .addOnCompleteListener(new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            mLastKnownLocation = task.getResult();
                            if (mLastKnownLocation != null) {
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                            } else {
                                final LocationRequest locationRequest = LocationRequest.create();
                                locationRequest.setInterval(10000);
                                locationRequest.setFastestInterval(5000);
                                locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                                locationCallback = new LocationCallback() {
                                    @Override
                                    public void onLocationResult(LocationResult locationResult) {
                                        super.onLocationResult(locationResult);
                                        if (locationResult == null) {
                                            return;
                                        }
                                        mLastKnownLocation = locationResult.getLastLocation();
                                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                                        mFusedLocationProviderClient.removeLocationUpdates(locationCallback);
                                    }
                                };
                                mFusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);

                            }
                        } else {
                            Toast.makeText(getActivity(), "unable to get last location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void openDialog(){
        MaplocateDialog maplocateDialog = new MaplocateDialog();
        maplocateDialog.show(getFragmentManager(),"Maplocate");
    }

    private class PlaceTask extends AsyncTask<String,Integer,String> {
        @Override
        protected String doInBackground(String... strings) {
            String data = null;
            // Init data
            try {
                data = downloadUrl(strings[0]);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return data;
        }

        @Override
        protected void onPostExecute(String s) {
            //Execute parser task
            new ParserTask().execute(s);
        }
    }

    private String downloadUrl(String string) throws IOException {
        //Initialize url
        URL url = new URL(string);
        //Init connection
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        //Connect connection
        connection.connect();
        //Initialize input stream
        InputStream stream = connection.getInputStream();
        // Initialize buffer reader
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        //Initialze string builder
        StringBuilder builder = new StringBuilder();
        //Initialize string variable
        String line = "";

        while ((line = reader.readLine()) != null){
            // Append line
            builder.append(line);
        }
        //Get appended data
        String data = builder.toString();
        //close
        reader.close();

        return data;
    }

    private class ParserTask extends AsyncTask<String,Integer, List<HashMap<String, String>>>{
        @Override
        protected List<HashMap<String, String>> doInBackground(String... strings) {
            // Create json parser class
            JsonParser jsonParser = new JsonParser();
            //Initialize hash map list
            List<HashMap<String,String>> mapList = null;
            //Initialize json object
            try {
                JSONObject object = new JSONObject(strings[0]);
                //Parse json object
                mapList = jsonParser.parseResult(object);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            // Return map list
            return mapList;
        }

        @Override
        protected void onPostExecute(List<HashMap<String, String>> hashMaps) {
            //Clear map
            mMap.clear();
            if(hashMaps.size() >= 5){
                for(int i=0; i<5; i++){
                    //Initialize hash map
                    HashMap<String,String> hashMapList = hashMaps.get(i);
                    //Get latitude
                    double lat = Double.parseDouble(hashMapList.get("lat"));
                    //Get longitude
                    double lng = Double.parseDouble(hashMapList.get("lng"));
                    //Get name
                    String name = hashMapList.get("name");
                    // Concat lat and lng
                    LatLng latLng = new LatLng(lat, lng);
                    //Init marker options
                    MarkerOptions options = new MarkerOptions();
                    //Set position
                    options.position(latLng);
                    options.title(name);
                    mMap.addMarker(options);
                }
            }else {
                for(int i=0; i<hashMaps.size(); i++){
                    //Initialize hash map
                    HashMap<String,String> hashMapList = hashMaps.get(i);
                    //Get latitude
                    double lat = Double.parseDouble(hashMapList.get("lat"));
                    //Get longitude
                    double lng = Double.parseDouble(hashMapList.get("lng"));
                    //Get name
                    String name = hashMapList.get("name");
                    // Concat lat and lng
                    LatLng latLng = new LatLng(lat, lng);
                    //Init marker options
                    MarkerOptions options = new MarkerOptions();
                    //Set position
                    options.position(latLng);
                    options.title(name);
                    mMap.addMarker(options);
                }
            }
        }
    }


}