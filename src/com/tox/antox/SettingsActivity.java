package com.tox.antox;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
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
        SharedPreferences pref = getPreferences(Context.MODE_PRIVATE);
        
        String userKeyText = pref.getString("saved_user_key_hint", "");
        String nameHintText = pref.getString("saved_name_hint", "");
        String dhtIpHintText = pref.getString("saved_dht_ip", "192.254.75.98");
        String dhtPortHintText = pref.getString("saved_dht_port", "33445");
        String dhtKeyHintText = pref.getString("saved_dht_key", "33445");
        String noteHintText = pref.getString("saved_note_hint", "");
        String statusHintText = pref.getString("saved_status_hint", "");

        if(userKeyText != "")
        {
        	TextView userKey = (TextView) findViewById(R.id.settings_user_key);
        	userKey.setText(userKeyText);
        }
        
        if(nameHintText != "")
        {
        	EditText nameHint = (EditText) findViewById(R.id.settings_name_hint);
        	nameHint.setText(nameHintText);
        }
        
        if(dhtIpHintText != "")
        {
        	EditText dhtIpHint = (EditText) findViewById(R.id.settings_dht_ip);
        	dhtIpHint.setText(dhtIpHintText);
        }
        
        if(dhtPortHintText != "")
        {
        	EditText dhtPortHint = (EditText) findViewById(R.id.settings_dht_port);
        	dhtPortHint.setText(dhtPortHintText);
        }
        
        if(dhtKeyHintText != "")
        {
        	EditText dhtKeyHint = (EditText) findViewById(R.id.settings_dht_key);
        	dhtKeyHint.setText(dhtKeyHintText);
        }
        
        if(noteHintText != "")
        {
        	EditText noteHint = (EditText) findViewById(R.id.settings_note_hint);
        	noteHint.setText(noteHintText);
        }
        
        if(statusHintText != "")
        {
        	EditText statusHint = (EditText) findViewById(R.id.settings_status_hint);
        	statusHint.setText(statusHintText);
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
		
		SharedPreferences pref = getPreferences(Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = pref.edit();
		
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
