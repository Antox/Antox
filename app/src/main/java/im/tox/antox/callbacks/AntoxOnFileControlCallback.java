package im.tox.antox.callbacks;

import android.content.Context;
import android.util.Log;

import im.tox.antox.data.AntoxDB;
import im.tox.antox.tox.ToxSingleton;
import im.tox.antox.utils.AntoxFriend;
import im.tox.jtoxcore.callbacks.OnFileControlCallback;
import im.tox.jtoxcore.ToxFileControl;
import im.tox.jtoxcore.callbacks.OnFileSendRequestCallback;

public class AntoxOnFileControlCallback implements OnFileControlCallback<AntoxFriend> {

    private static final String TAG = "OnFileControlCallback";
    private Context ctx;
    ToxSingleton toxSingleton = ToxSingleton.getInstance();

    public AntoxOnFileControlCallback(Context ctx) { this.ctx = ctx; };

    public void execute(AntoxFriend friend, boolean sending, int fileNumber, ToxFileControl control_type, byte[] data) {
        Log.d(TAG, "execute");
        if (control_type.equals(ToxFileControl.TOX_FILECONTROL_ACCEPT) && sending) {
            toxSingleton.sendFileData(friend.getId(), fileNumber, 0, ctx);
        }
        if (control_type.equals(ToxFileControl.TOX_FILECONTROL_FINISHED) && !sending) {
            toxSingleton.fileFinished(friend.getId(), fileNumber, ctx);
        }
    }
}
