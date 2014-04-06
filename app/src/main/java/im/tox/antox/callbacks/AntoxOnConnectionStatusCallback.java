package im.tox.antox.callbacks;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import im.tox.antox.data.AntoxDB;
import im.tox.antox.utils.AntoxFriend;
import im.tox.antox.utils.Constants;
import im.tox.antox.tox.ToxService;
import im.tox.jtoxcore.callbacks.OnConnectionStatusCallback;

/**
 * Created by soft on 03/03/14.
 */
public class AntoxOnConnectionStatusCallback implements OnConnectionStatusCallback<AntoxFriend> {

    private final static String TAG = "im.tox.antox.TAG";
    private Context ctx;

    public AntoxOnConnectionStatusCallback(Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public void execute(AntoxFriend friend, boolean online) {
        Log.d(TAG, "OnConnectionStatusCallback received");
        AntoxDB db = new AntoxDB(ctx);
        db.updateUserOnline(friend.getId(), online);
        db.close();
        Log.d(TAG, "OnConnectionStatusCallback id: " + friend.getId() + " status: " + online);
        Intent update = new Intent(Constants.BROADCAST_ACTION);
        update.putExtra("action", Constants.UPDATE);
        LocalBroadcastManager.getInstance(ctx).sendBroadcast(update);
        if (online) {
            Intent intent = new Intent(this.ctx, ToxService.class);
            intent.setAction(Constants.SEND_UNSENT_MESSAGES);
            this.ctx.startService(intent);
        }
    }
}
