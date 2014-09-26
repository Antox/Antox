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
import android.widget.EditText;
import android.widget.Toast;

import im.tox.antox.R;
import im.tox.antox.data.UserDB;
import im.tox.antox.tox.ToxDoService;
import im.tox.antox.utils.Constants;

public class LoginActivity extends ActionBarActivity {


    public void onClickLogin(View view) {

        EditText accountNameField = (EditText) findViewById(R.id.login_account_name);

        String account = accountNameField.getText().toString();

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
                /* Set that we're logged in and active user's details*/
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