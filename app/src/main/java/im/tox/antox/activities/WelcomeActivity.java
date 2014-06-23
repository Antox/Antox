package im.tox.antox.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import im.tox.antox.R;
import im.tox.antox.tox.ToxDoService;
import im.tox.antox.tox.ToxSingleton;
import im.tox.antox.utils.Constants;

public class WelcomeActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        getSupportActionBar().hide();

        /* Fix for an android 4.1.x bug */
        if(Build.VERSION.SDK_INT != Build.VERSION_CODES.JELLY_BEAN
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            );
        }
    }

    public void updateSettings(View view) {

        TextView username = (TextView) findViewById(R.id.welcome_name_hint);
        String usernameText = username.getText().toString();

        if (usernameText.trim().equals("")) {

            Context context = getApplicationContext();
            CharSequence text = getString(R.string.welcome_must_select_username);
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();

        } else {

            /* Update preferences */
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("nickname", usernameText);
            editor.putBoolean("beenLoaded", true); // Save the fact the user has seen this activity
            editor.commit();

            // Set result for activity
            setResult(RESULT_OK);

            /* Next thing to be called is MainAcitivity.onResume() so we need to do essential
             * initialisations here to stop client crashing when using it for the first time
             * Feels really ugly however so please change it
             */
            ToxSingleton toxSingleton = ToxSingleton.getInstance();
            toxSingleton.initSubjects(getApplicationContext());
            /* If the tox service isn't already running, start it */
            if (!toxSingleton.isRunning) {
                /* Start without checking for internet connection in case of LAN usage */
                Intent startToxIntent = new Intent(getApplicationContext(), ToxDoService.class);
                startToxIntent.setAction(Constants.START_TOX);
                getApplicationContext().startService(startToxIntent);
            }

            // Close activity
            finish();

        }
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }
}