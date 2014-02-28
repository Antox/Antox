package im.tox.antox;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import im.tox.jtoxcore.ToxUserStatus;

/**
 * Settings Activity where the user can change their username, status, note and DHT nodes.
 * Allows the user to specify their own DHT Node, or to pick one from a downloaded list of known
 * working nodes.
 *
 * @author Mark Winter (Astonex)
 */

public class SettingsActivity extends ActionBarActivity {
    /**
     * Spinner for displaying acceptable statuses (online/away/busy) to the users
     */
    Spinner statusSpinner;
    /**
     * Editable text box where the user can enter their own DHT IP address
     */
    EditText dhtIP;
    /**
     * Editable text box where the user can enter their own DHT Port
     */
    EditText dhtPort;
    /**
     * Editable text box where the user can enter their own DHT Public Key address
     */
    EditText dhtKey;
    /**
     * 2D string array to store DHT node details
     */
    String[][] downloadedDHTNodes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        statusSpinner = (Spinner) findViewById(R.id.settings_spinner_status);
        dhtIP = (EditText) findViewById(R.id.settings_dht_ip);
        dhtPort = (EditText) findViewById(R.id.settings_dht_port);
        dhtKey = (EditText) findViewById(R.id.settings_dht_key);

        /* Add acceptable statuses to the drop down menu */
        String[] statusItems = new String[]{"online", "away", "busy"};
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, statusItems);
        statusSpinner.setAdapter(statusAdapter);

		/* Get saved preferences */
        SharedPreferences pref = getSharedPreferences("settings",
                Context.MODE_PRIVATE);

        /* Sets the user key to the saved user key */
        TextView userKey = (TextView) findViewById(R.id.settings_user_key);
        userKey.setText(pref.getString("user_key", ""));

		/* If the preferences aren't blank, then add them to text fields
         * otherwise it will display the predefined hints in strings.xml
         */

        if (!pref.getString("saved_name_hint", "").equals("")) {
            EditText nameHint = (EditText) findViewById(R.id.settings_name_hint);
            nameHint.setText(pref.getString("saved_name_hint", ""));
        }

        if (!pref.getString("saved_dht_ip", "").equals("")) {
            dhtIP.setText(pref.getString("saved_dht_ip", ""));
            /* If saved DHT settings exist then show the extra dht boxes to begin with */
            RadioButton dhtButton = (RadioButton) findViewById(R.id.settings_radio_dht_self);
            dhtButton.toggle();
            dhtIP.setVisibility(View.VISIBLE);
            dhtPort.setVisibility(View.VISIBLE);
            dhtKey.setVisibility(View.VISIBLE);
        }

        if (!pref.getString("saved_dht_port", "").equals("")) {
            dhtPort.setText(pref.getString("saved_dht_port", ""));
        }

        if (!pref.getString("saved_dht_key","").equals("")) {
            dhtKey.setText(pref.getString("saved_dht_key",""));
        }

        if (!pref.getString("saved_note_hint", "").equals("")) {
            EditText noteHint = (EditText) findViewById(R.id.settings_note_hint);
            noteHint.setText(pref.getString("saved_note_hint", ""));
        }

        if (!pref.getString("saved_status_hint", "").equals("")) {
            String savedStatus = pref.getString("saved_status_hint", "");
            int statusPos = statusAdapter.getPosition(savedStatus);
            statusSpinner.setSelection(statusPos);
        }
    }

    /**
     * This method is called when the user updates their settings. It will check all the text fields
     * to see if they contain default values, and if they don't, save them using SharedPreferences
     *
     * @param view
     */
    public void updateSettings(View view) {
		/* Get all text from the fields */
        TextView userKeyText = (TextView) findViewById(R.id.settings_user_key);
        EditText nameHintText = (EditText) findViewById(R.id.settings_name_hint);
        EditText dhtIpHintText = (EditText) findViewById(R.id.settings_dht_ip);
        EditText dhtKeyHintText = (EditText) findViewById(R.id.settings_dht_key);
        EditText dhtPortHintText = (EditText) findViewById(R.id.settings_dht_port);
        EditText noteHintText = (EditText) findViewById(R.id.settings_note_hint);
        //EditText statusHintText = (EditText) findViewById(R.id.settings_status_hint);

		/* Save settings to file */

        SharedPreferences pref = getSharedPreferences("settings",
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();

		/*
		 * If the fields aren't equal to the default strings in strings.xml then
		 * they contain user entered data so they need saving
		 */
        if (!nameHintText.getText().toString().equals(getString(R.id.settings_name_hint))) {
            editor.putString("saved_name_hint", nameHintText.getText().toString());
            UserDetails.username = nameHintText.getText().toString();
        }
        if (!userKeyText.getText().toString().equals(getString(R.id.settings_user_key)))
            editor.putString("saved_user_key_hint", userKeyText.getText().toString());
        if (!noteHintText.getText().toString().equals(getString(R.id.settings_note_hint))) {
            editor.putString("saved_note_hint", noteHintText.getText().toString());
            UserDetails.note = noteHintText.getText().toString();
        }
        editor.putString("saved_status_hint", statusSpinner.getSelectedItem().toString());
        if (statusSpinner.getSelectedItem().toString() == "online")
            UserDetails.status = ToxUserStatus.TOX_USERSTATUS_NONE;
        if (statusSpinner.getSelectedItem().toString() == "away")
            UserDetails.status = ToxUserStatus.TOX_USERSTATUS_AWAY;
        if (statusSpinner.getSelectedItem().toString() == "busy")
            UserDetails.status = ToxUserStatus.TOX_USERSTATUS_BUSY;

        /* Also save DHT details to DhtNode class */
        if (!dhtIpHintText.getText().toString().equals(getString(R.id.settings_dht_ip))) {
            editor.putString("saved_dht_ip", dhtIpHintText.getText().toString());
            DhtNode.ipv4 = dhtIpHintText.getText().toString();
        }
        if (!dhtKeyHintText.getText().toString().equals(getString(R.id.settings_dht_key))) {
            editor.putString("saved_dht_key", dhtKeyHintText.getText().toString());
            DhtNode.key = dhtKeyHintText.getText().toString();
        }
        if (!dhtPortHintText.getText().toString().equals(getString(R.id.settings_dht_port))) {
            editor.putString("saved_dht_port", dhtPortHintText.getText().toString());
            DhtNode.port = dhtPortHintText.getText().toString();
        }

        editor.commit();

        Context context = getApplicationContext();
        CharSequence text = "Settings updated";
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context, text, duration);
        toast.show();

        finish();
    }
    /**
     * This method is called when the user clicks on the radio button for entering their own DHT
     * settings
     *
     * @param view
     */
    public void onDHTSelfClicked(View view) {
        dhtIP.setVisibility(View.VISIBLE);
        dhtPort.setVisibility(View.VISIBLE);
        dhtKey.setVisibility(View.VISIBLE);
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