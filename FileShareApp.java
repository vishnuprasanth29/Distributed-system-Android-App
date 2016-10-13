package com.Project258;
/**
 * Created by Raj on 2/24/2016.
 */
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Application;

import android.content.ComponentName;
import android.content.Intent;

import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import java.util.Date;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class FileShareApp extends Application implements Observable {
    private static final String TAG = "Project258.FileShareApp";
    public static String PACKAGE_NAME;

    public void onCreate() {
        Log.i(TAG, "onCreate()");
        PACKAGE_NAME = getApplicationContext().getPackageName();
        Intent intent = new Intent(this, BroadcastReceiver.class);
        mRunningService = startService(intent);

        if (mRunningService == null) {
            Log.i(TAG, "onCreate(): failed to startService()");
        }

    }

    ComponentName mRunningService = null;


    public void quit() {
        notifyObservers(APPLICATION_QUIT_EVENT);
        mRunningService = null;
    }


    public void checkin() {
        Log.i(TAG, "checkin()");
        if (mRunningService == null) {
            Log.i(TAG, "checkin():  Starting the BroadcastReceiver");
            Intent intent = new Intent(this, BroadcastReceiver.class);
            mRunningService = startService(intent);
            if (mRunningService == null) {
                Log.i(TAG, "checkin(): failed to startService()");
            }
        }
    }

    public static final String APPLICATION_QUIT_EVENT = "APPLICATION_QUIT_EVENT";


    public synchronized void alljoynError(Module m, String s) {
        mModule = m;
        mErrorString = s;
        notifyObservers(ALLJOYN_ERROR_EVENT);
    }


    public Module getErrorModule() {
        return mModule;
    }


    private Module mModule = Module.NONE;


    public static enum Module {
        NONE,
        GENERAL,
        USE,
        HOST
    }


    public String getErrorString() {
        return mErrorString;
    }


    private String mErrorString = "ER_OK";

    public static final String ALLJOYN_ERROR_EVENT = "ALLJOYN_ERROR_EVENT";


    public synchronized void addFoundChannel(String channel) {
        Log.i(TAG, "addFoundChannel(" + channel + ")");
        removeFoundChannel(channel);
        mChannels.add(channel);
        Log.i(TAG, "addFoundChannel(): added " + channel);
    }


    public synchronized void removeFoundChannel(String channel) {
        Log.i(TAG, "removeFoundChannel(" + channel + ")");

        for (Iterator<String> i = mChannels.iterator(); i.hasNext();) {
            String string = i.next();
            if (string.equals(channel)) {
                Log.i(TAG, "removeFoundChannel(): removed " + channel);
                i.remove();
            }
        }
    }


    public synchronized List<String> getFoundChannels() {
        Log.i(TAG, "getFoundChannels()");
        List<String> clone = new ArrayList<String>(mChannels.size());
        for (String string : mChannels) {
            Log.i(TAG, "getFoundChannels(): added " + string);
            clone.add(new String(string));
        }
        return clone;
    }


    private List<String> mChannels = new ArrayList<String>();


    public BroadcastReceiver.BusAttachmentState mBusAttachmentState = BroadcastReceiver.BusAttachmentState.DISCONNECTED;


    public synchronized void hostSetChannelState(BroadcastReceiver.HostChannelState state) {
        mHostChannelState = state;
        notifyObservers(HOST_CHANNEL_STATE_CHANGED_EVENT);
    }


    public synchronized BroadcastReceiver.HostChannelState hostGetChannelState() {
        return mHostChannelState;
    }

    private BroadcastReceiver.HostChannelState mHostChannelState = BroadcastReceiver.HostChannelState.IDLE;


    public synchronized void hostSetChannelName(String name) {
        mHostChannelName = name;
        notifyObservers(HOST_CHANNEL_STATE_CHANGED_EVENT);
    }


    public synchronized String hostGetChannelName() {
        return mHostChannelName;
    }


    private String mHostChannelName;

    public static final String HOST_CHANNEL_STATE_CHANGED_EVENT = "HOST_CHANNEL_STATE_CHANGED_EVENT";


    public synchronized void useSetChannelState(BroadcastReceiver.UseChannelState state) {
        mUseChannelState = state;
        notifyObservers(USE_CHANNEL_STATE_CHANGED_EVENT);
    }


    public synchronized BroadcastReceiver.UseChannelState useGetChannelState() {
        return mUseChannelState;
    }


    private BroadcastReceiver.UseChannelState mUseChannelState = BroadcastReceiver.UseChannelState.IDLE;


    private String mUseChannelName;


    public synchronized void useSetChannelName(String name) {
        mUseChannelName = name;
        notifyObservers(USE_CHANNEL_STATE_CHANGED_EVENT);
    }


    public synchronized String useGetChannelName() {
        return mUseChannelName;
    }


    public static final String USE_CHANNEL_STATE_CHANGED_EVENT = "USE_CHANNEL_STATE_CHANGED_EVENT";


    public synchronized void useJoinChannel() {
        clearHistory();
        notifyObservers(USE_CHANNEL_STATE_CHANGED_EVENT);
        notifyObservers(USE_JOIN_CHANNEL_EVENT);
    }


    public static final String USE_JOIN_CHANNEL_EVENT = "USE_JOIN_CHANNEL_EVENT";


    public synchronized void useLeaveChannel() {
        notifyObservers(USE_CHANNEL_STATE_CHANGED_EVENT);
        notifyObservers(USE_LEAVE_CHANNEL_EVENT);
    }


    public static final String USE_LEAVE_CHANNEL_EVENT = "USE_LEAVE_CHANNEL_EVENT";


    public synchronized void hostInitChannel() {
        notifyObservers(HOST_CHANNEL_STATE_CHANGED_EVENT);
        notifyObservers(HOST_INIT_CHANNEL_EVENT);
    }


    public static final String HOST_INIT_CHANNEL_EVENT = "HOST_INIT_CHANNEL_EVENT";


    public synchronized void hostStartChannel() {
        notifyObservers(HOST_CHANNEL_STATE_CHANGED_EVENT);
        notifyObservers(HOST_START_CHANNEL_EVENT);
    }


    public static final String HOST_START_CHANNEL_EVENT = "HOST_START_CHANNEL_EVENT";


    public synchronized void hostStopChannel() {
        notifyObservers(HOST_CHANNEL_STATE_CHANGED_EVENT);
        notifyObservers(HOST_STOP_CHANNEL_EVENT);
    }


    public static final String HOST_STOP_CHANNEL_EVENT = "HOST_STOP_CHANNEL_EVENT";


    public synchronized void newLocalUserMessage(JsonMessage message) {
        addInboundItem("Me", message);
        if (useGetChannelState() == BroadcastReceiver.UseChannelState.JOINED) {
            addOutboundItem(message);
        }
    }



    public synchronized void newRemoteUserMessage(String nickname, String json) {
        Gson gson = new Gson();
        JsonMessage message = gson.fromJson(json, JsonMessage.class);
        addInboundItem(nickname, message);
    }

    final int OUTBOUND_MAX = 5;

    public static final String OUTBOUND_CHANGED_EVENT = "OUTBOUND_CHANGED_EVENT";


    private List<String> mOutbound = new ArrayList<String>();


    private void addOutboundItem(JsonMessage message) {
        if (mOutbound.size() == OUTBOUND_MAX) {
            mOutbound.remove(0);
        }
        Gson gson = new Gson();
        String json = gson.toJson(message);
        mOutbound.add(json);
        notifyObservers(OUTBOUND_CHANGED_EVENT);
    }


    public synchronized String getOutboundItem() {
        if (mOutbound.isEmpty()) {
            return null;
        } else {
            return mOutbound.remove(0);
        }
    }


    public static final String HISTORY_CHANGED_EVENT = "HISTORY_CHANGED_EVENT";


    private void addInboundItem(String nickname, JsonMessage message) {
        addHistoryItem(nickname, message);
    }

    /////////////////////////////// History block ////////////////////////////////////

    final int HISTORY_MAX = 20;


    private List<String> mHistory = new ArrayList<String>();

    public void createDirectory(){
        File file = new File("sdcard/Project258/");
        if (!file.mkdirs()) {
            Log.e("createDirectory :: ", "Problem creating Project folder");
            file.mkdir();
        }
    }
//    public void historyAppendLog(String text){
//        String groupName = useGetChannelName();
//        createDirectory(groupName);
//        File file = new File("sdcard/Project258/" + groupName + ".txt");
//        if (!file.exists()){
//            try{
//                file.createNewFile();
//            }
//            catch (IOException e){
//                e.printStackTrace();
//            }
//        }
//        try{
//            //BufferedWriter for performance, true to set append to file flag
//            BufferedWriter buf = new BufferedWriter(new FileWriter(file, true));
//            buf.append(text);
//            buf.newLine();
//            buf.close();
//        }
//        catch (IOException e){
//            e.printStackTrace();
//        }
//    }

    public void writeFile(JsonMessage message){
      //  String groupName = useGetChannelName();
        createDirectory();
        File fileObject = new File("sdcard/Project258/" + message.fileName);
        if (!fileObject.exists()){
            try{
                fileObject.createNewFile();
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }
        try{
            byte [] fileBytesArray = Base64.decode(message.fileData, Base64.DEFAULT);
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(fileObject));
            bos.write(fileBytesArray);
            bos.flush();
            bos.close();
        }catch (IOException e){

        }
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


    private void addHistoryItem(String nickname, JsonMessage message) {
        if (mHistory.size() == HISTORY_MAX) {
            mHistory.remove(0);
        }

        String sender = getAccountName();
        DateFormat dateFormat = new SimpleDateFormat("HH:mm");
        DateFormat dateFull = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");
        ///need to check for arraylist empty to trigger date and time to be added?
        Date date = new Date();
        notifyObservers(HISTORY_CHANGED_EVENT);
        //String writeToLog = "[" + dateFormat.format(date) + "] (" + nickname + ") " + message;
        //historyAppendLog(writeToLog);
        writeFile(message);
        if(nickname == "Me") {
            mHistory.add("[" + dateFormat.format(date) + "] "+ nickname +": " + message.fileName);
        }
        else {
            mHistory.add("[" + dateFormat.format(date) + "] " + message.sender + ": " + message.fileName);
        }
    }


    private void clearHistory() {
        mHistory.clear();
        notifyObservers(HISTORY_CHANGED_EVENT);
    }


    public synchronized List<String> getHistory() {
        List<String> clone = new ArrayList<String>(mHistory.size());
        for (String string : mHistory) {
            clone.add(new String(string));
        }
        return clone;
    }


    public synchronized void addObserver(Observer obs) {
        Log.i(TAG, "addObserver(" + obs + ")");
        if (mObservers.indexOf(obs) < 0) {
            mObservers.add(obs);
        }
    }


    public synchronized void deleteObserver(Observer obs) {
        Log.i(TAG, "deleteObserver(" + obs + ")");
        mObservers.remove(obs);
    }


    private void notifyObservers(Object arg) {
        Log.i(TAG, "notifyObservers(" + arg + ")");
        for (Observer obs : mObservers) {
            Log.i(TAG, "notify observer = " + obs);
            obs.update(this, arg);
        }
    }


    private List<Observer> mObservers = new ArrayList<Observer>();


}
