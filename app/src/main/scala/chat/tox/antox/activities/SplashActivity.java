package chat.tox.antox.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by zoff99 on 08.01.2017.
 */
public class SplashActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(this, chat.tox.antox.activities.LoginActivity.class);
        startActivity(intent);
        finish();
    }
}
