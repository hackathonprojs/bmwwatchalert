package me.xbt.bmwwatchalert;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.view.WatchViewStub;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

public class WatchActivity extends Activity {

    /**
     * parameter name for msg.
     * used in intent.putExtra() to pass in a parameter.
     */
    public static final String PARAM_MSG = "param_msg";
    /** # of msg received */
    public static final String PARAM_NUM_MSG = "param_num_msg";
    /** whether we should alert user */
    public static final String PARAM_ALERT = "param_alert";


    Timer timer;
    TimerTask timerTask;

    //we are going to use a handler to be able to run in our TimerTask
    final Handler handler = new Handler();



    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
            }
        });

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String msg = extras.getString(PARAM_MSG);
            int numMsg = extras.getInt(PARAM_NUM_MSG);
            boolean alert = extras.getBoolean(PARAM_ALERT);
            if (alert) {
                startTimer();
            } else {
                stopTimer();
            }
        } else {
            stopTimer();
        }

        //startTimer();
    }

    public void startTimer() {
        //set a new Timer
        timer = new Timer();

        //initialize the TimerTask's job
        initializeTimerTask();

        //schedule the timer, after the first 1000ms the TimerTask will run every 2000ms
        timer.schedule(timerTask, 1000, 1000); //
    }

    public void stopTimer() {
        //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public void initializeTimerTask() {

        timerTask = new TimerTask() {
            public void run() {

                //use a handler to run a toast that shows the current timestamp
                handler.post(new Runnable() {
                    public void run() {
                        //toast();
                        toggleColor();
                    }
                });
            }

            /**
             * toggle background color
             */
            private void toggleColor() {
                // Now get a handle to any View contained
                // within the main layout you are using
                //View someView = findViewById(R.id.randomViewInMainLayout);

                // Find the root view
                View root = mTextView.getRootView();

                // Set the color


                final int red = getResources().getColor(android.R.color.holo_red_light);
                final int dark = getResources().getColor(android.R.color.background_dark);
                //int color = dark;
                Drawable background = root.getBackground();
                if (background instanceof ColorDrawable) {
                    int color = ((ColorDrawable) background).getColor();
                    if (color != red) {
                        root.setBackgroundColor(red);
                    } else {
                        root.setBackgroundColor(dark);
                    }
                }


//                getWindow().getDecorView().setBackgroundColor(Color.RED);
            }

            /** show toast */
            private void toast() {
                //get the current timeStamp
                Calendar calendar = Calendar.getInstance();
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd:MMMM:yyyy HH:mm:ss a");
                final String strDate = "Warning:\n" + simpleDateFormat.format(calendar.getTime());

                //show the toast
                int duration = Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(getApplicationContext(), strDate, duration);
                toast.show();
            }
        };
    }
}
