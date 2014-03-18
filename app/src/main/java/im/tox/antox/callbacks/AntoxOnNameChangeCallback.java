package im.tox.antox.callbacks;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import im.tox.antox.data.AntoxDB;
import im.tox.antox.utils.AntoxFriend;
import im.tox.antox.utils.Constants;
import im.tox.jtoxcore.callbacks.OnNameChangeCallback;

/**
 * Created by soft on 03/03/14.
 */
public class AntoxOnNameChangeCallback implements OnNameChangeCallback<AntoxFriend> {

    private final static String TAG = "im.tox.antox.TAG";
    private Context ctx;

    public AntoxOnNameChangeCallback(Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public void execute(AntoxFriend friend, String newName) {
        Log.d(TAG, "OnNameChangeCallback received");
        AntoxDB db = new AntoxDB(ctx);
        db.updateFriendName(friend.getId(), newName);
        db.close();
        Log.d(TAG, "OnNameChangeCallback id: " + friend.getId() + " name: " + newName);
        Intent update = new Intent(Constants.BROADCAST_ACTION);
        update.putExtra("action", Constants.UPDATE);
        LocalBroadcastManager.getInstance(ctx).sendBroadcast(update);
    }
}
