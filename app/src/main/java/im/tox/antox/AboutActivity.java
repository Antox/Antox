package im.tox.antox;

import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;

/**
 * Profile Activity where the user can change their username, status, and note.

 * @author Mark Winter (Astonex) & David Lohle (Proplex)
 */

public class AboutActivity extends ActionBarActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            getSupportActionBar().setIcon(R.drawable.ic_actionbar);
        }
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.welcome, menu);
        return true;
    }
}