package im.tox.antox.tox;

import android.app.NotificationManager;
import android.content.Context;
import android.util.Log;

import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;

import im.tox.antox.data.AntoxDB;
import im.tox.antox.utils.AntoxFriend;
import im.tox.antox.utils.AntoxFriendList;
import im.tox.antox.utils.Friend;
import im.tox.antox.utils.FriendInfo;
import im.tox.antox.utils.FriendRequest;
import im.tox.antox.utils.Tuple;
import im.tox.antox.utils.UserDetails;
import im.tox.jtoxcore.JTox;
import im.tox.jtoxcore.ToxException;
import im.tox.jtoxcore.ToxUserStatus;
import im.tox.jtoxcore.callbacks.CallbackHandler;
import rx.Observable;
import rx.functions.Func2;
import rx.functions.Func3;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;

import static rx.Observable.combineLatest;

public class ToxSingleton {

    private static final String TAG = "im.tox.antox.tox.ToxSingleton";
    public JTox jTox;
    private AntoxFriendList antoxFriendList;
    public CallbackHandler callbackHandler;
    public boolean toxStarted = false;
    public NotificationManager mNotificationManager;
    public ToxDataFile dataFile;
    public File qrFile;
    public BehaviorSubject<ArrayList<Friend>> friendListSubject;
    public BehaviorSubject<ArrayList<FriendRequest>> friendRequestSubject;
    public BehaviorSubject<HashMap> lastMessagesSubject;
    public BehaviorSubject<HashMap> unreadCountsSubject;
    public PublishSubject<String> activeKeySubject;
    public BehaviorSubject<Boolean> updatedMessagesSubject;
    public BehaviorSubject<Boolean> rightPaneOpen;
    public rx.Observable friendInfoListSubject;
    public rx.Observable activeKeyAndIsFriendSubject;
    public Observable friendListAndRequestsSubject;
    public Observable chatActiveAndKey;

    public AntoxFriend getAntoxFriend(String key) {
        return antoxFriendList.getById(key);
    }

    public void initSubjects(Context ctx){
        friendListSubject = BehaviorSubject.create(new ArrayList<Friend>());
        friendListSubject.subscribeOn(Schedulers.io());
        rightPaneOpen = BehaviorSubject.create(new Boolean(false));
        rightPaneOpen.subscribeOn(Schedulers.io());
        friendRequestSubject = BehaviorSubject.create(new ArrayList<FriendRequest>());
        friendRequestSubject.subscribeOn(Schedulers.io());
        lastMessagesSubject = BehaviorSubject.create(new HashMap());
        lastMessagesSubject.subscribeOn(Schedulers.io());
        unreadCountsSubject = BehaviorSubject.create(new HashMap());
        unreadCountsSubject.subscribeOn(Schedulers.io());
        activeKeySubject = PublishSubject.create();
        activeKeySubject.subscribeOn(Schedulers.io());
        updatedMessagesSubject = BehaviorSubject.create(new Boolean(true));
        updatedMessagesSubject.subscribeOn(Schedulers.io());
        friendInfoListSubject = combineLatest(friendListSubject, lastMessagesSubject, unreadCountsSubject, new Func3<ArrayList<Friend>, HashMap, HashMap, ArrayList<FriendInfo>>() {
            @Override
            public ArrayList<FriendInfo> call(ArrayList<Friend> fl, HashMap lm, HashMap uc) {
                ArrayList<FriendInfo> fi = new ArrayList<FriendInfo>();
                for (Friend f : fl) {
                    String lastMessage;
                    Timestamp lastMessageTimestamp;
                    int unreadCount;
                    if (lm.containsKey(f.friendKey)) {
                        lastMessage = (String) ((Tuple<String,Timestamp>) lm.get(f.friendKey)).x;
                        lastMessageTimestamp = (Timestamp) ((Tuple<String,Timestamp>) lm.get(f.friendKey)).y;
                    } else {
                        lastMessage = "";
                        lastMessageTimestamp = new Timestamp(0,0,0,0,0,0,0);
                    }
                    if (uc.containsKey(f.friendKey)) {
                        unreadCount = (Integer) uc.get(f.friendKey);
                    } else {
                        unreadCount = 0;
                    }
                    fi.add(new FriendInfo(f.icon, f.friendName, f.friendStatus, f.personalNote, f.friendKey, lastMessage, lastMessageTimestamp, unreadCount));
                }
                return fi;
            }
        });
        friendListAndRequestsSubject = combineLatest(friendInfoListSubject, friendRequestSubject, new Func2<ArrayList<FriendInfo>, ArrayList<FriendRequest>, Tuple<ArrayList<FriendInfo>, ArrayList<FriendRequest>>>() {
            @Override
            public Tuple<ArrayList<FriendInfo>, ArrayList<FriendRequest>> call (ArrayList<FriendInfo> fl, ArrayList<FriendRequest> fr) {
                return new Tuple(fl,fr);
            }
        });
        activeKeyAndIsFriendSubject = combineLatest(activeKeySubject, friendListSubject, new Func2<String, ArrayList<Friend>, Tuple<String,Boolean>> () {
            @Override
            public Tuple<String,Boolean> call (String key, ArrayList<Friend> fl) {
                boolean isFriend;
                isFriend = isKeyFriend(key, fl);
                return new Tuple<String,Boolean>(key, isFriend);
            }
        });
        chatActiveAndKey = combineLatest(rightPaneOpen, activeKeySubject, new Func2<Boolean, String, Tuple<String,Boolean>>(){
            @Override
            public Tuple<String,Boolean> call (Boolean rightActive, String key) {
                return new Tuple<String, Boolean>(key, rightActive);
            }

        });
    };

    private boolean isKeyFriend(String key, ArrayList<Friend> fl) {
        for(Friend f: fl) {
            if (f.friendKey.equals(key)) {
                return true;
            }
        }
        return false;
    };

    public void updateFriendsList(Context ctx) {
        try {
            AntoxDB antoxDB = new AntoxDB(ctx);

            ArrayList<Friend> friendList = antoxDB.getFriendList();

            antoxDB.close();

            friendListSubject.onNext(friendList);
        } catch (Exception e) {
            friendListSubject.onError(e);
        }
    }

    public void updateFriendRequests(Context ctx) {
        try {
            AntoxDB antoxDB = new AntoxDB(ctx);
            ArrayList<FriendRequest> friendRequest = antoxDB.getFriendRequestsList();
            antoxDB.close();
            friendRequestSubject.onNext(friendRequest);
        } catch (Exception e) {
            friendRequestSubject.onError(e);
        }
    }

    public void updateMessages(Context ctx) {
        updatedMessagesSubject.onNext(true);
        updateLastMessageMap(ctx);
        updateUnreadCountMap(ctx);
    }

    public void updateLastMessageMap(Context ctx) {
        try {
            AntoxDB antoxDB = new AntoxDB(ctx);
            HashMap map = antoxDB.getLastMessages();
            antoxDB.close();

            lastMessagesSubject.onNext(map);
        } catch (Exception e) {
            lastMessagesSubject.onError(e);
        }
    }
    public void updateUnreadCountMap(Context ctx) {
        try {
            AntoxDB antoxDB = new AntoxDB(ctx);
            HashMap map = antoxDB.getUnreadCounts();
            antoxDB.close();

            unreadCountsSubject.onNext(map);
        } catch (Exception e) {
            unreadCountsSubject.onError(e);
        }
    }
    private static volatile ToxSingleton instance = null;

    private ToxSingleton() {
    }

    public void initTox(Context ctx) {
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
