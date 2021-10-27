package com.example.noname;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ShopLstFrag#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ShopLstFrag extends Fragment {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private CollectionReference itemsRef;

    private View view;
    private ListView list;
    private ImageView addItem;
    private String userID;

    // A list of items to display
    private List<Map<String,Object>> listItems;

    // Heart
    private SimpleAdapter items;
    private ImageView heart;

    public ShopLstFrag() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ShoppingList.
     */
    // TODO: Rename and change types and number of parameters
    public static ShopLstFrag newInstance() {
        ShopLstFrag fragment = new ShopLstFrag();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void  onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ImageView addButton = getActivity().findViewById(R.id.addItem);
        addButton.setOnClickListener(v -> {
//            getSupportFragmentManager().beginTransaction()
//                    .replace(R.id.fragment_container, new ProfileFrag()).commit();
        });
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

        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_shopping_list, container, false);
        list = view.findViewById(R.id.list);


        addItem = view.findViewById(R.id.addItem);

        // get documents from fire store
        itemsRef = db.collection("list");
        // read data into listView
        readData(new FirestoreCallback() {
            @Override
            public void onCallback(SimpleAdapter items) {
                list.setAdapter(items);
                //list.setClickable(true);
                Log.d("List", "get adapter after setAdapter: " + list.getAdapter().toString());
                list.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                    public void onItemClick(AdapterView adapterView, View view, int position, long id) {
                        Toast.makeText(getActivity().getApplicationContext(), view.toString(), Toast.LENGTH_SHORT).show();

                    }
                });
            }
        });
        return view;
    }

    private void readData(FirestoreCallback firestoreCallback) {
        itemsRef.whereEqualTo("userID", userID)
                .orderBy("dueDate")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Map<String,Object>> favoriteItems = new ArrayList<>();
                        List<Map<String,Object>> normalItems = new ArrayList<>();
                        listItems =  new ArrayList<>();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Map<String,Object> map = new HashMap<>();
                            Map<String, Object> data = document.getData();
                            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");

                            map.put("title", data.get("title").toString());
                            map.put("due", sdf.format(((Timestamp) data.get("dueDate")).toDate()));

                            if((boolean) data.get("favorite")) {
                                map.put("favorite", R.drawable.ic_list_heart_full);
                                favoriteItems.add(map);
                            } else {
                                map.put("favorite", R.drawable.ic_list_heart_empty);
                                normalItems.add(map);
                            }
                        }

                        listItems.addAll(favoriteItems);
                        listItems.addAll(normalItems);
                        //favoriteItems.addAll(normalItems);
                        items = new ListAdapter(getActivity().getApplicationContext(), listItems, R.layout.fragment_shopping_list_item,
                                new String[]{"favorite", "title", "due"},
                                new int[]{R.id.favorite, R.id.title, R.id.due});
                        firestoreCallback.onCallback(items);
                    } else {

                    }
                });
    }

    private interface FirestoreCallback {
        void onCallback(SimpleAdapter items);
    }

    public class ListAdapter extends SimpleAdapter {
        public ListAdapter(Context context, List<? extends Map<String, ?>> data, int resource, String[] from, int[] to) {
            super(context, data, resource, from, to);

        }

        //This function is automatically called when the list item view is ready to be display or about to be display.
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            ImageView imageView=(ImageView) view.findViewById(R.id.favorite);
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Toast.makeText(getActivity().getApplicationContext(), listItems.get(position).get("favorite").toString(), Toast.LENGTH_SHORT).show();
                    if((int) listItems.get(position).get("favorite") == R.drawable.ic_list_heart_full){
                        listItems.get(position).put("favorite", R.drawable.ic_list_heart_empty);
                        notifyDataSetChanged();
                    }else{
                        listItems.get(position).put("favorite", R.drawable.ic_list_heart_full);
                        notifyDataSetChanged();
                    }

                }
            });
            return view;

        }

        @Override
        public int getCount() {
            return super.getCount();
        }
    }

}