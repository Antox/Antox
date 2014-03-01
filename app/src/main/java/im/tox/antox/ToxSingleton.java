package im.tox.antox;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.net.UnknownHostException;

import im.tox.jtoxcore.JTox;
import im.tox.jtoxcore.ToxException;
import im.tox.jtoxcore.callbacks.CallbackHandler;

/**
 * Created by soft on 01/03/14.
 */
public class ToxSingleton {

    private static final String TAG = "im.tox.antox.ToxSingleton";
    JTox jTox;
    AntoxFriendList antoxFriendList;
    CallbackHandler callbackHandler;

    private static volatile ToxSingleton instance = null;

    private ToxSingleton() {
        antoxFriendList = new AntoxFriendList();
        callbackHandler = new CallbackHandler(antoxFriendList);
        try {
            jTox = new JTox(antoxFriendList, callbackHandler);
            jTox.setName(UserDetails.username);
            jTox.setStatusMessage(UserDetails.note);
            jTox.setUserStatus(UserDetails.status);
            jTox.bootstrap(DhtNode.ipv4, Integer.parseInt(DhtNode.port), DhtNode.key);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (ToxException e) {
            e.printStackTrace();
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
