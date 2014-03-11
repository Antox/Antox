package im.tox.antox;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;

import im.tox.jtoxcore.JTox;
import im.tox.jtoxcore.ToxException;
import im.tox.jtoxcore.ToxUserStatus;
import im.tox.jtoxcore.callbacks.CallbackHandler;

/**
 * Created by soft on 01/03/14.
 */
public class ToxSingleton {

    private static final String TAG = "im.tox.antox.ToxSingleton";
    public JTox jTox;
    private AntoxFriendList antoxFriendList;
    public CallbackHandler callbackHandler;
    public ArrayList<FriendRequest> friend_requests = new ArrayList<FriendRequest>();
    public AntoxDB mDbHelper;
    SQLiteDatabase db;
    public boolean toxStarted = false;
    public AntoxFriendList friendsList;

    private static volatile ToxSingleton instance = null;

    private ToxSingleton() {

    }

    public void initTox() {
        friendsList = new AntoxFriendList();
        toxStarted = true;
        antoxFriendList = new AntoxFriendList();
        callbackHandler = new CallbackHandler(antoxFriendList);
        try {
            ToxDataFile dataFile = new ToxDataFile();

            /* Choose appropriate constructor depending on if data file exists */
            if(!dataFile.doesFileExist()) {
                Log.d(TAG, "Data file not found");
                jTox = new JTox(antoxFriendList, callbackHandler);
            } else {
                Log.d(TAG, "Data file has been found");
                if(dataFile.isExternalStorageReadable())
                    jTox = new JTox(dataFile.loadFile(), antoxFriendList, callbackHandler);
                else
                    Log.d(TAG, "Data file wasn't available for reading");
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
            if(dataFile.isExternalStorageWritable())
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
