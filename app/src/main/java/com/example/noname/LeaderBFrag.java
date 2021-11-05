package com.example.noname;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.google.android.gms.common.internal.FallbackServiceBroker;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

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

    // Used to connect FireStore and FireStorage
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private StorageReference storage;
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
        storage = FirebaseStorage.getInstance().getReference();
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
                    .limit(10L)
                    .get().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Load focusTime documents successfully");
                            int rank = 0;  // used to display rank
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                // get user's information

                                Map<String, Object> userData = document.getData();
                                Map<String,Object> map = new HashMap<>();
                                String userID = (String) document.getId(); // the documentId is the userId
                                long focusTime = (long) userData.get("total millis");
                                String ftParse = parseMillis(focusTime);
                                map.put("userID", userID);
                                map.put("rank", ++rank);
                                map.put("focusTime", ftParse);
                                map.put("userName", "");
                                map.put("favorite", R.drawable.ic_list_heart_full);
                                listItems.add(map);
                            }
                            loadUserInfo(); // load user collection and also the image storage
                        }else {
                            Log.d(TAG, "Error loading focusTime documents");
                        }
        });

        return view;
    }

    // callback function for loading user info data
    private void loadUserInfo() {
        // invoke function for creating list adapter
        createListView();

        // for each item, load the corresponding user info
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
                            long heartNum = (long) userData.get("Heart Number");
                            listItem.put("userName", userName);
                            listItem.put("heartNum", heartNum);
                            Log.d(TAG, "new name: " + (String) listItem.get("userName"));
                        }else {
                            Log.d(TAG, "Error loading user document");
                        }
                        items.notifyDataSetChanged();
            });

        }
    }


    // function for creating listView
    private void createListView(){
        if (getActivity() != null && isAdded()) {
            items = new leaderAdapter(getActivity().getApplicationContext(), listItems, R.layout.fragment_leader_b_item,
                    new String[]{"rank", "userName", "focusTime", "favorite", "heartNum"},
                    new int[]{R.id.leaderB_num, R.id.leaderB_userName, R.id.leaderB_focusTime, R.id.leaderB_favorite, R.id.leaderB_favorite_num});
            list.setAdapter(items);
        }
    }

    // function to format millis
    private String parseMillis(long millis){
        long hour,minute;
        hour = millis / 3600000L;
        minute = (millis % 3600000L)/60000L;
        return String.format("%dh %dm",hour,minute);
    }

    public class leaderAdapter extends SimpleAdapter {
        public leaderAdapter(Context context, List<? extends Map<String, ?>> data, int resource, String[] from, int[] to) {
            super(context, data, resource, from, to);

        }

        //This function is automatically called when the list item view is ready to be display or about to be display.
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);

            // load user avatar
            HashMap<String, Object> data = (HashMap<String, Object>) getItem(position);
            String uid = (String) data.get("userID");
            StorageReference sr = storage.child("users/" + uid + "/avatar.jpg"); // may throw error when this user does not have avatar
            ImageView avatar = view.findViewById(R.id.leaderB_avatar);
            sr.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    Picasso.get().load(uri).into(avatar);
                }
            });


            // click heart to encourage other user
            ImageView imageView=(ImageView) view.findViewById(R.id.leaderB_favorite);
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Toast.makeText(getActivity().getApplicationContext(), "Successfully encourage", Toast.LENGTH_SHORT).show();
                    // locate the current listView item
                    Map<String, Object> curItem = listItems.get(position);
                    // locate the corresponding user document
                    String userID = (String) curItem.get("userID");
                    // get the document reference
                    DocumentReference curDoc = userRef.document(userID);
                    curDoc.update("Heart Number", FieldValue.increment(1));
                    curItem.compute("heartNum", (k,v) -> (Long)v + 1L);
                    notifyDataSetChanged();
                }
            });
            return view;

        }

        @Override
        public int getCount() {
            return listItems.size();
        }
    }
}