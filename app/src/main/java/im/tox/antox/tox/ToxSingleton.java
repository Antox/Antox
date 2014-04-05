package im.tox.antox.tox;

import android.app.NotificationManager;
import android.content.Context;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;

import im.tox.antox.utils.AntoxFriendList;
import im.tox.antox.utils.FriendRequest;
import im.tox.antox.utils.UserDetails;
import im.tox.jtoxcore.JTox;
import im.tox.jtoxcore.ToxException;
import im.tox.jtoxcore.ToxUserStatus;
import im.tox.jtoxcore.callbacks.CallbackHandler;

/**
 * Created by soft on 01/03/14.
 */
public class ToxSingleton {

    private static final String TAG = "im.tox.antox.tox.ToxSingleton";
    public JTox jTox;
    private AntoxFriendList antoxFriendList;
    public CallbackHandler callbackHandler;
    public ArrayList<FriendRequest> friend_requests = new ArrayList<FriendRequest>();
    public boolean toxStarted = false;
    public AntoxFriendList friendsList;
    public String activeFriendRequestKey = null;
    public String activeFriendKey = null;
    public boolean rightPaneActive = false;
    public boolean leftPaneActive = false;
    public NotificationManager mNotificationManager;
    public ToxDataFile dataFile;
    public File qrFile;

    private static volatile ToxSingleton instance = null;

    private ToxSingleton() {

    }

    public void initTox(Context ctx) {
        friendsList = new AntoxFriendList();
        toxStarted = true;
        antoxFriendList = new AntoxFriendList();
        callbackHandler = new CallbackHandler(antoxFriendList);

        try {
            qrFile = ctx.getFileStreamPath("userkey_qr.png");
            dataFile = new ToxDataFile(ctx);

            /* Choose appropriate constructor depending on if data file exists */
            if(!dataFile.doesFileExist()) {
                Log.d(TAG, "Data file not found");
                jTox = new JTox(antoxFriendList, callbackHandler);
                
            } else {
                Log.d(TAG, "Data file has been found");
                jTox = new JTox(dataFile.loadFile(), antoxFriendList, callbackHandler);

            }

            if(UserDetails.username == null)
                UserDetails.username = "antoxUser";
            jTox.setName(UserDetails.username);

            if(UserDetails.note == null)
                UserDetails.note = "using antox";
            jTox.setStatusMessage(UserDetails.note);

            if(UserDetails.status == null)
                UserDetails.status = ToxUserStatus.TOX_USERSTATUS_NONE;
            jTox.setUserStatus(UserDetails.status);

            /* Save data file */
            dataFile.saveFile(jTox.save());

        } catch (ToxException e) {
            e.printStackTrace();
            Log.d(TAG, e.getError().toString());
        }
    }

    public static ToxSingleton getInstance() {
        /* Double-checked locking */
        if(instance == null) {
            synchronized (ToxSingleton.class) {
                if(instance == null) {
                    instance = new ToxSingleton();
                }
            }
        }

        return instance;
    }
}
