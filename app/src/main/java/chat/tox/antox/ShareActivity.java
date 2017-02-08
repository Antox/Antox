package chat.tox.antox;

import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by zoff99 on 29.01.2017.
 */

public class ShareActivity extends AppCompatActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        System.out.println("ShareActivity:" + "onNewIntent:intent=" + intent);

        if (Intent.ACTION_SEARCH.equals(intent.getAction()))
        {
            String query = intent.getStringExtra(SearchManager.QUERY);
        }
        else
        {
            Uri data = intent.getData();
            String dataString = intent.getDataString();
            String shareWith = dataString.substring(dataString.lastIndexOf('/') + 1);
        }
    }
}
