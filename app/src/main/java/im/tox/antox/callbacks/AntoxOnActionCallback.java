package im.tox.antox.callbacks;

import android.content.Context;

import im.tox.antox.utils.AntoxFriend;
import im.tox.jtoxcore.callbacks.OnActionCallback;

public class AntoxOnActionCallback implements OnActionCallback<AntoxFriend> {

    private final static String TAG = "im.tox.antox.TAG";
    private Context ctx;

    public AntoxOnActionCallback(Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public void execute(AntoxFriend friend, String action) {
    }

}
