package im.tox.antox.callbacks;

import android.content.Context;
import android.util.Log;

import im.tox.antox.utils.AntoxFriend;
import im.tox.jtoxcore.callbacks.OnActionCallback;

/**
 * Created by soft on 03/03/14.
 */
public class AntoxOnActionCallback implements OnActionCallback<AntoxFriend> {

    private final static String TAG = "im.tox.antox.TAG";
    private Context ctx;

    public AntoxOnActionCallback(Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public void execute(AntoxFriend friend, String action) {
        Log.d(TAG, "OnActionCallback received");
    }

}
