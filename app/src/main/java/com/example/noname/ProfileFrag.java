package com.example.noname;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.Fragment;

import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import io.grpc.Context;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ProfileFrag#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ProfileFrag extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private FirebaseUser user;

    private String userID;
    private LinearLayout logoutlayout;
    private LinearLayout aboutlayout;
    private LinearLayout editlayout;
    private LinearLayout aboutlayout;
    private ImageView profile_avatar;


    private FirebaseFirestore mydb = FirebaseFirestore.getInstance();
    private StorageReference storage = FirebaseStorage.getInstance().getReference();


    public ProfileFrag() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ProfileFrag.
     */
    // TODO: Rename and change types and number of parameters
    public static ProfileFrag newInstance(String param1, String param2) {
        ProfileFrag fragment = new ProfileFrag();
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
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        profile_avatar = view.findViewById(R.id.profile_avatar);
        StorageReference profileavatar = storage.child("users/" + mAuth.getCurrentUser().getUid() + "/avatar.jpg");
        profileavatar.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Picasso.get().load(uri).into(profile_avatar);
            }
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //log out section
        logoutlayout = getView().findViewById(R.id.logout_layout);
        logoutlayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAuth.signOut();
                startActivity(new Intent(getActivity(), Login.class));
            }
        });
        
        // about section
        aboutlayout = getView().findViewById(R.id.about_layout);
        aboutlayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getActivity(), About.class));
        });

        // edit profile section
        editlayout = getView().findViewById(R.id.edit_layout);
        editlayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new EditFrag()).commit();
            }
        });



        // load user information from the database
        user = FirebaseAuth.getInstance().getCurrentUser();
        userID = user.getUid();
        

        final TextView usernameTextView = (TextView) getView().findViewById(R.id.username_title);
        final TextView useremailTextview = (TextView) getView().findViewById(R.id.usernameText);
        final TextView likeNum = (TextView) getView().findViewById(R.id.like_num);


        // search the user and display the user name on profile page
        mydb.collection("user")
                .whereEqualTo("User ID", userID)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful() && !task.getResult().isEmpty()){
                            String username = task.getResult().getDocuments().get(0).get("User Name").toString();
                            String email = task.getResult().getDocuments().get(0).get("Email").toString();
                            String like = task.getResult().getDocuments().get(0).get("Heart Number").toString();
                            usernameTextView.setText(username);
                            useremailTextview.setText(email);
                            likeNum.setText("  " + like);
                            likeNum.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_list_heart_full, 0, 0, 0);
                        }

                        else{
                            Toast.makeText(getActivity(), "Something Wrong!", Toast.LENGTH_LONG).show();
                        }
                    }
                });


        final TextView focusTime = (TextView) getView().findViewById(R.id.focus_hour);

        mydb.collection("focusTime")
                .document(userID)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        long focusH = (long) documentSnapshot.get("total millis");
                        String focusParsed = parseMillis(focusH);
                        focusTime.setText(focusParsed);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                    }
                });




    }

    // function to format millis
    private String parseMillis(long millis){
        long hour,minute;
        hour = millis / 3600000L;
        minute = (millis % 3600000L)/60000L;
        return String.format("%dh %dm",hour,minute);
    }
}