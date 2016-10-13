package com.Project258;
/**
 * Created by Raj on 3/3/2016.
 */
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;

import android.app.Activity;
import android.app.Dialog;

import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.DrawerLayout;
import android.app.ActionBar;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
//import android.support.design.widget.NavigationView;


import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.io.RandomAccessFile;

public class UserActivity extends Activity implements Observer{
    private static final String TAG = "chat.UserActivity";
    private static final int SHARE_SELECTED_FILE = 1;

    protected String filePath;
    private GoogleApiClient client;
    protected JsonMessage messageObj = new JsonMessage();
    private String filePathstring = null;
    private FileShareApp mFileShareApp = null;
    private ActionBar ab;
    private ArrayAdapter<String> mHistoryList;

    private Button mJoinButton;
    private Button mLeaveButton;
    private Button mShareFile;
    private Button mShareFolder;

//    private TextView mChannelName;
//
//    private TextView mChannelStatus;

    private Intent selectFileIntent;

    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.use);

        // array list for populating the list of received/sent files //
        mHistoryList = new ArrayAdapter<String>(this, android.R.layout.test_list_item);
        ListView hlv = (ListView) findViewById(R.id.useHistoryList);
        hlv.setAdapter(mHistoryList);

        // calls the join group dialog builder //
        mJoinButton = (Button) findViewById(R.id.useJoin);
        mJoinButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showDialog(DIALOG_JOIN_ID);
            }
        });

        // calls the leave group dialog builder
        mLeaveButton = (Button) findViewById(R.id.useLeave);
        mLeaveButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showDialog(DIALOG_LEAVE_ID);
            }
        });

        // triggers the FileExplorer activity
        mShareFile = (Button) findViewById(R.id.useShareFile);
        mShareFile.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onShareFileClicked();
            }
        });

        // triggers the open folder intent for the Android OS //
        mShareFolder = (Button) findViewById(R.id.useShareFolder);
        mShareFolder.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                openFolder();
            }
        });

        // status text box for viewing currently joined group//
//        mChannelName = (TextView) findViewById(R.id.useChannelName);
//        mChannelStatus = (TextView) findViewById(R.id.useChannelStatus);


        ab = getActionBar();
        ab.setTitle("Project258");
        ab.setSubtitle("Join a P2P Group to Share Files");


        mFileShareApp = (FileShareApp) getApplication();
        mFileShareApp.checkin();


        updateChannelState();
        updateHistory();


        mFileShareApp.addObserver(this);

//        // ATTENTION: This was auto-generated to implement the App Indexing API.
//        // See https://g.co/AppIndexing/AndroidStudio for more information.
//        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    public void openAdminActivity(){
        Intent intent = new Intent(this, AdminActivity.class);
        startActivity(intent);
    }

    public void onDestroy() {
        Log.i(TAG, "onDestroy()");
        mFileShareApp = (FileShareApp) getApplication();
        mFileShareApp.deleteObserver(this);
        super.onDestroy();
    }

    public static final int DIALOG_JOIN_ID = 0;
    public static final int DIALOG_LEAVE_ID = 1;
    public static final int DIALOG_ALLJOYN_ERROR_ID = 2;
    public static final int DIALOG_SHARE_ID = 3;

    protected Dialog onCreateDialog(int id) {
        Log.i(TAG, "onCreateDialog()");
        Dialog result = null;
        switch (id) {
            case DIALOG_JOIN_ID: {
                DialogBuilder builder = new DialogBuilder();
                result = builder.createUseJoinDialog(this, mFileShareApp);
            }
            break;
            case DIALOG_LEAVE_ID: {
                DialogBuilder builder = new DialogBuilder();
                result = builder.createUseLeaveDialog(this, mFileShareApp);
            }
            break;
            case DIALOG_ALLJOYN_ERROR_ID: {
                DialogBuilder builder = new DialogBuilder();
                result = builder.createAllJoynErrorDialog(this, mFileShareApp);
            }
            break;
        }
        return result;
    }

    public synchronized void update(Observable o, Object arg) {
        Log.i(TAG, "update(" + arg + ")");
        String qualifier = (String) arg;

        if (qualifier.equals(FileShareApp.APPLICATION_QUIT_EVENT)) {
            Message message = mHandler.obtainMessage(HANDLE_APPLICATION_QUIT_EVENT);
            mHandler.sendMessage(message);
        }

        if (qualifier.equals(FileShareApp.HISTORY_CHANGED_EVENT)) {
            Message message = mHandler.obtainMessage(HANDLE_HISTORY_CHANGED_EVENT);
            mHandler.sendMessage(message);
        }

        if (qualifier.equals(FileShareApp.USE_CHANNEL_STATE_CHANGED_EVENT)) {
            Message message = mHandler.obtainMessage(HANDLE_CHANNEL_STATE_CHANGED_EVENT);
            mHandler.sendMessage(message);
        }

        if (qualifier.equals(FileShareApp.ALLJOYN_ERROR_EVENT)) {
            Message message = mHandler.obtainMessage(HANDLE_ALLJOYN_ERROR_EVENT);
            mHandler.sendMessage(message);
        }
    }

    private void updateHistory() {
        Log.i(TAG, "updateHistory()");
        mHistoryList.clear();
        List<String> messages = mFileShareApp.getHistory();
        for (String message : messages) {
            mHistoryList.add(message);
        }
        mHistoryList.notifyDataSetChanged();

    }

    private void updateChannelState() {
        Log.i(TAG, "updateHistory()");
        BroadcastReceiver.UseChannelState channelState = mFileShareApp.useGetChannelState();
        String name = mFileShareApp.useGetChannelName();
        if (name == null) {
            name = "Not set";
        }
//        mChannelName.setText(name);


        switch (channelState) {
            case IDLE:
//                mChannelStatus.setText("Idle");
                ab.setSubtitle("Join a P2P Group to Share Files");
                mJoinButton.setEnabled(true);
                mLeaveButton.setEnabled(false);
                mShareFile.setEnabled(false);
                break;
            case JOINED:
//                mChannelStatus.setText("Joined");
                ab.setSubtitle("Joined: " + name);
                mJoinButton.setEnabled(false);
                mLeaveButton.setEnabled(true);
                mShareFile.setEnabled(true);
                break;
        }
    }

    /**
     * An AllJoyn error has happened.  Since this activity pops up first we
     * handle the general errors.  We also handle our own errors.
     */
    private void alljoynError() {
        if (mFileShareApp.getErrorModule() == FileShareApp.Module.GENERAL ||
                mFileShareApp.getErrorModule() == FileShareApp.Module.USE) {
            showDialog(DIALOG_ALLJOYN_ERROR_ID);
        }
    }

    private static final int HANDLE_APPLICATION_QUIT_EVENT = 0;
    private static final int HANDLE_HISTORY_CHANGED_EVENT = 1;
    private static final int HANDLE_CHANNEL_STATE_CHANGED_EVENT = 2;
    private static final int HANDLE_ALLJOYN_ERROR_EVENT = 3;

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HANDLE_APPLICATION_QUIT_EVENT: {
                    Log.i(TAG, "mHandler.handleMessage(): HANDLE_APPLICATION_QUIT_EVENT");
                    finish();
                }
                break;
                case HANDLE_HISTORY_CHANGED_EVENT: {
                    Log.i(TAG, "mHandler.handleMessage(): HANDLE_HISTORY_CHANGED_EVENT");
                    updateHistory();
                    break;
                }
                case HANDLE_CHANNEL_STATE_CHANGED_EVENT: {
                    Log.i(TAG, "mHandler.handleMessage(): HANDLE_CHANNEL_STATE_CHANGED_EVENT");
                    updateChannelState();
                    break;
                }
                case HANDLE_ALLJOYN_ERROR_EVENT: {
                    Log.i(TAG, "mHandler.handleMessage(): HANDLE_ALLJOYN_ERROR_EVENT");
                    alljoynError();
                    break;
                }
                default:
                    break;
            }
        }
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.actionbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_BackupNow) {
            startActivity(new Intent(this,DriveActivity.class));
//            mFileShareApp.quit();
            return true;
        }
//        else if (id == R.id.action_RecoverBackup) {
//            //startActivity(new Intent(this,SettingsActivity.class));
//            mFileShareApp.quit();
//            return true;
//        }
        else if(id == R.id.action_hostSetting){
            openAdminActivity();
        }
        else if(id == R.id.action_settings){
            mFileShareApp.quit();
        }

        return super.onOptionsItemSelected(item);
    }



    public void onShareFileClicked(){
        Intent selectFileIntent = new Intent(this, FileExplorer.class);
        startActivityForResult(selectFileIntent, SHARE_SELECTED_FILE);
        //(selectFileIntent);
    }



    private String getAccountName() {

        String accountName = null;

        AccountManager manager = (AccountManager) getSystemService(ACCOUNT_SERVICE);
        Account[] list = manager.getAccounts();
        for (Account account : list) {
            if (account.type.equalsIgnoreCase("com.google")) {
                accountName = account.name;
                break;
            }
        }
        return accountName;
    }


    public void fileShare(String Path){
        try {

            Context context = getApplicationContext();
            CharSequence text;
            int duration = Toast.LENGTH_LONG;
            String senderInfo = getAccountName();
            File fileObject = new File(Path);
            messageObj.sender=senderInfo;
            messageObj.fileName=fileObject.getName();

            text = "File sent: "+fileObject.getName();
            //byte [] fileBytesArray = FileUtils.readFileToByteArray(fileObject);
            RandomAccessFile f = new RandomAccessFile(fileObject, "r");
            byte[] b = new byte[(int) f.length()];
            f.read(b);
            String contentsAsStringBase64 = Base64.encodeToString(b, Base64.DEFAULT);
            messageObj.fileData=contentsAsStringBase64;
            mFileShareApp.newLocalUserMessage(messageObj);                                   ///TODO Json message type
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        }catch (IOException e){

        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent intent)
    {

        //the intent is null if the FileExplorer was exited
        //without a selection being made
        if (intent == null)
        {
            return;
        }

        if (requestCode == SHARE_SELECTED_FILE)
        {
                //retrieve selected file from FileExplorer intent
                File selected = (File) intent.getExtras().get("file");
                filePathstring = selected.getAbsolutePath();
                Log.i(TAG,"selected file is " + filePathstring);
                //filePathstring = selected.getAbsolutePath();
                fileShare(filePathstring);
            return;
        }
    }

    public void openFolder()
    {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = Uri.parse(Environment.getExternalStorageDirectory().getPath()
                + "/Project258/");
        intent.setDataAndType(uri,"resource/folder");
        startActivity(Intent.createChooser(intent, "Open Folder With:"));

    }

}
