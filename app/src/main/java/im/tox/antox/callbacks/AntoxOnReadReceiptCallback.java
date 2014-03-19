package im.tox.antox.callbacks;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import im.tox.antox.utils.AntoxFriend;
import im.tox.antox.utils.Constants;
import im.tox.antox.tox.ToxService;
import im.tox.jtoxcore.callbacks.OnReadReceiptCallback;

/**
 * Created by soft on 03/03/14.
 */
public class AntoxOnReadReceiptCallback implements OnReadReceiptCallback<AntoxFriend> {

    private final static String TAG = "im.tox.antox.TAG";
    private Context ctx;

    public AntoxOnReadReceiptCallback(Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public void execute(AntoxFriend friend, int receipt) {
        Log.d(TAG, "OnReadReceiptCallback received, receipt id = " + receipt);
        Intent intent = new Intent(this.ctx, ToxService.class);
        intent.setAction(Constants.DELIVERY_RECEIPT);
        intent.putExtra("receipt", receipt);
        this.ctx.startService(intent);
    }
}
