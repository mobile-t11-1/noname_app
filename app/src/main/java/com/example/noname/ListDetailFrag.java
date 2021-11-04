package com.example.noname;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.io.PipedOutputStream;
import java.io.PipedInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A simple {@link Fragment} subclass.
 */
public class ListDetailFrag extends Fragment {
    private static final String TAG = "ListDetailFrag";
    private ShopLstFrag parent; // hold parent reference after constructing this object

    // database
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private CollectionReference itemsRef;

    // the status of this object
    private static final Calendar myCalendar = Calendar.getInstance();
    private String docID;
    private String userID;
    private Map<String, Object> docDetails;
    private DocumentReference docRef;

    // views of this object
    private EditText title;
    private ImageView backButton;
    private ImageView optionButton;
    private EditText notes;
    private TextView dueButton;
    private TextView emptyPrompt;
    private ListView subitem;
    private EditText additemText;
    private ImageView additemCommit;
    private ImageView additemButton;

    // A list of items to display
    private List<Map<String,Object>> subItems;
    private SimpleAdapter items;

    // docID: document ID
    // title: the new note title
    public ListDetailFrag(String docID, ShopLstFrag parent) {
        this.docID = docID;
        this.parent = parent;

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        userID = mAuth.getCurrentUser().getUid();
        itemsRef = db.collection("list"); // get documents from fire store
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        docRef = itemsRef.document(docID);

        // get the detail page
        View view = inflater.inflate(R.layout.fragment_list_detail, container, false);

        // get all views
        title = view.findViewById(R.id.list_detail_title);
        backButton = view.findViewById(R.id.list_detail_back);
        optionButton = view.findViewById(R.id.list_detail_options);
        notes = view.findViewById(R.id.list_detail_notes);
        dueButton = view.findViewById(R.id.list_detail_due);
        emptyPrompt = view.findViewById(R.id.list_detail_empty_prompt);

        subitem = view.findViewById(R.id.list_detail_subitem);
        additemText = view.findViewById(R.id.list_detail_additem_text);
        additemCommit = view.findViewById(R.id.list_detail_additem_commit);
        additemButton = view.findViewById(R.id.list_detail_addItem);

        // set view invisible until all data are loaded
        view.setVisibility(View.GONE);

        // make them gone until user click add button
        additemText.setVisibility(View.GONE);
        additemCommit.setVisibility(View.GONE);

        // listen for anywhere except editText to hide the add_list_commit
        listenAnyWhere(view);

        // read data and initialize page for different flag:
        // add new item or display the item details
        readData(items -> {
            // initialize new page for adding, or load data for displaying
            title.setText((String) docDetails.get("title"));
            notes.setText((String) docDetails.get("notes"));
            myCalendar.setTime(((Timestamp) docDetails.get("dueDate")).toDate());

            dueButton.setText("Due: " + myCalendar.get(Calendar.DAY_OF_MONTH) + "/" +
                    (myCalendar.get(Calendar.MONTH) + 1) + "/" + myCalendar.get(Calendar.YEAR));

            if (subItems.size() == 0) {
                // no item, display empty prompt
                emptyPrompt.setVisibility(View.VISIBLE);
                subitem.setVisibility(View.GONE);
            } else {
                // display all subitems
                emptyPrompt.setVisibility(View.GONE);
                subitem.setVisibility(View.VISIBLE);
                subitem.setAdapter(items);
                setListViewHeightBasedOnChildren(subitem); // set the height of the list
            }

            view.setVisibility(View.VISIBLE); // all data is loaded, make view visible
        });

        // back button
        backButton.setOnClickListener(v -> {
            // return to list main page
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, parent).commit();
        });

        // due date picker
        DatePickerDialog.OnDateSetListener date = (view1, year, monthOfYear, dayOfMonth) -> {
            myCalendar.set(Calendar.YEAR, year);
            myCalendar.set(Calendar.MONTH, monthOfYear);
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            dueButton.setText("Due: " + myCalendar.get(Calendar.DAY_OF_MONTH) + "/" +
                    (myCalendar.get(Calendar.MONTH) + 1) + "/" + myCalendar.get(Calendar.YEAR));

            // update database
            docRef.update("dueDate", new com.google.firebase.Timestamp(myCalendar.getTime()))
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

        };
        dueButton.setOnClickListener(v -> {
            new DatePickerDialog(getActivity(), date, myCalendar
                    .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                    myCalendar.get(Calendar.DAY_OF_MONTH)).show();
        });


        // option menu
        optionButton.setOnClickListener(v -> {
            // Creating the instance of PopupMenu
            PopupMenu popup = new PopupMenu(getActivity(), optionButton);
            // Inflating the Popup using xml file
            popup.getMenuInflater()
                    .inflate(R.menu.list_detail_option_menu, popup.getMenu());

            // registering popup with OnMenuItemClickListener
            popup.setOnMenuItemClickListener(item -> {
                String title = item.getTitle().toString();
                List<Map<String, Object>> databaseMap = (List<Map<String, Object>>) docDetails.get("subitems");

                // do operations according to the option
                switch(title) {
                    case "Check All Items":
                        // update view
                        for (Map<String, Object> eachItem:subItems) {
                            eachItem.put("isCheck", R.drawable.ic_list_detail_subitem_full);
                        }

                        // update database
                        for (Map<String, Object> eachItem:databaseMap) {
                            eachItem.put("isCheck", true);
                        }
                        docRef.update("subitems", docDetails.get("subitems"))
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
                        break;
                    case "Uncheck All Items":
                        // update view
                        for (Map<String, Object> eachItem:subItems) {
                            eachItem.put("isCheck", R.drawable.ic_list_detail_subitem_empty);
                        }

                        // update database
                        for (Map<String, Object> eachItem:databaseMap) {
                            eachItem.put("isCheck", false);
                        }
                        docRef.update("subitems", docDetails.get("subitems"))
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
                        break;
                    default:
                        // delete list
                        // popup confirmation window
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setTitle("Confirmation");
                        builder.setMessage("Do you want to delete this list?");
                        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();

                                // Delete from server
                                itemsRef.document(docID)
                                        .delete()
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                getActivity().getSupportFragmentManager().beginTransaction()
                                                        .replace(R.id.fragment_container, parent).commit();
                                                Log.d(TAG, "List item successfully deleted!");
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Log.w(TAG, "Error deleting List item", e);
                                            }
                                        });

                            }
                        });
                        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        });
                        AlertDialog alert = builder.create();
                        alert.show();
                }

                items.notifyDataSetChanged(); // update view
                return true;
            });

            popup.show(); //showing popup menu
        });

        // show or hide the add bar after clicking add item button
        additemButton.setOnClickListener(v -> {
            if(additemText.getVisibility() == View.VISIBLE && additemCommit.getVisibility() == View.VISIBLE){
                additemText.setVisibility(View.GONE);
                additemCommit.setVisibility(View.GONE);
            }else{
                additemText.setVisibility(View.VISIBLE);
                additemCommit.setVisibility(View.VISIBLE);
            }
        });

        // update view and database
        additemCommit.setOnClickListener(v -> {
            Map<String, Object> newItem = new HashMap<>();
            String note = additemText.getText().toString().trim();
            newItem.put("isCheck", R.drawable.ic_list_detail_subitem_empty);
            newItem.put("note", note);
            newItem.put("position", subItems.size());

            List<Map<String, Object>> docDetail = (List<Map<String, Object>>) docDetails.get("subitems");
            Map<String, Object> newItemDetail = new HashMap<>();
            newItemDetail.put("isCheck", false);
            newItemDetail.put("note", note);
            newItemDetail.put("position", subItems.size());

            subItems.add(newItem);
            docDetail.add(newItemDetail);

            docRef.update("subitems", docDetails.get("subitems"))
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

            items.notifyDataSetChanged();

            // finish input
            additemText.setImeOptions(EditorInfo.IME_ACTION_DONE);  // hide the keyboard
            additemText.getText().clear();  // clean input

            // hide the EditText
            additemText.setVisibility(View.GONE);
            additemCommit.setVisibility(View.GONE);

            emptyPrompt.setVisibility(View.GONE);
            subitem.setVisibility(View.VISIBLE);
            subitem.setAdapter(items);
            setListViewHeightBasedOnChildren(subitem);
        });

        // Inflate the layout for this fragment
        return view;
    }

    @Override
    public void onStop() {
        super.onStop();
        List<Map<String,Object>> updateList = (List<Map<String,Object>>) docDetails.get("subitems");
        for (int i = 0; i < updateList.size(); i++) {
            updateList.get(i).put("note", ((Map)items.getItem(i)).get("note"));
        }


        // update note
        docRef.update("notes", notes.getText().toString().trim())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
//                        Log.d(TAG, "DocumentSnapshot successfully updated!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
//                        Log.w(TAG, "Error updating document", e);
                    }
                });

        // Delete from server
        docRef.update("subitems", docDetails.get("subitems"))
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
//                        Log.d(TAG, "DocumentSnapshot successfully updated!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
//                        Log.w(TAG, "Error updating document", e);
                    }
                });

        docRef.update("title", title.getText().toString().trim())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
        //                        Log.d(TAG, "DocumentSnapshot successfully updated!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
//                        Log.w(TAG, "Error updating document", e);
                    }
                });

    }

    // reference from https://stackoverflow.com/questions/3495890/how-can-i-put-a-listview-into-a-scrollview-without-it-collapsing
    private void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = (ListAdapter) listView.getAdapter();
        if (listAdapter == null) {
            // pre-condition
            return;
        }

        int totalHeight = listView.getPaddingTop() + listView.getPaddingBottom();

        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);
            if (listItem instanceof ViewGroup) {
                listItem.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            }

            listItem.measure(0, 0);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
    }

    private void readData(FirestoreCallback firestoreCallback) {
        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                docDetails = task.getResult().getData();
                // deep copy to create new list, store the source url of check image
                subItems = (List<Map<String, Object>>) PipedDeepCopy.copy(docDetails.get("subitems"));
                for (Map<String, Object> eachitem: subItems) {
                    if((boolean) eachitem.get("isCheck")) {
                        eachitem.put("isCheck", R.drawable.ic_list_detail_subitem_full);
                    } else {
                        eachitem.put("isCheck", R.drawable.ic_list_detail_subitem_empty);
                    }
                }

                // sort item according to their positions
                positionSort();

                if(getActivity() != null) {
                    items = new ListAdapter(getActivity().getApplicationContext(), subItems, R.layout.fragment_list_detail_subitem,
                            new String[]{"isCheck", "note"},
                            new int[]{R.id.list_detail_subitem_check, R.id.list_detail_subitem_note});
                }

                firestoreCallback.onCallback(items);
            } else {
                Log.d(TAG, "No such document");
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
            ImageView check = (ImageView) view.findViewById(R.id.list_detail_subitem_check);
            TextView text = view.findViewById(R.id.list_detail_subitem_note);
            check.setOnClickListener(view1 -> {
                // locate the current listView item
                Map<String, Object> curItem = subItems.get(position);

                Map<String, Object> curMap = ((List<Map<String, Object>>) docDetails.get("subitems")).get(position);
                // click check logic
                if((int) curItem.get("isCheck") == R.drawable.ic_list_detail_subitem_empty){
                    curItem.put("isCheck", R.drawable.ic_list_detail_subitem_full);
                    curMap.put("isCheck", true);
                } else {
                    curItem.put("isCheck", R.drawable.ic_list_detail_subitem_empty);
                    curMap.put("isCheck", false);
                }

                docRef.update("subitems", docDetails.get("subitems"))
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

                notifyDataSetChanged();
            });

            text.setOnLongClickListener(v -> {
                // long click delete item
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("Confirmation");
                builder.setMessage("Do you want to delete this item?");
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        subItems.remove(position);
                        ((List<Map<String, Object>>) docDetails.get("subitems")).remove(position);

                        // Delete from server
                        docRef.update("subitems", docDetails.get("subitems"))
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
                        items.notifyDataSetChanged();
                    }
                });
                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
                AlertDialog alert = builder.create();
                alert.show();
                return false;
            });

            text.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    // update note in local variable
                    subItems.get(position).put("note", s.toString());
                }
            });
            return view;

        }

        @Override
        public int getCount() {
            return subItems.size();
        }
    }

    // abstract the logic of sorting by position
    private void positionSort(){
        // sort the listItems by favorite
        Collections.sort(subItems,new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                return Long.compare((Long) o1.get("position"),(Long) o2.get("position"));
            }
        });
    }

    // listen all views except of EditText
    // support cancel editing when user click other area
    public void listenAnyWhere(View view) {
        //Set up touch listener for non-EditText views to hide keyboard and itself
        if (!(view instanceof EditText)) {
            view.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    hideEditText();
                    subitem.requestFocus();
                    return false;
                }

            });
        }
    }

    public void hideEditText() {
        // hide the keyboard
        InputMethodManager inputMethodManager = (InputMethodManager)  getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
        if(getActivity().getCurrentFocus() != null) {
            inputMethodManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
        }

        // finish input
        additemText.setImeOptions(EditorInfo.IME_ACTION_DONE);  // hide the keyboard
        additemText.getText().clear();  // clean input

        // hide the EditText
        additemText.setVisibility(View.GONE);
        additemCommit.setVisibility(View.GONE);
    }
}


// reference from http://javatechniques.com/blog/low-memory-deep-copy-technique-for-java-objects/
class PipedDeepCopy {
    /**
     * Flag object used internally to indicate that deserialization failed.
     */
    private static final Object ERROR = new Object();

    /**
     * Returns a copy of the object, or null if the object cannot
     * be serialized.
     */
    public static Object copy(Object orig) {
        Object obj = null;
        try {
            // Make a connected pair of piped streams
            PipedInputStream in = new PipedInputStream();
            PipedOutputStream pos = new PipedOutputStream(in);

            // Make a deserializer thread (see inner class below)
            Deserializer des = new Deserializer(in);

            // Write the object to the pipe
            ObjectOutputStream out = new ObjectOutputStream(pos);
            out.writeObject(orig);

            // Wait for the object to be deserialized
            obj = des.getDeserializedObject();

            // See if something went wrong
            if (obj == ERROR)
                obj = null;
        }
        catch(IOException ioe) {
            ioe.printStackTrace();
        }

        return obj;
    }

    /**
     * Thread subclass that handles deserializing from a PipedInputStream.
     */
    private static class Deserializer extends Thread {
        /**
         * Object that we are deserializing
         */
        private Object obj = null;

        /**
         * Lock that we block on while deserialization is happening
         */
        private Object lock = null;

        /**
         * InputStream that the object is deserialized from.
         */
        private PipedInputStream in = null;

        public Deserializer(PipedInputStream pin) throws IOException {
            lock = new Object();
            this.in = pin;
            start();
        }

        public void run() {
            Object o = null;
            try {
                ObjectInputStream oin = new ObjectInputStream(in);
                o = oin.readObject();
            }
            catch(IOException e) {
                // This should never happen. If it does we make sure
                // that a the object is set to a flag that indicates
                // deserialization was not possible.
                e.printStackTrace();
            }
            catch(ClassNotFoundException cnfe) {
                // Same here...
                cnfe.printStackTrace();
            }

            synchronized(lock) {
                if (o == null)
                    obj = ERROR;
                else
                    obj = o;
                lock.notifyAll();
            }
        }

        /**
         * Returns the deserialized object. This method will block until
         * the object is actually available.
         */
        public Object getDeserializedObject() {
            // Wait for the object to show up
            try {
                synchronized(lock) {
                    while (obj == null) {
                        lock.wait();
                    }
                }
            }
            catch(InterruptedException ie) {
                // If we are interrupted we just return null
            }
            return obj;
        }
    }
}