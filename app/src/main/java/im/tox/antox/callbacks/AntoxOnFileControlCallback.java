package im.tox.antox.callbacks;

import android.content.Context;
import android.util.Log;

import im.tox.antox.utils.AntoxFriend;
import im.tox.jtoxcore.callbacks.OnFileControlCallback;
import im.tox.jtoxcore.ToxFileControl;
import im.tox.jtoxcore.callbacks.OnFileSendRequestCallback;

public class AntoxOnFileControlCallback implements OnFileControlCallback<AntoxFriend> {

    private static final String TAG = "OnFileControlCallback";
    private Context ctx;

    public AntoxOnFileControlCallback(Context ctx) { this.ctx = ctx; };

    public void execute(AntoxFriend friend, boolean sending, ToxFileControl control_type, byte[] data) {
        Log.d(TAG, "execute");
    }
}
