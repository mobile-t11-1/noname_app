package com.example.noname;

import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mancj.materialsearchbar.MaterialSearchBar;

import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MapView11#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MapView11 extends Fragment{

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private GoogleMap mMap;
    FloatingActionButton locatedFAB;
    private FusedLocationProviderClient mLocationClient;

    private PlacesClient placesClient;
    private List<AutocompletePrediction> predictionList;
    private View mapView;
    // store the current location
    private Location mLastKnownLoc;
    private LocationCallback locationCallback;

    private MaterialSearchBar searchBar;
    private Button findBtn;

    private final float DEFAULT_ZOOM = 18;


    public MapView11() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment MapView.
     */
    // TODO: Rename and change types and number of parameters
    public static MapView11 newInstance(String param1, String param2) {
        MapView11 fragment = new MapView11();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        SupportMapFragment supportMapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.google_map);
        // Async map
        supportMapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull GoogleMap googleMap) {
                // When map is loaded
                googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(@NonNull LatLng latLng) {
                        // When click on map
                        // Initialize marker option
                        MarkerOptions markerOptions = new MarkerOptions();

                        markerOptions.position(latLng);

                        markerOptions.title(latLng.latitude + ":" + latLng.longitude);
                        //remove all marker
                        googleMap.clear();
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM));
                        // add marker on map
                        googleMap.addMarker(markerOptions);
                    }
                });
            }
        });
        return view;
    }

//    @SuppressLint("VisibleForTests")
//    @Override
//    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
//        super.onViewCreated(view, savedInstanceState);
//
//        SupportMapFragment supportMapFragment = (SupportMapFragment) getChildFragmentManager()
//                .findFragmentById(R.id.google_map);
//
//
//        locatedFAB = getView().findViewById(R.id.locate_btn);
//        locatedFAB.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                getCurrLoc();
//            }
//        });
//
//        mLocationClient = new FusedLocationProviderClient(getActivity());
//
//
//        supportMapFragment.getMapAsync(new OnMapReadyCallback() {
//            @SuppressLint("MissingPermission")
//            @Override
//            public void onMapReady(@NonNull GoogleMap googleMap) {
//                mMap = googleMap;
////                mMap.setMyLocationEnabled(true);
////                mMap.getUiSettings().setMyLocationButtonEnabled(true);
////
////                if(mMap != null && mapView.findViewById(Integer.parseInt("1")) != null){
////                    View locationButton = ((View) mapView.findViewById(Integer.parseInt("1"))
////                            .getParent()).findViewById(Integer.parseInt("2"));
////
////                    RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();
////                    layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP,0);
////                    layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,RelativeLayout.TRUE);
////                }
//            }
//        });
//
//    }

//    @SuppressLint("MissingPermission")
//    private void getCurrLoc() {
//        mLocationClient.getLastLocation().addOnCompleteListener(task -> {
//            if (task.isSuccessful()){
//                Location location = task.getResult();
//                gotoLocation(location.getLatitude(), location.getLongitude());
//
//            }
//        });
//    }

//    private void gotoLocation(double latitude, double longitude) {
//        LatLng latLng = new LatLng(latitude, longitude);
//        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM);
//        mMap.moveCamera(cameraUpdate);
//        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
//    }
//
//    @Override
//    public void onMapReady(@NonNull GoogleMap googleMap) {
//        mMap = googleMap;
//    }

//    @Override
//    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
//        super.onViewCreated(view, savedInstanceState);
//
//        // Initialize fused location
//        client = LocationServices.getFusedLocationProviderClient(getActivity());
//
//        // check permission
//        if (ActivityCompat.checkSelfPermission(getActivity(),
//                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
//            // When permission grated
//            // Call method
//            getCurrentLocation();
//        }else{
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION},66);
//        }
//    }
//
//    private void getCurrentLocation() {
//        // Initialize task location
//        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
//                ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            Task<Location> task = client.getLastLocation();
//            task.addOnSuccessListener(new OnSuccessListener<Location>() {
//                @Override
//                public void onSuccess(Location location) {
//                    // When success
//                    if(location != null){
//                        // Sync map
//                        supportMapFragment.getMapAsync(new OnMapReadyCallback() {
//                            @Override
//                            public void onMapReady(@NonNull GoogleMap googleMap) {
//                                // Initialize lat lng
//                                com.google.android.gms.maps.model.LatLng latLng = new LatLng(location.getLatitude(),
//                                        location.getLongitude());
//                                // Create marker options
//                                MarkerOptions options = new MarkerOptions().position(latLng)
//                                        .title("I am there");
//
//                                //Zoom map
//                                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10));
//                                // Add marker on map
//                                googleMap.addMarker(options);
//
//                            }
//                        });
//                    }
//                }
//            });
//        }else {
//            return;
//        }
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == 66){
//            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
//                // When permission granted
//                // get current location again
//                getCurrentLocation();
//            }
//        }else {
//            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION},66);
//        }
//    }
}