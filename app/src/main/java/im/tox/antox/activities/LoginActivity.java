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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;

import im.tox.antox.R;
import im.tox.antox.data.UserDB;
import im.tox.antox.tox.ToxDoService;

public class LoginActivity extends ActionBarActivity implements AdapterView.OnItemSelectedListener {

    private String profileSelected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        getSupportActionBar().hide();

        /* Fix for an android 4.1.x bug */
        if(Build.VERSION.SDK_INT != Build.VERSION_CODES.JELLY_BEAN
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            );
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        UserDB db = new UserDB(this);

        // Check to see if any users exist. If not, start the create account activity instead
        if(!db.doUsersExist()) {
            db.close();
            Intent createAccount = new Intent(getApplicationContext(), CreateAcccountActivity.class);
            startActivity(createAccount);
            finish();

        } else if(preferences.getBoolean("loggedin", false)) {
            db.close();
            /* Attempt to start service in case it's not running */
            Intent startTox = new Intent(getApplicationContext(), ToxDoService.class);
            getApplicationContext().startService(startTox);

            /* Launch main activity */
            Intent main = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(main);

            finish();
        } else {
            ArrayList<String> profiles = db.getAllProfiles();
            db.close();
            // Populate the profile login spinner
            Spinner profileSpinner = (Spinner) findViewById(R.id.login_account_name);
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                    this,
                    android.R.layout.simple_spinner_dropdown_item,
                    profiles);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            profileSpinner.setAdapter(adapter);
            profileSpinner.setSelection(0);
            profileSpinner.setOnItemSelectedListener(this);
        }
    }

    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        profileSelected = parent.getItemAtPosition(pos).toString();
    }

    public void onNothingSelected(AdapterView<?> parent) {
    }


    public void onClickLogin(View view) {
        String account = profileSelected;

        if (account.equals("")) {
            Context context = getApplicationContext();
            CharSequence text = getString(R.string.login_must_fill_in);
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        } else {
            /* Attempt to login */
            UserDB db = new UserDB(this);

            if(db.login(account)) {
                /* Set that we're logged in and active user's details */
                String[] details = db.getUserDetails(account);
                db.close();
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean("loggedin", true);
                editor.putString("active_account", account);
                editor.putString("nickname", details[0]);
                editor.putString("status", details[1]);
                editor.putString("status_message", details[2]);
                editor.apply();

                /* Init Tox and start service */
                Intent startTox = new Intent(getApplicationContext(), ToxDoService.class);
                getApplicationContext().startService(startTox);

                /* Launch main activity */
                Intent main = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(main);

                finish();
            } else {
                Context context = getApplicationContext();
                CharSequence text = getString(R.string.login_bad_login);
                int duration = Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
            }
        }
    }

    public void onClickCreateAccount(View view) {
        Intent createAccount = new Intent(getApplicationContext(), CreateAcccountActivity.class);
        startActivityForResult(createAccount, 1);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == 1) {
            if(resultCode == RESULT_OK) {
                finish();
            }
        }
    }
}