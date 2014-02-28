package im.tox.antox;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

import im.tox.jtoxcore.JTox;
import im.tox.jtoxcore.ToxException;
import im.tox.jtoxcore.ToxUserStatus;
import im.tox.jtoxcore.callbacks.CallbackHandler;

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

    /**
     * List View for displaying all the friends in a scrollable list
     */
    private ListView friendListView;
    /**
     * Adapter for the friendListView
     */
    private FriendsListAdapter adapter;
    /**
     * Receiver for getting work reports from ToxService
     */
    private ResponseReceiver receiver;

    private Intent doToxIntent;

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
            // TODO: Decide on a whole what to do if the user isnt connected to the Internet
        }

        doToxIntent = new Intent(this, ToxService.class);
        doToxIntent.setAction(Constants.DO_TOX);
        this.startService(doToxIntent);

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
        if (settingsPref.getString("saved_status_hint", "") == "online")
            UserDetails.status = ToxUserStatus.TOX_USERSTATUS_NONE;
        else if (settingsPref.getString("saved_status_hint", "") == "away")
            UserDetails.status = ToxUserStatus.TOX_USERSTATUS_AWAY;
        else if (settingsPref.getString("saved_status_hint", "") == "busy")
            UserDetails.status = ToxUserStatus.TOX_USERSTATUS_BUSY;
        else
            UserDetails.status = ToxUserStatus.TOX_USERSTATUS_NONE;

        UserDetails.note = settingsPref.getString("saved_note_hint", "");

        /**
         * Stores a 2 dimensional string array holding friend details. Will be populated
         * by a tox function once implemented
         */
        String[][] friends = {
                // 0 - offline, 1 - online, 2 - away, 3 - busy
                {"1", "astonex", "status"}, {"0", "irungentoo", "status"},
                {"2", "nurupo", "status"}, {"3", "sonOfRa", "status"}
        };

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

        adapter = new FriendsListAdapter(this, R.layout.main_list_item,
                friends_list);

        friendListView = (ListView) findViewById(R.id.mainListView);

        friendListView.setAdapter(adapter);

        final Intent chatIntent = new Intent(this, ChatActivity.class);

        friendListView
                .setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view,
                                            int position, long id) {
                        // .toString() overridden in FriendsList.java to return
                        // the friend name
                        String friendName = parent.getItemAtPosition(position)
                                .toString();
                        chatIntent.putExtra(EXTRA_MESSAGE, friendName);
                        startActivity(chatIntent);
                    }

                });

        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
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
        this.startService(doToxIntent);
    }

    @Override
    public void onStop() {
        super.onStop();
        this.stopService(doToxIntent);
        setTitle("antox");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                openSettings();
                return true;
            case R.id.add_friend:
                addFriend();
                return true;
            case R.id.search_friend:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
                        MainActivity.this.adapter.getFilter().filter(
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

        return true;
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
        public String readIt(InputStream stream, int len) throws IOException,
                UnsupportedEncodingException {
            Reader reader = null;
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
            setTitle("antox - " + intent.getStringExtra(Constants.CONNECTED_STATUS));
        }
    }
}
