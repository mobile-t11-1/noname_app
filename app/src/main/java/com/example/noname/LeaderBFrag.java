package com.example.noname;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.google.android.gms.common.internal.FallbackServiceBroker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Leaderboard fragment
 */
public class LeaderBFrag extends Fragment {

    private static final String TAG  = "Leaderboard";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private CollectionReference userRef;
    private CollectionReference focusTimeRef;

    private View view;
    private ListView list;
    private String userID;
    private SimpleAdapter items;

    // A list of items to display
    private List<Map<String,Object>> listItems;

    // used two flags to handle asynchronous issues when loading double collection
    private boolean loadUser;
    private boolean loadFt;


    public LeaderBFrag() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        userID = mAuth.getCurrentUser().getUid();

        // inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_leader_b, container, false);
        list = view.findViewById(R.id.leaderB_list);

        // get user documents and focusTime documents from fire store
        userRef = db.collection("user");
        focusTimeRef = db.collection("focusTime");

        // set flags
        loadUser = false;
        loadFt = false;

        // load data
        listItems =  new ArrayList<>();


        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Load user documents successfully");
                for (QueryDocumentSnapshot document : task.getResult()) {
                    // get user's information
                    Map<String, Object> userData = document.getData();
                    Map<String,Object> map = new HashMap<>();
                    String userID = (String) userData.get("User ID");
                    String userName = (String) userData.get("User Name");
                    long focusTime = 0L;
                    map.put("userID", userID);
                    map.put("userName", userName);
                    map.put("focusTime", focusTime);
                    listItems.add(map);
                }
                loadFocusDoc(); // load focusTime
            }else {
                Log.d(TAG, "Error loading user documents");
            }
        });

        // waiting for loading user documents
//        while(!loadUser){
//            Log.d(TAG, "loading user documents");
//        }
//        Log.d(TAG, "loading user documents finish");



        // waiting for loading focusTime documents
//        while(!loadUser && !loadFt){
//            Log.d(TAG, "loading focusTime documents");
//        }
        return view;
    }

    // callback function for loading focusTime data
    private void loadFocusDoc() {
        // for each user, load the corresponding focusTime document
        for (Map<String, Object> listItem : listItems) {
            String uid = (String) listItem.get("userID");
            focusTimeRef.document(uid)
                    .get().addOnCompleteListener(task1 -> {
                if (task1.isSuccessful()){
                    Log.d(TAG, "Load user's focusTime document successfully");
                    DocumentSnapshot ftDocument = task1.getResult();
                    Map<String, Object> ftData = ftDocument.getData();
                    long focusTime = (long) ftData.get("total millis");
                    listItem.put("focusTime", focusTime);
                }else {
                    Log.d(TAG, "Error loading user's focusTime document");
                }
            });
        }

        // invoke callback function for creating list adapter
        createListView();
    }


    // callback function for creating listView
    private void createListView(){
        if (getActivity() != null && isAdded()) {
            items = new SimpleAdapter(getActivity().getApplicationContext(), listItems, R.layout.fragment_leader_b_item,
                    new String[]{"userName", "focusTime"},
                    new int[]{R.id.leaderB_userName, R.id.leaderB_focusTime});
            list.setAdapter(items);
        }
    }
}