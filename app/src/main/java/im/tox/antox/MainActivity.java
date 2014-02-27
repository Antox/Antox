package im.tox.antox;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

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
     * Stores the users public key - will be used by JTox
     */
    private String ourPubKey;
    /**
     * Receiver for getting work reports from ToxService
     */
    private ResponseReceiver receiver;

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /**
         *  Intent filter will listen only for intents with action Constants.Register
         *  @see im.tox.antox.Constants
         */
        IntentFilter mStatusIntentFilter = new IntentFilter(Constants.REGISTER);
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
        intent.putExtra(EXTRA_MESSAGE, ourPubKey);
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
        }
    }
}
