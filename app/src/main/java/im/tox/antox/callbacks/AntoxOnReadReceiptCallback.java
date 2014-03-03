package im.tox.antox.callbacks;

import android.content.Context;

import im.tox.antox.AntoxFriend;
import im.tox.jtoxcore.callbacks.OnReadReceiptCallback;

/**
 * Created by soft on 03/03/14.
 */
public class AntoxOnReadReceiptCallback implements OnReadReceiptCallback<AntoxFriend> {

    private Context ctx;

    public AntoxOnReadReceiptCallback(Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public void execute(AntoxFriend friend, int receipt) {

    }
}
