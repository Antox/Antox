package com.tox.antox;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;


public class MainActivity extends Activity {

	public final static String EXTRA_MESSAGE = "com.tox.antox.MESSAGE";
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        /* Check if first time ever running by checking the preferences */
        SharedPreferences pref = getSharedPreferences("main", Context.MODE_PRIVATE);
        
        //For testing WelcomeActivity
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt("beenLoaded", 0);
        editor.apply();
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
        
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
    }
    
    public void openSearch()
    {
    	Intent intent = new Intent(this, SettingsActivity.class);
    	startActivity(intent);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
    	switch(item.getItemId())
    	{
    	case R.id.action_settings:
    		openSearch();
    		return true;
    	case R.id.add_friend:
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
