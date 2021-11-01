package com.example.noname;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static android.content.Context.MODE_PRIVATE;
import static android.content.Context.VIBRATOR_SERVICE;
import static androidx.constraintlayout.motion.utils.Oscillator.TAG;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PomoFrag#newInstance} factory method to
 * create an instance of this fragment.
 */
// TODO: 1.5 做传感器关闭屏幕 1. 做session中间的间隔，震动提醒
// TODO: Xinhao: UI fixes, section indicator reset
public class  PomoFrag extends Fragment implements ClockDialog.DialogListener{

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;


    //database related variables
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userID;
    private DocumentReference idRef;

    // Date formatter
    SimpleDateFormat sdf = new SimpleDateFormat("dd/M/yyyy hh:mm:ss");
    Date startDate; // record start date
    int totalFocusTime; // record total focus timme


    //components from xml
    private TextView mTextViewCountDown;
    private TextView mTextViewRest;
    private ImageButton mButtonStartPause;
    private ImageButton mButtonReset;
    private ImageButton settingBtn;
    private ProgressBar clockProgress;
    private TextView t1,t2,t3,t4;
    private TextView m0, m1, m2;

    private CountDownTimer mCountDownTimer;


    private boolean mTimerRunning; //record the status of the timer
    private boolean mFaceUp; //record the status of the screen
    //private variables
    private int sessionID = 1; //record the current session: odd is work, even is rest
    private int mStartTimeInMillis = 10000; //25*60*1000 25min //set timer
    private int sBreakTimeInMillis = 5000; //short break time
    private int lBreakTimeInMillis = 5000; //long break time 
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

        db = FirebaseFirestore.getInstance();
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        userID = mAuth.getCurrentUser().getUid();
        idRef = db.collection("focusTime").document(userID);

        //assignment
        mTextViewCountDown = view.findViewById(R.id.text_view_countdown);
        mTextViewRest = view.findViewById(R.id.text_view_rest);
        mButtonReset = view.findViewById(R.id.button_reset);
        mButtonStartPause = view.findViewById(R.id.button_start_pause);
        clockProgress = view.findViewById(R.id.clock_progress);
        settingBtn = view.findViewById(R.id.btn_settings);

        t1 = view.findViewById(R.id.section_1);
        t2 = view.findViewById(R.id.section_2);
        t3 = view.findViewById(R.id.section_3);
        t4 = view.findViewById(R.id.section_4);

        m0 = view.findViewById(R.id.pomo_instruction);
        m1 = view.findViewById(R.id.text_finished);
        m2 = view.findViewById(R.id.text_restart);

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
                    mFaceUp = false;
                }else{ // screen face up
                    if(mTimerRunning && sessionID %2 != 0){
                        pauseTimer();
                    }
                    mFaceUp = true;
                }
                //System.out.println("faceup: " + mFaceUp);
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
                resetTimer();
                startView();

//                if (mTimerRunning) {
//                    pauseTimer();
//                } else {
//                    startTimer();
//                }
            }
        });

        //reset timer
        mButtonReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetTimer();
                startView();
            }
        });

        return view;

    }


    private void startView(){
        settingBtn.setVisibility(View.VISIBLE);
        mButtonReset.setVisibility(View.INVISIBLE);
        resetIndicators();
        mTextViewCountDown.setVisibility(View.VISIBLE);
        m0.setVisibility(View.VISIBLE);
        m1.setVisibility(View.INVISIBLE);
        m2.setVisibility(View.INVISIBLE);
    }

    private void endView(){
        //congrats message
        mButtonReset.setVisibility(View.INVISIBLE);
        mButtonStartPause.setVisibility(View.VISIBLE);
        mButtonStartPause.setImageResource(R.drawable.ic_replay);
        mTextViewCountDown.setVisibility(View.INVISIBLE);
        m1.setVisibility(View.VISIBLE);
        m2.setVisibility(View.VISIBLE);
    }


    private void resetIndicators(){
        t1.setBackground(getResources().getDrawable(R.drawable.pomo_no_section));
        t2.setBackground(getResources().getDrawable(R.drawable.pomo_no_section));
        t3.setBackground(getResources().getDrawable(R.drawable.pomo_no_section));
        t4.setBackground(getResources().getDrawable(R.drawable.pomo_no_section));
    }

    private void checkSection(int sessionID){
        if(sessionID == 1 || sessionID == 2){
            t1.setBackground(getResources().getDrawable(R.drawable.pomo_indicator_style));
        }
        else if(sessionID == 3 || sessionID == 4){
            t1.setBackground(getResources().getDrawable(R.drawable.pomo_finished_indicator));
            t2.setBackground(getResources().getDrawable(R.drawable.pomo_indicator_style));
        }
        else if(sessionID == 5 || sessionID == 6){
            t1.setBackground(getResources().getDrawable(R.drawable.pomo_finished_indicator));
            t2.setBackground(getResources().getDrawable(R.drawable.pomo_finished_indicator));
            t3.setBackground(getResources().getDrawable(R.drawable.pomo_indicator_style));
        }
        else if(sessionID == 7 || sessionID == 8){
            t1.setBackground(getResources().getDrawable(R.drawable.pomo_finished_indicator));
            t2.setBackground(getResources().getDrawable(R.drawable.pomo_finished_indicator));
            t3.setBackground(getResources().getDrawable(R.drawable.pomo_finished_indicator));
            t4.setBackground(getResources().getDrawable(R.drawable.pomo_indicator_style));
        }
        else {
            t4.setBackground(getResources().getDrawable(R.drawable.pomo_finished_indicator));
        }
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
        checkSection(sessionID);

        mCountDownTimer = new CountDownTimer(mTimeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if(mFaceUp && sessionID % 2 != 0){
                    pauseTimer();
                }
                mTimeLeftInMillis = millisUntilFinished;
                updateCountDownText();
                updateClockProgress();
            }


            @Override
            public void onFinish() {
                mTimerRunning = false;
                sessionID += 1;
                System.out.println(sessionID);
                //let the phone vibrates
                if(Build.VERSION.SDK_INT >= 26) {
                    vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
                    //createWaveform(long[] timings, int[] amplitudes, int repeat) to create a waveform vibrations
                }else{
                    vibrator.vibrate(200);
                }


                if(sessionID % 2 == 0){ //enters break session
                    addTimeToDatabase(mStartTimeInMillis/60000);
                    mTimeLeftInMillis = sBreakTimeInMillis; //5 min rest 5*60*1000
                    if(sessionID == 8){
                        mTimeLeftInMillis = lBreakTimeInMillis; // 20 min last rest
                    }
                    mTextViewRest.setVisibility(View.VISIBLE);
                }else{
                    mTimeLeftInMillis = mStartTimeInMillis;
                    mTextViewRest.setVisibility(View.INVISIBLE);
                }
                if(sessionID == 9){ //four work sessions then quit
                    endView();
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
        mCountDownTimer.cancel(); //nullpointerexception
        mTimerRunning = false;
        updateWatchInterface();
        //resume button
        mButtonStartPause.setImageResource(R.drawable.ic_play_fill);

    }


    private void resetTimer() {
        if(mTimerRunning){
            pauseTimer();
        }
        //reset sessionID
        sessionID = 1;
        mTimeLeftInMillis = mStartTimeInMillis;
        updateCountDownText();
        updateClockProgress();
        updateWatchInterface();
        mTextViewRest.setVisibility(View.INVISIBLE);

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
            settingBtn.setVisibility(View.INVISIBLE);
            mButtonStartPause.setVisibility(View.INVISIBLE);
            //mButtonStartPause.setImageResource(R.drawable.ic_pause);
        }
    }

    private void updateClockProgress() {
        int percentage = 0;
        float f = (mTimeLeftInMillis * 1.0f);
        if(sessionID == 8){
            f = f / lBreakTimeInMillis;
        }else if(sessionID % 2 == 0){
            f = f / sBreakTimeInMillis;
        }else {
            f = f / mStartTimeInMillis;
        }
        percentage = 100 - ((int) (f * 100) - 9);
        if(sessionID == 8 && mTimeLeftInMillis == lBreakTimeInMillis){
            percentage = 0;
        }else if(sessionID % 2 == 0 && mTimeLeftInMillis == sBreakTimeInMillis){
            percentage = 0;
        }else if(mTimeLeftInMillis == mStartTimeInMillis){
            percentage = 0;
        }
        //System.out.println(percentage);

        clockProgress.setProgress(percentage);
    }



    //To save the timer if the app closes
    @Override
    public void onStop() {
        super.onStop();
        SharedPreferences prefs = getActivity().getSharedPreferences("prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putInt("focusTime", mStartTimeInMillis);
        editor.putInt("sBreakTime", sBreakTimeInMillis);
        editor.putInt("lBreakTime", lBreakTimeInMillis);
        editor.putLong("millisLeft", mTimeLeftInMillis);
        editor.putBoolean("timerRunning", mTimerRunning);
        editor.putLong("endTime", mEndTime);

        editor.apply();

        if(sessionID % 2 != 0 && mTimerRunning){
            pauseTimer();
        }

        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
        }

    }

    //To resume the timer when the app opens back up
    @Override
    public void onStart() {
        super.onStart();
        SharedPreferences prefs = getActivity().getSharedPreferences("prefs", MODE_PRIVATE);
        mStartTimeInMillis = prefs.getInt("focusTime", 10000);
        sBreakTimeInMillis = prefs.getInt("sBreakTime", 5000);
        lBreakTimeInMillis = prefs.getInt("lBreakTime", 5000);

        if(sessionID % 2 == 0) {

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
            return;
        }

        mTimeLeftInMillis = prefs.getInt("focusTime", mStartTimeInMillis);

        clockProgress.setProgress(0);
        updateCountDownText();
    }

    // call this function to get focus time, short break time, long break time
    // assign them to the variables
    @Override
    public void getTimeData(String focusTime, String shortBreak, String longBreak) {
        //if the user entered any invalid time then do nothing and stick with default time settings
        //convert string to long and assign variables

        if(focusTime != "0") {
            try {
                mStartTimeInMillis = Integer.parseInt(focusTime) * 1000;
            } catch (NumberFormatException nfe) {
                System.out.println("Could not parse focus time " + nfe);
            }
        }
        if(shortBreak != "0") {
            try {
                sBreakTimeInMillis = Integer.parseInt(shortBreak) * 1000;
            } catch (NumberFormatException nfe) {
                System.out.println("Could not parse short break " + nfe);
            }
        }

        if(longBreak != "0"){
            try {
                lBreakTimeInMillis = Integer.parseInt(longBreak) *1000;
            } catch(NumberFormatException nfe) {
                System.out.println("Could not parse long break " + nfe);
            }
        }

        settingBtn.setVisibility(View.INVISIBLE);
        //reset timer to make the assignment effective
        resetTimer();
    }

    // call this method to focus time to database
    // duration: focus time to be stored
    private void addTimeToDatabase(int duration){
        Calendar calendar = Calendar.getInstance();
        Date currentDate = calendar.getTime();
        idRef.get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        DocumentSnapshot document = task.getResult();
                        // if the document exists
                        if (document.exists()){
                            String start = document.getString("start date");
                            try {
                                startDate = sdf.parse(start);
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                            totalFocusTime = document.getLong("total minutes").intValue();

                            // if we the current user has records in database
                            // we update relevant field depending on the timestamps (within a week)
                            Map<String, Object> update = createUpdateData(startDate, currentDate, totalFocusTime, duration);
                            // then we update this map into firestore
                            idRef.update(update)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void unused) {
                                            Log.d(TAG, "Successfully Updated!");
                                        }
                                    });

                        }
                        // if there is no such document in firestore
                        else{
                            // we create a new record
                            Map<String, Object> record = new HashMap<>();
                            String date = sdf.format(currentDate);
                            record.put("start date", date);
                            record.put("last date", date);
                            record.put("total minutes", duration);

                            // add the new record into Firestore
                            db.collection("focusTime").document(userID)
                                    .set(record)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void unused) {
                                            Log.d(TAG, "New record stored!");
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.d(TAG, "Failed to store");
                                }
                            });
                        }
                    }
                });
    }


    private Map<String, Object> createUpdateData(Date startDate, Date currentDate, int totalTime, int duration){
        Map<String, Object> update = new HashMap<>();
        // convert date into string format
        String date = sdf.format(currentDate);

        // if within a week, we only update focus time and thisDate
        if(checkDuration(startDate,currentDate)){
            update.put("last date", date);
            update.put("total minutes", totalTime + duration);
        }else {
            update.put("start date", date);
            update.put("last date", date);
            update.put("total minutes", duration);
        }

        return update;

    }

    private boolean checkDuration(Date startDate, Date currentDate){
        //milliseconds
        long different = currentDate.getTime() - startDate.getTime();

        long secondsInMilli = 1000;
        long minutesInMilli = secondsInMilli * 60;
        long hoursInMilli = minutesInMilli * 60;
        long daysInMilli = hoursInMilli * 24;
        long elapsedDays = different / daysInMilli;

        if (elapsedDays < 7){
            return true;
        }

        return false;
    }


}