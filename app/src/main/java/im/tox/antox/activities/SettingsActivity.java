package im.tox.antox.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.Locale;

import im.tox.antox.R;
import im.tox.antox.fragments.DHTDialogFragment;
import im.tox.antox.utils.DhtNode;

/**
 * Settings Activity DHT nodes.
 * Allows the user to specify their own DHT Node, or to pick one from a downloaded list of known
 * working nodes.
 */

public class SettingsActivity extends ActionBarActivity
        implements DHTDialogFragment.DHTDialogListener {
    /**
     * Spinner for displaying acceptable statuses (online/away/busy) to the users
     */
    private Spinner statusSpinner;
    /**
     * Checkbox that inflates a DHTDialog where the user can enter their own DHT settings
     */
    private CheckBox dhtBox;
    /**
     * String that store's the user's DHT IP address entry
     */
    private String dhtIP;
    /**
     * String that store's the user's DHT Port entry
     */
    private String dhtPort;
    /**
     * String that store's the user's DHT Public Key address entry
     */
    private String dhtKey;
    /**
     * 2D string array to store DHT node details
     */
    String[][] downloadedDHTNodes;
    /**
     * Spinner for displaying available languages
     */
    private Spinner languageSpinner;

    /**
     * Spinner for displaying themes
     */
    private Spinner themeSpinner;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Fix for an android 4.1.x bug */
        if(Build.VERSION.SDK_INT != Build.VERSION_CODES.JELLY_BEAN
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            );
        }

        setContentView(R.layout.activity_settings);
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            getSupportActionBar().setIcon(R.drawable.ic_actionbar);
        }

//        statusSpinner = (Spinner) findViewById(R.id.settings_spinner_status);
        dhtBox = (CheckBox) findViewById(R.id.settings_dht_box);

        SharedPreferences pref = getSharedPreferences("settings",
                Context.MODE_PRIVATE);

		/* If the preferences aren't blank, then add them to text fields
         * otherwise it will display the predefined hints in strings.xml
         */

        if (!pref.getString("saved_dht_ip", "").equals("")) {
            dhtIP = pref.getString("saved_dht_ip", "");
        }

        if (!pref.getString("saved_dht_port", "").equals("")) {
            dhtPort = pref.getString("saved_dht_port", "");
        }

        if (!pref.getString("saved_dht_key", "").equals("")) {
            dhtKey = pref.getString("saved_dht_key", "");
        }

        // Set dhtBox as checked if it is set
        dhtBox.setChecked(pref.getBoolean("saved_custom_dht", false));
        if(pref.getBoolean("saved_custom_dht", false)) {
            dhtBox.setChecked(true);
        }

        themeSpinner = (Spinner) findViewById(R.id.spinner_theme_options);
        String theme = pref.getString("theme", "");
        switch (theme) {
            case "Light":
                themeSpinner.setSelection(0);
                break;
            case "Dark":
                themeSpinner.setSelection(1);
                break;
            default:
                themeSpinner.setSelection(0);
        }

        languageSpinner = (Spinner) findViewById(R.id.spinner_language_settings);
        String lang = pref.getString("language", "");
        switch (lang) {
            case "English":
                languageSpinner.setSelection(0);
                break;
            case "Deutsch":
                languageSpinner.setSelection(1);
                break;
            case "Español":
                languageSpinner.setSelection(2);
                break;
            case "Français":
                languageSpinner.setSelection(3);
                break;
            case "Italiano":
                languageSpinner.setSelection(4);
                break;
            case "Nederlands":
                languageSpinner.setSelection(5);
                break;
            case "Polski":
                languageSpinner.setSelection(6);
                break;
            case "Svenska":
                languageSpinner.setSelection(7);
                break;
            case "Türkçe":
                languageSpinner.setSelection(8);
                break;
            case "Русский":
                languageSpinner.setSelection(9);
                break;
            case "Український":
                languageSpinner.setSelection(10);
                break;
            default:
                break;
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

		/* Save settings to file */

        SharedPreferences pref = getSharedPreferences("settings",
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();

        /* Also save DHT details to DhtNode class */
        editor.putBoolean("saved_custom_dht", dhtBox.isChecked());

        if (dhtBox.isChecked() && !dhtIP.equals(getString(R.id.settings_dht_ip))) {
            editor.putString("saved_dht_ip", dhtIP);
            DhtNode.ipv4.add(dhtIP);
        }
        if (dhtBox.isChecked() && !dhtKey.equals(getString(R.id.settings_dht_key))) {
            editor.putString("saved_dht_key", dhtKey);
            DhtNode.key.add(dhtKey);
        }
        if (dhtBox.isChecked() && !dhtPort.equals(getString(R.id.settings_dht_port))) {
            editor.putString("saved_dht_port", dhtPort);
            DhtNode.port.add(dhtPort);
        }

        // Save the theme
        String theme = themeSpinner.getSelectedItem().toString();
        if(!pref.getString("theme", "").equals(theme)) {
            editor.putString("theme", theme);
        }

        //save the language
        String language = languageSpinner.getSelectedItem().toString();
        if(!pref.getString("language", "").equals(language)) {
            setResult(RESULT_OK);
            editor.putString("language", language);
            //set the language
            Locale locale = null;
            switch (language) {
                case "English":
                    locale = new Locale("en");
                    break;
                case "Deutsch":
                    locale = new Locale("de");
                    break;
                case "Español":
                    locale = new Locale("es");
                    break;
                case "Français":
                    locale = new Locale("fr");
                    break;
                case "Italiano":
                    locale = new Locale("it");
                    break;
                case "Nederlands":
                    locale = new Locale("nl");
                    break;
                case "Polski":
                    locale = new Locale("pl");
                    break;
                case "Svenska":
                    locale = new Locale("sv");
                    break;
                case "Türkçe":
                    locale = new Locale("tr");
                    break;
                case "Русский":
                    locale = new Locale("ru");
                    break;
                case "Український":
                    locale = new Locale("uk");
                    break;
                default:
                    break;
            }
            Locale.setDefault(locale);
            Configuration config = new Configuration();
            config.locale = locale;
            getApplicationContext().getResources().updateConfiguration(config, getApplicationContext().getResources().getDisplayMetrics());
        }
        else {
            //something different from RESULT_OK
            setResult(RESULT_OK - 1);
        }


        editor.commit();

        Context context = getApplicationContext();
        CharSequence text = getString(R.string.settings_updated);
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
            DialogFragment dialog = new DHTDialogFragment();
            Bundle bundle = new Bundle();
            bundle.putString(dhtIP, dhtIP);
            bundle.putString(dhtPort, dhtPort);
            bundle.putString(dhtKey, dhtKey);
            dialog.setArguments(bundle);

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

        if(dhtIP.toString().equals("") || dhtPort.toString().equals("") || dhtKey.toString().equals("")) {
            Context context = getApplicationContext();
            CharSequence text = getString(R.string.settings_empty_strings);
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
            dhtBox.setChecked(false);
        }
        if(!validateDHTKey(dhtKey)) {
            Context context = getApplicationContext();
            CharSequence text = getString(R.string.settings_invalid_key);
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
            dhtBox.setChecked(false);
        }
    }

    private boolean validateKey(String friendKey) {
        if (friendKey.length() != 76 || friendKey.matches("[[:xdigit:]]")) {
            return false;
        }
        int x = 0;
        try {
            for (int i = 0; i < friendKey.length(); i += 4) {
                x = x ^ Integer.valueOf(friendKey.substring(i, i + 4), 16);
            }
        }
        catch (NumberFormatException e) {
            return false;
        }
        return x == 0;
    }

    private boolean validateDHTKey(String DHTKey) {
        if (DHTKey.length() != 64 || DHTKey.matches("[[:xdigit:]]")) {
            return false;
        } else {
            return true;
        }
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