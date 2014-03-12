package im.tox.antox;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.support.v4.app.NotificationCompat;
import android.app.Notification;
import android.support.v4.app.TaskStackBuilder;
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
import im.tox.jtoxcore.ToxFriend;
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
            }, 0, 10, TimeUnit.MILLISECONDS);
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
            if (onlineFriends.size() > 0) {
                Log.d(TAG, "Friends found in friendsList");
                String[] names = new String[onlineFriends.size()];
                String[] notes = new String[onlineFriends.size()];
                for (int i = 0; i < onlineFriends.size(); i++) {
                    names[i] = onlineFriends.get(i).getName();
                    notes[i] = onlineFriends.get(i).getStatusMessage();
                }

                Intent notify = new Intent(Constants.BROADCAST_ACTION);
                notify.setAction(Constants.FRIEND_LIST);
                notify.putExtra("names", names);
                notify.putExtra("notes", notes);
                LocalBroadcastManager.getInstance(this).sendBroadcast(notify);
            }
        } else if (intent.getAction().equals(Constants.ON_MESSAGE)) {
            Log.d(TAG, "Constants.ON_MESSAGE");
            String key = intent.getStringExtra(AntoxOnMessageCallback.KEY);
            String message = intent.getStringExtra(AntoxOnMessageCallback.MESSAGE);
            String name = toxSingleton.friendsList.getById(key).getName();
            int friend_number = intent.getIntExtra(AntoxOnMessageCallback.FRIEND_NUMBER, -1);
            toxSingleton.mDbHelper.addMessage(-1, key, message, false, true);
            /* Broadcast */
            Intent notify = new Intent(Constants.BROADCAST_ACTION);
            notify.putExtra("action", Constants.UPDATE_MESSAGES);
            notify.putExtra("key", key);
            LocalBroadcastManager.getInstance(this).sendBroadcast(notify);
            /* Notification */
            if (!(toxSingleton.rightPaneActive && toxSingleton.activeFriendKey.equals(key))) {
                NotificationCompat.Builder mBuilder =
                        new NotificationCompat.Builder(this)
                                .setSmallIcon(R.drawable.ic_launcher)
                                .setContentTitle(name)
                                .setContentText(message)
                                .setDefaults(Notification.DEFAULT_ALL);
                // Creates an explicit intent for an Activity in your app
                Intent resultIntent = new Intent(this, MainActivity.class);
                resultIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                resultIntent.setAction(Constants.SWITCH_TO_FRIEND);
                resultIntent.putExtra("key", key);
                resultIntent.putExtra("name", name);

                // The stack builder object will contain an artificial back stack for the
                // started Activity.
                // This ensures that navigating backward from the Activity leads out of
                // your application to the Home screen.
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
                // Adds the back stack for the Intent (but not the Intent itself)
                stackBuilder.addParentStack(MainActivity.class);
                // Adds the Intent that starts the Activity to the top of the stack
                stackBuilder.addNextIntent(resultIntent);
                PendingIntent resultPendingIntent =
                        stackBuilder.getPendingIntent(
                                0,
                                PendingIntent.FLAG_UPDATE_CURRENT
                        );
                mBuilder.setContentIntent(resultPendingIntent);
                NotificationManager mNotificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                mNotificationManager.notify(friend_number, mBuilder.build());
            }
        } else if (intent.getAction().equals(Constants.DELETE_FRIEND)) {
            Log.d(TAG, "Constants.DELETE_FRIEND");
            String key = intent.getStringExtra("key");
            boolean wasException = false;
            // Remove friend from tox friend list
            AntoxFriend friend = toxSingleton.friendsList.getById(key);
            if(friend != null) {

                try {
                    toxSingleton.jTox.deleteFriend(friend.getFriendnumber());
                } catch (ToxException e) {
                    wasException = true;
                    Log.d(TAG, e.getError().toString());
                    e.printStackTrace();
                }
                Log.d(TAG, "Friend deleted from tox list. New size: " + toxSingleton.friendsList.all().size());
                if (!wasException) {
                    //Delete friend from list
                    toxSingleton.friendsList.removeFriend(friend.getFriendnumber());
                    // Delete friend from database
                    toxSingleton.mDbHelper.deleteFriend(key);
                    toxSingleton.mDbHelper.close();
                    //Broadcast to update left pane
                    Intent notify = new Intent(Constants.BROADCAST_ACTION);
                    notify.putExtra("action", Constants.UPDATE_LEFT_PANE);
                    notify.putExtra("key", key);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(notify);
                }
            }
        }else if(intent.getAction().equals((Constants.DELETE_FRIEND_AND_CHAT)))
        {
            Log.d(TAG, "Constants.DELETE_FRIEND");
            String key = intent.getStringExtra("key");
            boolean wasException = false;
            // Remove friend from tox friend list
            AntoxFriend friend = toxSingleton.friendsList.getById(key);
            if(friend != null) {

                try {
                    toxSingleton.jTox.deleteFriend(friend.getFriendnumber());
                } catch (ToxException e) {
                    wasException = true;
                    Log.d(TAG, e.getError().toString());
                    e.printStackTrace();
                }
                Log.d(TAG, "Friend deleted from tox list. New size: " + toxSingleton.friendsList.all().size());
                if (!wasException) {
                    //Delete friend from list
                    toxSingleton.friendsList.removeFriend(friend.getFriendnumber());
                    // Delete friend from database
                    toxSingleton.mDbHelper.deleteFriend(key);
                    toxSingleton.mDbHelper.deleteChat(key);
                    toxSingleton.mDbHelper.close();
                    //Broadcast to update left pane
                    Intent notify = new Intent(Constants.BROADCAST_ACTION);
                    notify.putExtra("action", Constants.UPDATE_LEFT_PANE);
                    notify.putExtra("key", key);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(notify);
                }
            }
        }
        else if (intent.getAction().equals(Constants.SEND_MESSAGE)) {
            Log.d(TAG, "Constants.SEND_MESSAGE");
            String key = intent.getStringExtra("key");
            String message = intent.getStringExtra("message");
            /* Send message */
            ToxFriend friend = null;
            boolean sendingSucceeded = true;
            try {
                friend = toxSingleton.friendsList.getById(key);
            } catch (Exception e) {
                Log.d(TAG, e.toString());
            }
            try {
                if (friend != null) {
                    Log.d(TAG, "Sending message to " + friend.getName());
                    toxSingleton.jTox.sendMessage(friend, message);
                }
            } catch (ToxException e) {
                Log.d(TAG, e.toString());
                e.printStackTrace();
                sendingSucceeded = false;
            }
            if (sendingSucceeded) {
            /* Add message to chatlog */
                toxSingleton.mDbHelper.addMessage(-1, key, message, true, false);
            /* Broadcast to update UI */
                Intent notify = new Intent(Constants.BROADCAST_ACTION);
                notify.putExtra("action", Constants.UPDATE_MESSAGES);
                notify.putExtra("key", key);
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

                /* Add friend to tox friends list */
                //This is so wasteful. Should pass the info in the intent with the key
                AntoxDB db = new AntoxDB(getApplicationContext());
                ArrayList<Friend> friends = db.getFriendList();
                //Long statement but just getting size of friends list and adding one for the friend number
                AntoxFriend friend = toxSingleton.friendsList.addFriend(toxSingleton.friendsList.all().size()+1);
                int pos = -1;
                for(int i = 0; i < friends.size(); i++) {
                    if(friends.get(i).friendKey == key) {
                        pos = i;
                        break;
                    }
                }
                if(pos != -1) {
                    friend.setId(key);
                    friend.setName(friends.get(pos).friendName);
                    friend.setStatusMessage(friends.get(pos).personalNote);
                }

                toxSingleton.jTox.save();
                Log.d(TAG, "Saving request");

                Log.d(TAG, "Tox friend list updated. New size: " + toxSingleton.friendsList.all().size());

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
