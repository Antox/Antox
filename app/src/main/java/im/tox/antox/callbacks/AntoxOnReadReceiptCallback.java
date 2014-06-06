package im.tox.antox.callbacks;

import android.content.Context;

import im.tox.antox.data.AntoxDB;
import im.tox.antox.tox.ToxSingleton;
import im.tox.antox.utils.AntoxFriend;
import im.tox.jtoxcore.callbacks.OnReadReceiptCallback;

public class AntoxOnReadReceiptCallback implements OnReadReceiptCallback<AntoxFriend> {

    private final static String TAG = "im.tox.antox.TAG";
    private Context ctx;
    ToxSingleton toxSingleton = ToxSingleton.getInstance();

    public AntoxOnReadReceiptCallback(Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public void execute(AntoxFriend friend, int receipt) {
        AntoxDB db = new AntoxDB(this.ctx);
        String key = db.setMessageReceived(receipt);
        db.close();

        /* Broadcast */
        toxSingleton.updateMessages(ctx);
    }
}
