package im.tox.antox.callbacks;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

import im.tox.antox.R;
import im.tox.antox.activities.MainActivity;
import im.tox.antox.data.AntoxDB;
import im.tox.antox.tox.ToxSingleton;
import im.tox.antox.utils.Constants;
import im.tox.antox.utils.FriendRequest;
import im.tox.jtoxcore.callbacks.OnFriendRequestCallback;

public class AntoxOnFriendRequestCallback implements OnFriendRequestCallback {

    private static final String TAG = "im.tox.antox.TAG";
    public static final String FRIEND_KEY = "im.tox.antox.FRIEND_KEY";
    public static final String FRIEND_MESSAGE = "im.tox.antox.FRIEND_MESSAGE";

    private Context ctx;

    ToxSingleton toxSingleton = ToxSingleton.getInstance();

    public AntoxOnFriendRequestCallback(Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public void execute(String publicKey, String message){

        /* Add friend request to database */
        AntoxDB db = new AntoxDB(this.ctx);
        if(!db.isFriendBlocked(publicKey))
            db.addFriendRequest(publicKey, message);
        db.close();

        /* Notifications for friend requests */
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this.ctx);
        /* Check user accepts friend request notifcations in their settings */
        if(preferences.getBoolean("notifications_enable_notifications", true) != false
                && preferences.getBoolean("notifications_friend_request", true) != false) {
                /* Notification */
            if (!toxSingleton.leftPaneActive) {
                NotificationCompat.Builder mBuilder =
                        new NotificationCompat.Builder(this.ctx)
                                .setSmallIcon(R.drawable.ic_actionbar)
                                .setContentTitle(this.ctx.getString(R.string.friend_request))
                                .setContentText(message)

                                .setDefaults(Notification.DEFAULT_ALL).setAutoCancel(true);

                int ID = toxSingleton.friend_requests.size();
                Intent targetIntent = new Intent(this.ctx, MainActivity.class);
                PendingIntent contentIntent = PendingIntent.getActivity(this.ctx, 0, targetIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                mBuilder.setContentIntent(contentIntent);
                toxSingleton.mNotificationManager.notify(ID, mBuilder.build());
            }
        }
        toxSingleton.updateFriendRequests(this.ctx);
    }
}
