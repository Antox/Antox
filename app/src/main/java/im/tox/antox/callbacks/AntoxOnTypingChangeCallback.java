package im.tox.antox.callbacks;

import android.content.Context;
import android.util.Log;

import im.tox.antox.utils.AntoxFriend;
import im.tox.jtoxcore.callbacks.OnTypingChangeCallback;

/**
 * Created by soft on 27/03/14.
 */
public class AntoxOnTypingChangeCallback implements OnTypingChangeCallback<AntoxFriend> {

    private static final String TAG = "OnTypingChangeCallback";
    private Context ctx;

    public AntoxOnTypingChangeCallback(Context ctx) { this.ctx = ctx; };

    public void execute(AntoxFriend friend, boolean typing) {
        Log.d(TAG, "Typing Callback received from: " + friend.getName() + " value: " + typing);
    }
}
