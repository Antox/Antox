package im.tox.antox;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SlidingPaneLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;

import im.tox.jtoxcore.ToxUserStatus;

/**
 * The Main Activity which is launched when the app icon is pressed in the app tray and acts as the
 * central part of the entire app. It also displays the friends list to the user.
 *
 * @author Mark Winter (Astonex)
 */

public class MainActivity extends ActionBarActivity {

    /**
     * Extra message to be passed with intents - Should be unique from every other app
     */
    public final static String EXTRA_MESSAGE = "im.tox.antox.MESSAGE";
    private static final String TAG = "im.tox.antox.MainActivity";


    private Intent startToxIntent;

    public LeftPaneAdapter leftPaneAdapter;


    public SlidingPaneLayout pane;
    public ChatFragment chat;
    private ContactsFragment contacts;
    private IntentFilter filter;
    private boolean tempRightPaneActive;

    /**
     * Stores all friend details and used by the contactsAdapter for displaying
     */
    public String activeTitle = "Antox";


    public ArrayList<String> leftPaneKeyList;

    ToxSingleton toxSingleton = ToxSingleton.getInstance();

    public ArrayList<Friend> friendList;
    private PaneListener paneListener;

    /*
     * Allows menu to be accessed from menu unrelated subroutines such as the pane opened
     */
    private Menu menu;

    private BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "broadcast received");
            String action = intent.getStringExtra("action");
            if (action != null) {
            Log.d(TAG, "action: " + action);
                if (action == Constants.FRIEND_REQUEST) {

                } else if (action == Constants.UPDATE_LEFT_PANE) {
                    updateLeftPane();
                } else if (action == Constants.REJECT_FRIEND_REQUEST) {
                    updateLeftPane();
                    Context ctx = getApplicationContext();
                    String text = "Friend request deleted";
                    int duration = Toast.LENGTH_SHORT;
                    Toast toast = Toast.makeText(ctx, text, duration);
                    toast.show();
                } else if (action == Constants.UPDATE_MESSAGES) {
                    Log.d(TAG, "UPDATE_MESSAGES, intent key = " + intent.getStringExtra("key") + ", activeFriendKey = " + toxSingleton.activeFriendKey);
                    updateLeftPane();
                    if (intent.getStringExtra("key").equals(toxSingleton.activeFriendKey)) {
                        updateChat(toxSingleton.activeFriendKey);
                    }
                } else if (action == Constants.ACCEPT_FRIEND_REQUEST) {
                    updateLeftPane();
                    Context ctx = getApplicationContext();
                    String text = "Friend request accepted";
                    int duration = Toast.LENGTH_SHORT;
                    Toast toast = Toast.makeText(ctx, text, duration);
                    toast.show();
                } else if (action == Constants.FRIEND_LIST) {

                } else if (action == Constants.UPDATE) {
                    updateLeftPane();
                }
            }
        }
    };

    
    void updateChat(String key) {
        Log.d(TAG, "updating chat");
        //avoid changing name of pending request to "(null) !" if they are currently the active friend
        if(toxSingleton.friendsList.getById(key)!=null
                && toxSingleton.friendsList.getById(key).getName()!=null ){
            AntoxDB db = new AntoxDB(this);
            if (toxSingleton.rightPaneActive) {
                db.markIncomingMessagesRead(key);
            }
            chat.updateChat(db.getMessageList(key));
            db.close();
            updateLeftPane();
        }
    };

    @Override
    protected void onNewIntent(Intent i) {
        if (i.getAction() == Constants.SWITCH_TO_FRIEND && toxSingleton.friendsList.getById(i.getStringExtra("key")) != null) {
            String key = i.getStringExtra("key");
            String name = i.getStringExtra("name");
            Fragment newFragment = new ChatFragment();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.right_pane, newFragment);
            transaction.addToBackStack(null);
            transaction.commit();
            toxSingleton.activeFriendKey = key;
            toxSingleton.activeFriendRequestKey = null;
            activeTitle = name;
            pane.closePane();
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            setTitle(activeTitle);
            tempRightPaneActive = true;
            toxSingleton.rightPaneActive = true;
            clearUselessNotifications();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        toxSingleton.activeFriendKey=null;
        toxSingleton.activeFriendRequestKey=null;
        setContentView(R.layout.activity_main);

        toxSingleton.leftPaneActive = true;

        /* Check if connected to the Internet */
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected())
        {
            // Executes in a separate thread so UI experience isn't affected
           // Downloads the DHT node details
            if(DhtNode.ipv4.size() == 0)
                new DHTNodeDetails().execute();
        }
        else {
            showAlertDialog(MainActivity.this, "No Internet Connection",
                    "You are not connected to the Internet");
        }

        /* If the tox service isn't already running, start it */
        if(!isToxServiceRunning()) {
            /* If the service wasn't running then we wouldn't have gotten callbacks for a user
            *  going offline so default everyone to offline and just wait for callbacks.
            */
            AntoxDB db = new AntoxDB(getApplicationContext());
            db.setAllOffline();
            db.close();

            startToxIntent = new Intent(this, ToxService.class);
            startToxIntent.setAction(Constants.START_TOX);
            this.startService(startToxIntent);

        }

        Intent getFriendsList = new Intent(this, ToxService.class);
        getFriendsList.setAction(Constants.FRIEND_LIST);
        this.startService(getFriendsList);

		/* Check if first time ever running by checking the preferences */
        SharedPreferences pref = getSharedPreferences("main",
                Context.MODE_PRIVATE);

        // If beenLoaded is 0, then never been run
        if (pref.getInt("beenLoaded", 0) == 0) {
            // Launch welcome activity which will run the user through initial
            // settings
            // and give a brief description of antox
            Intent intent = new Intent(this, WelcomeActivity.class);
            startActivity(intent);
        }

        /* Load user details */
        SharedPreferences settingsPref = getSharedPreferences("settings", Context.MODE_PRIVATE);
        UserDetails.username = settingsPref.getString("saved_name_hint", "");
        if (settingsPref.getString("saved_status_hint", "").equals("online"))
            UserDetails.status = ToxUserStatus.TOX_USERSTATUS_NONE;
        else if (settingsPref.getString("saved_status_hint", "").equals("away"))
            UserDetails.status = ToxUserStatus.TOX_USERSTATUS_AWAY;
        else if (settingsPref.getString("saved_status_hint", "").equals("busy"))
            UserDetails.status = ToxUserStatus.TOX_USERSTATUS_BUSY;
        else
            UserDetails.status = ToxUserStatus.TOX_USERSTATUS_NONE;

        UserDetails.note = settingsPref.getString("saved_note_hint", "");

        pane = (SlidingPaneLayout) findViewById(R.id.slidingpane_layout);
        paneListener = new PaneListener();
        pane.setPanelSlideListener(paneListener);
        pane.openPane();
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        contacts = (ContactsFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_contacts);

        updateLeftPane();
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
    private Message mostRecentMessage(String key, ArrayList<Message> messages) {
        for (int i=0; i<messages.size(); i++) {
            if (key.equals(messages.get(i).key)) {
                return messages.get(i);
            }
        }
        return new Message(-1, key, "", false, true, true, new Timestamp(0,0,0,0,0,0,0));
    }

    private int countUnreadMessages(String key, ArrayList<Message> messages) {
        int counter = 0;
        Message m;
        for (int i=0; i<messages.size(); i++) {
            m = messages.get(i);
            if (m.key.equals(key) && !m.is_outgoing) {
                if (!m.has_been_read) {
                    counter += 1;
                } else {
                    return counter;
                }
            }
        }
        return counter;
    }
    public void updateLeftPane() {

        AntoxDB antoxDB = new AntoxDB(this);

        friendList = antoxDB.getFriendList();
        ArrayList<Message> messageList = antoxDB.getMessageList("");

        Friend friends_list[] = new Friend[friendList.size()];
        friends_list = friendList.toArray(friends_list);

        FriendRequest friend_requests_list[] = new FriendRequest[toxSingleton.friend_requests.size()];
        friend_requests_list = toxSingleton.friend_requests.toArray(friend_requests_list);

        leftPaneAdapter = new LeftPaneAdapter(this);

        leftPaneKeyList = new ArrayList<String>();

        Message msg;

        if (friend_requests_list.length > 0) {
            LeftPaneItem friend_request_header = new LeftPaneItem(Constants.TYPE_HEADER, getResources().getString(R.string.main_friend_requests), null, 0);
            leftPaneAdapter.addItem(friend_request_header);
            leftPaneKeyList.add("");
            for (int i = 0; i < friend_requests_list.length; i++) {
                LeftPaneItem friend_request = new LeftPaneItem(Constants.TYPE_FRIEND_REQUEST, friend_requests_list[i].requestKey, friend_requests_list[i].requestMessage, 0);
                leftPaneAdapter.addItem(friend_request);
                leftPaneKeyList.add(friend_requests_list[i].requestKey);
            }
        }
        if (friends_list.length > 0) {
            LeftPaneItem friends_header = new LeftPaneItem(Constants.TYPE_HEADER, getResources().getString(R.string.main_friends), null, 0);
            leftPaneAdapter.addItem(friends_header);
            leftPaneKeyList.add("");
            for (int i = 0; i < friends_list.length; i++) {
                msg = mostRecentMessage(friends_list[i].friendKey, messageList);
                LeftPaneItem friend = new LeftPaneItem(Constants.TYPE_CONTACT, friends_list[i].friendName, msg.message, friends_list[i].icon, countUnreadMessages(friends_list[i].friendKey, messageList), msg.timestamp);
                leftPaneAdapter.addItem(friend);
                leftPaneKeyList.add(friends_list[i].friendKey);
            }
        }

        antoxDB.close();
        contacts.updateLeftPane();
    }

    /**
     * Starts a new intent to open the SettingsActivity class
     *
     * @see im.tox.antox.SettingsActivity
     */
    private void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }
    /**
     * Starts a new intent to open the SettingsActivity class
     *
     * @see im.tox.antox.ProfileActivity
     */
    private void openProfile() {
        Intent intent = new Intent(this, ProfileActivity.class);
        startActivity(intent);
    }
    /**
     * Starts a new intent to open the AboutActivity class
     *
     * @see im.tox.antox.AboutActivity
     */
    private void openAbout() {
        Intent intent = new Intent(this, AboutActivity.class);
        startActivity(intent);
    }
    /**
     * Starts a new intent to open the AddFriendActivity class
     *
     * @see im.tox.antox.AddFriendActivity
     */
    private void addFriend() {
        Intent intent = new Intent(this, AddFriendActivity.class);
        startActivityForResult(intent, Constants.ADD_FRIEND_REQUEST_CODE);
    }

    private void clearUselessNotifications () {
        if (toxSingleton.rightPaneActive && toxSingleton.activeFriendKey != null
                && toxSingleton.friendsList.all().size() > 0) {
            AntoxFriend friend = toxSingleton.friendsList.getById(toxSingleton.activeFriendKey);
            toxSingleton.mNotificationManager.cancel(friend.getFriendnumber());
        }
    }

    @Override
    public void onResume() {
        Log.i(TAG, "onResume");
        toxSingleton.rightPaneActive = tempRightPaneActive;
        filter = new IntentFilter(Constants.BROADCAST_ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);
        clearUselessNotifications();
        super.onResume();
    }

    @Override
    public void onPause() {
        Log.i(TAG, "onPause");
        tempRightPaneActive = toxSingleton.rightPaneActive;
        toxSingleton.rightPaneActive = false;
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        toxSingleton.leftPaneActive = false;
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.i(TAG, "onStop");
        super.onStop();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                openSettings();
                return true;
            case R.id.action_profile:
                openProfile();
                return true;
            case R.id.action_about:
                openAbout();
                return true;
            case R.id.add_friend:
                if(toxSingleton.rightPaneActive)
                    addFriendToGroup();
                else
                    addFriend();
                return true;
            case R.id.search_friend:
                return true;
            case android.R.id.home:
                pane.openPane();
                return true;
            case R.id.action_exit:
                Intent stopToxIntent = new Intent(this, ToxService.class);
                stopToxIntent.setAction(Constants.STOP_TOX);
                this.startService(stopToxIntent);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void addFriendToGroup() {
        Log.v("Add friend to group method","To implement");
    }


    @SuppressLint("NewApi")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        final MenuItem menuItem = menu.findItem(R.id.search_friend);
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(menuItem);
        searchView
                .setOnQueryTextListener(new SearchView.OnQueryTextListener() {

                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        // do nothing
                        return false;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        //MainActivity.this.contactsAdapter.getFilter().filter(
                        //        newText);
                        return true;
                    }
                });
        searchView
                .setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {

                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        MenuItemCompat.collapseActionView(menuItem);

                    }
                });
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

    public void showAlertDialog(Context context, String title, String message) {
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


    // Downloads the the first working DHT node
    private class DHTNodeDetails extends AsyncTask<Void, Void, Void> {
       String nodeDetails[] = new String[7];



        @Override
        protected Void doInBackground(Void... params) {
            try {
                // Connect to the web site
                Document document = Jsoup.connect("http://wiki.tox.im/Nodes").get();
                Elements nodeRows = document.getElementsByTag("tr");

                for(Element nodeRow : nodeRows)
                {
                    Elements nodeElements = nodeRow.getElementsByTag("td");
                    int c = 0;
                    for(Element nodeElement : nodeElements)
                         nodeDetails[c++]=nodeElement.text();


                    if(nodeDetails[6]!=null && nodeDetails[6].equals("WORK"))
                    {
                        DhtNode.ipv4.add(nodeDetails[0]);
                        DhtNode.ipv6.add(nodeDetails[1]);
                        DhtNode.port.add(nodeDetails[2]);
                        DhtNode.key.add(nodeDetails[3]);
                        DhtNode.owner.add(nodeDetails[4]);
                        DhtNode.location.add(nodeDetails[5]);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result)
        {
            try {
                //Checking the details
                System.out.println("node details:");
                System.out.println(DhtNode.ipv4);
                System.out.println(DhtNode.ipv6);
                System.out.println(DhtNode.port);
                System.out.println(DhtNode.key);
                System.out.println(DhtNode.owner);
                System.out.println(DhtNode.location);
            }catch (NullPointerException e){
                Toast.makeText(MainActivity.this,"Error Downloading Nodes List",Toast.LENGTH_SHORT).show();
            }
            /**
             * There is a chance that downloading finishes later than the bootstrapping call in the
             * ToxService, because both are in separate threads. In that case to make sure the nodes
             * are bootstrapped we restart the ToxService
             */
            if(!DhtNode.connected)
            {
                Intent restart = new Intent(getApplicationContext(), ToxService.class);
                restart.setAction(Constants.START_TOX);
                getApplicationContext().startService(restart);
            }
        }
    }

    public void friendRequest(Intent intent) {
        Context ctx = getApplicationContext();
        CharSequence msg = intent.getStringExtra("message");
        CharSequence key = intent.getStringExtra("key");
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(ctx, "Friend request received", duration);
        toast.show();
        Log.d(TAG, toxSingleton.friend_requests.toString());
        updateLeftPane();
    }

    @Override
    public void onBackPressed() {
        if (!pane.isOpen()) {
            pane.openPane();
        } else {
            finish();
        }
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==Constants.ADD_FRIEND_REQUEST_CODE && resultCode==RESULT_OK){
            updateLeftPane();
        }
    }
    private class PaneListener implements SlidingPaneLayout.PanelSlideListener {

        @Override
        public void onPanelClosed(View view) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            setTitle(activeTitle);
            MenuItem af = menu.findItem(R.id.add_friend);
            af.setIcon(R.drawable.ic_action_add_group);
            af.setTitle(R.string.add_to_group);
            toxSingleton.rightPaneActive = true;
            System.out.println("Panel closed");
            toxSingleton.leftPaneActive = false;
            clearUselessNotifications();
        }

        @Override
        public void onPanelOpened(View view) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            setTitle(R.string.app_name);
            MenuItem af = menu.findItem(R.id.add_friend);
            af.setIcon(R.drawable.ic_action_add_person);
            af.setTitle(R.string.add_friend);
            toxSingleton.rightPaneActive =false;
            InputMethodManager imm = (InputMethodManager)getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            /* This is causing a null pointer exception */
            //imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            System.out.println("Panel opened");
        }

        @Override
        public void onPanelSlide(View view, float arg1) {
        }

    }


}
