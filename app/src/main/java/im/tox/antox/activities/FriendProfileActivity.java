package im.tox.antox.activities;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import im.tox.antox.data.AntoxDB;

/**
 * Created by soft on 24/03/14.
 */
public class FriendProfileActivity extends ActionBarActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String friendKey = getIntent().getStringExtra("key");
        AntoxDB db = new AntoxDB(this);
        String[] friendDetails = db.getFriendDetails(friendKey);
        String friendName = friendDetails[0];
        String friendAlias = friendDetails[1];
        String friendNote = friendDetails[2];
    }
}
