/*
 * Copyright AllSeen Alliance. All rights reserved.
 *
 *    Permission to use, copy, modify, and/or distribute this software for any
 *    purpose with or without fee is hereby granted, provided that the above
 *    copyright notice and this permission notice appear in all copies.
 *
 *    THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 *    WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 *    MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 *    ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 *    WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 *    ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 *    OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package com.Project258;

import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusListener;
import org.alljoyn.bus.MessageContext;
import org.alljoyn.bus.SessionListener;
import org.alljoyn.bus.SessionPortListener;
import org.alljoyn.bus.Mutable;
import org.alljoyn.bus.SessionOpts;
import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.SignalEmitter;
import org.alljoyn.bus.Status;
import org.alljoyn.bus.annotation.BusSignalHandler;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;

import android.content.Intent;

import android.util.Log;

public class BroadcastReceiver extends Service implements Observer {
    private static final String TAG = "chat.BroadcastReceiver";

// bind not used to communicate with clients //
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind()");
        return null;
    }


    public void onCreate() {
        Log.i(TAG, "onCreate()");
        startBusThread();
        mFileShareApp = (FileShareApp)getApplication();
        mFileShareApp.addObserver(this);

        // android notification data //
        CharSequence title = "Project258: Connection Manager";
        CharSequence message = "Connection Manager Service is Running";
        Intent intent = new Intent(this, UserActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        // set a persistant notification indicating the BroadcastReceiver service is running//
        Notification notification = new Notification(R.mipmap.ic_launcher, null, System.currentTimeMillis());
        notification.setLatestEventInfo(this, title, message, pendingIntent);
        notification.flags |= Notification.DEFAULT_SOUND | Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;

        Log.i(TAG, "onCreate(): startForeground()");
        startForeground(NOTIFICATION_ID, notification);

    // connect to the ALLJOYN bus and start searching for peers using a HandlerThread declared later //
        mBackgroundHandler.connect();
        mBackgroundHandler.startDiscovery();
    }

    private static final int NOTIFICATION_ID = 0xdefaced;

// onDestroy called by Android, we set all threads to abort , stop peer discovery, disconnect from peers and destroy all handlers //
    public void onDestroy() {
        Log.i(TAG, "onDestroy()");
        mBackgroundHandler.cancelDiscovery();
        mBackgroundHandler.disconnect();
        stopBusThread();
        mFileShareApp.deleteObserver(this);
    }


// return START_STICKY to make the BroadcastReceiver service never ending and destroyed only on Quit application call //
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand()");
        return START_STICKY;
    }

// initializing the FileShareApp class object, Model part of the MVC arch use in this application //
    private FileShareApp mFileShareApp = null;


//update method is called when a change has occurred in the state of the observable//
    public synchronized void update(Observable o, Object arg) {
        Log.i(TAG, "update(" + arg + ")");
        String qualifier = (String)arg;

        if (qualifier.equals(FileShareApp.APPLICATION_QUIT_EVENT)) {
            Message message = mHandler.obtainMessage(HANDLE_APPLICATION_QUIT_EVENT);
            mHandler.sendMessage(message);
        }

        if (qualifier.equals(FileShareApp.USE_JOIN_CHANNEL_EVENT)) {
            Message message = mHandler.obtainMessage(HANDLE_USE_JOIN_CHANNEL_EVENT);
            mHandler.sendMessage(message);
        }

        if (qualifier.equals(FileShareApp.USE_LEAVE_CHANNEL_EVENT)) {
            Message message = mHandler.obtainMessage(HANDLE_USE_LEAVE_CHANNEL_EVENT);
            mHandler.sendMessage(message);
        }

        if (qualifier.equals(FileShareApp.HOST_INIT_CHANNEL_EVENT)) {
            Message message = mHandler.obtainMessage(HANDLE_HOST_INIT_CHANNEL_EVENT);
            mHandler.sendMessage(message);
        }

        if (qualifier.equals(FileShareApp.HOST_START_CHANNEL_EVENT)) {
            Message message = mHandler.obtainMessage(HANDLE_HOST_START_CHANNEL_EVENT);
            mHandler.sendMessage(message);
        }

        if (qualifier.equals(FileShareApp.HOST_STOP_CHANNEL_EVENT)) {
            Message message = mHandler.obtainMessage(HANDLE_HOST_STOP_CHANNEL_EVENT);
            mHandler.sendMessage(message);
        }

        if (qualifier.equals(FileShareApp.OUTBOUND_CHANGED_EVENT)) {
            Message message = mHandler.obtainMessage(HANDLE_OUTBOUND_CHANGED_EVENT);
            mHandler.sendMessage(message);
        }
    }



    // handler thread to handle the observer/observable functions//
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HANDLE_APPLICATION_QUIT_EVENT:                                             // application quit event
                {
                    Log.i(TAG, "mHandler.handleMessage(): APPLICATION_QUIT_EVENT");
                    mBackgroundHandler.leaveSession();
                    mBackgroundHandler.cancelAdvertise();
                    mBackgroundHandler.unbindSession();
                    mBackgroundHandler.releaseName();
                    mBackgroundHandler.exit();
                    stopSelf();
                }
                break;
            case HANDLE_USE_JOIN_CHANNEL_EVENT:                                                   // Join group event -- user functionality
                {
                    Log.i(TAG, "mHandler.handleMessage(): USE_JOIN_CHANNEL_EVENT");
                    mBackgroundHandler.joinSession();
                }
                break;
            case HANDLE_USE_LEAVE_CHANNEL_EVENT:                                                   // leave group event -- user functionality
                {
                    Log.i(TAG, "mHandler.handleMessage(): USE_LEAVE_CHANNEL_EVENT");
                    mBackgroundHandler.leaveSession();
                }
                break;
            case HANDLE_HOST_INIT_CHANNEL_EVENT:                                                    // admin inti group event -- admin functionality
                {
                    Log.i(TAG, "mHandler.handleMessage(): HOST_INIT_CHANNEL_EVENT");
                }
                break;
            case HANDLE_HOST_START_CHANNEL_EVENT:                                                   // start group event -- admin functionality
                {
                    Log.i(TAG, "mHandler.handleMessage(): HOST_START_CHANNEL_EVENT");
                    mBackgroundHandler.requestName();
                    mBackgroundHandler.bindSession();
                    mBackgroundHandler.advertise();
                }
                break;
            case HANDLE_HOST_STOP_CHANNEL_EVENT:                                                    //stop group event -- admin fucntionality
                {
                    Log.i(TAG, "mHandler.handleMessage(): HOST_STOP_CHANNEL_EVENT");
                    mBackgroundHandler.cancelAdvertise();
                    mBackgroundHandler.unbindSession();
                    mBackgroundHandler.releaseName();
                }
                break;
            case HANDLE_OUTBOUND_CHANGED_EVENT:                                                     // send outbound message -- user functionality
                {
                    Log.i(TAG, "mHandler.handleMessage(): OUTBOUND_CHANGED_EVENT");
                    mBackgroundHandler.sendMessages();
                }
                break;
            default:
                break;
            }
        }
    };


    private static final int HANDLE_APPLICATION_QUIT_EVENT = 0;

    private static final int HANDLE_USE_JOIN_CHANNEL_EVENT = 1;

    private static final int HANDLE_USE_LEAVE_CHANNEL_EVENT = 2;

    private static final int HANDLE_HOST_INIT_CHANNEL_EVENT = 3;

    private static final int HANDLE_HOST_START_CHANNEL_EVENT = 4;

    private static final int HANDLE_HOST_STOP_CHANNEL_EVENT = 5;

    private static final int HANDLE_OUTBOUND_CHANGED_EVENT = 6;


    public static enum BusAttachmentState {
        DISCONNECTED,    // The bus attachment is not connected to the AllJoyn bus
        CONNECTED,        // The  bus attachment is connected to the AllJoyn bus
        DISCOVERING        // The bus attachment is discovering remote attachments hosting chat channels
    }


    private BusAttachmentState mBusAttachmentState = BusAttachmentState.DISCONNECTED;

    public static enum HostChannelState {
        IDLE,            // There is no hosted chat channel
        NAMED,            // The well-known name for the channel has been successfully acquired
        BOUND,            // A session port has been bound for the channel
        ADVERTISED,       // The bus attachment has advertised itself as hosting an chat channel
        CONNECTED         // At least one remote device has connected to a session on the channel
    }


    private HostChannelState mHostChannelState = HostChannelState.IDLE;


    public static enum UseChannelState {
        IDLE,            /** There is no used chat channel */
        JOINED,            /** The session for the channel has been successfully joined */
    }


    private UseChannelState mUseChannelState = UseChannelState.IDLE;

      private final class BackgroundHandler extends Handler {
        public BackgroundHandler(Looper looper) {
            super(looper);
        }


        public void exit() {
            Log.i(TAG, "mBackgroundHandler.exit()");
            Message msg = mBackgroundHandler.obtainMessage(EXIT);
            mBackgroundHandler.sendMessage(msg);
        }


        public void connect() {
            Log.i(TAG, "mBackgroundHandler.connect()");
            Message msg = mBackgroundHandler.obtainMessage(CONNECT);
            mBackgroundHandler.sendMessage(msg);
        }


        public void disconnect() {
            Log.i(TAG, "mBackgroundHandler.disconnect()");
            Message msg = mBackgroundHandler.obtainMessage(DISCONNECT);
            mBackgroundHandler.sendMessage(msg);
        }


        public void startDiscovery() {
            Log.i(TAG, "mBackgroundHandler.startDiscovery()");
            Message msg = mBackgroundHandler.obtainMessage(START_DISCOVERY);
            mBackgroundHandler.sendMessage(msg);
        }


        public void cancelDiscovery() {
            Log.i(TAG, "mBackgroundHandler.stopDiscovery()");
            Message msg = mBackgroundHandler.obtainMessage(CANCEL_DISCOVERY);
            mBackgroundHandler.sendMessage(msg);
        }

        public void requestName() {
            Log.i(TAG, "mBackgroundHandler.requestName()");
            Message msg = mBackgroundHandler.obtainMessage(REQUEST_NAME);
            mBackgroundHandler.sendMessage(msg);
        }

        public void releaseName() {
            Log.i(TAG, "mBackgroundHandler.releaseName()");
            Message msg = mBackgroundHandler.obtainMessage(RELEASE_NAME);
            mBackgroundHandler.sendMessage(msg);
        }

        public void bindSession() {
            Log.i(TAG, "mBackgroundHandler.bindSession()");
            Message msg = mBackgroundHandler.obtainMessage(BIND_SESSION);
            mBackgroundHandler.sendMessage(msg);
        }

        public void unbindSession() {
            Log.i(TAG, "mBackgroundHandler.unbindSession()");
            Message msg = mBackgroundHandler.obtainMessage(UNBIND_SESSION);
            mBackgroundHandler.sendMessage(msg);
        }

        public void advertise() {
            Log.i(TAG, "mBackgroundHandler.advertise()");
            Message msg = mBackgroundHandler.obtainMessage(ADVERTISE);
            mBackgroundHandler.sendMessage(msg);
        }

        public void cancelAdvertise() {
            Log.i(TAG, "mBackgroundHandler.cancelAdvertise()");
            Message msg = mBackgroundHandler.obtainMessage(CANCEL_ADVERTISE);
            mBackgroundHandler.sendMessage(msg);
        }

        public void joinSession() {
            Log.i(TAG, "mBackgroundHandler.joinSession()");
            Message msg = mBackgroundHandler.obtainMessage(JOIN_SESSION);
            mBackgroundHandler.sendMessage(msg);
        }

        public void leaveSession() {
            Log.i(TAG, "mBackgroundHandler.leaveSession()");
            Message msg = mBackgroundHandler.obtainMessage(LEAVE_SESSION);
            mBackgroundHandler.sendMessage(msg);
        }

        public void sendMessages() {
            Log.i(TAG, "mBackgroundHandler.sendMessages()");
            Message msg = mBackgroundHandler.obtainMessage(SEND_MESSAGES);
            mBackgroundHandler.sendMessage(msg);
        }


        public void handleMessage(Message msg) {
            switch (msg.what) {
            case CONNECT:
                doConnect();
                break;
            case DISCONNECT:
                doDisconnect();
                break;
            case START_DISCOVERY:
                doStartDiscovery();
                break;
            case CANCEL_DISCOVERY:
                doStopDiscovery();
                break;
            case REQUEST_NAME:
                doRequestName();
                break;
            case RELEASE_NAME:
                doReleaseName();
                break;
            case BIND_SESSION:
                doBindSession();
                break;
            case UNBIND_SESSION:
                doUnbindSession();
                break;
            case ADVERTISE:
                doAdvertise();
                break;
            case CANCEL_ADVERTISE:
                doCancelAdvertise();
                break;
            case JOIN_SESSION:
                doJoinSession();
                break;
            case LEAVE_SESSION:
                doLeaveSession();
                break;
            case SEND_MESSAGES:
                doSendMessages();
                break;
            case EXIT:
                getLooper().quit();
                break;
            default:
                break;
            }
        }
    }

    private static final int EXIT = 1;
    private static final int CONNECT = 2;
    private static final int DISCONNECT = 3;
    private static final int START_DISCOVERY = 4;
    private static final int CANCEL_DISCOVERY = 5;
    private static final int REQUEST_NAME = 6;
    private static final int RELEASE_NAME = 7;
    private static final int BIND_SESSION = 8;
    private static final int UNBIND_SESSION = 9;
    private static final int ADVERTISE = 10;
    private static final int CANCEL_ADVERTISE = 11;
    private static final int JOIN_SESSION = 12;
    private static final int LEAVE_SESSION = 13;
    private static final int SEND_MESSAGES = 14;


    private BackgroundHandler mBackgroundHandler = null;


    private void startBusThread() {
        HandlerThread busThread = new HandlerThread("BackgroundHandler");
        busThread.start();
        mBackgroundHandler = new BackgroundHandler(busThread.getLooper());
    }


    private void stopBusThread() {
        mBackgroundHandler.exit();
    }

    private BusAttachment mBus  = new BusAttachment(FileShareApp.PACKAGE_NAME, BusAttachment.RemoteMessage.Receive);

    private static final String NAME_PREFIX = "com.Project258";

    private static final short CONTACT_PORT = 27;

    private static final String OBJECT_PATH = "/chatService";

    private class ChatBusListener extends BusListener {

        public void foundAdvertisedName(String name, short transport, String namePrefix) {
            Log.i(TAG, "mBusListener.foundAdvertisedName(" + name + ")");
            FileShareApp application = (FileShareApp)getApplication();
            application.addFoundChannel(name);
        }

        public void lostAdvertisedName(String name, short transport, String namePrefix) {
            Log.i(TAG, "mBusListener.lostAdvertisedName(" + name + ")");
            FileShareApp application = (FileShareApp)getApplication();
            application.removeFoundChannel(name);
        }
    }

    private ChatBusListener mBusListener = new ChatBusListener();

    private void doConnect() {
        Log.i(TAG, "doConnect()");
        org.alljoyn.bus.alljoyn.DaemonInit.PrepareDaemon(getApplicationContext());
        assert(mBusAttachmentState == BusAttachmentState.DISCONNECTED);
        mBus.useOSLogging(true);
        mBus.setDebugLevel("ALLJOYN_JAVA", 7);
        mBus.registerBusListener(mBusListener);

        Status status = mBus.registerBusObject(mChatService, OBJECT_PATH);
        if (Status.OK != status) {
            mFileShareApp.alljoynError(FileShareApp.Module.HOST, "Unable to register the Alljoyn bus object: (" + status + ")");
            return;
        }

        status = mBus.connect();
        if (status != Status.OK) {
            mFileShareApp.alljoynError(FileShareApp.Module.GENERAL, "Unable to connect to the bus: (" + status + ")");
            return;
        }

        status = mBus.registerSignalHandlers(this);
        if (status != Status.OK) {
            mFileShareApp.alljoynError(FileShareApp.Module.GENERAL, "Unable to register signal handlers: (" + status + ")");
            return;
        }

        mBusAttachmentState = BusAttachmentState.CONNECTED;
    }


    private boolean doDisconnect() {
        Log.i(TAG, "doDisonnect()");
        assert(mBusAttachmentState == BusAttachmentState.CONNECTED);
        mBus.unregisterBusListener(mBusListener);
        mBus.disconnect();
        mBusAttachmentState = BusAttachmentState.DISCONNECTED;
        return true;
    }


    private void doStartDiscovery() {
        Log.i(TAG, "doStartDiscovery()");
        assert(mBusAttachmentState == BusAttachmentState.CONNECTED);
        Status status = mBus.findAdvertisedName(NAME_PREFIX);
        if (status == Status.OK) {
            mBusAttachmentState = BusAttachmentState.DISCOVERING;
            return;
        } else {
            mFileShareApp.alljoynError(FileShareApp.Module.USE, "Unable to start finding advertised names: (" + status + ")");
            return;
        }
    }


    private void doStopDiscovery() {
        Log.i(TAG, "doStopDiscovery()");
        assert(mBusAttachmentState == BusAttachmentState.CONNECTED);
        mBus.cancelFindAdvertisedName(NAME_PREFIX);
        mBusAttachmentState = BusAttachmentState.CONNECTED;
    }


    private void doRequestName() {
        Log.i(TAG, "doRequestName()");

        int stateRelation = mBusAttachmentState.compareTo(BusAttachmentState.DISCONNECTED);
        assert (stateRelation >= 0);

        String wellKnownName = NAME_PREFIX + "." + mFileShareApp.hostGetChannelName();
        Status status = mBus.requestName(wellKnownName, BusAttachment.ALLJOYN_REQUESTNAME_FLAG_DO_NOT_QUEUE);
        if (status == Status.OK) {
            mHostChannelState = HostChannelState.NAMED;
            mFileShareApp.hostSetChannelState(mHostChannelState);
        } else {
            mFileShareApp.alljoynError(FileShareApp.Module.USE, "Unable to acquire well-known name: (" + status + ")");
        }
    }


    private void doReleaseName() {
        Log.i(TAG, "doReleaseName()");

        int stateRelation = mBusAttachmentState.compareTo(BusAttachmentState.DISCONNECTED);
        assert (stateRelation >= 0);
        assert(mBusAttachmentState == BusAttachmentState.CONNECTED || mBusAttachmentState == BusAttachmentState.DISCOVERING);

        assert(mHostChannelState == HostChannelState.NAMED);

        String wellKnownName = NAME_PREFIX + "." + mFileShareApp.hostGetChannelName();

        mBus.releaseName(wellKnownName);
        mHostChannelState = HostChannelState.IDLE;
        mFileShareApp.hostSetChannelState(mHostChannelState);
    }


    private void doBindSession() {
        Log.i(TAG, "doBindSession()");

        Mutable.ShortValue contactPort = new Mutable.ShortValue(CONTACT_PORT);
        SessionOpts sessionOpts = new SessionOpts(SessionOpts.TRAFFIC_MESSAGES, true, SessionOpts.PROXIMITY_ANY, SessionOpts.TRANSPORT_ANY);

        Status status = mBus.bindSessionPort(contactPort, sessionOpts, new SessionPortListener() {

            public boolean acceptSessionJoiner(short sessionPort, String joiner, SessionOpts sessionOpts) {
                Log.i(TAG, "SessionPortListener.acceptSessionJoiner(" + sessionPort + ", " + joiner + ", " + sessionOpts.toString() + ")");

                /*
                 * Accept anyone who can get our contact port correct.
                 */
                if (sessionPort == CONTACT_PORT) {
                    return true;
                }
                return false;
            }


            public void sessionJoined(short sessionPort, int id, String joiner) {
                Log.i(TAG, "SessionPortListener.sessionJoined(" + sessionPort + ", " + id + ", " + joiner + ")");
                mHostSessionId = id;
                SignalEmitter emitter = new SignalEmitter(mChatService, id, SignalEmitter.GlobalBroadcast.Off);
                mHostFileShareInterface = emitter.getInterface(FileShareInterface.class);
            }
        });

        if (status == Status.OK) {
            mHostChannelState = HostChannelState.BOUND;
            mFileShareApp.hostSetChannelState(mHostChannelState);
        } else {
            mFileShareApp.alljoynError(FileShareApp.Module.HOST, "Unable to bind session contact port: (" + status + ")");
            return;
        }
    }


    private void doUnbindSession() {
        Log.i(TAG, "doUnbindSession()");
        mBus.unbindSessionPort(CONTACT_PORT);
        mHostFileShareInterface = null;
        mHostChannelState = HostChannelState.NAMED;
        mFileShareApp.hostSetChannelState(mHostChannelState);
    }


    int mHostSessionId = -1;

    boolean mJoinedToSelf = false;

    FileShareInterface mHostFileShareInterface = null;

    private void doAdvertise() {
        Log.i(TAG, "doAdvertise()");

        String wellKnownName = NAME_PREFIX + "." + mFileShareApp.hostGetChannelName();
        Status status = mBus.advertiseName(wellKnownName, SessionOpts.TRANSPORT_ANY);

        if (status == Status.OK) {
            mHostChannelState = HostChannelState.ADVERTISED;
            mFileShareApp.hostSetChannelState(mHostChannelState);
        } else {
            mFileShareApp.alljoynError(FileShareApp.Module.HOST, "Unable to advertise well-known name: (" + status + ")");
            return;
        }
    }


    private void doCancelAdvertise() {
        Log.i(TAG, "doCancelAdvertise()");

        String wellKnownName = NAME_PREFIX + "." + mFileShareApp.hostGetChannelName();
        Status status = mBus.cancelAdvertiseName(wellKnownName, SessionOpts.TRANSPORT_ANY);

        if (status != Status.OK) {
            mFileShareApp.alljoynError(FileShareApp.Module.HOST, "Unable to cancel advertisement of well-known name: (" + status + ")");
            return;
        }

        mHostChannelState = HostChannelState.BOUND;
        mFileShareApp.hostSetChannelState(mHostChannelState);
    }

    private void doJoinSession() {
        Log.i(TAG, "doJoinSession()");

        if (mHostChannelState != HostChannelState.IDLE) {
            if (mFileShareApp.useGetChannelName().equals(mFileShareApp.hostGetChannelName())) {
                mUseChannelState = UseChannelState.JOINED;
                mFileShareApp.useSetChannelState(mUseChannelState);
                mJoinedToSelf = true;
                return;
            }
        }

        String wellKnownName = NAME_PREFIX + "." + mFileShareApp.useGetChannelName();


        short contactPort = CONTACT_PORT;
        SessionOpts sessionOpts = new SessionOpts(SessionOpts.TRAFFIC_MESSAGES, true, SessionOpts.PROXIMITY_ANY, SessionOpts.TRANSPORT_ANY);
        Mutable.IntegerValue sessionId = new Mutable.IntegerValue();

        Status status = mBus.joinSession(wellKnownName, contactPort, sessionId, sessionOpts, new SessionListener() {
            /**
             * This method is called when the last remote participant in the
             * chat session leaves for some reason and we no longer have anyone
             * to chat with.
             *
             * In the class documentation for the BusListener note that it is a
             * requirement for this method to be multithread safe.  This is
             * accomplished by the use of a monitor on the FileShareApp as
             * exemplified by the synchronized attribute of the removeFoundChannel
             * method there.
             */
            public void sessionLost(int sessionId, int reason) {
                Log.i(TAG, "BusListener.sessionLost(sessionId=" + sessionId + ",reason=" + reason + ")");
                mFileShareApp.alljoynError(FileShareApp.Module.USE, "The Alljoyn session has been lost");
                mUseChannelState = UseChannelState.IDLE;
                mFileShareApp.useSetChannelState(mUseChannelState);
            }
        });

        if (status == Status.OK) {
            Log.i(TAG, "doJoinSession(): use sessionId is " + mUseSessionId);
            mUseSessionId = sessionId.value;
        } else {
            mFileShareApp.alljoynError(FileShareApp.Module.USE, "Unable to join Alljoyn session: (" + status + ")");
            return;
        }

        SignalEmitter emitter = new SignalEmitter(mChatService, mUseSessionId, SignalEmitter.GlobalBroadcast.Off);
        mFileShareInterface = emitter.getInterface(FileShareInterface.class);

        mUseChannelState = UseChannelState.JOINED;
        mFileShareApp.useSetChannelState(mUseChannelState);
    }


    FileShareInterface mFileShareInterface = null;


    private void doLeaveSession() {
        Log.i(TAG, "doLeaveSession()");
        if (mJoinedToSelf == false) {
            mBus.leaveSession(mUseSessionId);
        }
        mUseSessionId = -1;
        mJoinedToSelf = false;
        mUseChannelState = UseChannelState.IDLE;
        mFileShareApp.useSetChannelState(mUseChannelState);
    }


    int mUseSessionId = -1;


    private void doSendMessages() {
        Log.i(TAG, "doSendMessages()");

        String message;
        while ((message = mFileShareApp.getOutboundItem()) != null) {
            Log.i(TAG, "doSendMessages(): sending message \"" + message + "\"");

            try {
                if (mJoinedToSelf) {
                    if (mHostFileShareInterface != null) {
                        mHostFileShareInterface.Chat(message);
                    }
                } else {
                    mFileShareInterface.Chat(message);
                }
            } catch (BusException ex) {
                mFileShareApp.alljoynError(FileShareApp.Module.USE, "Bus exception while sending message: (" + ex + ")");
            }
        }
    }


    class FileShareService implements FileShareInterface, BusObject {

        public void Chat(String str) throws BusException {
        }
    }


    private FileShareService mChatService = new FileShareService();


    @BusSignalHandler(iface = "com.Project258", signal = "Chat")
    public void Chat(String string) {

        String uniqueName = mBus.getUniqueName();
        MessageContext ctx = mBus.getMessageContext();
        Log.i(TAG, "Chat(): use sessionId is " + mUseSessionId);
        Log.i(TAG, "Chat(): message sessionId is " + ctx.sessionId);


        if (ctx.sender.equals(uniqueName)) {
            Log.i(TAG, "Chat(): dropped our own signal received on session " + ctx.sessionId);
            return;
        }


        if (mJoinedToSelf == false && ctx.sessionId == mHostSessionId) {
            Log.i(TAG, "Chat(): dropped signal received on hosted session " + ctx.sessionId + " when not joined-to-self");
            return;
        }

        String nickname = ctx.sender;

        Log.i(TAG, "Chat(): signal " + string + " received from nickname " + nickname);
        mFileShareApp.newRemoteUserMessage(nickname, string);
    }


    static {
        Log.i(TAG, "System.loadLibrary(\"alljoyn_java\")");
        System.loadLibrary("alljoyn_java");
    }
}
