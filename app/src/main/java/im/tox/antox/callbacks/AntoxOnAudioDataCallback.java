package im.tox.antox.callbacks;

import android.content.Context;

import im.tox.antox.utils.AntoxFriend;
import im.tox.jtoxcore.callbacks.OnAudioDataCallback;

/**
 * Created by Mark Winter on 03/09/14.
 */
public class AntoxOnAudioDataCallback implements OnAudioDataCallback<AntoxFriend> {

    private Context ctx;

    public AntoxOnAudioDataCallback(Context ctx) {
        this.ctx = ctx;
    }

    public void execute(int callID, byte[] data) {

    }
}
