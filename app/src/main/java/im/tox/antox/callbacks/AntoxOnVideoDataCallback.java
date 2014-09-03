package im.tox.antox.callbacks;

import android.content.Context;

import im.tox.antox.utils.AntoxFriend;
import im.tox.jtoxcore.callbacks.OnVideoDataCallback;

/**
 * Created by Mark Winter on 03/09/14.
 */
public class AntoxOnVideoDataCallback implements OnVideoDataCallback<AntoxFriend> {

    private Context ctx;

    public AntoxOnVideoDataCallback(Context ctx) {
        this.ctx = ctx;
    }

    public void execute(int callID, byte[] data, int width, int height) {

    }
}
