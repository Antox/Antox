package com.tox.antox;

import android.annotation.SuppressLint;
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

public class SettingsActivity extends Activity {

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
		{
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
		
		/* Get saved preferences */
        SharedPreferences pref = getSharedPreferences("settings", Context.MODE_PRIVATE);
        
        /* If the preferences aren't blank/default, then add them to text fields */
        /* If they are blank/default, the text from strings.xml will be displayed instead */
        if(pref.getString("saved_user_key_hint", "") != "")
        {
        	TextView userKey = (TextView) findViewById(R.id.settings_user_key);
        	userKey.setText(pref.getString("saved_user_key_hint", ""));
        }
        
        if(pref.getString("saved_name_hint", "") != "")
        {
        	EditText nameHint = (EditText) findViewById(R.id.settings_name_hint);
        	nameHint.setText(pref.getString("saved_name_hint", ""));
        }
        
        if(pref.getString("saved_dht_ip", "192.254.75.98") != "192.254.75.98")
        {
        	EditText dhtIpHint = (EditText) findViewById(R.id.settings_dht_ip);
        	dhtIpHint.setText(pref.getString("saved_dht_ip", "192.254.75.98"));
        }
        
        if(pref.getString("saved_dht_port", "33445") != "33445")
        {
        	EditText dhtPortHint = (EditText) findViewById(R.id.settings_dht_port);
        	dhtPortHint.setText(pref.getString("saved_dht_port", "33445"));
        }
        
        if(pref.getString("saved_dht_key", "FE3914F4616E227F29B2103450D6B55A836AD4BD23F97144E2C4ABE8D504FE1B")
        		!= "FE3914F4616E227F29B2103450D6B55A836AD4BD23F97144E2C4ABE8D504FE1B")
        {
        	EditText dhtKeyHint = (EditText) findViewById(R.id.settings_dht_key);
        	dhtKeyHint.setText(pref.getString(
        			"saved_dht_key", 
        			"FE3914F4616E227F29B2103450D6B55A836AD4BD23F97144E2C4ABE8D504FE1B"
        			)
        			);
        }
        
        if(pref.getString("saved_note_hint", "") != "")
        {
        	EditText noteHint = (EditText) findViewById(R.id.settings_note_hint);
        	noteHint.setText(pref.getString("saved_note_hint", ""));
        }
        
        if(pref.getString("saved_status_hint", "") != "")
        {
        	EditText statusHint = (EditText) findViewById(R.id.settings_status_hint);
        	statusHint.setText(pref.getString("saved_status_hint", ""));
        }
        
		
	}

	public void updateSettings(View view)
	{
		/* Get all text from the fields */
		TextView userKeyText = (TextView) findViewById(R.id.settings_user_key);
		EditText nameHintText = (EditText) findViewById(R.id.settings_name_hint);
		EditText dhtIpHintText = (EditText) findViewById(R.id.settings_dht_ip);
		EditText dhtKeyHintText = (EditText) findViewById(R.id.settings_dht_key);
		EditText dhtPortHintText = (EditText) findViewById(R.id.settings_dht_port);
		EditText noteHintText = (EditText) findViewById(R.id.settings_note_hint);
		EditText statusHintText = (EditText) findViewById(R.id.settings_status_hint);
		
		
		/* Save settings to file */
		
		SharedPreferences pref = getSharedPreferences("settings", Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = pref.edit();
		
		/* If the fields aren't equal to the default strings in strings.xml then they contain user entered data 
		 * so they need saving
		 */
		if(nameHintText.getText().toString() != getString(R.id.settings_name_hint))
			editor.putString("saved_name_hint", nameHintText.getText().toString());
		if(userKeyText.getText().toString() != getString(R.id.settings_user_key))
			editor.putString("saved_user_key_hint", userKeyText.getText().toString());
		if(dhtIpHintText.getText().toString() != getString(R.id.settings_dht_ip))
			editor.putString("saved_dht_ip", dhtIpHintText.getText().toString());
		if(dhtKeyHintText.getText().toString() != getString(R.id.settings_dht_key))
			editor.putString("saved_dht_key", dhtKeyHintText.getText().toString());
		if(dhtPortHintText.getText().toString() != getString(R.id.settings_dht_port))
			editor.putString("saved_dht_port", dhtPortHintText.getText().toString());
		if(noteHintText.getText().toString() != getString(R.id.settings_note_hint))
			editor.putString("saved_note_hint", noteHintText.getText().toString());
		if(statusHintText.getText().toString() != getString(R.id.settings_status_hint))
			editor.putString("saved_status_hint", statusHintText.getText().toString());
		
		editor.apply();
		
		Context context = getApplicationContext();
		CharSequence text = "Settings updated";
		int duration = Toast.LENGTH_SHORT;
		Toast toast = Toast.makeText(context, text, duration);
		toast.show();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.settings, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
