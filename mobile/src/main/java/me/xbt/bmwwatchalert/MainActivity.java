package me.xbt.bmwwatchalert;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubError;
import com.pubnub.api.PubnubException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends ActionBarActivity {

    /** whether there is an alert */
    private static boolean alert = false;

    private static final String TAG = "MainActivity: ";
    GoogleApiClient mGoogleApiClient;

    private Timer timer = null;
    private TimerTask timerTask = null;
    //we are going to use a handler to be able to run in our TimerTask
    final Handler handler = new Handler();


    private TextView mTextView = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.d(TAG, "onConnected: " + connectionHint);
                        //  "onConnected: null" is normal.
                        //  There's nothing in our bundle.

                        // Now you can use the Data Layer API
//                        List<Node> nodes = getNodes();
//                        if (nodes.size() > 0) {
//                            Log.d(TAG, "number of nodes=" + nodes.size());
//                            Log.i(TAG, "nodes=" + nodes);
//
//                            Node node = nodes.get(0); // there should be only one node
//                            MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
//                                    mGoogleApiClient, node.getId(), START_ACTIVITY_PATH, null).await();
//                            if (!result.getStatus().isSuccess()) {
//                                Log.e(TAG, "ERROR: failed to send Message: " + result.getStatus());
//                            }
//                        }
                        //tellWatchConnectedState("connected");
                    }
                    @Override
                    public void onConnectionSuspended(int cause) {
                        Log.d(TAG, "onConnectionSuspended: " + cause);
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.d(TAG, "onConnectionFailed: " + result);
                    }
                })
                        // Request access only to the Wearable API
                .addApi(Wearable.API)
                .build();

        // need to add <meta-data> com.google.android.gms.version tag into androidmanifest.xml
        mGoogleApiClient.connect();


        subscribePubnub();


        mTextView = (TextView) findViewById(R.id.textView);

        // add button listener
        final Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tellWatchConnectedState("{\"alert\": true}");
                alert = true;
            }
        });
        final Button button2 = (Button) findViewById(R.id.button2);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tellWatchConnectedState("sending message");
                alert = false;
            }
        });

        startTimer();
    }


    private void tellWatchConnectedState(final String state){

        // it is necessary to put the following code in asynctask
        // getNodes() uses await(), which cannot be used on ui thread.
        // we will see an error about await cannot be called on ui thread if we do not use asynctask.
        new AsyncTask<Void, Void, List<Node>>(){

            private static final String START_ACTIVITY = "/start_activity";

            @Override
            protected List<Node> doInBackground(Void... params) {
                return getNodes();
            }

            @Override
            protected void onPostExecute(List<Node> nodeList) {
                for(Node node : nodeList) {
                    String msg = "telling " + node.getDisplayName() + " - " + node.getId() + " i am " + state;
                    Log.v(TAG, msg);

                    PendingResult<MessageApi.SendMessageResult> result = Wearable.MessageApi.sendMessage(
                            mGoogleApiClient,
                            node.getId(),
                            START_ACTIVITY, //"/listener/lights/" + state,
                            msg.getBytes()
                    );

                    result.setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                            Log.v(TAG, "Phone: " + sendMessageResult.getStatus().getStatusMessage());
                        }
                    });
                }
            }
        }.execute();

    }


    /**
     * get a list of connected nodes that you can potentially send messages to
     */
    private List<Node> getNodes() {
        ArrayList<Node> results = new ArrayList<Node>();
        NodeApi.GetConnectedNodesResult nodes =
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
        for (Node node : nodes.getNodes()) {
            results.add(node);
        }
        return results;
    }


    private void subscribePubnub() {
        String pubkey = "demo"; // publish key
        String subkey = "demo"; // subscribe key
        String channelName = "bmwwatchalert";

        Pubnub pubnub = new Pubnub(pubkey, subkey);

        try {
            pubnub.subscribe(channelName, new Callback() {

                        @Override
                        public void connectCallback(String channel, Object message) {
                            System.out.println("SUBSCRIBE : CONNECT on channel:" + channel
                                    + " : " + message.getClass() + " : "
                                    + message.toString());
                        }

                        @Override
                        public void disconnectCallback(String channel, Object message) {
                            System.out.println("SUBSCRIBE : DISCONNECT on channel:" + channel
                                    + " : " + message.getClass() + " : "
                                    + message.toString());
                        }

                        public void reconnectCallback(String channel, Object message) {
                            System.out.println("SUBSCRIBE : RECONNECT on channel:" + channel
                                    + " : " + message.getClass() + " : "
                                    + message.toString());
                        }

                        @Override
                        public void successCallback(String channel, Object message) {
                            System.out.println("received data from pubnub : " + channel + " : "
                                    + message.getClass() + " : " + message.toString());
                            // send message to wear
                            tellWatchConnectedState(message.toString());
                            alert = (message.toString().contains("alert"));
                        }

                        @Override
                        public void errorCallback(String channel, PubnubError error) {
                            System.out.println("SUBSCRIBE : ERROR on channel " + channel
                                    + " : " + error.toString());
                        }
                    }
            );
        } catch (PubnubException e) {
            System.out.println(e.toString());
        }

        Callback callback = new Callback() {
            public void successCallback(String channel, Object response) {
                System.out.println(response.toString());
            }
            public void errorCallback(String channel, PubnubError error) {
                System.out.println(error.toString());
            }
        };
        //pubnub.publish(channelName, "Hello World !!" , callback);
    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.disconnect();
    }





    public void startTimer() {
        //set a new Timer
        timer = new Timer();

        //initialize the TimerTask's job
        initializeTimerTask();

        //schedule the timer, after the first 500ms the TimerTask will run every 500ms
        timer.schedule(timerTask, 500, 500); //
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
                        if (alert) {
                            //toast();
                            toggleColor();

                            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                            v.vibrate(400);
                        } else {
                            final int dark = getResources().getColor(android.R.color.background_dark);
                            setColor(dark);
                        }
                    }
                });
            }

            private void setColor(int color) {
                // Find the root view
                View root = mTextView.getRootView();
                root.setBackgroundColor(color);
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
