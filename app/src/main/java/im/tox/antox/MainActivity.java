package im.tox.antox;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SlidingPaneLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import im.tox.antox.callbacks.AntoxOnFriendRequestCallback;
import im.tox.jtoxcore.ToxUserStatus;

/**
 * The Main Activity which is launched when the app icon is pressed in the app tray and acts as the
 * central part of the entire app. It also displays the friends list to the user.
 *
 * @author Mark Winter (Astonex)
 */

public class MainActivity extends ActionBarActivity implements ContactsFragment.ContactListener {

    /**
     * Extra message to be passed with intents - Should be unique from every other app
     */
    public final static String EXTRA_MESSAGE = "im.tox.antox.MESSAGE";


    /**
     * Receiver for getting work reports from ToxService
     */
    private ResponseReceiver receiver;

    private Intent doToxIntent;
    private Intent startToxIntent;

    public FriendsListAdapter contactsAdapter;
    public FriendRequestsAdapter friendRequestsAdapter;

    private SlidingPaneLayout pane;
    private ChatFragment chat;
    private ContactsFragment contacts;

    /**
     * Stores all friend details and used by the contactsAdapter for displaying
     */
    private String[][] friends;

    /**
     * Stores the friends list returned by ToxService to feed into String[][] friends
     */
    private String[] friendNames;

    // Stores friend requests
    private ArrayList<FriendRequests> friend_requests;

    private String activeContactName;


    /*
     * Allows menu to be accessed from menu unrelated subroutines such as the pane opened
     */
    private Menu menu;
    private boolean isInChat=false;
    private List<String> connectedUsers;


    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* Check if connected to the Internet */
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            // Executes in a separate thread so UI experience isn't affected
            new DownloadDHTList().execute("http://markwinter.me/servers.php");
        } else {

        }

        /* If the tox service isn't already running, start it */
        if(!isToxServiceRunning()) {
            startToxIntent = new Intent(this, ToxService.class);
            startToxIntent.setAction(Constants.START_TOX);
            doToxIntent = new Intent(this, ToxService.class);
            doToxIntent.setAction(Constants.DO_TOX);
            this.startService(startToxIntent);
            ScheduledExecutorService scheduleTaskExecutor = Executors.newScheduledThreadPool(5);
// This schedule a runnable task every 2 minutes
            scheduleTaskExecutor.scheduleAtFixedRate(new Runnable() {
                Context ctx = getApplicationContext();
                public void run() {
                    ctx.startService(doToxIntent);
                }
            }, 0, 50, TimeUnit.MILLISECONDS);
        }

        Intent getFriendsList = new Intent(this, ToxService.class);
        getFriendsList.setAction(Constants.FRIEND_LIST);
        this.startService(getFriendsList);

        /**
         *  Intent filter will listen only for intents with action Constants.Register
         *  @see im.tox.antox.Constants
         */
        IntentFilter mStatusIntentFilter = new IntentFilter(Constants.BROADCAST_ACTION);
        receiver = new ResponseReceiver();
        /**
         * Local Broadcast Manager for listening for work reports from ToxService.
         * Local is used as it's more efficient and to stop other apps reading the messages
         */
        LocalBroadcastManager.getInstance(this).registerReceiver(
                receiver,
                mStatusIntentFilter);

		/* Check if first time ever running by checking the preferences */
        SharedPreferences pref = getSharedPreferences("main",
                Context.MODE_PRIVATE);

        // For testing WelcomeActivity
        // SharedPreferences.Editor editor = pref.edit();
        // editor.putInt("beenLoaded", 0);
        // editor.apply();
        // End testing

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
        pane.setPanelSlideListener(new PaneListener());
        pane.openPane();
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        chat = (ChatFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_chat);
        contacts = (ContactsFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_contacts);

        updateFriends();
        friend_requests = new ArrayList<FriendRequests>();


    }

    private void updateFriends() {
        friends = new String[1][3];
        friends[0][0] = "0";
        friends[0][1] = "You have no friends";
        friends[0][2] = "Why not try adding some?";

        /* Go through status strings and set appropriate resource image */
        FriendsList friends_list[] = new FriendsList[friends.length];

        for (int i = 0; i < friends.length; i++) {
            if (friends[i][0].equals("1"))
                friends_list[i] = new FriendsList(R.drawable.ic_status_online,
                        friends[i][1], friends[i][2]);
            else if (friends[i][0].equals("0"))
                friends_list[i] = new FriendsList(R.drawable.ic_status_offline,
                        friends[i][1], friends[i][2]);
            else if (friends[i][0].equals("2"))
                friends_list[i] = new FriendsList(R.drawable.ic_status_away,
                        friends[i][1], friends[i][2]);
            else if (friends[i][0].equals("3"))
                friends_list[i] = new FriendsList(R.drawable.ic_status_busy,
                        friends[i][1], friends[i][2]);
        }

        contactsAdapter = new FriendsListAdapter(this, R.layout.contact_list_item,
                friends_list);

        contacts.updateFriends();
    }

    private void updateFriendRequests() {
        FriendRequests friend_requests_list[] = new FriendRequests[friend_requests.size()];
        friend_requests_list = friend_requests.toArray(friend_requests_list);
        friendRequestsAdapter = new FriendRequestsAdapter(this, R.layout.friendrequest_list_item,
                friend_requests_list);
        contacts.updateFriendRequests();
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
     * Starts a new intent to open the AddFriendActivity class
     *
     * @see im.tox.antox.AddFriendActivity
     */
    private void addFriend() {
        Intent intent = new Intent(this, AddFriendActivity.class);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        if(!isToxServiceRunning()) {
            this.startService(doToxIntent);
        }
    }

    @Override
    public void onStop() {
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
            case R.id.add_friend:
                if(isInChat)
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
                this.stopService(doToxIntent);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void addFriendToGroup() {
        Log.v("Add friend to group method","To implement");
    }

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
                        MainActivity.this.contactsAdapter.getFilter().filter(
                                newText);
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
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (ToxService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private class DownloadDHTList extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {

            // params comes from the execute() call: params[0] is the url.
            try {
                return downloadUrl(urls[0]);
            } catch (IOException e) {
                return "Unable to retrieve web page. URL may be invalid.";
            }
        }

        /**
         * Downloads the data and will return a string of it
         *
         * @param myurl
         * @return
         * @throws IOException
         */
        private String downloadUrl(String myurl) throws IOException {
            InputStream is = null;
            // Only display the first 500 characters of the retrieved
            // web page content.
            int len = 160;

            try {
                URL url = new URL(myurl);
                HttpURLConnection conn = (HttpURLConnection) url
                        .openConnection();
                conn.setReadTimeout(10000 /* milliseconds */);
                conn.setConnectTimeout(15000 /* milliseconds */);
                conn.setRequestMethod("GET");
                conn.setDoInput(true);
                // Starts the query
                conn.connect();
                is = conn.getInputStream();

                // Convert the InputStream into a string

                return readIt(is, len);

                // Makes sure that the InputStream is closed after the app is
                // finished using it.
            } finally {
                if (is != null) {
                    is.close();
                }
            }
        }

        /**
         * Take the Input Stream and convert it from a char[] buffer to a String
         *
         * @param stream
         * @param len
         * @return
         * @throws IOException
         * @throws UnsupportedEncodingException
         */
        public String readIt(InputStream stream, int len) throws IOException {
            Reader reader;
            reader = new InputStreamReader(stream, "UTF-8");
            char[] buffer = new char[len];
            reader.read(buffer);
            return new String(buffer);
        }

        /**
         * Will take the return of the AsyncTask to be used for operations. Specifically, it will
         * take the result of downloading the JSON file, parse it, and add it to the DHT spinner
         *
         * @param result
         */
        protected void onPostExecute(String result) {
            //Parse the page and store it so it can be used to automatically connect to a DHT
            String[] dhtDetails = new String[7];

            int posOfFirst = 1;
            int counter = 0;
            for(int i = 2; i < result.length() - 2; i++) {
                if(result.charAt(i) == '"') {
                    dhtDetails[counter] = result.substring(posOfFirst+1, i);
                    posOfFirst = i + 2;
                    counter++;
                    i = i+3;
                }
            }

            DhtNode.ipv4 = dhtDetails[0];
            DhtNode.ipv6 = dhtDetails[1];
            DhtNode.port = dhtDetails[2];
            DhtNode.key = dhtDetails[3];
            DhtNode.owner = dhtDetails[4];
            DhtNode.location = dhtDetails[5];
        }
    }

    /**
     * Response receiver for receiving work reports from ToxService to update the UI with results
     */
    private class ResponseReceiver extends BroadcastReceiver {

        /**
         * Uses the info passed in the Intent to update the UI with ToxService reports
         *
         * @param context
         * @param intent
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            //Do something with received broadcasted message
            if(intent.getAction().equals(Constants.FRIEND_LIST)) {

            }

            if(intent.getAction().equals(Constants.FRIEND_REQUEST)) {
                Context ctx = getApplicationContext();
                CharSequence msg = intent.getStringExtra(AntoxOnFriendRequestCallback.FRIEND_MESSAGE);
                CharSequence key = intent.getStringExtra(AntoxOnFriendRequestCallback.FRIEND_KEY);
                int duration = Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(ctx, msg, duration);
                toast.show();
                friend_requests.add(new FriendRequests((String) key, (String) msg));
                updateFriendRequests();
            }

            if(intent.getAction().equals(Constants.CONNECTION_STATUS)) {

            }
        }
    }
    @Override
    public void onBackPressed() {
        if (!pane.isOpen()) {
            pane.openPane();
        } else {
            finish();
        }
    }

    public void onChangeContact(int position, String contact) {
        activeContactName = contact;
        pane.closePane();
        chat.setContact(position, contact);
    }

    public void sendMessage(View v){
        chat.sendMessage(v);
    }

    private class PaneListener implements SlidingPaneLayout.PanelSlideListener {

        @Override
        public void onPanelClosed(View view) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            setTitle(activeContactName);
            MenuItem af = (MenuItem)menu.findItem(R.id.add_friend);
            af.setIcon(R.drawable.ic_action_add_group);
            isInChat=true;
            System.out.println("Panel closed");
        }

        @Override
        public void onPanelOpened(View view) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            setTitle(R.string.app_name);
            MenuItem af = (MenuItem)menu.findItem(R.id.add_friend);
            af.setIcon(R.drawable.ic_action_add_person);
            isInChat=false;
            System.out.println("Panel opened");
        }

        @Override
        public void onPanelSlide(View view, float arg1) {
        }

    }


}
