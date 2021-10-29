package com.example.noname;


import android.app.DatePickerDialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;

/**
 * A simple {@link Fragment} subclass.
 */
public class ListDetailFrag extends Fragment {

    private static final String TAG = "ListDetailFrag";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private CollectionReference itemsRef;

    private static final Calendar myCalendar = Calendar.getInstance();
    private Boolean isAdd;
    private String docID;
    private String userID;

    Map<String, Object> details;


    public ListDetailFrag(Boolean isAdd, String docID) {
        this.isAdd = isAdd;
        this.docID = docID;


        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        userID = mAuth.getCurrentUser().getUid();
        // get documents from fire store
        itemsRef = db.collection("list");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list_detail, container, false);

        TextView title = view.findViewById(R.id.list_detail_title);
        ImageView backButton = view.findViewById(R.id.list_detail_back);
        TextView dueButton = view.findViewById(R.id.list_detail_due);
        EditText notes = view.findViewById(R.id.list_detail_notes);
        RelativeLayout itemLayout = view.findViewById(R.id.list_detail_subitems);

        view.setVisibility(View.GONE);

        if (isAdd) {
            title.setText("New List");
            dueButton.setText("Due: " + myCalendar.get(Calendar.DAY_OF_MONTH) + "/" +
                    (myCalendar.get(Calendar.MONTH) + 1) + "/" + myCalendar.get(Calendar.YEAR));
        } else {
            // read data
            readData(new FirestoreCallback() {
                @Override
                public void onCallback() {
                    // asynchronous problem
                    title.setText((String) details.get("title"));
                    notes.setText((String) details.get("notes"));
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                    dueButton.setText("Due: " + sdf.format(((Timestamp) details.get("dueDate")).toDate()));

                    itemLayout.removeAllViews();


//                    JSONObject json = new JSONObject(map);
//                    System.out.println(json);
                    view.setVisibility(View.VISIBLE);
                }
            });
        }

        // back picker & save
        backButton.setOnClickListener(v -> {
            String note = notes.getText().toString().trim();


            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new ShopLstFrag()).commit();
        });

        // due date picker
        DatePickerDialog.OnDateSetListener date = (view1, year, monthOfYear, dayOfMonth) -> {
            // TODO Auto-generated method stub
            myCalendar.set(Calendar.YEAR, year);
            myCalendar.set(Calendar.MONTH, monthOfYear);
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            dueButton.setText("Due: " + myCalendar.get(Calendar.DAY_OF_MONTH) + "/" + (myCalendar.get(Calendar.MONTH) + 1) + "/" + myCalendar.get(Calendar.YEAR));
        };

        dueButton.setOnClickListener(v -> {
            // TODO Auto-generated method stub
            new DatePickerDialog(getActivity(), date, myCalendar
                    .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                    myCalendar.get(Calendar.DAY_OF_MONTH)).show();
        });



        ImageView optionButton = view.findViewById(R.id.list_detail_options);
        optionButton.setOnClickListener(v -> {
            //Creating the instance of PopupMenu
            PopupMenu popup = new PopupMenu(getActivity(), optionButton);
            //Inflating the Popup using xml file
            popup.getMenuInflater()
                    .inflate(R.menu.list_add_option_menu, popup.getMenu());

            //registering popup with OnMenuItemClickListener
            popup.setOnMenuItemClickListener(item -> {
                Toast.makeText(
                        getActivity(),
                        "You Clicked : " + item.getTitle(),
                        Toast.LENGTH_SHORT
                ).show();
                return true;
            });

            popup.show(); //showing popup menu
        });

        // Inflate the layout for this fragment
        return view;
    }

    private void readData(FirestoreCallback firestoreCallback) {
        DocumentReference docRef = itemsRef.document(docID);
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        details = document.getData();
                        firestoreCallback.onCallback();
                    } else {
                        Log.d(TAG, "No such document");
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });
    }

    private interface FirestoreCallback {
        void onCallback();
    }
}