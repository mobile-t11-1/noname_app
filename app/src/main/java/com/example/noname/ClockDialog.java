package com.example.noname;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.fragment.app.DialogFragment;

public class ClockDialog extends DialogFragment {

    private EditText editTextfocusTime;
    private EditText editTextshortTime;
    private EditText editTextlongTime;
    private DialogListener listener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.popup_window1, null);

        builder.setView(view)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                })
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String focusTime = editTextfocusTime.getText().toString();
                        String shortBreak = editTextshortTime.getText().toString();
                        String longBreak = editTextlongTime.getText().toString();
                        listener.getTimeData(focusTime,shortBreak,longBreak);
                    }
                });

        editTextfocusTime = view.findViewById(R.id.focus_input);
        editTextshortTime = view.findViewById(R.id.short_input);
        editTextlongTime = view.findViewById(R.id.long_input);


        return builder.create();
    }



    public interface DialogListener{
        void getTimeData(String focusTime, String shortBreak, String longBreak);
    }
}
