package im.tox.antox.callbacks;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import im.tox.antox.data.AntoxDB;
import im.tox.antox.tox.ToxSingleton;
import im.tox.antox.utils.AntoxFriend;
import im.tox.antox.utils.Constants;
import im.tox.jtoxcore.ToxUserStatus;
import im.tox.jtoxcore.callbacks.OnUserStatusCallback;

public class AntoxOnUserStatusCallback implements OnUserStatusCallback<AntoxFriend> {

    private final static String TAG = "im.tox.antox.TAG";
    private Context ctx;

    ToxSingleton toxSingleton = ToxSingleton.getInstance();
    public AntoxOnUserStatusCallback(Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public void execute(AntoxFriend friend, ToxUserStatus newStatus) {
        AntoxDB db = new AntoxDB(ctx);
        db.updateUserStatus(friend.getId(), newStatus);
        db.close();
        toxSingleton.updateFriendsList(ctx);
    }
}
