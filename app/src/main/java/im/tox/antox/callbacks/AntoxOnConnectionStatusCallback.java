package im.tox.antox.callbacks;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

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
        Intent intent = new Intent(this.ctx, ToxService.class);
        intent.setAction(Constants.CONNECTION_STATUS);
        intent.putExtra("name", friend.getName());
        intent.putExtra("connection_status", online);
        this.ctx.startService(intent);
    }
}
