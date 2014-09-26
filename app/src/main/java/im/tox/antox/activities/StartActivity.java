package im.tox.antox.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.view.WindowManager;

import im.tox.antox.R;
import im.tox.antox.tox.ToxDoService;

/**
 * Created by david on 9/26/14.
 */
public class StartActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        
        getSupportActionBar().hide();

        /* Fix for an android 4.1.x bug */
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.JELLY_BEAN
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            );
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (preferences.getBoolean("loggedin", false)) {
            /* Attempt to start service in case it's not running */
            Intent startTox = new Intent(getApplicationContext(), ToxDoService.class);
            getApplicationContext().startService(startTox);

            /* Launch main activity */
            Intent main = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(main);

            finish();
        } else {
            Intent setup = new Intent(getApplicationContext(), CreateAcccountActivity.class);
            startActivity(setup);

            finish();
        }
    }

}