package im.tox.antox.tox;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import im.tox.antox.callbacks.AntoxOnTypingChangeCallback;
import im.tox.antox.data.AntoxDB;
import im.tox.antox.utils.AntoxFriend;
import im.tox.antox.utils.AntoxFriendList;
import im.tox.antox.utils.Constants;
import im.tox.antox.utils.DhtNode;
import im.tox.antox.utils.Friend;
import im.tox.antox.utils.FriendRequest;
import im.tox.antox.callbacks.AntoxOnActionCallback;
import im.tox.antox.callbacks.AntoxOnConnectionStatusCallback;
import im.tox.antox.callbacks.AntoxOnFriendRequestCallback;
import im.tox.antox.callbacks.AntoxOnMessageCallback;
import im.tox.antox.callbacks.AntoxOnNameChangeCallback;
import im.tox.antox.callbacks.AntoxOnReadReceiptCallback;
import im.tox.antox.callbacks.AntoxOnStatusMessageCallback;
import im.tox.antox.callbacks.AntoxOnUserStatusCallback;
import im.tox.jtoxcore.ToxException;

/**
 * Created by soft on 13/03/14.
 */
public class ToxDoService extends IntentService {

    private static final String TAG = "im.tox.antox.tox.ToxDoService";

    private ToxScheduleTaskExecutor toxScheduleTaskExecutor = new ToxScheduleTaskExecutor(1);

    private boolean toxStarted;
    private ToxSingleton toxSingleton;

    public ToxDoService() {
        super("ToxDoService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        toxSingleton = ToxSingleton.getInstance();


        if (intent.getAction().equals(Constants.START_TOX)) {

            try {
                System.load("/data/data/im.tox.antox/lib/libsodium.so");
                System.load("/data/data/im.tox.antox/lib/libtoxcore.so");
            } catch (Exception e) {
                Log.d(TAG, "Failed System.load()");
                e.printStackTrace();
            }

            try {
                Log.d(TAG, "Handling intent START_TOX");
                toxSingleton.initTox(getApplicationContext());

                AntoxDB db = new AntoxDB(getApplicationContext());
                ArrayList<FriendRequest> friendRequests = db.getFriendRequestsList();
                toxSingleton.friend_requests = friendRequests;
                Log.d(TAG, "Loaded requests from database");

                Intent notify = new Intent(Constants.BROADCAST_ACTION);
                notify.putExtra("action", Constants.UPDATE_LEFT_PANE);
                LocalBroadcastManager.getInstance(this).sendBroadcast(notify);

                /* Populate tox friends list with saved friends in database */
                db = new AntoxDB(getApplicationContext());
                ArrayList<Friend> friends = db.getFriendList();
                db.close();

                toxSingleton.friendsList = (AntoxFriendList) toxSingleton.jTox.getFriendList();

                if(friends.size() > 0) {
                    Log.d(TAG, "Adding friends to tox friendlist");
                    for (int i = 0; i < friends.size(); i++) {
                        try {
                            toxSingleton.jTox.confirmRequest(friends.get(i).friendKey);
                        } catch (Exception e) {

                        }
                        AntoxFriend friend = toxSingleton.friendsList.addFriendIfNotExists(i);
                        friend.setId(friends.get(i).friendKey);
                        friend.setName(friends.get(i).friendName);
                        friend.setStatusMessage(friends.get(i).personalNote);
                    }
                    Log.d(TAG, "Size of tox friendlist: " + toxSingleton.friendsList.all().size());
                }

                AntoxOnMessageCallback antoxOnMessageCallback = new AntoxOnMessageCallback(getApplicationContext());
                AntoxOnFriendRequestCallback antoxOnFriendRequestCallback = new AntoxOnFriendRequestCallback(getApplicationContext());
                AntoxOnActionCallback antoxOnActionCallback = new AntoxOnActionCallback(getApplicationContext());
                AntoxOnConnectionStatusCallback antoxOnConnectionStatusCallback = new AntoxOnConnectionStatusCallback(getApplicationContext());
                AntoxOnNameChangeCallback antoxOnNameChangeCallback = new AntoxOnNameChangeCallback(getApplicationContext());
                AntoxOnReadReceiptCallback antoxOnReadReceiptCallback = new AntoxOnReadReceiptCallback(getApplicationContext());
                AntoxOnStatusMessageCallback antoxOnStatusMessageCallback = new AntoxOnStatusMessageCallback(getApplicationContext());
                AntoxOnUserStatusCallback antoxOnUserStatusCallback = new AntoxOnUserStatusCallback(getApplicationContext());
                AntoxOnTypingChangeCallback antoxOnTypingChangeCallback = new AntoxOnTypingChangeCallback(getApplicationContext());

                toxSingleton.callbackHandler.registerOnMessageCallback(antoxOnMessageCallback);
                toxSingleton.callbackHandler.registerOnFriendRequestCallback(antoxOnFriendRequestCallback);
                toxSingleton.callbackHandler.registerOnActionCallback(antoxOnActionCallback);
                toxSingleton.callbackHandler.registerOnConnectionStatusCallback(antoxOnConnectionStatusCallback);
                toxSingleton.callbackHandler.registerOnNameChangeCallback(antoxOnNameChangeCallback);
                toxSingleton.callbackHandler.registerOnReadReceiptCallback(antoxOnReadReceiptCallback);
                toxSingleton.callbackHandler.registerOnStatusMessageCallback(antoxOnStatusMessageCallback);
                toxSingleton.callbackHandler.registerOnUserStatusCallback(antoxOnUserStatusCallback);
                toxSingleton.callbackHandler.registerOnTypingChangeCallback(antoxOnTypingChangeCallback);

                SharedPreferences settingsPref = getSharedPreferences("settings", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = settingsPref.edit();
                editor.putString("user_key", toxSingleton.jTox.getAddress());
                editor.commit();


                try {
                    //If counter has reached max size set it back to zero and to try all nodes again
                    if(DhtNode.counter >= DhtNode.ipv4.size())
                        DhtNode.counter = 0;

                    //DhtNode.port.get(DhtNode.counter) will give indexoutofbounds exception when nothing is downloaded
                    if (DhtNode.port.size()>0 || DhtNode.ipv4.size()>0  || DhtNode.key.size()>0) {
                        toxSingleton.jTox.bootstrap(DhtNode.ipv4.get(DhtNode.counter),
                                Integer.parseInt(DhtNode.port.get(DhtNode.counter)), DhtNode.key.get(DhtNode.counter));
                        DhtNode.connected = true;
                        Log.d(TAG, "Connected to node: " + DhtNode.owner.get(DhtNode.counter));
                    }

                } catch (UnknownHostException e) {
                    this.stopService(intent);
                    DhtNode.counter++;
                    Intent restart = new Intent(getApplicationContext(), ToxDoService.class);
                    restart.setAction(Constants.START_TOX);
                    this.startService(restart);
                    e.printStackTrace();
                }
            } catch (ToxException e) {
                Log.d(TAG, e.getError().toString());
                e.printStackTrace();
            }
            Log.d("Service", "Start do_tox");
            toxScheduleTaskExecutor.scheduleAtFixedRate(new DoTox(), 0, 5, TimeUnit.MILLISECONDS);
            toxSingleton.toxStarted = true;
        } else if (intent.getAction().equals(Constants.STOP_TOX)) {
            if (toxScheduleTaskExecutor != null) {
                toxScheduleTaskExecutor.shutdownNow();
            }
            stopSelf();
        }
    }

    private class ToxScheduleTaskExecutor extends ScheduledThreadPoolExecutor {

        public ToxScheduleTaskExecutor(int size) {
            super(1);
        }

        @Override
        public ScheduledFuture scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
            return super.scheduleAtFixedRate(wrapRunnable(command), initialDelay, period, unit);
        }

        @Override
        public ScheduledFuture scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
            return super.scheduleWithFixedDelay(wrapRunnable(command), initialDelay, delay, unit);
        }

        private Runnable wrapRunnable(Runnable command) {
            return new LogOnExceptionRunnable(command);
        }

        private class LogOnExceptionRunnable implements Runnable{
            private Runnable theRunnable;
            public LogOnExceptionRunnable(Runnable theRunnable) {
                super();
                this.theRunnable = theRunnable;
            }
            @Override
            public void run() {
                try {
                    theRunnable.run();
                } catch (Exception e) {
                    Log.d(TAG, "Executor has caught an exception");
                    e.printStackTrace();
                    toxScheduleTaskExecutor.scheduleAtFixedRate(new DoTox(), 0, 5, TimeUnit.MILLISECONDS);
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private class DoTox implements Runnable {
        @Override
        public void run() {
            /* Praise the sun */
            try {
                toxSingleton.jTox.doTox();
                if(!toxSingleton.jTox.isConnected() && DhtNode.connected == true) {
                    Log.v(TAG, "not connected to tox");
                }
            } catch (ToxException e) {
                Log.d(TAG, e.getError().toString());
                e.printStackTrace();
            }
        }
    }
}
