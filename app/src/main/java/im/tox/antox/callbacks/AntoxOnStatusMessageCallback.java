package im.tox.antox.callbacks;

import android.content.Context;

import im.tox.antox.AntoxFriend;
import im.tox.jtoxcore.callbacks.OnStatusMessageCallback;

/**
 * Created by soft on 03/03/14.
 */
public class AntoxOnStatusMessageCallback implements OnStatusMessageCallback<AntoxFriend> {

    private Context ctx;

    public AntoxOnStatusMessageCallback(Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public void execute(AntoxFriend friend, String newStatus) {

    }
}
