package com.tox.antox;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

public class MainActivity extends Activity {

	public final static String EXTRA_MESSAGE = "com.tox.antox.MESSAGE";
	
	private ListView friendListView;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        /* Check if first time ever running by checking the preferences */
        SharedPreferences pref = getSharedPreferences("main", Context.MODE_PRIVATE);
        
        //For testing WelcomeActivity
        //SharedPreferences.Editor editor = pref.edit();
        //editor.putInt("beenLoaded", 0);
        //editor.apply();
        //End testing
        
        //If beenLoaded is blank, then never been run
        int beenLoaded = pref.getInt("beenLoaded", 0);
        if (beenLoaded == 0)
        {
        	//Launch welcome activity which will run the user through initial settings
        	//and give a brief description of antox
        	Intent intent = new Intent(this, WelcomeActivity.class);
        	startActivity(intent);
        }

        /* Set up friends list using a ListView */
        
        FriendsList friends_list[] = new FriendsList[]
        {
        		new FriendsList(
        				R.drawable.ic_status_online, 
        				"astonex", 
        				"my first status. lol how do i android"
        				),
        				
        		new FriendsList(
        				R.drawable.ic_status_offline, 
        				"irungentoo", 
        				"everyone should install gentoo"
        				),
        				
        		new FriendsList(
        				R.drawable.ic_status_away, 
        				"sonOfRa", 
        				"wut is JNI pls halp"
        				),
        				
        		new FriendsList(
        				R.drawable.ic_status_busy, 
        				"nurupo", 
        				"how do I into QT GUI"
        				)
        };
        
        FriendsListAdapter adapter = new FriendsListAdapter(this,
        		R.layout.main_list_item, friends_list);

        friendListView = (ListView) findViewById(R.id.mainListView);
        
        friendListView.setAdapter(adapter);

        final Intent chatIntent = new Intent(this, ChatActivity.class);
 
        friendListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {        	
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) 
			{				
				//.toString() overridden in FriendsList.java to return the friend name
				String friendName = parent.getItemAtPosition(position).toString();
				chatIntent.putExtra(EXTRA_MESSAGE, friendName);
				startActivity(chatIntent);
			}
        	
        });
        
        getActionBar().setHomeButtonEnabled(true);
    }
    
    public void openSettings()
    {
    	Intent intent = new Intent(this, SettingsActivity.class);
    	startActivity(intent);
    }
    
    public void addFriend()
    {
    	Intent intent = new Intent(this, AddFriendActivity.class);
    	startActivity(intent);
    }
    
    public void searchFriend()
    {
    	
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
    	switch(item.getItemId())
    	{
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
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
}
