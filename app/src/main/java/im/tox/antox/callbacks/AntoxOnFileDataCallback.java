package im.tox.antox.callbacks;

import android.content.Context;
import android.util.Log;

import im.tox.antox.utils.AntoxFriend;
import im.tox.jtoxcore.callbacks.OnFileDataCallback;
import im.tox.jtoxcore.callbacks.OnFileSendRequestCallback;

public class AntoxOnFileDataCallback implements OnFileDataCallback<AntoxFriend> {

    private static final String TAG = "OnFileDataCallback";
    private Context ctx;

    public AntoxOnFileDataCallback(Context ctx) { this.ctx = ctx; };

    public void execute(AntoxFriend friend, int filenumber, byte[] data) {
        Log.d(TAG, "execute");
    }
}
