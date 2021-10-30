package com.example.noname;


import android.app.DatePickerDialog;
import android.media.Image;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
    private Map<String, Object> subitemMap;
    private Map<String, Object> details;


    // isAdd: add item or load detail
    // docID: document ID
    // title: the new note title
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
        LinearLayout itemLayout = view.findViewById(R.id.list_detail_subitems);

        view.setVisibility(View.GONE);

        if (isAdd) {
            // initialize the page for adding new item
            title.setText("New List"); // should change to new title
            dueButton.setText("Due: " + myCalendar.get(Calendar.DAY_OF_MONTH) + "/" +
                    (myCalendar.get(Calendar.MONTH) + 1) + "/" + myCalendar.get(Calendar.YEAR)); // today

            subitemMap = new HashMap<>();
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

                    // details
                    // key: subitem
                    // value: all item detail map

                    // subitemMap
                    // key: the index of each group
                    // value: the map of title and all items
                    subitemMap = (Map<String, Object>) details.get("subitem");
                    boolean first = true; // default group

                    // get each subitem group and construct view
                    for (int i = 0; i < subitemMap.size(); i++) {
                        // sectionDetails
                        // key1: sectionName
                        // value1: the section name
                        // key2: item
                        // value2: list of item detail map
                        Map<String, Object> sectionDetails = (Map<String, Object>) subitemMap.get(String.valueOf(i));

                        if (first) {
                            first = false; // the default group don't have section title
                        } else {
                            // initialize title of subitem and set option button onclicklistener
                            View view = inflater.inflate(R.layout.fragment_list_detail_subtitle, container, false);
                            EditText note = view.findViewById(R.id.list_detail_subitem_subtitle);
                            note.setText((String) sectionDetails.get("sectionName"));

                            ImageView optionButton = view.findViewById(R.id.list_detail_subitem_options);
                            optionButton.setOnClickListener(v -> {
                                //Creating the instance of PopupMenu
                                PopupMenu popup = new PopupMenu(getActivity(), optionButton);
                                //Inflating the Popup using xml file
                                popup.getMenuInflater()
                                        .inflate(R.menu.list_detail_subitem_option_menu, popup.getMenu());

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
                            itemLayout.addView(view); // add to layout
                        }

                        // get list of item detail map
                        List<Map<String, Object>> allItemDetails = (List<Map<String, Object>>) sectionDetails.get("item");

                        // for each item, set there check image and note
                        for (Map<String, Object> item: allItemDetails) {
                            View view = inflater.inflate(R.layout.fragment_list_detail_subitem, container, false);
                            ImageView check = view.findViewById(R.id.list_detail_subitem_check);
                            EditText itemNotes = view.findViewById(R.id.list_detail_subitem_note);

                            boolean isCheck = (boolean) item.get("isCheck");
                            if(isCheck) {
                                check.setImageResource(R.drawable.ic_list_detail_subitem_full);
                            } else {
                                check.setImageResource(R.drawable.ic_list_detail_subitem_empty);
                            }

                            String note = (String) item.get("note");
                            itemNotes.setText(note);

                            itemLayout.addView(view); // add to layout
                        }
                    }
                    view.setVisibility(View.VISIBLE);
                }
            });
        }

        // back button & save
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
                    .inflate(R.menu.list_detail_option_menu, popup.getMenu());

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


        // set displaying input of add item button
        ImageView addButton = view.findViewById(R.id.list_detail_addItem);
        EditText additemText = view.findViewById(R.id.list_detail_additem_text);
        ImageView additemButton = view.findViewById(R.id.list_detail_additem_commit);

        // make them gone until user click add button
        additemText.setVisibility(View.GONE);
        additemButton.setVisibility(View.GONE);

        addButton.setOnClickListener(v -> {
            if(additemText.getVisibility() == View.VISIBLE && additemButton.getVisibility() == View.VISIBLE){
                additemText.setVisibility(View.GONE);
                additemButton.setVisibility(View.GONE);
            }else{
                additemText.setVisibility(View.VISIBLE);
                additemButton.setVisibility(View.VISIBLE);
            }
        });


        additemButton.setOnClickListener(v -> {
            Map<String, Object> newItem = new HashMap<>();
            String note = additemText.getText().toString().trim();
            newItem.put("isCheck", false);
            newItem.put("note", note);
            int defaultLength = 0; // to display at the end of default list

            if(subitemMap.containsKey("0")) {
                Map<String, Object> defaultGroup = (Map<String, Object>) subitemMap.get("0");
                List<Map<String, Object>> items = (List<Map<String, Object>>) defaultGroup.get("item");
                items.add(newItem);
                defaultGroup.put("item", items);
                defaultLength = items.size() - 1;
            } else {
                Map<String, Object> defaultGroup = new HashMap<>();
                List<Map<String, Object>> items = new LinkedList<>();
                items.add(newItem);
                defaultGroup.put("item", items);
                subitemMap.put("0", defaultGroup);
            }

            View itemView = inflater.inflate(R.layout.fragment_list_detail_subitem, container, false);
            ImageView check = view.findViewById(R.id.list_detail_subitem_check);
            EditText itemNotes = view.findViewById(R.id.list_detail_subitem_note);
            check.setImageResource(R.drawable.ic_list_detail_subitem_empty);
            itemNotes.setText(note);

            itemLayout.addView(itemView, defaultLength);
            view.invalidate(); // refresh
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