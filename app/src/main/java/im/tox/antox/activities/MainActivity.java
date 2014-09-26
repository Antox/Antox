package im.tox.antox.activities;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import java.util.Locale;

import im.tox.antox.R;
import im.tox.antox.data.AntoxDB;
import im.tox.antox.fragments.DialogToxID;
import im.tox.antox.tox.ToxSingleton;
import im.tox.antox.utils.AntoxFriend;
import im.tox.antox.utils.BitmapManager;
import im.tox.antox.utils.Constants;
import im.tox.antox.utils.Triple;
import im.tox.antox.utils.Tuple;
import im.tox.jtoxcore.ToxCallType;
import im.tox.jtoxcore.ToxCodecSettings;
import im.tox.jtoxcore.ToxException;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * @author Mark Winter (Astonex)
 */

public class MainActivity extends ActionBarActivity implements DialogToxID.DialogToxIDListener {

    public DrawerLayout pane;
    public View chat;
    public View request;

    private final ToxSingleton toxSingleton = ToxSingleton.getInstance();

    Subscription activeKeySub;
    Subscription chatActiveSub;
    Subscription doClosePaneSub;

    SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Pressing the volume keys will affect STREAM_MUSIC played from this app
        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        /* Check if a language has been set or not */
        String language = preferences.getString("language", "-1");
        if (language.equals("-1")) {
            SharedPreferences.Editor editor = preferences.edit();
            String currentLanguage = getResources().getConfiguration().locale.getCountry().toLowerCase();
            editor.putString("language", currentLanguage);
            editor.commit();
        } else {
            Locale locale = new Locale(language);
            Locale.setDefault(locale);
            Configuration config = new Configuration();
            config.locale = locale;
            getApplicationContext().getResources().updateConfiguration(config, getApplicationContext().getResources().getDisplayMetrics());
        }

        setContentView(R.layout.activity_main);

        getSupportActionBar().hide();

        /* Fix for an android 4.1.x bug */
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.JELLY_BEAN
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            );
        }

        /* Check if connected to the Internet */
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && !networkInfo.isConnected()) {
                /* Display lack of internet connection warning */
            showAlertDialog(MainActivity.this, getString(R.string.main_no_internet),
                    getString(R.string.main_not_connected));
        }

        chat = (View) findViewById(R.id.fragment_chat);
        request = (View) findViewById(R.id.fragment_friendrequest);
        pane = (DrawerLayout) findViewById(R.id.slidingpane_layout);
        DrawerLayout.DrawerListener paneListener = new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {

            }

            @Override
            public void onDrawerOpened(View drawerView) {
                Log.d("MainActivity", "Drawer listener, drawer open");
                toxSingleton.rightPaneActiveSubject.onNext(true);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                Log.d("MainActivity", "Drawer listener, drawer closed");
                toxSingleton.rightPaneActiveSubject.onNext(false);
            }

            @Override
            public void onDrawerStateChanged(int newState) {

            }
        };
        pane.setDrawerListener(paneListener);

        toxSingleton.mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        //Init Bitmap Manager
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            new BitmapManager();

        //Get epoch time for online/offline messages
        Constants.epoch = System.currentTimeMillis() / 1000; // Current time in seconds

        //Initialize the RxJava Subjects in tox singleton;
        toxSingleton.initSubjects(this);

        //Update lists
        toxSingleton.updateFriendsList(this);
        toxSingleton.updateLastMessageMap(this);
        toxSingleton.updateUnreadCountMap(this);

        AntoxDB db = new AntoxDB(getApplicationContext());
        db.clearFileNumbers();
        db.close();

        updateLeftPane();
    }

    public void updateLeftPane() {
        toxSingleton.updateFriendRequests(getApplicationContext());
        toxSingleton.updateFriendsList(getApplicationContext());
        toxSingleton.updateMessages(getApplicationContext());
    }

    public void onClickAddFriend(View v) {
        Intent intent = new Intent(this, AddFriendActivity.class);
        startActivityForResult(intent, Constants.ADD_FRIEND_REQUEST_CODE);
    }

    public void onClickVoiceCallFriend(View v) {
        ToxCodecSettings toxCodecSettings = new ToxCodecSettings(ToxCallType.TYPE_AUDIO, 0, 0, 0, 64000, 20, 48000, 1);
        AntoxFriend friend = toxSingleton.getAntoxFriend(toxSingleton.activeKey);
        int userID = friend.getFriendnumber();
        try {
            toxSingleton.jTox.avCall(userID, toxCodecSettings, 10);
        } catch (ToxException e) {
        }
    }

    public void onClickVideoCallFriend(View v) {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==Constants.ADD_FRIEND_REQUEST_CODE && resultCode==RESULT_OK){
            toxSingleton.updateFriendsList(this);
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        toxSingleton.activeKey = "";
    }

    @Override
    public void onResume(){
        super.onResume();
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        doClosePaneSub = toxSingleton.doClosePaneSubject.observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean close) {
                        if (close) {
                            pane.openDrawer(Gravity.RIGHT);
                        } else {
                            pane.closeDrawer(Gravity.RIGHT);
                        }
                    }
                });
        activeKeySub = toxSingleton.rightPaneActiveAndKeyAndIsFriendSubject.observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Triple<Boolean, String, Boolean>>() {
                    @Override
                    public void call(Triple<Boolean, String, Boolean> rightPaneActiveAndActiveKeyAndIfFriend) {
                        boolean rightPaneActive = rightPaneActiveAndActiveKeyAndIfFriend.x;
                        String activeKey = rightPaneActiveAndActiveKeyAndIfFriend.y;
                        boolean isFriend = rightPaneActiveAndActiveKeyAndIfFriend.z;
                        Log.d("activeKeySub","oldkey: " + toxSingleton.activeKey + " newkey: " + activeKey + " isfriend: " + isFriend);
                        if (activeKey.equals("")) {
                            chat.setVisibility(View.GONE);
                            request.setVisibility(View.GONE);
                        } else {
                            if (!activeKey.equals(toxSingleton.activeKey)) {
                                toxSingleton.doClosePaneSubject.onNext(true);
                                if (isFriend) {
                                    chat.setVisibility(View.VISIBLE);
                                    request.setVisibility(View.GONE);

                                } else {
                                    chat.setVisibility(View.GONE);
                                    request.setVisibility(View.VISIBLE);
                                }
                            }
                        }
                        toxSingleton.activeKey = activeKey;
                        if (!activeKey.equals("") && rightPaneActive && isFriend) {
                            AntoxDB antoxDB = new AntoxDB(getApplicationContext());
                            antoxDB.markIncomingMessagesRead(activeKey);
                            toxSingleton.clearUselessNotifications(activeKey);
                            toxSingleton.updateMessages(getApplicationContext());
                            antoxDB.close();
                            toxSingleton.chatActive = true;
                        } else {
                            toxSingleton.chatActive = false;
                        }
                    }
                });

    }

    @Override
    public void onPause(){
        super.onPause();
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if(preferences.getBoolean("beenLoaded", false)) {
            activeKeySub.unsubscribe();
            doClosePaneSub.unsubscribe();
            toxSingleton.chatActive = false;
        }
    }

    void showAlertDialog(Context context, String title, String message) {
        AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.setIcon(R.drawable.ic_launcher);
        alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        alertDialog.show();
    }

    @Override
    public void onBackPressed() {
        if(pane.isDrawerOpen(Gravity.RIGHT))
            pane.closeDrawers();
        else
            finish();
    }

    /* Needed for Tox ID dialog in settings fragment */
    @Override
    public void onDialogClick(DialogFragment fragment) {

    }

    /* Method for the Tox ID copy button in settings fragment */
    public void copyToxID(View view) {
        /* Copy ID to clipboard */
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        Context context = getApplicationContext();
        android.text.ClipboardManager clipboard = (android.text.ClipboardManager) context
                .getSystemService(context.CLIPBOARD_SERVICE);
        clipboard.setText(sharedPreferences.getString("tox_id", ""));
    }
}
