package im.tox.antox.tox;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Random;

import im.tox.antox.R;
import im.tox.antox.activities.MainActivity;
import im.tox.antox.callbacks.AntoxOnFriendRequestCallback;
import im.tox.antox.callbacks.AntoxOnMessageCallback;
import im.tox.antox.data.AntoxDB;
import im.tox.antox.utils.AntoxFriend;
import im.tox.antox.utils.Constants;
import im.tox.antox.utils.Friend;
import im.tox.antox.utils.FriendRequest;
import im.tox.antox.utils.Message;
import im.tox.jtoxcore.FriendExistsException;
import im.tox.jtoxcore.ToxException;

public class ToxService extends IntentService {
    private static final String TAG = "im.tox.antox.tox.ToxService";
    private ToxSingleton toxSingleton;

    public ToxService() {
        super("ToxService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        toxSingleton = ToxSingleton.getInstance();
        toxSingleton.mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        String action = intent.getAction();

        String key;
        Intent notify;
        AntoxDB db;

        switch (action) {
            case Constants.DELETE_FRIEND:
                key = intent.getStringExtra("key");
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
                    if (!wasException) {
                        //Delete friend from list
                        toxSingleton.friendsList.removeFriend(friend.getFriendnumber());
                        //Broadcast to update left pane
                        notify = new Intent(Constants.BROADCAST_ACTION);
                        notify.putExtra("action", Constants.UPDATE_LEFT_PANE);
                        notify.putExtra("key", key);
                        LocalBroadcastManager.getInstance(this).sendBroadcast(notify);
                    }
                }
                break;

            case Constants.DELETE_FRIEND_AND_CHAT:
                key = intent.getStringExtra("key");
                wasException = false;
                // Remove friend from tox friend list
                friend = toxSingleton.friendsList.getById(key);
                if(friend != null) {

                    try {
                        toxSingleton.jTox.deleteFriend(friend.getFriendnumber());
                    } catch (ToxException e) {
                        wasException = true;
                        Log.d(TAG, e.getError().toString());
                        e.printStackTrace();
                    }
                    if (!wasException) {
                        //Delete friend from list
                        toxSingleton.friendsList.removeFriend(friend.getFriendnumber());
                        //Broadcast to update left pane
                        notify = new Intent(Constants.BROADCAST_ACTION);
                        notify.putExtra("action", Constants.UPDATE_LEFT_PANE);
                        notify.putExtra("key", key);
                        LocalBroadcastManager.getInstance(this).sendBroadcast(notify);
                    }
                }
                break;

            case Constants.FRIEND_REQUEST:
                key = intent.getStringExtra(AntoxOnFriendRequestCallback.FRIEND_KEY);
                String message = intent.getStringExtra(AntoxOnFriendRequestCallback.FRIEND_MESSAGE);
            /* Add friend request to arraylist */
                toxSingleton.friend_requests.add(new FriendRequest((String) key, (String) message));
            /* Add friend request to database */
                db = new AntoxDB(getApplicationContext());
                if(!db.isFriendBlocked(key))
                    db.addFriendRequest(key, message);
                db.close();

                if(preferences.getBoolean("notifications_enable_notifications", true) != false
                        && preferences.getBoolean("notifications_friend_request", true) != false) {
                /* Notification */
                    if (!toxSingleton.leftPaneActive) {
                        NotificationCompat.Builder mBuilder =
                                new NotificationCompat.Builder(this)
                                        .setSmallIcon(R.drawable.ic_actionbar)
                                        .setContentTitle(getString(R.string.friend_request))
                                        .setContentText(message)

                                        .setDefaults(Notification.DEFAULT_ALL).setAutoCancel(true);

                        int ID = toxSingleton.friend_requests.size();
                        Intent targetIntent = new Intent(getApplicationContext(), MainActivity.class);
                        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, targetIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                        mBuilder.setContentIntent(contentIntent);
                        toxSingleton.mNotificationManager.notify(ID, mBuilder.build());
                    }
                }

                /* Update friends list */
                Intent update = new Intent(Constants.BROADCAST_ACTION);
                update.putExtra("action", Constants.UPDATE);
                LocalBroadcastManager.getInstance(this).sendBroadcast(update);
                break;

            case Constants.REJECT_FRIEND_REQUEST:
                key = intent.getStringExtra("key");
                if (toxSingleton.friend_requests.size() != 0) {
                    for (int j = 0; j < toxSingleton.friend_requests.size(); j++) {
                        for (int i = 0; i < toxSingleton.friend_requests.size(); i++) {
                            if (key.equalsIgnoreCase(toxSingleton.friend_requests.get(i).requestKey)) {
                                toxSingleton.friend_requests.remove(i);
                                break;
                            }
                        }
                    }
                }
            /* Broadcast */
                notify = new Intent(Constants.BROADCAST_ACTION);
                notify.putExtra("action", Constants.REJECT_FRIEND_REQUEST);
                notify.putExtra("key", key);
                LocalBroadcastManager.getInstance(this).sendBroadcast(notify);
                break;


            case Constants.ACCEPT_FRIEND_REQUEST:
                key = intent.getStringExtra("key");
                try {
                    toxSingleton.jTox.confirmRequest(key);

                    //This is so wasteful. Should pass the info in the intent with the key
                    db = new AntoxDB(getApplicationContext());
                    ArrayList<Friend> friends = db.getFriendList();
                    //Long statement but just getting size of friends list and adding one for the friend number
                    friend = toxSingleton.friendsList.addFriend(toxSingleton.friendsList.all().size()+1);
                    int pos = -1;
                    for(int i = 0; i < friends.size(); i++) {
                        if(friends.get(i).friendKey.equals(key)) {
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
                } catch (Exception e) {

                }

                if (toxSingleton.friend_requests.size() != 0) {

                    for (int i = 0; i < toxSingleton.friend_requests.size(); i++) {
                        if (key.equalsIgnoreCase(toxSingleton.friend_requests.get(i).requestKey)) {
                            toxSingleton.friend_requests.remove(i);
                            break;
                        }
                    }

                    db = new AntoxDB(getApplicationContext());
                    db.deleteFriendRequest(key);
                    db.close();

                /* Broadcast */
                    notify = new Intent(Constants.BROADCAST_ACTION);
                    notify.putExtra("action", Constants.ACCEPT_FRIEND_REQUEST);
                    notify.putExtra("key", key);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(notify);
                }
                break;

            default:
                break;
        }
    }

}
