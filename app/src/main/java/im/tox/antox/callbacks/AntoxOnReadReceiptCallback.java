package im.tox.antox.callbacks;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import im.tox.antox.data.AntoxDB;
import im.tox.antox.utils.AntoxFriend;
import im.tox.antox.utils.Constants;
import im.tox.jtoxcore.callbacks.OnReadReceiptCallback;

public class AntoxOnReadReceiptCallback implements OnReadReceiptCallback<AntoxFriend> {

    private final static String TAG = "im.tox.antox.TAG";
    private Context ctx;

    public AntoxOnReadReceiptCallback(Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public void execute(AntoxFriend friend, int receipt) {
        AntoxDB db = new AntoxDB(this.ctx);
        String key = db.setMessageReceived(receipt);
        db.close();

        /* Broadcast */
        Intent notify;
        notify = new Intent(Constants.BROADCAST_ACTION);
        notify.putExtra("action", Constants.UPDATE_MESSAGES);
        notify.putExtra("key", key);
        LocalBroadcastManager.getInstance(this.ctx).sendBroadcast(notify);
    }
}
