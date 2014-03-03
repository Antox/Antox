package im.tox.antox.callbacks;

import android.content.Context;

import im.tox.antox.AntoxFriend;
import im.tox.jtoxcore.ToxFriend;
import im.tox.jtoxcore.callbacks.OnActionCallback;

/**
 * Created by soft on 03/03/14.
 */
public class AntoxOnActionCallback implements OnActionCallback<AntoxFriend> {

    private Context ctx;

    public AntoxOnActionCallback(Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public void execute(AntoxFriend friend, String action) {

    }

}
