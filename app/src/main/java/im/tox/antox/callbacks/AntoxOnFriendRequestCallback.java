package im.tox.antox.callbacks;

import android.content.Context;

import im.tox.jtoxcore.callbacks.OnFriendRequestCallback;

/**
 * Created by soft on 02/03/14.
 */
public class AntoxOnFriendRequestCallback implements OnFriendRequestCallback {

    private Context ctx;

    AntoxOnFriendRequestCallback(Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public void execute(String publicKey, String message){
        
    }
}
