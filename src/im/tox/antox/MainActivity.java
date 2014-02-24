package im.tox.antox;

import java.net.UnknownHostException;
import java.util.List;
import im.tox.jtoxcore.FriendList;
import im.tox.jtoxcore.JTox;
import im.tox.jtoxcore.ToxException;
import im.tox.jtoxcore.ToxUserStatus;
import im.tox.jtoxcore.ToxWorker;
import im.tox.jtoxcore.callbacks.CallbackHandler;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;

public class MainActivity extends Activity {

	public final static String EXTRA_MESSAGE = "im.tox.antox.MESSAGE";

	private ListView friendListView;
	private FriendsListAdapter adapter;

	JTox jt;
	String ourPubKey;
	
	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		AntoxFriendList jFriendList = new AntoxFriendList();
		CallbackHandler jHandler = new CallbackHandler(jFriendList);
		try {
			jt = new JTox(jFriendList, jHandler);
			ToxWorker toxWorker = new ToxWorker(jt);
		} catch (ToxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		/* Get public key - Should really be in WelcomeActivity but only testing it */
		try {
			ourPubKey = jt.getAddress();
		} catch (ToxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			jt.bootstrap("192.254.75.98", 
					33445, 
					"FE3914F4616E227F29B2103450D6B55A836AD4BD23F97144E2C4ABE8D504FE1B");
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ToxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		/* Check if first time ever running by checking the preferences */
		SharedPreferences pref = getSharedPreferences("main",
				Context.MODE_PRIVATE);

		// For testing WelcomeActivity
		// SharedPreferences.Editor editor = pref.edit();
		// editor.putInt("beenLoaded", 0);
		// editor.apply();
		// End testing

		// If beenLoaded is 0, then never been run
		int beenLoaded = pref.getInt("beenLoaded", 0);
		if (beenLoaded == 0) {
			// Launch welcome activity which will run the user through initial
			// settings
			// and give a brief description of antox
			Intent intent = new Intent(this, WelcomeActivity.class);
			startActivity(intent);
		}

		/* Set up friends list using a ListView */

		String[][] friends = {
				// 0 - offline, 1 - online, 2 - away, 3 - busy
				{ "1", "astonex", "status" }, { "0", "irungentoo", "status" },
				{ "2", "nurupo", "status" }, { "3", "sonOfRa", "status" } };
		
		List<AntoxFriend> allFriends = jFriendList.all();
		if (!allFriends.isEmpty()) {
			for (int i = 0; i < allFriends.size(); i++) {
			    AntoxFriend antoxFriend = allFriends.get(i);
				
			    if(antoxFriend.isOnline())
			    	friends[i][0] = "1";
			    else
			    	friends[i][0] = "0";
			    
			    if(antoxFriend.getStatus() == ToxUserStatus.TOX_USERSTATUS_AWAY)
			    	friends[i][0] = "2";
			    
			    else if(antoxFriend.getStatus() == ToxUserStatus.TOX_USERSTATUS_BUSY)
			    	friends[i][0] = "3";
			    
				friends[i][1] = antoxFriend.getNickname();
				friends[i][2] = antoxFriend.getStatusMessage();
			}
		}
		
		FriendsList friends_list[] = new FriendsList[friends.length];

		for (int i = 0; i < friends.length; i++) {
			if (friends[i][0] == "1")
				friends_list[i] = new FriendsList(R.drawable.ic_status_online,
						friends[i][1], friends[i][2]);
			else if (friends[i][0] == "0")
				friends_list[i] = new FriendsList(R.drawable.ic_status_offline,
						friends[i][1], friends[i][2]);
			else if (friends[i][0] == "2")
				friends_list[i] = new FriendsList(R.drawable.ic_status_away,
						friends[i][1], friends[i][2]);
			else if (friends[i][0] == "3")
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

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}

	public void openSettings() {
		Intent intent = new Intent(this, SettingsActivity.class);
		intent.putExtra(EXTRA_MESSAGE, ourPubKey);
		startActivity(intent);
	}

	public void addFriend() {
		//Intent intent = new Intent(this, AddFriendActivity.class);
		//startActivity(intent);
		try {
			if(jt.isConnected()) {
				setTitle("antox - connected");
			} else {
				setTitle("antox - disconnected");
			}
		} catch (ToxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void searchFriend() {

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
			searchFriend();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@SuppressLint("NewApi")
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			final MenuItem menuItem = menu.findItem(R.id.search_friend);
			final SearchView searchView = (SearchView) menuItem.getActionView();
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
							menuItem.collapseActionView();
						}
					});
		}

		return true;
	}
	
	/* This Async task is not necessarily the way tox should be implemented */
	private class DoTox extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			while(true) {
				try {
					jt.doTox();
				} catch (ToxException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
	}
}
