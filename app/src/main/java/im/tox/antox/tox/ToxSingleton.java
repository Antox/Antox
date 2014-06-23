package im.tox.antox.tox;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.content.CursorLoader;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import im.tox.antox.callbacks.AntoxOnActionCallback;
import im.tox.antox.callbacks.AntoxOnConnectionStatusCallback;
import im.tox.antox.callbacks.AntoxOnFileControlCallback;
import im.tox.antox.callbacks.AntoxOnFileDataCallback;
import im.tox.antox.callbacks.AntoxOnFileSendRequestCallback;
import im.tox.antox.callbacks.AntoxOnFriendRequestCallback;
import im.tox.antox.callbacks.AntoxOnMessageCallback;
import im.tox.antox.callbacks.AntoxOnNameChangeCallback;
import im.tox.antox.callbacks.AntoxOnReadReceiptCallback;
import im.tox.antox.callbacks.AntoxOnStatusMessageCallback;
import im.tox.antox.callbacks.AntoxOnTypingChangeCallback;
import im.tox.antox.callbacks.AntoxOnUserStatusCallback;
import im.tox.antox.data.AntoxDB;
import im.tox.antox.utils.AntoxFriend;
import im.tox.antox.utils.AntoxFriendList;
import im.tox.antox.utils.Constants;
import im.tox.antox.utils.DHTNodeDetails;
import im.tox.antox.utils.DhtNode;
import im.tox.antox.utils.Friend;
import im.tox.antox.utils.FriendInfo;
import im.tox.antox.utils.FriendRequest;
import im.tox.antox.utils.Message;
import im.tox.antox.utils.Tuple;
import im.tox.jtoxcore.JTox;
import im.tox.jtoxcore.ToxException;
import im.tox.jtoxcore.ToxFileControl;
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
    public NotificationManager mNotificationManager;
    public ToxDataFile dataFile;
    public File qrFile;
    public BehaviorSubject<ArrayList<Friend>> friendListSubject;
    public BehaviorSubject<ArrayList<FriendRequest>> friendRequestSubject;
    public BehaviorSubject<HashMap> lastMessagesSubject;
    public BehaviorSubject<HashMap> unreadCountsSubject;
    public BehaviorSubject<String> activeKeySubject;
    public BehaviorSubject<Boolean> updatedMessagesSubject;
    public BehaviorSubject<Boolean> rightPaneOpenSubject;
    public rx.Observable friendInfoListSubject;
    public rx.Observable activeKeyAndIsFriendSubject;
    public Observable friendListAndRequestsSubject;
    public Observable chatActiveAndKey;
    public HashMap<Integer, Integer> progressMap = new HashMap<Integer, Integer>();

    public String activeKey; //ONLY FOR USE BY CALLBACKS
    public boolean chatActive; //ONLY FOR USE BY CALLBACKS

    public boolean isRunning = false;

    public AntoxFriend getAntoxFriend(String key) {
        return antoxFriendList.getById(key);
    }

    public void initSubjects(Context ctx) {
        friendListSubject = BehaviorSubject.create(new ArrayList<Friend>());
        friendListSubject.subscribeOn(Schedulers.io());
        rightPaneOpenSubject = BehaviorSubject.create(new Boolean(false));
        rightPaneOpenSubject.subscribeOn(Schedulers.io());
        friendRequestSubject = BehaviorSubject.create(new ArrayList<FriendRequest>());
        friendRequestSubject.subscribeOn(Schedulers.io());
        lastMessagesSubject = BehaviorSubject.create(new HashMap());
        lastMessagesSubject.subscribeOn(Schedulers.io());
        unreadCountsSubject = BehaviorSubject.create(new HashMap());
        unreadCountsSubject.subscribeOn(Schedulers.io());
        activeKeySubject = BehaviorSubject.create("");
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
                        lastMessage = (String) ((Tuple<String, Timestamp>) lm.get(f.friendKey)).x;
                        lastMessageTimestamp = (Timestamp) ((Tuple<String, Timestamp>) lm.get(f.friendKey)).y;
                    } else {
                        lastMessage = "";
                        lastMessageTimestamp = new Timestamp(0, 0, 0, 0, 0, 0, 0);
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
            public Tuple<ArrayList<FriendInfo>, ArrayList<FriendRequest>> call(ArrayList<FriendInfo> fl, ArrayList<FriendRequest> fr) {
                return new Tuple(fl, fr);
            }
        });
        activeKeyAndIsFriendSubject = combineLatest(activeKeySubject, friendListSubject, new Func2<String, ArrayList<Friend>, Tuple<String, Boolean>>() {
            @Override
            public Tuple<String, Boolean> call(String key, ArrayList<Friend> fl) {
                boolean isFriend;
                isFriend = isKeyFriend(key, fl);
                return new Tuple<String, Boolean>(key, isFriend);
            }
        });
        chatActiveAndKey = combineLatest(rightPaneOpenSubject, activeKeySubject, new Func2<Boolean, String, Tuple<String, Boolean>>() {
            @Override
            public Tuple<String, Boolean> call(Boolean rightActive, String key) {
                return new Tuple<String, Boolean>(key, rightActive);
            }

        });
    }


    private boolean isKeyFriend(String key, ArrayList<Friend> fl) {
        for (Friend f : fl) {
            if (f.friendKey.equals(key)) {
                return true;
            }
        }
        return false;
    }

    public void sendFileSendRequest(String path, String key, Context context) {
        Log.d("sendFileSendRequest path", path);
        File file = new File(path);
        String[] splitPath = path.split("/");
        String fileName = splitPath[splitPath.length - 1];
        if (fileName != null) {
            int fileNumber = -1;
            try {
                fileNumber = jTox.toxNewFileSender(getAntoxFriend(activeKey).getFriendnumber(), file.length(), fileName);
            } catch (Exception e) {
                Log.d("toxNewFileSender error", e.toString());
            }
            if (fileNumber != -1) {
                AntoxDB antoxDB = new AntoxDB(context);
                antoxDB.addFileTransfer(key, path, fileNumber, (int) file.length(), true);
                antoxDB.close();
            }
        }
    }

    public void fileSendRequest(String key, int fileNumber, String fileName, long fileSize, Context context) {
        Log.d("fileSendRequest, fileNumber: ",Integer.toString(fileNumber));
        String fileN = fileName;
        String[] fileSplit = fileName.split("\\.");
        String filePre = "";
        String fileExt = fileSplit[fileSplit.length-1];
        for (int j=0; j<fileSplit.length-1; j++) {
            filePre = filePre.concat(fileSplit[j]);
            if (j<fileSplit.length-2) {
                filePre = filePre.concat(".");
            }
        }
        File dirfile = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), Constants.DOWNLOAD_DIRECTORY);
        if (!dirfile.mkdirs()) {
            Log.e("acceptFile", "Directory not created");
        }
        File file = new File(dirfile.getPath(), fileN);
        if (file.exists()) {
            int i = 1;
            do {
                fileN = filePre + "(" + Integer.toString(i) + ")" + "." + fileExt;
                file = new File(dirfile.getPath(), fileN);
                i++;
            } while (file.exists());
        }
        AntoxDB antoxDB = new AntoxDB(context);
        antoxDB.addFileTransfer(key, fileN, fileNumber, (int) fileSize, false);
        antoxDB.close();
        acceptFile(key, fileNumber, context);
    }

    public void acceptFile(String key, int fileNumber, Context context) {
        try {
            jTox.toxFileSendControl(antoxFriendList.getById(key).getFriendnumber(), false, fileNumber, ToxFileControl.TOX_FILECONTROL_ACCEPT.ordinal(), new byte[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }
        AntoxDB antoxDB = new AntoxDB(context);
        antoxDB.fileTransferStarted(key, fileNumber);
        antoxDB.close();
        updatedMessagesSubject.onNext(true);
    }

    public void receiveFileData(String key, int fileNumber, byte[] data, Context context) {
        AntoxDB antoxDB = new AntoxDB(context);
        String fileName = antoxDB.getFilePath(key, fileNumber);
        int id = antoxDB.getFileId(key, fileNumber);
        antoxDB.close();
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            File dirfile = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), Constants.DOWNLOAD_DIRECTORY);
            if (!dirfile.mkdirs()) {
                Log.e("acceptFile", "Directory not created");
            }
            File file = new File(dirfile.getPath(), fileName);
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(file, true);
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                output.write(data);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                incrementProgress(id, data.length);
                updatedMessagesSubject.onNext(true);
                try {
                    output.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void incrementProgress(int id, int length) {
        Integer idObject = id;
        if (id != -1) {
            if (!progressMap.containsKey(idObject)) {
                progressMap.put(idObject, length);
            } else {
                Integer current = progressMap.get(idObject);
                progressMap.put(idObject, current+length);
            }
        }
    }

    public void fileFinished(String key, int fileNumber, Context context) {
        Log.d("ToxSingleton","fileFinished");
        AntoxDB db = new AntoxDB(context);
        db.fileFinished(key, fileNumber);
        db.close();
        updatedMessagesSubject.onNext(true);
    }

    public int getProgress(int id) {
        if (id != -1 && progressMap.containsKey(id)) {
            return progressMap.get(id);
        } else {
            return 0;
        }
    }

    public void sendFileData(final String key, final int fileNumber, final int startPosition, final Context context) {
        class sendFileTask extends AsyncTask<Void, Void, Void> {
            @Override
            protected Void doInBackground(Void... params) {
                boolean result = doSendFileData(key, fileNumber, startPosition, context);
                Log.d("doSendFileData finished, result: ", Boolean.toString(result));
                AntoxDB db = new AntoxDB(context);
                db.clearFileNumber(key, fileNumber);
                db.close();
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
            }
        }
        new sendFileTask().execute();
    }

    public boolean doSendFileData(final String key, final int fileNumber, final int startPosition, final Context context) {
        String path = "";
        AntoxDB antoxDB = new AntoxDB(context);
        path = antoxDB.getFilePath(key, fileNumber);
        antoxDB.close();
        int result = -1;
        if (!path.equals("")) {
            int chunkSize = 1;
            try {
                chunkSize = jTox.toxFileDataSize(getAntoxFriend(key).getFriendnumber());
            } catch (Exception e) {
                e.printStackTrace();
            }
            File file = new File(path);
            byte[] bytes = new byte[(int) file.length()];
            BufferedInputStream buf = null;
            try {
                buf = new BufferedInputStream(new FileInputStream(file));
            } catch (Exception e) {
                e.printStackTrace();
            }
            int i = startPosition;
            if (buf != null) {
                for (i = startPosition; i < bytes.length; i = i + chunkSize) {
                    byte[] data = new byte[chunkSize];
                    try {
                        buf.mark(chunkSize*2);
                        int read = buf.read(data, 0, chunkSize);
                    } catch (Exception e) {
                        e.printStackTrace();
                        break;
                    }
                    try {
                        result = jTox.toxFileSendData(getAntoxFriend(key).getFriendnumber(), fileNumber, data);
                    } catch (Exception e) {
                        e.printStackTrace();
                        break;
                    }
                    if (result == -1) {
                        Log.d("sendFileDataTask", "toxFileSendData failed");
                        try {
                            jTox.doTox();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        SystemClock.sleep(50);
                        i = i - chunkSize;
                        try {
                            buf.reset();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                if (i > bytes.length) {
                    i = bytes.length;
                }
                try {
                    buf.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (result != -1) {
                try {
                    Log.d("toxFileSendControl", "FINISHED");
                    jTox.toxFileSendControl(getAntoxFriend(key).getFriendnumber(), true, fileNumber, ToxFileControl.TOX_FILECONTROL_FINISHED.ordinal(), new byte[0]);
                    fileFinished(key, fileNumber, context);
                    return true;
                } catch (Exception e) {
                    Log.d("toxFileSendControl error", e.toString());
                }
            } else {
                return false;
            }
        }
        return false;
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private static byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

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

    public void clearUselessNotifications (String key) {
        if (key != null && !key.equals("")) {
            try {
                mNotificationManager.cancel(getAntoxFriend(key).getFriendnumber());
            } catch (Exception e) {

            }
        }
    }

    public void sendUnsentMessages(Context ctx) {
            AntoxDB db = new AntoxDB(ctx);
            ArrayList<Message> unsentMessageList = db.getUnsentMessageList();
            for (int i = 0; i<unsentMessageList.size(); i++) {
                AntoxFriend friend = null;
                int id = unsentMessageList.get(i).message_id;
                boolean sendingSucceeded = true;
                try {
                    friend = getAntoxFriend(unsentMessageList.get(i).key);
                } catch (Exception e) {
                    Log.d(TAG, e.toString());
                }
                try {
                    if (friend != null) {
                        jTox.sendMessage(friend, unsentMessageList.get(i).message, id);
                    }
                } catch (ToxException e) {
                    Log.d(TAG, e.toString());
                    e.printStackTrace();
                    sendingSucceeded = false;
                }
                if (sendingSucceeded) {
                    db.updateUnsentMessage(id);
                }
            }
            db.close();
            updateMessages(ctx);
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

        try {
            System.load("/data/data/im.tox.antox/lib/libsodium.so");
            System.load("/data/data/im.tox.antox/lib/libtoxcore.so");
        } catch (Exception e) {
            Log.d(TAG, "Failed System.load()");
            e.printStackTrace();
        }


        antoxFriendList = new AntoxFriendList();
        callbackHandler = new CallbackHandler(antoxFriendList);

        qrFile = ctx.getFileStreamPath("userkey_qr.png");
        dataFile = new ToxDataFile(ctx);

        /* Choose appropriate constructor depending on if data file exists */
        if (!dataFile.doesFileExist()) {
            try {
                jTox = new JTox(antoxFriendList, callbackHandler);
                /* Save data file */
                dataFile.saveFile(jTox.save());
                /* Save users public key to settings */
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("tox_id", jTox.getAddress());
                editor.commit();
            } catch (ToxException e) {
                e.printStackTrace();
            }
        } else {
            try {
                jTox = new JTox(dataFile.loadFile(), antoxFriendList, callbackHandler);
            } catch (ToxException e) {
                e.printStackTrace();
            }
        }

        /* If the service wasn't running then we wouldn't have gotten callbacks for a user
         *  going offline so default everyone to offline and just wait for callbacks.
        */
        AntoxDB db = new AntoxDB(ctx);
        db.setAllOffline();

        /* Populate tox friends list with saved friends in database */
        ArrayList<Friend> friends = db.getFriendList();
        db.close();

        if (friends.size() > 0) {
            for (int i = 0; i < friends.size(); i++) {
                try {
                    jTox.confirmRequest(friends.get(i).friendKey);
                } catch (Exception e) {
                }
            }
        }

        /* Instansiate and register callback classes */
        AntoxOnMessageCallback antoxOnMessageCallback = new AntoxOnMessageCallback(ctx);
        AntoxOnFriendRequestCallback antoxOnFriendRequestCallback = new AntoxOnFriendRequestCallback(ctx);
        AntoxOnActionCallback antoxOnActionCallback = new AntoxOnActionCallback(ctx);
        AntoxOnConnectionStatusCallback antoxOnConnectionStatusCallback = new AntoxOnConnectionStatusCallback(ctx);
        AntoxOnNameChangeCallback antoxOnNameChangeCallback = new AntoxOnNameChangeCallback(ctx);
        AntoxOnReadReceiptCallback antoxOnReadReceiptCallback = new AntoxOnReadReceiptCallback(ctx);
        AntoxOnStatusMessageCallback antoxOnStatusMessageCallback = new AntoxOnStatusMessageCallback(ctx);
        AntoxOnUserStatusCallback antoxOnUserStatusCallback = new AntoxOnUserStatusCallback(ctx);
        AntoxOnTypingChangeCallback antoxOnTypingChangeCallback = new AntoxOnTypingChangeCallback(ctx);
        AntoxOnFileSendRequestCallback antoxOnFileSendRequestCallback = new AntoxOnFileSendRequestCallback(ctx);
        AntoxOnFileControlCallback antoxOnFileControlCallback = new AntoxOnFileControlCallback(ctx);
        AntoxOnFileDataCallback antoxOnFileDataCallback = new AntoxOnFileDataCallback(ctx);

        callbackHandler.registerOnMessageCallback(antoxOnMessageCallback);
        callbackHandler.registerOnFriendRequestCallback(antoxOnFriendRequestCallback);
        callbackHandler.registerOnActionCallback(antoxOnActionCallback);
        callbackHandler.registerOnConnectionStatusCallback(antoxOnConnectionStatusCallback);
        callbackHandler.registerOnNameChangeCallback(antoxOnNameChangeCallback);
        callbackHandler.registerOnReadReceiptCallback(antoxOnReadReceiptCallback);
        callbackHandler.registerOnStatusMessageCallback(antoxOnStatusMessageCallback);
        callbackHandler.registerOnUserStatusCallback(antoxOnUserStatusCallback);
        callbackHandler.registerOnTypingChangeCallback(antoxOnTypingChangeCallback);
        callbackHandler.registerOnFileSendRequestCallback(antoxOnFileSendRequestCallback);
        callbackHandler.registerOnFileControlCallback(antoxOnFileControlCallback);
        callbackHandler.registerOnFileDataCallback(antoxOnFileDataCallback);

        /* Load user details */
        try {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
            jTox.setName(preferences.getString("nickname", ""));
            jTox.setStatusMessage(preferences.getString("status_message", ""));
            ToxUserStatus newStatus = ToxUserStatus.TOX_USERSTATUS_NONE;
            String newStatusString = preferences.getString("status", "");
            if (newStatusString.equals("2"))
                newStatus = ToxUserStatus.TOX_USERSTATUS_AWAY;
            if (newStatusString.equals("3"))
                newStatus = ToxUserStatus.TOX_USERSTATUS_BUSY;
            jTox.setUserStatus(newStatus);
        } catch (ToxException e) {

        }

        /* Check if connected to the Internet */
        ConnectivityManager connMgr = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        /* If connected to internet, download nodes */
        if (networkInfo != null && networkInfo.isConnected()) {
            try {
                if(DhtNode.ipv4.size() == 0)
                    new DHTNodeDetails(ctx).execute().get(); // Make sure finished getting nodes first
                /* Try and bootstrap to online nodes*/
                while (DhtNode.connected == false && networkInfo.isConnected()) {
                    try {
                        if (DhtNode.ipv4.size() > 0) {
                            try {
                                jTox.bootstrap(DhtNode.ipv4.get(DhtNode.counter),
                                        Integer.parseInt(DhtNode.port.get(DhtNode.counter)), DhtNode.key.get(DhtNode.counter));
                            } catch (ToxException e) {

                            }

                            Log.d(TAG, "Connected to node: " + DhtNode.owner.get(DhtNode.counter));
                            DhtNode.connected = true;
                        }
                    } catch (UnknownHostException e) {
                        DhtNode.counter = DhtNode.counter >= DhtNode.ipv4.size() ? 0 : DhtNode.counter++;
                    }
                }
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
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
