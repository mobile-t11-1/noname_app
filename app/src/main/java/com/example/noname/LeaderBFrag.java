package com.example.noname;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static androidx.constraintlayout.motion.utils.Oscillator.TAG;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link LeaderBFrag#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LeaderBFrag extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private Button testBtn;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userID;
    private DocumentReference idRef;

    // Date formatter
    SimpleDateFormat sdf = new SimpleDateFormat("dd/M/yyyy hh:mm:ss");
    Date startDate; // record start date
    int totalFocusTime; // record total focus timme


    public LeaderBFrag() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment LeaderBFrag.
     */
    // TODO: Rename and change types and number of parameters
    public static LeaderBFrag newInstance(String param1, String param2) {
        LeaderBFrag fragment = new LeaderBFrag();
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
        View view = inflater.inflate(R.layout.fragment_leader_b, container, false);
        db = FirebaseFirestore.getInstance();
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        userID = mAuth.getCurrentUser().getUid();
        idRef = db.collection("focusTime").document(userID);

        testBtn = view.findViewById(R.id.test_b);
        testBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Date currentDate = new Date();
//                System.out.println(currentDate);
//
////                String currentTime = Calendar.getInstance().getTime().toString();
////                //System.out.println(currentTime.getTime());
////                System.out.println(currentTime);
//
//                String date = sdf.format(currentDate);
//                System.out.println(date);
//
//                try {
//                    Date formattedDate = sdf.parse(date);
//                    System.out.println(formattedDate);
//                } catch (ParseException e) {
//                    e.printStackTrace();
//                }
                addTimeToDatabase(10);

            }
        });

        return view;
    }
 

    private void addTimeToDatabase(int duration){
        Calendar calendar = Calendar.getInstance();
        Date currentDate = calendar.getTime();
        idRef.get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        DocumentSnapshot document = task.getResult();
                        // if the document exists
                        if (document.exists()){
                            String start = document.getString("start date");
                            try {
                                startDate = sdf.parse(start);
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                            totalFocusTime = document.getLong("total minutes").intValue();

                            // if we the current user has records in database
                            // we update relevant field depending on the timestamps (within a week)
                            Map<String, Object> update = createUpdateData(startDate, currentDate, totalFocusTime, duration);
                            // then we update this map into firestore
                            idRef.update(update)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void unused) {
                                            Log.d(TAG, "Successfully Updated!");
                                        }
                                    });

                        }
                        // if there is no such document in firestore
                        else{
                            // we create a new record
                            Map<String, Object> record = new HashMap<>();
                            String date = sdf.format(currentDate);
                            record.put("start date", date);
                            record.put("last date", date);
                            record.put("total minutes", duration);

                            // add the new record into Firestore
                            db.collection("focusTime").document(userID)
                                    .set(record)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void unused) {
                                            Log.d(TAG, "New record stored!");
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.d(TAG, "Failed to store");
                                        }
                                    });
                        }
                    }
                });
    }



    private Map<String, Object> createUpdateData(Date startDate, Date currentDate, int totalTime, int duration){
        Map<String, Object> update = new HashMap<>();
        // convert date into string format
        String date = sdf.format(currentDate);

        // if within a week, we only update focus time and thisDate
        if(checkDuration(startDate,currentDate)){
            update.put("last date", date);
            update.put("total minutes", totalTime + duration);
        }else {
            update.put("start date", date);
            update.put("last date", date);
            update.put("total minutes", duration);
        }

        return update;

    }


    private boolean checkDuration(Date startDate, Date currentDate){
        //milliseconds
        long different = currentDate.getTime() - startDate.getTime();

        long secondsInMilli = 1000;
        long minutesInMilli = secondsInMilli * 60;
        long hoursInMilli = minutesInMilli * 60;
        long daysInMilli = hoursInMilli * 24;
        long elapsedDays = different / daysInMilli;

        if (elapsedDays < 7){
            return true;
        }

        return false;
    }



}