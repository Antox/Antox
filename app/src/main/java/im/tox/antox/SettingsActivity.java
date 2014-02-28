package im.tox.antox;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
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

public class SettingsActivity extends ActionBarActivity
        implements DHTDialogFragment.DHTDialogListener {
    /**
     * Spinner for displaying acceptable statuses (online/away/busy) to the users
     */
    Spinner statusSpinner;
    /**
     * Checkbox that inflates a DHTDialog where the user can enter their own DHT settings
     */
    CheckBox dhtBox;
    /**
     * String that store's the user's DHT IP address entry
     */
    String dhtIP;
    /**
     * String that store's the user's DHT Port entry
     */
    String dhtPort;
    /**
     * String that store's the user's DHT Public Key address entry
     */
    String dhtKey;
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
        dhtBox = (CheckBox) findViewById(R.id.settings_dht_box);

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

        if (!pref.getString("saved_note_hint", "").equals("")) {
            EditText noteHint = (EditText) findViewById(R.id.settings_note_hint);
            noteHint.setText(pref.getString("saved_note_hint", ""));
        }

        if (!pref.getString("saved_status_hint", "").equals("")) {
            String savedStatus = pref.getString("saved_status_hint", "");
            int statusPos = statusAdapter.getPosition(savedStatus);
            statusSpinner.setSelection(statusPos);
        }

        if (!pref.getString("saved_dht_ip", "").equals("")) {
            dhtIP = pref.getString("saved_dht_ip", "");
        }

        if (!pref.getString("saved_dht_port", "").equals("")) {
            dhtPort = pref.getString("saved_dht_port", "");
        }

        if (!pref.getString("saved_dht_key","").equals("")) {
            dhtKey = pref.getString("saved_dht_key","");
        }

        // Set dhtBox as checked if it is set
        dhtBox.setChecked(pref.getBoolean("saved_custom_dht", false));
        if(pref.getBoolean("saved_custom_dht", false)) {
            dhtBox.setChecked(true);
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
        editor.putBoolean("saved_custom_dht", dhtBox.isChecked());
        if (dhtBox.isChecked() && !dhtIP.equals(getString(R.id.settings_dht_ip))) {
            editor.putString("saved_dht_ip", dhtIP);
            DhtNode.ipv4 = dhtIP;
        }
        if (dhtBox.isChecked() && !dhtKey.toString().equals(getString(R.id.settings_dht_key))) {
            editor.putString("saved_dht_key", dhtKey);
            DhtNode.key = dhtKey;
        }
        if (dhtBox.isChecked() && !dhtPort.toString().equals(getString(R.id.settings_dht_port))) {
            editor.putString("saved_dht_port", dhtPort);
            DhtNode.port = dhtPort;
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
     * This method is called when the user clicks on the check button for entering their own DHT
     * settings
     *
     * @param view
     */
    public void onDhtBoxClicked(View view) {
        //If the user is checking the box, create a dialog prompting the user for the information
        boolean checked = ((CheckBox) view).isChecked();
        if(checked) {
            // Create an instance of the dialog fragment and show it
            DialogFragment dialog = new DHTDialogFragment(dhtIP, dhtPort, dhtKey);
            dialog.show(getSupportFragmentManager(), "NoticeDialogFragment");
        }
    }

    //Called when the DHT settings dialog is confirmed
    @Override
    public void onDialogPositiveClick(DialogFragment dialog,
                                      String dhtIP_, String dhtPort_, String dhtKey_) {
        dhtIP = dhtIP_;
        dhtPort = dhtPort_;
        dhtKey = dhtKey_;
    }

    //Called when the DHT settings dialog is canceled
    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        dhtBox.setChecked(false);
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