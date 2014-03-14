package im.tox.antox;

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
import java.util.concurrent.TimeUnit;

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

    private static final String TAG = "im.tox.antox.ToxService";

    private ScheduledExecutorService scheduleTaskExecutor;

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
                toxSingleton.mDbHelper = new AntoxDB(getApplicationContext());
                toxSingleton.db = toxSingleton.mDbHelper.getWritableDatabase();

                // Define a projection that specifies which columns from the database
                // you will actually use after this query.
                String[] projection = {
                        Constants.COLUMN_NAME_KEY,
                        Constants.COLUMN_NAME_MESSAGE
                };

                if (!toxSingleton.db.isOpen())
                    toxSingleton.db = toxSingleton.mDbHelper.getWritableDatabase();

                Cursor cursor = toxSingleton.db.query(
                        Constants.TABLE_FRIEND_REQUEST,  // The table to query
                        projection,                               // The columns to return
                        null,                                // The columns for the WHERE clause
                        null,                            // The values for the WHERE clause
                        null,                                     // don't group the rows
                        null,                                     // don't filter by row groups
                        null                                 // The sort order
                );
                try {
                    int count = cursor.getCount();
                    cursor.moveToFirst();
                    for (int i = 0; i < count; i++) {
                        String key = cursor.getString(
                                cursor.getColumnIndexOrThrow(Constants.COLUMN_NAME_KEY)
                        );
                        String message = cursor.getString(
                                cursor.getColumnIndexOrThrow(Constants.COLUMN_NAME_MESSAGE)
                        );
                        toxSingleton.friend_requests.add(new FriendRequest(key, message));
                        cursor.moveToNext();
                    }
                } finally {
                    cursor.close();
                }
                toxSingleton.mDbHelper.close();
                Log.d(TAG, "Loaded requests from database");

                Intent notify = new Intent(Constants.BROADCAST_ACTION);
                notify.putExtra("action", Constants.UPDATE_LEFT_PANE);
                LocalBroadcastManager.getInstance(this).sendBroadcast(notify);

                /* Populate tox friends list with saved friends in database */
                AntoxDB db = new AntoxDB(getApplicationContext());
                ArrayList<Friend> friends = db.getFriendList();

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

                toxSingleton.callbackHandler.registerOnMessageCallback(antoxOnMessageCallback);
                toxSingleton.callbackHandler.registerOnFriendRequestCallback(antoxOnFriendRequestCallback);
                toxSingleton.callbackHandler.registerOnActionCallback(antoxOnActionCallback);
                toxSingleton.callbackHandler.registerOnConnectionStatusCallback(antoxOnConnectionStatusCallback);
                toxSingleton.callbackHandler.registerOnNameChangeCallback(antoxOnNameChangeCallback);
                toxSingleton.callbackHandler.registerOnReadReceiptCallback(antoxOnReadReceiptCallback);
                toxSingleton.callbackHandler.registerOnStatusMessageCallback(antoxOnStatusMessageCallback);
                toxSingleton.callbackHandler.registerOnUserStatusCallback(antoxOnUserStatusCallback);

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

                    }

                } catch (UnknownHostException e) {
                    this.stopService(intent);
                    DhtNode.counter++;
                    Intent restart = new Intent(getApplicationContext(), ToxService.class);
                    restart.setAction(Constants.START_TOX);
                    this.startService(restart);
                    e.printStackTrace();
                }
            } catch (ToxException e) {
                Log.d(TAG, e.getError().toString());
                e.printStackTrace();
            }
            scheduleTaskExecutor = Executors.newScheduledThreadPool(5);
            // This schedule a runnable task every 2 minutes
            Log.d("Service", "Start do_tox");
            scheduleTaskExecutor.scheduleAtFixedRate(new Runnable() {
                ToxSingleton toxS = ToxSingleton.getInstance();

                public void run() {
                    try {
                        toxS.jTox.doTox();
                    } catch (ToxException e) {
                        Log.d(TAG, e.getError().toString());
                        e.printStackTrace();
                    }
                }
            }, 0, 50, TimeUnit.MILLISECONDS);
            toxSingleton.toxStarted = true;
        } else if (intent.getAction().equals(Constants.STOP_TOX)) {
            if (scheduleTaskExecutor != null) {
                scheduleTaskExecutor.shutdownNow();
            }
            stopSelf();
        }
    }
}
