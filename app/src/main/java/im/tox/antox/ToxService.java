package im.tox.antox;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
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
import im.tox.jtoxcore.FriendExistsException;
import im.tox.jtoxcore.ToxException;
import im.tox.jtoxcore.ToxUserStatus;

public class ToxService extends IntentService {

    private static final String TAG = "im.tox.antox.ToxService";

    private ScheduledExecutorService scheduleTaskExecutor;

    private boolean toxStarted;

    public ToxService() {
        super("ToxService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        AntoxState state = AntoxState.getInstance();
        ToxSingleton toxSingleton = ToxSingleton.getInstance();
        ArrayList<String> boundActivities = state.getBoundActivities();

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
                toxSingleton.initTox();
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
                notify.putExtra("action", Constants.UPDATE_FRIEND_REQUESTS);
                LocalBroadcastManager.getInstance(this).sendBroadcast(notify);

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
                    if (DhtNode.port != null)
                        toxSingleton.jTox.bootstrap(DhtNode.ipv4, Integer.parseInt(DhtNode.port), DhtNode.key);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                try {
                    toxSingleton.jTox.getSelfUserStatus();
                } catch (Exception e) {
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
                        Log.v("Service", "do_tox");
                        toxS.jTox.doTox();
                    } catch (ToxException e) {
                        Log.d(TAG, e.getError().toString());
                        e.printStackTrace();
                    }
                }
            }, 0, 50, TimeUnit.MILLISECONDS);
        } else if (intent.getAction().equals(Constants.STOP_TOX)) {
            if (scheduleTaskExecutor != null) {
                scheduleTaskExecutor.shutdownNow();
            }
            stopSelf();
        } else if (intent.getAction().equals(Constants.ADD_FRIEND)) {
            try {
                String[] friendData = intent.getStringArrayExtra("friendData");
                toxSingleton.jTox.addFriend(friendData[0], friendData[1]);
            } catch (FriendExistsException e) {
                Log.d(TAG, "Friend already exists");
                e.printStackTrace();
            } catch (ToxException e) {
                Log.d(TAG, "ToxException: " + e.getError().toString());
                e.printStackTrace();
            }
        } else if (intent.getAction().equals(Constants.UPDATE_SETTINGS)) {
            String[] newSettings = intent.getStringArrayExtra("newSettings");

            /* If not empty, update the users settings which is passed in intent from SettingsActivity */
            try {
                if (!newSettings[0].equals(""))
                    toxSingleton.jTox.setName(newSettings[0]);

                if (!newSettings[1].equals("")) {
                    if (newSettings[1].equals("away"))
                        toxSingleton.jTox.setUserStatus(ToxUserStatus.TOX_USERSTATUS_AWAY);
                    else if (newSettings[1].equals("busy"))
                        toxSingleton.jTox.setUserStatus(ToxUserStatus.TOX_USERSTATUS_BUSY);
                    else
                        toxSingleton.jTox.setUserStatus(ToxUserStatus.TOX_USERSTATUS_NONE);
                }

                if (!newSettings[2].equals(""))
                    toxSingleton.jTox.setStatusMessage(newSettings[2]);
            } catch (ToxException e) {
                e.printStackTrace();
            }

        } else if (intent.getAction().equals(Constants.FRIEND_LIST)) {
            Log.d(TAG, "Constants.FRIEND_LIST");
            List<AntoxFriend> onlineFriends = toxSingleton.friendsList.getOnlineFriends();
            if(onlineFriends.size() > 0) {
                Log.d(TAG, "Friends found in friendsList");
                String[] names = new String[onlineFriends.size()];
                String[] notes = new String[onlineFriends.size()];
                for(int i = 0; i < onlineFriends.size(); i++) {
                    names[i] = onlineFriends.get(i).getName();
                    notes[i] = onlineFriends.get(i).getStatusMessage();
                }

                Intent notify = new Intent(Constants.BROADCAST_ACTION);
                notify.setAction(Constants.FRIEND_LIST);
                notify.putExtra("names", names);
                notify.putExtra("notes", notes);
                LocalBroadcastManager.getInstance(this).sendBroadcast(notify);
            }

        } else if (intent.getAction().equals(Constants.FRIEND_REQUEST)) {
            Log.d(TAG, "Constants.FRIEND_REQUEST");
            String key = intent.getStringExtra(AntoxOnFriendRequestCallback.FRIEND_KEY);
            String message = intent.getStringExtra(AntoxOnFriendRequestCallback.FRIEND_MESSAGE);
            /* Add friend request to arraylist */
            toxSingleton.friend_requests.add(new FriendRequest((String) key, (String) message));
            /* Add friend request to database */
            if (!toxSingleton.db.isOpen())
                toxSingleton.db = toxSingleton.mDbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(Constants.COLUMN_NAME_KEY, key);
            values.put(Constants.COLUMN_NAME_MESSAGE, message);
            toxSingleton.db.insert(
                    Constants.TABLE_FRIEND_REQUEST,
                    null,
                    values);
            toxSingleton.mDbHelper.close();
            /* Broadcast */
            Intent notify = new Intent(Constants.BROADCAST_ACTION);
            notify.putExtra("action", Constants.FRIEND_REQUEST);
            notify.putExtra("key", key);
            notify.putExtra("message", message);
            LocalBroadcastManager.getInstance(this).sendBroadcast(notify);
            /* Update friends list */
            Intent updateFriends = new Intent(this, ToxService.class);
            updateFriends.setAction(Constants.FRIEND_LIST);
            this.startService(updateFriends);

        } else if (intent.getAction().equals(Constants.CONNECTED_STATUS)) {
            Log.d(TAG, "Constants.CONNECTION_STATUS");
        } else if (intent.getAction().equals(Constants.REJECT_FRIEND_REQUEST)) {
            String key = intent.getStringExtra("key");
            if (toxSingleton.friend_requests.size() != 0) {
                for (int j = 0; j < toxSingleton.friend_requests.size(); j++) {
                    for (int i = 0; i < toxSingleton.friend_requests.size(); i++) {
                        if (key.equalsIgnoreCase(toxSingleton.friend_requests.get(i).requestKey)) {
                            toxSingleton.friend_requests.remove(i);
                            break;
                        }
                    }
                }

                if (!toxSingleton.db.isOpen())
                    toxSingleton.db = toxSingleton.mDbHelper.getWritableDatabase();

                toxSingleton.db.delete(Constants.TABLE_FRIEND_REQUEST,
                        Constants.COLUMN_NAME_KEY + "='" + key + "'",
                        null);
                toxSingleton.db.close();
            }
            /* Broadcast */
            Intent notify = new Intent(Constants.BROADCAST_ACTION);
            notify.putExtra("action", Constants.REJECT_FRIEND_REQUEST);
            notify.putExtra("key", key);
            LocalBroadcastManager.getInstance(this).sendBroadcast(notify);

        } else if (intent.getAction().equals(Constants.ACCEPT_FRIEND_REQUEST)) {

            String key = intent.getStringExtra("key");
            try {
                toxSingleton.jTox.confirmRequest(key);
            } catch (Exception e) {

            }

            if (toxSingleton.friend_requests.size() != 0) {

                for (int i = 0; i < toxSingleton.friend_requests.size(); i++) {
                    if (key.equalsIgnoreCase(toxSingleton.friend_requests.get(i).requestKey)) {
                        toxSingleton.friend_requests.remove(i);
                        break;
                    }
                }

                if (!toxSingleton.db.isOpen())
                    toxSingleton.db = toxSingleton.mDbHelper.getWritableDatabase();

                toxSingleton.db.delete(Constants.TABLE_FRIEND_REQUEST,
                        Constants.COLUMN_NAME_KEY + "='" + key + "'",
                        null);
                toxSingleton.db.close();

                /* Broadcast */
                Intent notify = new Intent(Constants.BROADCAST_ACTION);
                notify.putExtra("action", Constants.ACCEPT_FRIEND_REQUEST);
                notify.putExtra("key", key);
                LocalBroadcastManager.getInstance(this).sendBroadcast(notify);
            }
        }

    }

}
