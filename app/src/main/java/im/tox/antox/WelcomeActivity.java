package im.tox.antox;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Welcome Activity is displayed when the user is using the app for the very first time to get
 * some required details such as a username.
 *
 * @author Mark Winter (Astonex)
 */

public class WelcomeActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        SharedPreferences.Editor editor;

		/* Save the fact the user has seen the welcome message */
        SharedPreferences pref = getSharedPreferences("main",
                Context.MODE_PRIVATE);
        editor = pref.edit();
        editor.putInt("beenLoaded", 1);
        editor.commit();
    }

    public void updateSettings(View view) {
        TextView username = (TextView) findViewById(R.id.welcome_name_hint);
        String usernameText = username.getText().toString();

        if (usernameText.equals("")) {
            Context context = getApplicationContext();
            CharSequence text = "You must select a username";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        } else {
            SharedPreferences pref = getSharedPreferences("settings",
                    Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();
            editor.putString("saved_name_hint", usernameText);
            editor.commit();
            Context context = getApplicationContext();
            CharSequence text = "Your details have been sent to the NSA";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();

            // Close activity
            finish();
        }
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