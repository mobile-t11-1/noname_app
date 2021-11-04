package com.example.noname;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.fragment.app.DialogFragment;

import static androidx.constraintlayout.motion.utils.Oscillator.TAG;

public class ClockDialog extends DialogFragment {

    private EditText editTextfocusTime;
    private EditText editTextshortTime;
    private EditText editTextlongTime;
    private TextView mBtnCancel, mBtnConfirm;
    public DialogListener listener;

    public interface DialogListener{
        void getTimeData(String focusTime, String shortBreak, String longBreak);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.popup_window1,container,false);

        editTextfocusTime = view.findViewById(R.id.focus_input);
        editTextshortTime = view.findViewById(R.id.short_input);
        editTextlongTime = view.findViewById(R.id.long_input);

        mBtnCancel = view.findViewById(R.id.pop_cancel);
        mBtnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getDialog().dismiss();
            }
        });

        mBtnConfirm = view.findViewById(R.id.pop_confirm);
        mBtnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String focusTime = editTextfocusTime.getText().toString();
                String shortBreak = editTextshortTime.getText().toString();
                String longBreak = editTextlongTime.getText().toString();

                if (!focusTime.equals("") || !shortBreak.equals("") || !longBreak.equals("")){
                    listener.getTimeData(focusTime,shortBreak,longBreak);
                }

                getDialog().dismiss();
            }
        });

        return view;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try {
            listener = (DialogListener) getTargetFragment();
        } catch (ClassCastException e) {
            Log.e(TAG, "onAttach: ClassCastException: " + e.getMessage());
        }
    }
}
