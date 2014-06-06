package im.tox.antox.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.SlidingPaneLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Locale;

import im.tox.QR.Contents;
import im.tox.QR.QRCodeEncode;
import im.tox.antox.R;
import im.tox.antox.adapters.LeftPaneAdapter;
import im.tox.antox.data.AntoxDB;
import im.tox.antox.fragments.ChatFragment;
import im.tox.antox.fragments.DialogToxID;
import im.tox.antox.fragments.FriendRequestFragment;
import im.tox.antox.fragments.PinDialogFragment;
import im.tox.antox.tox.ToxDoService;
import im.tox.antox.tox.ToxSingleton;
import im.tox.antox.utils.Constants;
import im.tox.antox.utils.DHTNodeDetails;
import im.tox.antox.utils.DhtNode;
import im.tox.antox.utils.Friend;
import im.tox.antox.utils.Message;
import im.tox.antox.utils.Tuple;
import im.tox.antox.utils.UserDetails;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

/**
 * The Main Activity which is launched when the app icon is pressed in the app tray and acts as the
 * central part of the entire app. It also displays the friends list to the user.
 *
 * @author Mark Winter (Astonex)
 */

public class MainActivity extends ActionBarActivity implements DialogToxID.DialogToxIDListener {

    private static final String TAG = "im.tox.antox.activities.MainActivity";


    public LeftPaneAdapter leftPaneAdapter;


    public SlidingPaneLayout pane;
    public ChatFragment chat;
    private boolean tempRightPaneActive;

    /**
     * Stores all friend details and used by the contactsAdapter for displaying
     */
    public String activeTitle = "Antox";

    private final ToxSingleton toxSingleton = ToxSingleton.getInstance();

    Subscription activeKeySub;
    Subscription chatActiveSub;

    /*
     * Allows menu to be accessed from menu unrelated subroutines such as the pane opened
     */
    private Menu menu;

    @Override
    protected void onNewIntent(Intent i) {
        if (i.getAction() != null) {
            if (i.getAction().equals(Constants.SWITCH_TO_FRIEND) && toxSingleton.getAntoxFriend(i.getStringExtra("key")) != null) {
                String key = i.getStringExtra("key");
                Fragment newFragment = new ChatFragment();
                tempRightPaneActive = true;
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.right_pane, newFragment);
                transaction.addToBackStack(null);
                transaction.commit();
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                setTitle(activeTitle);
                clearUselessNotifications();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* Fix for an android 4.1.x bug */
        if(Build.VERSION.SDK_INT != Build.VERSION_CODES.JELLY_BEAN
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            );
        }

        /* Check if first time ever running by checking the preferences */
        SharedPreferences pref = getSharedPreferences("main",
                Context.MODE_PRIVATE);

        // If beenLoaded is 0, then never been run
        if (pref.getInt("beenLoaded", 0) == 0) {
            // Launch welcome activity which will run the user through initial
            // settings
            // and give a brief description of antox
            Intent intent = new Intent(this, WelcomeActivity.class);
            startActivityForResult(intent, Constants.WELCOME_ACTIVITY_REQUEST_CODE);
        }

        // Checks to see if a language is set in settings
        SharedPreferences settingsPref = PreferenceManager.getDefaultSharedPreferences(this);
        String language = settingsPref.getString("language", "-1");
        // If it has not, set it based on phone locale
        if (language.equals("-1")){
            SharedPreferences.Editor editor = settingsPref.edit();
            editor.putString("language", getCurrentLanguageOnStart());
            editor.commit();
        }
        // Otherwise, check which language has been selected and set it based on that
        else{
            Locale locale = null;
            switch (language) {
                case "English":
                    locale = new Locale("en");
                    break;
                case "Deutsch":
                    locale = new Locale("de");
                    break;
                case "Español":
                    locale = new Locale("es");
                    break;
                case "Français":
                    locale = new Locale("fr");
                    break;
                case "Italiano":
                    locale = new Locale("it");
                    break;
                case "Nederlands":
                    locale = new Locale("nl");
                    break;
                case "Polski":
                    locale = new Locale("pl");
                    break;
                case "Svenska":
                    locale = new Locale("sv");
                    break;
                case "Türkçe":
                    locale = new Locale("tr");
                    break;
                case "Русский":
                    locale = new Locale("ru");
                    break;
                case "Український":
                    locale = new Locale("uk");
                    break;
                case "Português":
                    locale = new Locale("pt");
                    break;
                default:
                    break;
            }
            Locale.setDefault(locale);
            Configuration config = new Configuration();
            config.locale = locale;
            getApplicationContext().getResources().updateConfiguration(config, getApplicationContext().getResources().getDisplayMetrics());
        }

        /* Check if connected to the Internet */
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected())
        {
            // Executes in a separate thread so UI experience isn't affected
            // Downloads the DHT node details
            if(DhtNode.ipv4.size() == 0)
                new DHTNodeDetails(getApplicationContext()).execute();
        }
        else {
            showAlertDialog(MainActivity.this, getString(R.string.main_no_internet),
                    getString(R.string.main_not_connected));
        }

        /* If the tox service isn't already running, start it */
        if(!isToxServiceRunning()) {
            /* If the service wasn't running then we wouldn't have gotten callbacks for a user
            *  going offline so default everyone to offline and just wait for callbacks.
            */
            AntoxDB db = new AntoxDB(getApplicationContext());
            db.setAllOffline();
            db.close();

            Intent startToxIntent = new Intent(this, ToxDoService.class);
            startToxIntent.setAction(Constants.START_TOX);
            this.startService(startToxIntent);

        }

        UserDetails.note = settingsPref.getString("saved_note_hint", "");

        pane = (SlidingPaneLayout) findViewById(R.id.slidingpane_layout);
        PaneListener paneListener = new PaneListener();
        pane.setPanelSlideListener(paneListener);
        pane.openPane();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            getSupportActionBar().setIcon(R.drawable.ic_actionbar);
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().hide();

        //Initialize the RxJava Subjects in tox singleton;
        toxSingleton.initSubjects(this);

        //Grab the friends list from the database and send it to all listeners of the friends list Subject
        toxSingleton.updateFriendsList(this);
        //Do the same for the last message map and unread count map subjects
        toxSingleton.updateLastMessageMap(this);
        toxSingleton.updateUnreadCountMap(this);

        onNewIntent(getIntent());
    }

    @Override
    protected void onStart() {
        Log.i(TAG, "onStart");
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
    }

    private String getCurrentLanguageOnStart() {
        String currentLanguage = getResources().getConfiguration().locale.getCountry().toLowerCase();
        String language;
        switch (currentLanguage) {
            case "en":
                language = "English";
                break;
            case "de":
                language = "Deutsch";
                break;
            case "es":
                language = "Español";
                break;
            case "fr":
                language = "Français";
                break;
            case "it":
                language = "Italiano";
                break;
            case "nl":
                language = "Nederlands";
                break;
            case "pl":
                language = "Polski";
                break;
            case "sv":
                language = "Svenska";
                break;
            case "tr":
                language = "Türkçe";
                break;
            case "pt":
                language = "Português";
                break;
            default:
                language = "English";
        }
        return language;
    }

    public void updateLeftPane() {
        toxSingleton.updateFriendsList(getApplicationContext());
        toxSingleton.updateMessages(getApplicationContext());
    }

    /**
     * Starts a new intent to open the AddFriendActivity class
     *
     * @see im.tox.antox.activities.AddFriendActivity
     */
    private void addFriend() {
        Intent intent = new Intent(this, AddFriendActivity.class);
        startActivityForResult(intent, Constants.ADD_FRIEND_REQUEST_CODE);
    }

    public void onClickAddFriend(View v) {
        Intent intent = new Intent(this, AddFriendActivity.class);
        startActivityForResult(intent, Constants.ADD_FRIEND_REQUEST_CODE);
    }
    private void clearUselessNotifications () {
        AntoxDB db = new AntoxDB(getApplicationContext());
        //todo: clear notifications if rightpane active
        db.close();
    }

    @Override
    public void onResume(){
        super.onResume();
        Log.d("MainActivity","onResume");
        chatActiveSub = toxSingleton.chatActiveAndKey.observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Tuple<String,Boolean>>() {
                    @Override
                    public void call(Tuple<String,Boolean> t) {
                        String activeKey = t.x;
                        boolean chatActive = t.y;
                        if (chatActive) {
                            AntoxDB db = new AntoxDB(getApplicationContext());
                            db.markIncomingMessagesRead(activeKey);
                            db.close();
                            toxSingleton.updateMessages(getApplicationContext());
                            toxSingleton.updateFriendsList(getApplicationContext());
                        }

                    }
                });
        activeKeySub = toxSingleton.activeKeyAndIsFriendSubject.distinctUntilChanged().observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Tuple<String,Boolean>>() {
                    @Override
                    public void call(Tuple<String,Boolean> activeKeyAndIfFriend) {
                        String activeKey = activeKeyAndIfFriend.x;
                        boolean isFriend = activeKeyAndIfFriend.y;
                        if (activeKey.equals("")) {
                            pane.openPane();
                            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.right_pane);
                            if (fragment != null) {
                                getSupportFragmentManager().beginTransaction().remove(fragment).commit();
                            }
                        } else {
                            if (isFriend) {
                                Log.d("MainActivity","chat fragment creation, isFriend: " + isFriend);
                                ChatFragment newFragment = new ChatFragment(activeKey);
                                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                                transaction.replace(R.id.right_pane, newFragment);
                                transaction.addToBackStack(null);
                                transaction.commit();
                            } else {
                                Log.d("MainActivity","friend request fragment creation, isFriend: " + isFriend);
                                FriendRequestFragment newFragment = new FriendRequestFragment(activeKey);
                                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                                transaction.replace(R.id.right_pane, newFragment);
                                transaction.addToBackStack(null);
                                transaction.commit();
                            }
                            pane.closePane();
                        }
                    }
                });
    }

    @Override
    public void onPause(){
        super.onPause();
        Log.d("MainActivity", "onPause");
        activeKeySub.unsubscribe();
        chatActiveSub.unsubscribe();
    }

    @Override
    public void onStop() {
        Log.i(TAG, "onStop");
        super.onStop();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add_friend:
                addFriend();
                return true;
            case android.R.id.home:
                pane.openPane();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        //the class menu property is now the initialized menu
        this.menu=menu;

        return true;
    }


    /**
     * Method to see if the tox service is already running so it isn't restarted
     */
    private boolean isToxServiceRunning() {
        return toxSingleton.toxStarted;
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
        if (!pane.isOpen()) {
            pane.openPane();
        } else {
            finish();
        }
    }

    private void restartActivity() {
        Intent intent = getIntent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        finish();
        startActivity(intent);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==Constants.ADD_FRIEND_REQUEST_CODE && resultCode==RESULT_OK){
            updateLeftPane();
        } else if(requestCode==Constants.SENDFILE_PICKEDFRIEND_CODE && resultCode==RESULT_OK) {
            Uri uri=  data.getData();
            File pickedFile = new File(uri.getPath());
            Log.d("file picked",""+pickedFile.getAbsolutePath() );
            Log.d("file type",""+getContentResolver().getType(uri));
        } else if(requestCode==Constants.UPDATE_SETTINGS_REQUEST_CODE && resultCode==RESULT_OK) {
            restartActivity();
        } else if(requestCode==Constants.WELCOME_ACTIVITY_REQUEST_CODE && resultCode==RESULT_CANCELED) {
            finish();
        }
    }

    private class PaneListener implements SlidingPaneLayout.PanelSlideListener {

        @Override
        public void onPanelClosed(View view) {
            toxSingleton.rightPaneOpen.onNext(true);

            clearUselessNotifications();
        }

        @Override
        public void onPanelOpened(View view) {
            toxSingleton.rightPaneOpen.onNext(false);

            supportInvalidateOptionsMenu();
        }

        @Override
        public void onPanelSlide(View view, float arg1) {
        }

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
