package com.example.noname;

import android.app.DatePickerDialog;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MapFrag#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ListAddFrag extends Fragment {

    private static final Calendar myCalendar = Calendar.getInstance();


    public ListAddFrag() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment MapFrag.
     */
    // TODO: Rename and change types and number of parameters
    public static ListAddFrag newInstance() {
        ListAddFrag fragment = new ListAddFrag();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_list_add, container, false);

        ImageView backButton = view.findViewById(R.id.list_add_back);
        backButton.setOnClickListener(v -> {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container,new ShopLstFrag()).commit();
        });


        TextView dueButton = view.findViewById(R.id.list_add_due);
        dueButton.setText("Due: " + myCalendar.get(Calendar.DAY_OF_MONTH) + "/" + (myCalendar.get(Calendar.MONTH)+1) + "/" + myCalendar.get(Calendar.YEAR));

        DatePickerDialog.OnDateSetListener date = (view1, year, monthOfYear, dayOfMonth) -> {
            // TODO Auto-generated method stub
            myCalendar.set(Calendar.YEAR, year);
            myCalendar.set(Calendar.MONTH, monthOfYear);
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            dueButton.setText("Due: " + myCalendar.get(Calendar.DAY_OF_MONTH) + "/" + (myCalendar.get(Calendar.MONTH)+1) + "/" + myCalendar.get(Calendar.YEAR));
        };

        dueButton.setOnClickListener(v -> {
            // TODO Auto-generated method stub
            new DatePickerDialog(getActivity(), date, myCalendar
                    .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                    myCalendar.get(Calendar.DAY_OF_MONTH)).show();
        });


        ImageView optionButton = view.findViewById(R.id.list_add_options);
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
}