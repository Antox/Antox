package com.tox.antox;

import java.util.ArrayList;
import java.util.Arrays;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;


public class MainActivity extends Activity {

	public final static String EXTRA_MESSAGE = "com.tox.antox.MESSAGE";
	
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
        
        final ListView listView = (ListView) findViewById(R.id.mainListView);
        /* Placeholder values until tox binding is done */
        String[] friends = new String[] { 
        		"astonex", 
        		"irungentoo", 
        		"sonOfRa", 
        		"stqism",  
        		"nurupo",
        		"jfreegman",
        		"fullName"
        		};
        
        //Sort strings for better optimisation of searchFriend()
        Arrays.sort(friends);
        
        ArrayList<String> valuesList = new ArrayList<String>();
        valuesList.addAll(Arrays.asList(friends));
        
        
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, 
        		 R.layout.main_list_item, friends);
        
        listView.setAdapter(adapter);
        
        final Intent chatIntent = new Intent(this, ChatActivity.class);
        
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {        	
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) 
			{
				CharSequence friendName = (String) listView.getItemAtPosition(position);
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
    	/* Binary search friends */
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
