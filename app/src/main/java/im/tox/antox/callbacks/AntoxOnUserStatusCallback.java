package im.tox.antox.callbacks;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import im.tox.antox.data.AntoxDB;
import im.tox.antox.utils.AntoxFriend;
import im.tox.antox.utils.Constants;
import im.tox.jtoxcore.ToxUserStatus;
import im.tox.jtoxcore.callbacks.OnUserStatusCallback;

/**
 * Created by soft on 03/03/14.
 */
public class AntoxOnUserStatusCallback implements OnUserStatusCallback<AntoxFriend> {

    private final static String TAG = "im.tox.antox.TAG";
    private Context ctx;

    public AntoxOnUserStatusCallback(Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public void execute(AntoxFriend friend, ToxUserStatus newStatus) {
        Log.d(TAG, "OnUserStatusCallback received");
        AntoxDB db = new AntoxDB(ctx);
        db.updateUserStatus(friend.getId(), newStatus);
        db.close();
        Log.d(TAG, "OnUserStatusCallback id: " + friend.getId() + " userStatus: " + newStatus.toString());
        Intent update = new Intent(Constants.BROADCAST_ACTION);
        update.putExtra("action", Constants.UPDATE);
        LocalBroadcastManager.getInstance(ctx).sendBroadcast(update);
    }
}
