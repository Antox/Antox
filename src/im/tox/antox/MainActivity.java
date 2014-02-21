package im.tox.antox;

import im.tox.antox.R;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

public class MainActivity extends Activity {

	public final static String EXTRA_MESSAGE = "com.tox.antox.MESSAGE";
	
	private ListView friendListView;
	
	@SuppressLint("NewApi")
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
        
        //If beenLoaded is 0, then never been run
        int beenLoaded = pref.getInt("beenLoaded", 0);
        if (beenLoaded == 0)
        {
        	//Launch welcome activity which will run the user through initial settings
        	//and give a brief description of antox
        	Intent intent = new Intent(this, WelcomeActivity.class);
        	startActivity(intent);
        }

       
        
        /* Set up friends list using a ListView */
        
        //Will be populated by a Tox core method
        String[][] friends = { 
        		//0 - offline, 1 - online, 2 - away, 3 - busy 
        		{"1", "astonex", "status"}, 
        		{"0", "irungentoo", "status"}, 
        		{"2", "nurupo", "status"}, 
        		{"3", "sonOfRa", "status"} 
        };
        
        FriendsList friends_list[] = new FriendsList[friends.length];
        
        for(int i = 0; i < friends.length; i++)
        {
        	if(friends[i][0] == "1")
        		friends_list[i] = new FriendsList(R.drawable.ic_status_online, friends[i][1], friends[i][2]);
        	else if(friends[i][0] == "0")
        		friends_list[i] = new FriendsList(R.drawable.ic_status_offline, friends[i][1], friends[i][2]);
        	else if(friends[i][0] == "2")
        		friends_list[i] = new FriendsList(R.drawable.ic_status_away, friends[i][1], friends[i][2]);
        	else if(friends[i][0] == "3")
        		friends_list[i] = new FriendsList(R.drawable.ic_status_busy, friends[i][1], friends[i][2]);
        }

        
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
        
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
		{
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
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
