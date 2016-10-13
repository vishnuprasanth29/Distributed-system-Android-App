package com.Project258;
/**
 * Created by Raj on 3/1/2016.
 */
import android.app.ActionBar;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import android.app.Activity;
import android.app.Dialog;

import android.view.View;

import android.widget.Button;
import android.widget.TextView;

import android.util.Log;

public class AdminActivity extends Activity implements Observer {
    private static final String TAG = "chat.AdminActivity";
    private ActionBar ab;


    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.host);

        // buttons for the searching groups, leaving group, share files and open the folder ////
        mSetNameButton = (Button)findViewById(R.id.hostSetName);
        mSetNameButton.setEnabled(true);
        mSetNameButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showDialog(DIALOG_SET_NAME_ID);
            }
        });

        mStartButton = (Button)findViewById(R.id.hostStart);
        mStartButton.setEnabled(false);
        mStartButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showDialog(DIALOG_START_ID);
            }
        });

        mStopButton = (Button)findViewById(R.id.hostStop);
        mStopButton.setEnabled(false);
        mStopButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showDialog(DIALOG_STOP_ID);
            }
        });

    // create an object to our data structure aka model of MVC arch in the application i.e FileShareApp
        // checkin is performed to check if the BroadcastReceiver service is running
        mFileShareApp = (FileShareApp)getApplication();
        mFileShareApp.checkin();

        ab = getActionBar();
        ab.setTitle("P2P Group Admin Settings");
        ab.setSubtitle("Create a P2P Group to Host");

    // call to get current state of the group
        updateChannelState();

    // accept observer notification from other components in the app
        mFileShareApp.addObserver(this);

    }
    // method to to remove the observer setup for the Model
    public void onDestroy() {
        Log.i(TAG, "onDestroy()");
        mFileShareApp = (FileShareApp)getApplication();
        mFileShareApp.deleteObserver(this);
        super.onDestroy();
    }
    // initialize fileshareapp to null
    private FileShareApp mFileShareApp = null;

    // static id map for triggerign the corresponding dialog box //
    static final int DIALOG_SET_NAME_ID = 0;
    static final int DIALOG_START_ID = 1;
    static final int DIALOG_STOP_ID = 2;
    public static final int DIALOG_ALLJOYN_ERROR_ID = 3;


    // Android dialog builder for various notifications, dialogs are defined in DialogBuilder.class ////
    protected Dialog onCreateDialog(int id) {
        Log.i(TAG, "onCreateDialog()");
        Dialog result = null;
        switch(id) {
        case DIALOG_SET_NAME_ID:
            {
                DialogBuilder builder = new DialogBuilder();
                result = builder.createHostNameDialog(this, mFileShareApp);
            }
            break;
        case DIALOG_START_ID:
            {
                DialogBuilder builder = new DialogBuilder();
                result = builder.createHostStartDialog(this, mFileShareApp);
            }
            break;
        case DIALOG_STOP_ID:
            {
                DialogBuilder builder = new DialogBuilder();
                result = builder.createHostStopDialog(this, mFileShareApp);
            }
            break;
        case DIALOG_ALLJOYN_ERROR_ID:
            {
                DialogBuilder builder = new DialogBuilder();
                result = builder.createAllJoynErrorDialog(this, mFileShareApp);
            }
            break;
        }
        return result;
    }

    // update for the observer/observable functionality to have message passing among other activities///
    public synchronized void update(Observable o, Object arg) {
        Log.i(TAG, "update(" + arg + ")");
        String qualifier = (String)arg;

        if (qualifier.equals(FileShareApp.APPLICATION_QUIT_EVENT)) {
            Message message = mHandler.obtainMessage(HANDLE_APPLICATION_QUIT_EVENT);
            mHandler.sendMessage(message);
        }

        if (qualifier.equals(FileShareApp.HOST_CHANNEL_STATE_CHANGED_EVENT)) {
            Message message = mHandler.obtainMessage(HANDLE_CHANNEL_STATE_CHANGED_EVENT);
            mHandler.sendMessage(message);
        }

        if (qualifier.equals(FileShareApp.ALLJOYN_ERROR_EVENT)) {
            Message message = mHandler.obtainMessage(HANDLE_ALLJOYN_ERROR_EVENT);
            mHandler.sendMessage(message);
        }
    }

    // method to get the current state of the group and set the corresponding state in the string ////
    private void updateChannelState() {
        BroadcastReceiver.HostChannelState channelState = mFileShareApp.hostGetChannelState();
        String name = mFileShareApp.hostGetChannelName();
        boolean haveName = true;
        if (name == null) {
            haveName = false;
            name = "Not set";
        }
        if(name == "Not set")
                ab.setSubtitle("Create a P2P group to Host");
        else
                ab.setSubtitle(name);

        switch (channelState) {
        case IDLE:
            if(name == "Not set")
                ab.setSubtitle("Create a P2P group to Host");
            else
                ab.setSubtitle(name + ": " + "Idle");
            break;
        case NAMED:
            if(name == "Not set")
                ab.setSubtitle("Create a P2P group to Host");
            else
                ab.setSubtitle(name + ": " + "Named");

            break;
        case BOUND:
            if(name == "Not set")
                ab.setSubtitle("Create a P2P group to Host");
            else
                ab.setSubtitle(name + ": " + "Bound");
            break;
        case ADVERTISED:
            if(name == "Not set")
                ab.setSubtitle("Create a P2P group to Host");
            else
                ab.setSubtitle(name + ": " + "Advertised");

            break;
        case CONNECTED:
            if(name == "Not set")
                ab.setSubtitle("Create a P2P group to Host");
            else
                ab.setSubtitle(name + ": " + "Connected");

            break;
        default:
            if(name == "Not set")
                ab.setSubtitle("Create a P2P group to Host");
            else
                ab.setSubtitle(name + ": " + "Unknown");

            break;
        }

        if (channelState == BroadcastReceiver.HostChannelState.IDLE) {
            mSetNameButton.setEnabled(true);
            if (haveName) {
                mStartButton.setEnabled(true);
            } else {
                mStartButton.setEnabled(false);
            }
            mStopButton.setEnabled(false);
        } else {
            mSetNameButton.setEnabled(false);
            mStartButton.setEnabled(false);
            mStopButton.setEnabled(true);
        }
    }

    // setup ui resources
    private TextView mChannelName;
    private TextView mChannelStatus;
    private Button mSetNameButton;
    private Button mStartButton;
    private Button mStopButton;
    private Button mQuitButton;

    // service the error received by alljoyn api and throw a dialog
    private void alljoynError() {
        if (mFileShareApp.getErrorModule() == FileShareApp.Module.GENERAL ||
                mFileShareApp.getErrorModule() == FileShareApp.Module.USE) {
            showDialog(DIALOG_ALLJOYN_ERROR_ID);
        }
    }

    /// IDs aka event codes ////
    private static final int HANDLE_APPLICATION_QUIT_EVENT = 0;
    private static final int HANDLE_CHANNEL_STATE_CHANGED_EVENT = 1;
    private static final int HANDLE_ALLJOYN_ERROR_EVENT = 2;

    //handler to handle to the observer notifications received from other components
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case HANDLE_APPLICATION_QUIT_EVENT:
                {
                    Log.i(TAG, "mHandler.handleMessage(): HANDLE_APPLICATION_QUIT_EVENT");
                    finish();
                }
                break;
            case HANDLE_CHANNEL_STATE_CHANGED_EVENT:
                {
                    Log.i(TAG, "mHandler.handleMessage(): HANDLE_CHANNEL_STATE_CHANGED_EVENT");
                    updateChannelState();
                }
                break;
            case HANDLE_ALLJOYN_ERROR_EVENT:
                {
                    Log.i(TAG, "mHandler.handleMessage(): HANDLE_ALLJOYN_ERROR_EVENT");
                    alljoynError();
                }
                break;
            default:
                break;
            }
        }
    };

}