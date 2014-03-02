package im.tox.antox.callbacks;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import im.tox.antox.Constants;
import im.tox.jtoxcore.callbacks.OnFriendRequestCallback;

/**
 * Created by soft on 02/03/14.
 */
public class AntoxOnFriendRequestCallback implements OnFriendRequestCallback {

    private static final String TAG = "im.tox.antox.TAG";

    private Context ctx;

    public AntoxOnFriendRequestCallback(Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public void execute(String publicKey, String message){
        Log.d(TAG, "Friend request callback");
        Context context = ctx;
        CharSequence text = "You have received a friend request: " + message;
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }
}
