package im.tox.antox.tox;

import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.util.ArrayList;
import java.util.Observable;

import im.tox.antox.data.AntoxDB;
import im.tox.antox.utils.AntoxFriend;
import im.tox.antox.utils.AntoxFriendList;
import im.tox.antox.utils.Constants;
import im.tox.antox.utils.Friend;
import im.tox.antox.utils.FriendRequest;
import im.tox.antox.utils.LeftPaneItem;
import im.tox.antox.utils.Message;
import im.tox.antox.utils.UserDetails;
import im.tox.jtoxcore.JTox;
import im.tox.jtoxcore.ToxException;
import im.tox.jtoxcore.ToxUserStatus;
import im.tox.jtoxcore.callbacks.CallbackHandler;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;
import rx.subscriptions.Subscriptions;

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
    public BehaviorSubject<ArrayList<Friend>> friendListSubject;
    public rx.Observable<ArrayList<Friend>> friendListObservable(final Context ctx) {
        return rx.Observable.create(new rx.Observable.OnSubscribeFunc<ArrayList<Friend>>() {
            @Override
            public Subscription onSubscribe(Observer<? super ArrayList<Friend>> observer) {
                try {
                    AntoxDB antoxDB = new AntoxDB(ctx);

                    ArrayList<Friend> friendList = antoxDB.getFriendList(Constants.OPTION_ALL_FRIENDS);

                    antoxDB.close();

                    observer.onNext(friendList);
                    observer.onCompleted();
                } catch (Exception e) {
                    observer.onError(e);
                }
                return Subscriptions.empty();
            }
        });
    };

    public void initFriendsList(Context ctx){
        ArrayList<Friend> fl = new ArrayList<Friend>();
        friendListSubject = BehaviorSubject.create(fl);
    };

    public Subscription updateFriendsList(Context ctx) {
        return friendListObservable(ctx)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<ArrayList<Friend>>(){
                    @Override
                    public void call(ArrayList<Friend> fl) {
                        Log.d("FRIENDS LIST SHIT HAPPENING","");
                        friendListSubject.onNext(fl);
                    }
                });
    }

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
                jTox = new JTox(antoxFriendList, callbackHandler);
                
            } else {
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
