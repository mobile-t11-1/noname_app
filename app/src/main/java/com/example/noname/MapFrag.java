package com.example.noname;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MapFrag#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MapFrag extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private Button testBtn;
    private final float DEFAULT_ZOOM = 18;

    public MapFrag() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment MapFrag.
     */
    // TODO: Rename and change types and number of parameters
    public static MapFrag newInstance(String param1, String param2) {
        MapFrag fragment = new MapFrag();
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
        // Inflate the layout for this fragment
//        View view = inflater.inflate(R.layout.fragment_map, container, false);
//        SupportMapFragment supportMapFragment = (SupportMapFragment) getChildFragmentManager()
//                .findFragmentById(R.id.google_map);
//        // Async map
//        supportMapFragment.getMapAsync(new OnMapReadyCallback() {
//            @Override
//            public void onMapReady(@NonNull GoogleMap googleMap) {
//                // When map is loaded
//                googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
//                    @Override
//                    public void onMapClick(@NonNull LatLng latLng) {
//                        // When click on map
//                        // Initialize marker option
//                        MarkerOptions markerOptions = new MarkerOptions();
//
//                        markerOptions.position(latLng);
//
//                        markerOptions.title(latLng.latitude + ":" + latLng.longitude);
//                        //remove all marker
//                        googleMap.clear();
//                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM));
//                        // add marker on map
//                        googleMap.addMarker(markerOptions);
//                    }
//                });
//            }
//        });
//        return view;
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        testBtn = (Button) getView().findViewById(R.id.test_btn);
        testBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getActivity(), MapView.class));
            }
        });
    }

    //    @Override
//    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
//        super.onViewCreated(view, savedInstanceState);
//        testBtn = (Button) getView().findViewById(R.id.test_btn);
//        testBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                getChildFragmentManager().beginTransaction()
//                        .replace(R.id.fragment_container, new MapView() ).commit();
//            }
//        });

//        // check permission
//        if (ContextCompat.checkSelfPermission(getContext(),
//                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
//            // When permission grated
//            getFragmentManager().beginTransaction()
//                    .replace(R.id.fragment_container, new MapView() ).commit();
//            return;
//        }else{
//
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//
//            //ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.
//            // ACCESS_FINE_LOCATION},66);
//
//            testBtn.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    getChildFragmentManager().beginTransaction()
//                            .replace(R.id.fragment_container, new MapView() ).commit();
////                    Dexter.withActivity(getActivity())
////                            .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
////                            .withListener(new PermissionListener() {
////                                @Override
////                                public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
////                                    getFragmentManager().beginTransaction()
////                                            .replace(R.id.fragment_container, new MapView() ).commit();
////                                }
////
////                                @Override
////                                public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
////                                    if (permissionDeniedResponse.isPermanentlyDenied()){
////                                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
////                                        builder.setTitle("Permission Denied")
////                                                .setMessage("Permission to access device location is permanently denied.")
////                                                .setNegativeButton("Cancel", null)
////                                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
////                                                    @Override
////                                                    public void onClick(DialogInterface dialogInterface, int i) {
////                                                        Intent intent = new Intent();
////                                                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
////                                                        intent.setData(Uri.fromParts("package", getActivity().getPackageName(),null));
////                                                    }
////                                                })
////                                                .show();
////                                    }else {
////                                        Toast.makeText(getActivity(), "Permission Denied", Toast.LENGTH_LONG).show();
////                                    }
////                                }
////
////                                @Override
////                                public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
////                                    permissionToken.continuePermissionRequest();
////                                }
////                            }).check();
//                }
//            });


}