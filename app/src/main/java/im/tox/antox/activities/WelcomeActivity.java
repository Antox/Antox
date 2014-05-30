package im.tox.antox.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import im.tox.antox.R;
import im.tox.antox.utils.UserDetails;
import im.tox.jtoxcore.ToxUserStatus;

public class WelcomeActivity extends ActionBarActivity {

    TextView username;
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

        setContentView(R.layout.activity_welcome);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            getSupportActionBar().setIcon(R.drawable.ic_actionbar);
        }
        username = (EditText) findViewById(R.id.welcome_name_hint);
        username.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                    updateSettings(v);
                    handled = true;
                }
                return handled;
            }
        });

//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public void updateSettings(View view) {

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
            editor.commit();

            /* Update easy access storage class */
            UserDetails.username = usernameText;
            UserDetails.note = "Hey! I'm using Antox";
            UserDetails.status = ToxUserStatus.TOX_USERSTATUS_NONE;
            
		    /* Save the fact the user has seen the welcome message */
            SharedPreferences.Editor editorMain;
            SharedPreferences prefMain = getSharedPreferences("main",
                    Context.MODE_PRIVATE);
            editorMain = prefMain.edit();
            editorMain.putInt("beenLoaded", 1);
            editorMain.commit();

            //set result
            setResult(RESULT_OK);

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