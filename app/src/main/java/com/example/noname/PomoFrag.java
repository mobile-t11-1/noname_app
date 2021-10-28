package com.example.noname;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.os.CountDownTimer;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Locale;

import static android.content.Context.MODE_PRIVATE;
import static android.content.Context.VIBRATOR_SERVICE;
import static androidx.constraintlayout.motion.utils.Oscillator.TAG;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PomoFrag#newInstance} factory method to
 * create an instance of this fragment.
 */
// TODO: 1.5 做传感器关闭屏幕 1. 做session中间的间隔，震动提醒
public class PomoFrag extends Fragment implements ClockDialog.DialogListener{

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;



    //components from xml
    private TextView mTextViewCountDown;
    private TextView mTextViewRest;
    private ImageButton mButtonStartPause;
    private ImageButton mButtonReset;
    private ImageButton settingBtn;
    private ProgressBar clockProgress;

    private CountDownTimer mCountDownTimer;


    private boolean mTimerRunning; //record the status of the timer
    private boolean mFaceUp; //record the status of the screen
    //private variables
    private int sessionID = 1; //record the current session: odd is work, even is rest
    private static final long mStartTimeInMillis = 10000; //25*60*1000 25min //set timer
    private long mTimeLeftInMillis; //remaining time
    private long mEndTime;
    private Vibrator vibrator; // vibrate
    // proximity sensor
    private SensorManager sensorManager;
    private Sensor proximitySensor;
    private SensorEventListener proximityListener;

    public PomoFrag() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment PomoFrag.
     */
    // TODO: Rename and change types and number of parameters
    public static PomoFrag newInstance(String param1, String param2) {
        PomoFrag fragment = new PomoFrag();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_pomo, container, false);
        //assignment
        mTextViewCountDown = view.findViewById(R.id.text_view_countdown);
        mTextViewRest = view.findViewById(R.id.text_view_rest);
        mButtonStartPause = view.findViewById(R.id.button_start_pause);
        mButtonReset = view.findViewById(R.id.button_reset);
        clockProgress = view.findViewById(R.id.clock_progress);
        settingBtn = view.findViewById(R.id.btn_settings);

        //define vibrator
        vibrator = (Vibrator) getActivity().getSystemService(VIBRATOR_SERVICE);

        //declare proximity sensor variable
        sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        if(sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) != null){
            proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        }
        //flip screen actions
        proximityListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                // screen face down
                if(sensorEvent.values[0] < proximitySensor.getMaximumRange()){
                    if(!mTimerRunning){
                        startTimer();
                    }
                }else{ // screen face up
                    if(mTimerRunning){
                        pauseTimer();
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };

        sensorManager.registerListener(proximityListener, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);

        settingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openSettingDialog();
            }
        });

        //pause and resume timer
        mButtonStartPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTimerRunning) {
                    pauseTimer();
                } else {
                    startTimer();
                }
            }
        });

        //reset timer
        mButtonReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetTimer();
            }
        });

        return view;
    }

    private void openSettingDialog() {
        ClockDialog clockDialog = new ClockDialog();
        clockDialog.setTargetFragment(PomoFrag.this,1);
        clockDialog.show(getFragmentManager(),"SettingDialog");

    }

    @Override
    public void onPause(){
        super.onPause();
        sensorManager.unregisterListener(proximityListener);
    }


    private void startTimer() {
        mEndTime = System.currentTimeMillis() + mTimeLeftInMillis;

        mCountDownTimer = new CountDownTimer(mTimeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                mTimeLeftInMillis = millisUntilFinished;
                updateCountDownText();
                updateClockProgress();
            }

            @Override
            public void onFinish() {
                mTimerRunning = false;
                sessionID += 1;
                //let the phone vibrates
                if(Build.VERSION.SDK_INT >= 26) {
                    vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
                    //createWaveform(long[] timings, int[] amplitudes, int repeat) to create a waveform vibrations
                }else{
                    vibrator.vibrate(200);
                }

                if(sessionID % 2 == 0){
                    mTimeLeftInMillis = 5000; //5 min rest 5*60*1000
                    if(sessionID == 8){
                        mTimeLeftInMillis = 20*60*1000; // 20 min last rest
                    }
                    mTextViewRest.setVisibility(View.VISIBLE);
                }else{
                    mTimeLeftInMillis = mStartTimeInMillis;
                    mTextViewRest.setVisibility(View.INVISIBLE);
                }
                if(sessionID == 9){ //four work sessions then quit
                    mButtonStartPause.setImageResource(R.drawable.ic_replay);
                    mButtonStartPause.setVisibility(View.VISIBLE);
                    //mButtonReset.setVisibility(View.VISIBLE);
                    return;
                }
                //auto start the next session
                startTimer();

            }
        }.start();

        //vibrate when click start
        if(Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
            //createWaveform(long[] timings, int[] amplitudes, int repeat) to create a waveform vibrations
        }else{
            vibrator.vibrate(200);
        }

        mTimerRunning = true;
        updateWatchInterface();
    }

    private void pauseTimer() {
        mCountDownTimer.cancel();
        mTimerRunning = false;
        updateWatchInterface();
        //resume button
        mButtonStartPause.setImageResource(R.drawable.ic_play_fill);
    }

    private void resetTimer() {
        mTimeLeftInMillis = mStartTimeInMillis;
        updateCountDownText();
        updateClockProgress();
        updateWatchInterface();
        //reset sessionID
        sessionID = 1;
    }

    private void updateCountDownText() {
        int hours = (int) (mTimeLeftInMillis / 1000) / 3600;
        int minutes = (int) ((mTimeLeftInMillis / 1000) % 3600) / 60;
        int seconds = (int) (mTimeLeftInMillis / 1000) % 60;

        String timeLeftFormatted;
        if (hours > 0) {
            timeLeftFormatted = String.format(Locale.getDefault(),
                    "%d:%02d:%02d", hours, minutes, seconds);
        } else {
            timeLeftFormatted = String.format(Locale.getDefault(),
                    "%02d:%02d", minutes, seconds);
        }

        mTextViewCountDown.setText(timeLeftFormatted);
    }


    private void updateWatchInterface() {
        if (mTimerRunning) {
            mButtonReset.setVisibility(View.VISIBLE);
            //mButtonStartPause.setImageResource(R.drawable.ic_pause);
        } else {
            //mButtonStartPause.setText("Start");

            if (mTimeLeftInMillis < 1000) {
                //mButtonStartPause.setVisibility(View.INVISIBLE);
                mButtonReset.setVisibility(View.VISIBLE);
            } else {
                mButtonReset.setVisibility(View.VISIBLE);
                //mButtonStartPause.setVisibility(View.VISIBLE);
            }

            if (mTimeLeftInMillis < mStartTimeInMillis) {
                mButtonReset.setVisibility(View.VISIBLE);
                //mButtonStartPause.setImageResource(R.drawable.ic_play_fill);
            } else {
                mButtonReset.setVisibility(View.VISIBLE);
                //mButtonStartPause.setImageResource(R.drawable.ic_play_fill);
            }
        }
    }

    private void updateClockProgress(){
        float f = (mTimeLeftInMillis * 1.0f) / mStartTimeInMillis;
        int percentage = 100 - ((int) (f *100) - 9);
        System.out.println(percentage);

        clockProgress.setProgress(percentage);
    }



    //To save the timer if the app closes
    @Override
    public void onStop() {
        super.onStop();

        SharedPreferences prefs = getActivity().getSharedPreferences("prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        editor.putLong("millisLeft", mTimeLeftInMillis);
        editor.putBoolean("timerRunning", mTimerRunning);
        editor.putLong("endTime", mEndTime);

        editor.apply();

        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
        }
    }

    //To resume the timer when the app opens back up
    @Override
    public void onStart() {
        super.onStart();

        SharedPreferences prefs = getActivity().getSharedPreferences("prefs", MODE_PRIVATE);
        //possible bug
        //mStartTimeInMillis = prefs.getLong("startTimeInMillis", 900000);
        mTimeLeftInMillis = prefs.getLong("millisLeft", mStartTimeInMillis);
        mTimerRunning = prefs.getBoolean("timerRunning", false);

        updateCountDownText();
        updateClockProgress();
        updateWatchInterface();

        if (mTimerRunning) {
            mEndTime = prefs.getLong("endTime", 0);
            mTimeLeftInMillis = mEndTime - System.currentTimeMillis();

            if (mTimeLeftInMillis < 0) {
                mTimeLeftInMillis = 0;
                mTimerRunning = false;
                updateCountDownText();
                updateClockProgress();
                updateWatchInterface();
            } else {
                startTimer();
            }
        }
    }

    @Override
    public void getTimeData(String focusTime, String shortBreak, String longBreak) {
        Log.d(TAG, "getTimeData: " + focusTime + shortBreak + longBreak);
    }


}