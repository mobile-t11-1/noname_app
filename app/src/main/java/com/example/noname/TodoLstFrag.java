package com.example.noname;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

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

/**
 * The To-do List fragment
 */
public class TodoLstFrag extends Fragment {

    // TAG for locate log.d
    private static final String TAG = "List";

    // Used to connect Firestore
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private CollectionReference itemsRef;

    private View view;
    private ListView list;
    private TextView emptyPrompt;
    private ImageView addItem;
    private String userID;

    // View of adding list
    private EditText addList;
    private ImageView addListCommit;

    // A list of items to display
    private List<Map<String,Object>> listItems;

    // The adapter of the listView
    private SimpleAdapter items;

    public TodoLstFrag() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v, @Nullable ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.list_item_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        AdapterView.AdapterContextMenuInfo acmi = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        Map<String,Object> itemAtPosition = (Map<String,Object>) list.getItemAtPosition(acmi.position);
        Log.v("long clicked",String.valueOf(itemAtPosition.get("position")));

        if(item.getItemId() == R.id.delete){
            //Toast.makeText(getActivity().getApplicationContext(), "Delete Clicked", Toast.LENGTH_LONG).show();

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Confirmation");
            builder.setMessage("Do you want to delete this list?");
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();

                    // Delete from listView
                    // First we update other item's position value, all items at the back of the target position are pushed forward by 1
                    for (Map<String, Object> listItem : listItems) {
                        if ((Long)listItem.get("position") > (Long)itemAtPosition.get("position")){
                            listItem.compute("position", (k,v) -> (Long)v - 1L);
                        }else {
                            continue;
                        }
                    }
                    // Then do delete, and notify the adapter
                    long l = (Long) itemAtPosition.get("position");
                    int pos = (int) l ;
                    listItems.remove(pos);
                    items.notifyDataSetChanged();

                    if (listItems.size() == 0) {
                        list.setVisibility(View.GONE);
                        emptyPrompt.setVisibility(View.VISIBLE);
                    } else {
                        list.setVisibility(View.VISIBLE);
                        emptyPrompt.setVisibility(View.GONE);
                    }


                    // Delete from server
                    itemsRef.document((String) itemAtPosition.get("docID"))
                            .delete()
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Log.d(TAG, "List item successfully deleted!");
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.w(TAG, "Error deleting List item", e);
                                }
                            });

                    // Update the changes of position of remain items
                    posUpdate();

                }
            });
            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                }
            });
            AlertDialog alert = builder.create();
            alert.show();
        }else{
            return false;
        }
        return true;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        userID = mAuth.getCurrentUser().getUid();

        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_todo_list, container, false);
        list = view.findViewById(R.id.list_main);
        emptyPrompt = view.findViewById(R.id.list_empty_prompt);
        registerForContextMenu(list); // register listView for context menu

        // Set the add button
        ImageView addButton = view.findViewById(R.id.list_main_addItem);
        addButton.setOnClickListener(v -> {
            // Show or hide the input box
            if(addList.getVisibility() == View.VISIBLE && addListCommit.getVisibility() == View.VISIBLE){
                addList.setVisibility(View.GONE);
                addListCommit.setVisibility(View.GONE);
            }else{
                addList.setVisibility(View.VISIBLE);
                addListCommit.setVisibility(View.VISIBLE);
            }
        });

        addItem = addButton;

        view.setVisibility(View.GONE);

        // set view of adding list
        addList = view.findViewById(R.id.list_add_text);
        addListCommit = view.findViewById(R.id.list_add_commit);

        // set add item page jump
        addListCommit.setOnClickListener( v -> {
            // randomly generate document ID for this new ListItem
            DocumentReference newListItem = itemsRef.document();
            String newDocID = newListItem.getId();

            // create data
            String titleValue = addList.getText().toString().trim(); // user input
            String notesValue = ""; //default notes
            Timestamp dueValue = new Timestamp(new Date()); // default dueDate
            Timestamp createTimeValue = new Timestamp(new Date());
            long posValue = listItems.size();  // new item will be the last one in the listView
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
            dataMap.put("subitems", new ArrayList<>());

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
            adapterItem.put("subitems", new ArrayList<>());

            // update to the listItems for adapter
            listItems.add(adapterItem);
            items.notifyDataSetChanged();

            // finish input
            addList.setImeOptions(EditorInfo.IME_ACTION_DONE);  // hide the keyboard
            addList.getText().clear();  // clean input

            // hide the EditText
            addList.setVisibility(View.GONE);
            addListCommit.setVisibility(View.GONE);

            list.setVisibility(View.VISIBLE);
            emptyPrompt.setVisibility(View.GONE);


            // jump to list detail fragment
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new ListDetailFrag(newDocID, TodoLstFrag.this)).commit();
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
                        //Toast.makeText(getActivity().getApplicationContext(), ""+position, Toast.LENGTH_SHORT).show();
                        Map<String,Object> detail = listItems.get(position);
                        String docID = (String) detail.get("docID");


                        getActivity().getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, new ListDetailFrag(docID, TodoLstFrag.this)).commit();
                    }
                });
                // show or hide the empty listView prompt
                if (listItems.size() == 0) {
                    list.setVisibility(View.GONE);
                    emptyPrompt.setVisibility(View.VISIBLE);
                } else {
                    list.setVisibility(View.VISIBLE);
                    emptyPrompt.setVisibility(View.GONE);
                }

                view.setVisibility(View.VISIBLE);
            }
        });
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // listen for anywhere except editText to hide the add_list_commit
        listenAnyWhere(view);
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
                            Log.d(TAG, data.get("position").toString());
                            Log.d(TAG, data.get("position").getClass().toString());
                            // record the docID
                            map.put("docID", docID);

                            // check if this list needs to be topped
                            if((boolean) data.get("favorite")) {
                                map.put("favorite", R.drawable.ic_list_heart_full);
                                favoriteItems.add(map);
                            } else {
                                map.put("favorite", R.drawable.ic_list_heart_empty);
                                normalItems.add(map);
                            }

                            // data used to put in the adapter
                            listItems.add(map);
                        }

                        // create the adapter for listView
                        if (getActivity() != null && isAdded()) {
                            items = new ListAdapter(getActivity().getApplicationContext(), listItems, R.layout.fragment_todo_list_item,
                                    new String[]{"favorite", "title", "due"},
                                    new int[]{R.id.list_item_favorite, R.id.list_item_title, R.id.list_item_due});
                            firestoreCallback.onCallback(items);
                        }
                    } else {
                        Log.d(TAG, "Failed to read database");
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
            ImageView imageView = view.findViewById(R.id.list_item_favorite);
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
//                    Toast.makeText(getActivity().getApplicationContext(), "Successfully topped", Toast.LENGTH_SHORT).show();
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
                        // sort by heart
                        heartSort();
                    }else{
                        curItem.put("favorite", R.drawable.ic_list_heart_full);
                        // make the latest heart to be the top one (instead of only using heartSort)
                        for (Map<String, Object> item : listItems) {
                            if ((Long)item.get("position") < (Long)curItem.get("position")){
                                item.compute("position", (k,v) -> (Long)v + 1L);
                            }else {
                                continue;
                            }
                        }
                        curItem.put("position",(Long) 0L);

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

                        // sort by position
                        positionSort();
                    }

                    // call posUpdate function to update the position of each item
                    posUpdate();
                    notifyDataSetChanged();
                }
            });

            TextView title = view.findViewById(R.id.list_item_title);
            String dueTimeString = (String) (listItems.get(position)).get("due");
            try {
                Date dueDate = new SimpleDateFormat("MMM dd, yyyy").parse(dueTimeString);
                Date today = new Date();
                today.setHours(0);
                today.setMinutes(0);
                today.setSeconds(0);
                long different = dueDate.getTime() - today.getTime();
                long daysInMilli = 1000 * 60 * 60 * 24 - 1000;
                long differentDay = different / daysInMilli;
                if (differentDay < 0) {
                    title.setTextColor(Color.parseColor("#7E8398")); // gray
                } else if (differentDay == 0) {
                    title.setTextColor(Color.parseColor("#DA7E70")); // red
                } else {
                    title.setTextColor(Color.parseColor("#f5a824")); // orange
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }

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

    // abstract the logic of sorting by position
    private void positionSort(){
        // sort the listItems by favorite
        Collections.sort(listItems,new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                return Long.compare((Long) o1.get("position"),(Long) o2.get("position"));
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
            item.put("position", (long) pos);  // also update the local one
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

    // This method is used to allow any click to collapse the input box
    public void listenAnyWhere(View view) {
        //Set up touch listener for non-EditText views to hide keyboard and itself
        if (!(view instanceof EditText)) {
            view.setOnTouchListener(new View.OnTouchListener() {

                public boolean onTouch(View v, MotionEvent event) {
                    hideEditText();
                    view.findViewById(R.id.list_items_layout).requestFocus();
                    return false;
                }

            });
        }
    }

    // This method is used to hide the input bos
    public void hideEditText() {
        // hide the keyboard
        InputMethodManager inputMethodManager = (InputMethodManager)  getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
        if(getActivity().getCurrentFocus() != null) {
            inputMethodManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
        }

        // finish input
        addList.setImeOptions(EditorInfo.IME_ACTION_DONE);  // hide the keyboard
        addList.getText().clear();  // clean input

        // hide the EditText
        addList.setVisibility(View.GONE);
        addListCommit.setVisibility(View.GONE);
    }
}