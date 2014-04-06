package im.tox.antox.callbacks;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import im.tox.antox.data.AntoxDB;
import im.tox.antox.utils.AntoxFriend;
import im.tox.antox.utils.Constants;
import im.tox.jtoxcore.callbacks.OnStatusMessageCallback;

/**
 * Created by soft on 03/03/14.
 */
public class AntoxOnStatusMessageCallback implements OnStatusMessageCallback<AntoxFriend> {

    private final static String TAG = "im.tox.antox.TAG";
    private Context ctx;

    public AntoxOnStatusMessageCallback(Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public void execute(AntoxFriend friend, String newStatus) {
        Log.d(TAG, "OnStatusMessageCallback received");
        AntoxDB db = new AntoxDB(ctx);
        db.updateStatusMessage(friend.getId(), newStatus);
        db.close();
        Log.d(TAG, "OnStatusMessageCallback id: " + friend.getId() + " status: " + newStatus);
        Intent update = new Intent(Constants.BROADCAST_ACTION);
        update.putExtra("action", Constants.UPDATE);
        LocalBroadcastManager.getInstance(ctx).sendBroadcast(update);
    }
}
