package im.tox.antox.callbacks;

import android.content.Context;
import android.util.Log;

import im.tox.antox.tox.ToxSingleton;
import im.tox.antox.utils.AntoxFriend;
import im.tox.jtoxcore.ToxAvCallbackID;
import im.tox.jtoxcore.ToxCallType;
import im.tox.jtoxcore.ToxCodecSettings;
import im.tox.jtoxcore.ToxException;
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
        Log.d("OnAvCallbackCallback", "Received a callback from: " + callID);
        ToxSingleton toxSingleton = ToxSingleton.getInstance();
        ToxCodecSettings toxCodecSettings = new ToxCodecSettings(ToxCallType.TYPE_AUDIO, 0, 0, 0, 64000, 20, 48000, 1);
        try {
            toxSingleton.jTox.avAnswer(0, toxCodecSettings);
        } catch (ToxException e) {
        }
    }
}
