package chat.tox.antox;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by zoff99 on 29.01.2017.
 */

public class CrashActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        System.out.println("CrashActivity:onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crash);
    }
}
