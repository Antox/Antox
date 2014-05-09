package im.tox.antox.callbacks;

import android.content.Context;
import android.util.Log;

import im.tox.antox.utils.AntoxFriend;
import im.tox.jtoxcore.callbacks.OnTypingChangeCallback;

public class AntoxOnTypingChangeCallback implements OnTypingChangeCallback<AntoxFriend> {

    private static final String TAG = "OnTypingChangeCallback";
    private Context ctx;

    public AntoxOnTypingChangeCallback(Context ctx) { this.ctx = ctx; };

    public void execute(AntoxFriend friend, boolean typing) {
    }
}
