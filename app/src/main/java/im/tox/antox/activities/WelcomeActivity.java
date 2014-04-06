package im.tox.antox.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import im.tox.antox.R;
import im.tox.antox.utils.UserDetails;
import im.tox.jtoxcore.ToxUserStatus;

/**
 * Welcome Activity is displayed when the user is using the app for the very first time to get
 * some required details such as a username.
 *
 * @author Mark Winter (Astonex)
 */

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
            SharedPreferences pref = getSharedPreferences("settings",
                    Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();
            editor.putString("saved_name_hint", usernameText);
            editor.commit();
            UserDetails.username = usernameText;
            UserDetails.note = "";
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.welcome, menu);
        return true;
    }
//    No need of action bar Home button in WelcomeActivity.
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case android.R.id.home:
//                This ID represents the Home or Up button. In the case of this
//                activity, the Up button is shown. Use NavUtils to allow users
//                to navigate up one level in the application structure. For
//                more details, see the Navigation pattern on Android Design:
//
//                http://developer.android.com/design/patterns/navigation.html#up-vs-back
//
//                NavUtils.navigateUpFromSameTask(this);
//                return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }

}