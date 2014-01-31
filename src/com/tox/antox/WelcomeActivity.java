package com.tox.antox;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class WelcomeActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_welcome);
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
		{
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
		
		SharedPreferences.Editor editor;
		
		/* Save the fact the user has seen the welcome message */
		SharedPreferences pref = getSharedPreferences("main", Context.MODE_PRIVATE);
		editor = pref.edit();
		editor.putInt("beenLoaded", 1);
		editor.apply();
		
		/* Load default values for DHT */
		EditText dhtIP = (EditText) findViewById(R.id.welcome_dht_ip);
		dhtIP.setText("192.254.75.98");
		
		EditText dhtPort = (EditText) findViewById(R.id.welcome_dht_port);
		dhtPort.setText("33445");
		
		EditText dhtKey = (EditText) findViewById(R.id.welcome_dht_key);
		dhtKey.setText("FE3914F4616E227F29B2103450D6B55A836AD4BD23F97144E2C4ABE8D504FE1B");
	}

	public void updateSettings(View view)
	{
		SharedPreferences pref = getSharedPreferences("settings", Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = pref.edit();
		
		TextView username = (TextView) findViewById(R.id.welcome_name_hint);
		String usernameText = username.getText().toString();
		
		EditText dhtIP = (EditText) findViewById(R.id.welcome_dht_ip);
		String dhtIPText = dhtIP.getText().toString();
		EditText dhtPort = (EditText) findViewById(R.id.welcome_dht_port);
		String dhtPortText = dhtPort.getText().toString();
		EditText dhtKey = (EditText) findViewById(R.id.welcome_dht_key);
		String dhtKeyText = dhtKey.getText().toString();

		editor.putString("saved_name_hint", usernameText);
		editor.putString("saved_dht_ip", dhtIPText);
		editor.putString("saved_dht_port", dhtPortText);
		editor.putString("saved_dht_key", dhtKeyText);
		editor.apply();
		
		Context context = getApplicationContext();
		CharSequence text = "Your details have been sent to the NSA";
		int duration = Toast.LENGTH_SHORT;
		Toast toast = Toast.makeText(context, text, duration);
		toast.show();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.welcome, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
