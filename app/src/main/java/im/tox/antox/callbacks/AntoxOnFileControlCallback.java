package im.tox.antox.callbacks;

import android.content.Context;
import android.util.Log;

import java.nio.ByteBuffer;

import im.tox.antox.data.AntoxDB;
import im.tox.antox.tox.ToxSingleton;
import im.tox.antox.utils.AntoxFriend;
import im.tox.jtoxcore.ToxFileControl;
import im.tox.jtoxcore.callbacks.OnFileControlCallback;

public class AntoxOnFileControlCallback implements OnFileControlCallback<AntoxFriend> {

    private static final String TAG = "OnFileControlCallback";
    private Context ctx;
    ToxSingleton toxSingleton = ToxSingleton.getInstance();

    public AntoxOnFileControlCallback(Context ctx) { this.ctx = ctx; }

    public void execute(AntoxFriend friend, boolean sending, int fileNumber, ToxFileControl control_type, byte[] data) {
        Log.d(TAG, "execute, control type: " + control_type.name() + " sending: " + sending);
        if (control_type.equals(ToxFileControl.TOX_FILECONTROL_FINISHED) && !sending) {
            Log.d(TAG, "TOX_FILECONTROL_FINISHED");
            toxSingleton.fileFinished(friend.getId(), fileNumber, ctx);
        }
        if (control_type.equals(ToxFileControl.TOX_FILECONTROL_ACCEPT) && sending) {
            AntoxDB antoxDB = new AntoxDB(ctx);
            int id = antoxDB.getFileId(friend.getId(), fileNumber);
            if (id != -1) {
                if (!toxSingleton.fileStatusMap.containsKey(id) || toxSingleton.fileStatusMap.get(id).equals(ToxSingleton.FileStatus.REQUESTSENT)) {
                    antoxDB.fileTransferStarted(friend.getId(), fileNumber);
                }
            }
            antoxDB.close();
            if (id != -1) {
                if (!toxSingleton.fileStatusMap.containsKey(id) || toxSingleton.fileStatusMap.get(id).equals(ToxSingleton.FileStatus.REQUESTSENT)) {
                    antoxDB.fileTransferStarted(friend.getId(), fileNumber);
                    toxSingleton.updatedMessagesSubject.onNext(true);
                    toxSingleton.sendFileData(friend.getId(), fileNumber, 0, ctx);
                } else if (toxSingleton.fileStatusMap.get(id).equals(ToxSingleton.FileStatus.PAUSED)) {
                    toxSingleton.sendFileData(friend.getId(), fileNumber, toxSingleton.getProgress(id), ctx);
                }
            }
        }
        if (control_type.equals(ToxFileControl.TOX_FILECONTROL_RESUME_BROKEN) && sending) {
            try {
                toxSingleton.jTox.fileSendControl(friend.getFriendnumber(), true, fileNumber, ToxFileControl.TOX_FILECONTROL_ACCEPT.ordinal(), new byte[0]);
            } catch (Exception e) {
                e.printStackTrace();
            }
            toxSingleton.sendFileData(friend.getId(), fileNumber, (int) ByteBuffer.wrap(data).getLong(), ctx);
        }
        if (control_type.equals(ToxFileControl.TOX_FILECONTROL_PAUSE) && sending) {
            AntoxDB antoxDB = new AntoxDB(ctx);
            int id = antoxDB.getFileId(friend.getId(), fileNumber);
            antoxDB.close();
            if (id != -1) {
                toxSingleton.fileStatusMap.put(id, ToxSingleton.FileStatus.PAUSED);
            }
        }
        if (control_type.equals(ToxFileControl.TOX_FILECONTROL_KILL) && sending) {
            toxSingleton.cancelFile(friend.getId(), fileNumber, ctx);
        }
    }
}
