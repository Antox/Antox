package im.tox.antox;

import android.content.Context;
import android.content.Intent;
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
 * Profile Activity where the user can change their username, status, and note.

 * @author Mark Winter (Astonex) & David Lohle (Proplex)
 */

public class ProfileActivity extends ActionBarActivity {
    /**
     * Spinner for displaying acceptable statuses (online/away/busy) to the users
     */
    private Spinner statusSpinner;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        statusSpinner = (Spinner) findViewById(R.id.settings_spinner_status);

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
    }

    /**
     * This method is called when the user updates their settings. It will check all the text fields
     * to see if they contain default values, and if they don't, save them using SharedPreferences
     *
     * @param view
     */
    public void updateSettings(View view) {
        /**
         * String array to store updated details to be passed by intent to ToxService
         */
        String[] updatedSettings = { null, null, null};

		/* Get all text from the fields */
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
            updatedSettings[0] = nameHintText.getText().toString();
        }

        if (!noteHintText.getText().toString().equals(getString(R.id.settings_note_hint))) {
            editor.putString("saved_note_hint", noteHintText.getText().toString());
            UserDetails.note = noteHintText.getText().toString();
            updatedSettings[2] = noteHintText.getText().toString();
        }
        editor.putString("saved_status_hint", statusSpinner.getSelectedItem().toString());
        if (statusSpinner.getSelectedItem().toString().equals("online"))
            UserDetails.status = ToxUserStatus.TOX_USERSTATUS_NONE;
        if (statusSpinner.getSelectedItem().toString().equals("away"))
            UserDetails.status = ToxUserStatus.TOX_USERSTATUS_AWAY;
        if (statusSpinner.getSelectedItem().toString().equals("busy"))
            UserDetails.status = ToxUserStatus.TOX_USERSTATUS_BUSY;

        updatedSettings[1] = statusSpinner.getSelectedItem().toString();

        editor.commit();

        /* Send an intent to ToxService notifying change of settings */
        Intent updateSettings = new Intent(this, ToxService.class);
        updateSettings.setAction(Constants.UPDATE_SETTINGS);
        updateSettings.putExtra("newSettings", updatedSettings);
        this.startService(updateSettings);

        Context context = getApplicationContext();
        CharSequence text = "Settings updated";
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context, text, duration);
        toast.show();

        finish();
    }


}