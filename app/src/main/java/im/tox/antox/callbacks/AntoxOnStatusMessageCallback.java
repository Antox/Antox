package im.tox.antox.callbacks;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import im.tox.antox.data.AntoxDB;
import im.tox.antox.tox.ToxSingleton;
import im.tox.antox.utils.AntoxFriend;
import im.tox.antox.utils.Constants;
import im.tox.jtoxcore.callbacks.OnStatusMessageCallback;

public class AntoxOnStatusMessageCallback implements OnStatusMessageCallback<AntoxFriend> {

    private final static String TAG = "im.tox.antox.TAG";
    private Context ctx;

    ToxSingleton toxSingleton = ToxSingleton.getInstance();
    public AntoxOnStatusMessageCallback(Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public void execute(AntoxFriend friend, String newStatus) {
        AntoxDB db = new AntoxDB(ctx);
        db.updateStatusMessage(friend.getId(), newStatus);
        db.close();
        toxSingleton.updateFriendsList(ctx);
    }
}
