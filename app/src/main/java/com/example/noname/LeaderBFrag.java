package com.example.noname;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.google.android.gms.common.internal.FallbackServiceBroker;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

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

        // initialize the Map<String, Object> list
        listItems =  new ArrayList<>();

        // load focusTime collection
        focusTimeRef.orderBy("total millis", Query.Direction.DESCENDING)
                    .limit(5L)
                    .get().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Load focusTime documents successfully");
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                // get user's information

                                Map<String, Object> userData = document.getData();
                                Map<String,Object> map = new HashMap<>();
                                String userID = (String) document.getId(); // the documentId is the userId
                                long focusTime = (long) userData.get("total millis");
                                map.put("userID", userID);
                                map.put("focusTime", focusTime);
                                map.put("userName", "");
                                listItems.add(map);
                            }
                            loadUserDoc(); // load user collection
                        }else {
                            Log.d(TAG, "Error loading focusTime documents");
                        }
        });

        return view;
    }

    // callback function for loading user info data
    private void loadUserDoc() {
        if (getActivity() != null && isAdded()) {
            items = new SimpleAdapter(getActivity().getApplicationContext(), listItems, R.layout.fragment_leader_b_item,
                    new String[]{"userName", "focusTime"},
                    new int[]{R.id.leaderB_userName, R.id.leaderB_focusTime});
            list.setAdapter(items);
        }

        // for each user, load the corresponding focusTime document
        for (Map<String, Object> listItem : listItems) {
            String uid = (String) listItem.get("userID");
            userRef.document(uid)
                    .get()
                    .addOnCompleteListener(task1 -> {
                        if (task1.isSuccessful()){
                            Log.d(TAG, "Load user document successfully");
                            DocumentSnapshot userDoc = task1.getResult();
                            Map<String, Object> userData = userDoc.getData();
                            String userName = (String) userData.get("User Name");
                            listItem.put("userName", userName);
                            Log.d(TAG, "new name: " + (String) listItem.get("userName"));
                        }else {
                            Log.d(TAG, "Error loading user document");
                        }
                        items.notifyDataSetChanged();
            });
        }
        // invoke callback function for creating list adapter
        //createListView();
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