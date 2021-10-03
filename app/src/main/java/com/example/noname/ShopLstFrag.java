package com.example.noname;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
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

    private View view;
    private ListView list;
    private ImageView checkbox, atTop;
    private TextView listTitle, dueDate;

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_shopping_list, container, false);
        list = view.findViewById(R.id.list);


        db.collection("list")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Map<String,Object>> allitems = new ArrayList<>();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Map<String,Object> map = new HashMap<>();
                            Map<String, Object> data = document.getData();
                            DateFormat sdf = new SimpleDateFormat("MM dd, yyyy");

                            if((boolean) data.get("top")) {
                                map.put("favorite", R.drawable.ic_list_heart_full);
                            } else {
                                map.put("favorite", R.drawable.ic_list_heart_empty);
                            }

                            String title = data.get("title").toString();
                            String dueDate = data.get("dueDate").toString();
                            SpannableString formatString = new SpannableString(title + "\n"
                                    + dueDate);
                            formatString.setSpan(new AbsoluteSizeSpan(20),
                                    0, title.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            formatString.setSpan(new AbsoluteSizeSpan(15),
                                    title.length()+1, title.length()+dueDate.length()+1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                            map.put("title", formatString);
                            allitems.add(map);
                        }
                        SimpleAdapter items = new SimpleAdapter(getActivity().getApplicationContext(), allitems, R.layout.fragment_shopping_list_item,
                                new String[]{"favorite", "title"},
                                new int[]{R.id.favorite, R.id.listTitle});
                        list.setAdapter(items);
                    } else {

                    }
                });

        return view;
    }
}