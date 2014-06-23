package im.tox.antox.callbacks;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.ArrayList;

import im.tox.antox.data.AntoxDB;
import im.tox.antox.tox.ToxSingleton;
import im.tox.antox.utils.AntoxFriend;
import im.tox.antox.utils.Constants;
import im.tox.antox.utils.Message;
import im.tox.jtoxcore.ToxException;
import im.tox.jtoxcore.callbacks.OnConnectionStatusCallback;

public class AntoxOnConnectionStatusCallback implements OnConnectionStatusCallback<AntoxFriend> {

    private final static String TAG = "im.tox.antox.TAG";
    private Context ctx;

    ToxSingleton toxSingleton = ToxSingleton.getInstance();

    public AntoxOnConnectionStatusCallback(Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public void execute(AntoxFriend friend, boolean online) {
        AntoxDB db = new AntoxDB(ctx);
        db.updateUserOnline(friend.getId(), online);

        String[] det = db.getFriendDetails(friend.getId());
        String tmp;

        if(!det[1].equals(""))
            tmp = det[1];
        else
            tmp = det[0];

        String tmp2 = online ? "come online" : "gone offline";
        db.addMessage(-1, friend.getId(), tmp + " has " + tmp2, true, true, true, 5);
        db.close();
        if (online) {
            toxSingleton.sendUnsentMessages(ctx);
        }
        toxSingleton.updateMessages(ctx);
    }
}
