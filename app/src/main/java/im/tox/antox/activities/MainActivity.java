package im.tox.antox.activities;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import java.util.Locale;

import im.tox.antox.R;
import im.tox.antox.data.AntoxDB;
import im.tox.antox.fragments.ChatFragment;
import im.tox.antox.fragments.DialogToxID;
import im.tox.antox.fragments.FriendRequestFragment;
import im.tox.antox.tox.ToxSingleton;
import im.tox.antox.utils.BitmapManager;
import im.tox.antox.utils.Constants;
import im.tox.antox.utils.Tuple;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * @author Mark Winter (Astonex)
 */

public class MainActivity extends ActionBarActivity implements DialogToxID.DialogToxIDListener {

    public DrawerLayout pane;
    public ChatFragment chat;

    private final ToxSingleton toxSingleton = ToxSingleton.getInstance();

    Subscription activeKeySub;
    Subscription chatActiveSub;
    Subscription doClosePaneSub;

    SharedPreferences preferences;

    @Override
    protected void onNewIntent(Intent i) {
        if (i.getAction() != null) {
            if (i.getAction().equals(Constants.SWITCH_TO_FRIEND) && toxSingleton.getAntoxFriend(i.getStringExtra("key")) != null) {
                toxSingleton.changeActiveKey(i.getStringExtra("key"));
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        /* Check if connected to the Internet */
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && !networkInfo.isConnected()) {
                /* Display lack of internt connection warning */
            showAlertDialog(MainActivity.this, getString(R.string.main_no_internet),
                    getString(R.string.main_not_connected));
        }

        pane = (DrawerLayout) findViewById(R.id.slidingpane_layout);
        DrawerLayout.DrawerListener paneListener = new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {

            }

            @Override
            public void onDrawerOpened(View drawerView) {

            }

            @Override
            public void onDrawerClosed(View drawerView) {

            }

            @Override
            public void onDrawerStateChanged(int newState) {

            }
        };
        pane.setDrawerListener(paneListener);

        toxSingleton.mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        //Init Bitmap Manager
        BitmapManager bm = new BitmapManager();

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

        onNewIntent(getIntent());
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
            chatActiveSub = toxSingleton.chatActiveSubject.subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                    .subscribe(new Action1<String>() {
                        @Override
                        public void call(String activeKey) {
                            if (!activeKey.equals("")) {
                                toxSingleton.chatActive = true;
                                AntoxDB antoxDB = new AntoxDB(getApplicationContext());
                                antoxDB.markIncomingMessagesRead(activeKey);
                                toxSingleton.clearUselessNotifications(activeKey);
                                toxSingleton.updateMessages(getApplicationContext());
                                antoxDB.close();
                            } else {
                                toxSingleton.chatActive = false;
                            }
                        }
                    });
            activeKeySub = toxSingleton.activeKeyAndIsFriendSubject.distinctUntilChanged().observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<Tuple<String, Boolean>>() {
                        @Override
                        public void call(Tuple<String, Boolean> activeKeyAndIfFriend) {
                            String activeKey = activeKeyAndIfFriend.x;
                            boolean isFriend = activeKeyAndIfFriend.y;
                            Log.d("activeKeySub","oldkey: " + toxSingleton.activeKey + " newkey: " + activeKey + " isfriend: " + isFriend);
                            if (activeKey.equals("")) {
                                Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.right_pane);
                                if (fragment != null) {
                                    getSupportFragmentManager().beginTransaction().remove(fragment).commit();
                                }
                            } else {
                                if (!activeKey.equals(toxSingleton.activeKey)) {
                                    if (isFriend) {
                                        Log.d("MainActivity", "chat fragment creation, isFriend: " + isFriend);
                                        ChatFragment newFragment = new ChatFragment(activeKey);
                                        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                                        transaction.replace(R.id.right_pane, newFragment);
                                        transaction.addToBackStack(null);
                                        transaction.commit();
                                    } else {
                                        Log.d("MainActivity", "friend request fragment creation, isFriend: " + isFriend);
                                        FriendRequestFragment newFragment = new FriendRequestFragment(activeKey);
                                        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                                        transaction.replace(R.id.right_pane, newFragment);
                                        transaction.addToBackStack(null);
                                        transaction.commit();
                                    }
                                    toxSingleton.doClosePaneSubject.onNext(true);
                                }
                            }
                            toxSingleton.activeKey = activeKey;
                        }
                    });
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

    }

    @Override
    public void onPause(){
        super.onPause();
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if(preferences.getBoolean("beenLoaded", false)) {
            activeKeySub.unsubscribe();
            chatActiveSub.unsubscribe();
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
