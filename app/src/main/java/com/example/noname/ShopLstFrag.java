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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
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

    // TAG for locate log.d
    private static final String TAG = "List";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private CollectionReference itemsRef;

    private View view;
    private ListView list;
    private ImageView addItem;
    private String userID;

    // View of adding list
    private EditText addList;
    private ImageView addListCommit;

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
        list = view.findViewById(R.id.list_main);

        // set add item page jump
        ImageView addButton = view.findViewById(R.id.list_main_addItem);
        addButton.setOnClickListener(v -> {
            if(addList.getVisibility() == View.VISIBLE && addListCommit.getVisibility() == View.VISIBLE){
                addList.setVisibility(View.GONE);
                addListCommit.setVisibility(View.GONE);
            }else{
                addList.setVisibility(View.VISIBLE);
                addListCommit.setVisibility(View.VISIBLE);
            }
//            getActivity().getSupportFragmentManager().beginTransaction()
//                    .replace(R.id.fragment_container, new ListDetailFrag(true, "")).commit();
        });

        addItem = addButton;

        // set view of adding list
        addList = view.findViewById(R.id.list_add_text);
        addListCommit = view.findViewById(R.id.list_add_commit);

        addListCommit.setOnClickListener( v -> {
            // randomly generate document ID for this new ListItem
            DocumentReference newListItem = itemsRef.document();
            String newDocID = newListItem.getId();

            // create data
            String titleValue = addList.getText().toString().trim(); // user input
            String notesValue = ""; //default notes
            Timestamp dueValue = new Timestamp(new Date()); // default dueDate
            Timestamp createTimeValue = new Timestamp(new Date());
            int posValue = listItems.size();  // new item will be the last one in the listView
            boolean favoriteValue = false; // default favorite -> false
            String userIDValue = userID;

            // create dataMap for server
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("createTime", createTimeValue);
            dataMap.put("dueDate", dueValue);
            dataMap.put("favorite", favoriteValue);
            dataMap.put("notes", notesValue);
            dataMap.put("position", posValue);
            dataMap.put("title", titleValue);
            dataMap.put("userID", userIDValue);
            dataMap.put("docID", newDocID);

            // update to the server
            newListItem.set(dataMap)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d(TAG, "New Document is successfully added!");
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w(TAG, "Error adding new document", e);
                            }
                        });

            // create dataMap for adapter
            Map<String, Object> adapterItem = new HashMap<>();
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");
            adapterItem.put("createTime", sdf.format(createTimeValue.toDate()));
            adapterItem.put("due", sdf.format(dueValue.toDate()));
            adapterItem.put("favorite", R.drawable.ic_list_heart_empty);
            adapterItem.put("notes", notesValue);
            adapterItem.put("position", posValue);
            adapterItem.put("title", titleValue);
            adapterItem.put("userID", userIDValue);
            adapterItem.put("docID", newDocID);

            // update to the listItems for adapter
            listItems.add(adapterItem);
            items.notifyDataSetChanged();

            // hidden the EditText
            addList.setVisibility(View.GONE);
            addListCommit.setVisibility(View.GONE);
        });

        // make them gone until user click add button
        addList.setVisibility(View.GONE);
        addListCommit.setVisibility(View.GONE);


        // set sort button
        ImageView sortButton = view.findViewById(R.id.list_sort);
        sortButton.setOnClickListener(v -> {
            //Creating the instance of PopupMenu
            PopupMenu popup = new PopupMenu(getActivity(), sortButton);
            //Inflating the Popup using xml file
            popup.getMenuInflater()
                    .inflate(R.menu.list_sort_menu, popup.getMenu());

            //registering popup with OnMenuItemClickListener
            popup.setOnMenuItemClickListener(item -> {
                // item logic
                Toast.makeText(
                        getActivity(),
                        "You Clicked : " + item.getTitle(),
                        Toast.LENGTH_SHORT
                ).show();

                // sorting by due date
                if(item.getItemId() == R.id.list_sort_byDue){
                    dueSort();
                    items.notifyDataSetChanged();
                    posUpdate();
                }

                return true;
            });

            popup.show(); //showing popup menu
        });

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
                        Toast.makeText(getActivity().getApplicationContext(), ""+position, Toast.LENGTH_SHORT).show();
                        Map<String,Object> detail = listItems.get(position);
                        String docID = (String) detail.get("docID");
                        docID = "0Tk3w2rIvq1UtnNUGSdo";

                        getActivity().getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, new ListDetailFrag(false, docID, ShopLstFrag.this)).commit();
                    }
                });
            }
        });
        return view;
    }

    private void readData(FirestoreCallback firestoreCallback) {
        itemsRef.whereEqualTo("userID", userID)
                .orderBy("position")  // sort by item position
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Load database successfully");
                        List<Map<String,Object>> favoriteItems = new ArrayList<>();
                        List<Map<String,Object>> normalItems = new ArrayList<>();
                        listItems =  new ArrayList<>();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String docID = document.getId();
                            Map<String,Object> map = new HashMap<>();
                            Map<String, Object> data = document.getData();
                            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");


                            map.put("title", data.get("title").toString());
                            map.put("due", sdf.format(((Timestamp) data.get("dueDate")).toDate()));

                            // record the item position
                            map.put("position", data.get("position"));
                            // record the docID
                            map.put("docID", docID);

                            if((boolean) data.get("favorite")) {
                                map.put("favorite", R.drawable.ic_list_heart_full);
                                favoriteItems.add(map);
                            } else {
                                map.put("favorite", R.drawable.ic_list_heart_empty);
                                normalItems.add(map);
                            }

                            listItems.add(map);
                        }


                        items = new ListAdapter(getActivity().getApplicationContext(), listItems, R.layout.fragment_shopping_list_item,
                                new String[]{"favorite", "title", "due"},
                                new int[]{R.id.list_item_favorite, R.id.list_item_title, R.id.list_item_due});
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
            ImageView imageView=(ImageView) view.findViewById(R.id.list_item_favorite);
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Toast.makeText(getActivity().getApplicationContext(), listItems.get(position).get("favorite").toString(), Toast.LENGTH_SHORT).show();
                    // locate the current listView item
                    Map<String, Object> curItem = listItems.get(position);
                    // locate the corresponding document id of this current item
                    //String curDocID = itemToDoc.get(curItem);
                    String curDocID = (String) curItem.get("docID");
                    // get the document reference
                    DocumentReference curDoc = itemsRef.document(curDocID);

                    // click heart logic
                    if((int) curItem.get("favorite") == R.drawable.ic_list_heart_full){
                        curItem.put("favorite", R.drawable.ic_list_heart_empty);
                        // The hashcode of curItem may change, so re-put it
                        curDoc.update("favorite", false)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        Log.d(TAG, "DocumentSnapshot successfully updated!");
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.w(TAG, "Error updating document", e);
                                    }
                                });
                    }else{
                        curItem.put("favorite", R.drawable.ic_list_heart_full);
                        curDoc.update("favorite", true)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        Log.d(TAG, "DocumentSnapshot successfully updated!");
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.w(TAG, "Error updating document", e);
                                    }
                                });
                    }

                    // sorting by favorite
                    heartSort();
                    // call posUpdate function to update the position of each item
                    posUpdate();
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
    // abstract the logic of sorting by favorite
    private void heartSort(){
        // sort the listItems by favorite
        Collections.sort(listItems,new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                return -Integer.compare((Integer) o1.get("favorite"),(Integer)o2.get("favorite"));
            }
        });
    }

    // abstract the logic of sorting by due date
    private void dueSort(){
        // use to check how many favorite item on the top
        int p = 0;
        for (Map<String, Object> listItem : listItems) {
            if((Integer) listItem.get("favorite") == R.drawable.ic_list_heart_full){
                p++;
            }
        }


        // sort the favorite items by due date
        Collections.sort(listItems.subList(0,p),new Comparator<Map<String, Object>>() {
            DateFormat f = new SimpleDateFormat("MMM dd, yyyy");
            @Override
            public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                try {
                    return f.parse((String) o1.get("due")).compareTo(f.parse((String) o2.get("due")));
                } catch (ParseException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        });

        // sort the unfavorite items by due date
        Collections.sort(listItems.subList(p,listItems.size()),new Comparator<Map<String, Object>>() {
            DateFormat f = new SimpleDateFormat("MMM dd, yyyy");
            @Override
            public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                try {
                    return f.parse((String) o1.get("due")).compareTo(f.parse((String) o2.get("due")));
                } catch (ParseException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        });
    }

    // abstract the position update logic
    private void posUpdate(){
        // update the position field of corresponding document
        for(int pos=0; pos < listItems.size(); pos++){
            // get the corresponding document of this item
            Map<String, Object> item = listItems.get(pos);
            String docID = (String) item.get("docID");
            DocumentReference docRef = itemsRef.document(docID);
            // update the new position of each item
            docRef.update("position", pos)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Log.d(TAG, "item position successfully updated!");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error updating item position", e);
                        }
                    });
        }
    }
}