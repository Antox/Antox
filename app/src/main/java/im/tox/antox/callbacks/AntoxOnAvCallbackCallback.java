package im.tox.antox.callbacks;

import android.content.Context;

import im.tox.antox.utils.AntoxFriend;
import im.tox.jtoxcore.ToxAvCallbackID;
import im.tox.jtoxcore.callbacks.OnAvCallbackCallback;

/**
 * Created by Mark Winter on 03/09/14.
 */
public class AntoxOnAvCallbackCallback implements OnAvCallbackCallback<AntoxFriend> {

    private Context ctx;

    public AntoxOnAvCallbackCallback(Context ctx) {
        this.ctx = ctx;
    }

    public void execute(int callID, ToxAvCallbackID callbackID) {

    }
}
