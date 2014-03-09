package im.tox.antox.callbacks;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import im.tox.antox.AntoxDB;
import im.tox.antox.AntoxFriend;
import im.tox.antox.Constants;
import im.tox.antox.ToxService;
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
    }
}
