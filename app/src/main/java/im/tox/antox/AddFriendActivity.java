package im.tox.antox;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import im.tox.QR.IntentIntegrator;
import im.tox.QR.IntentResult;

/**
 * Activity to allow the user to add a friend. Also as a URI handler to automatically insert public
 * keys from tox:// links. See AndroidManifest.xml for more information on the URI handler.
 *
 * @author Mark Winter (Astonex)
 */

public class AddFriendActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friend);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        EditText friendID = (EditText) findViewById(R.id.addfriend_key);
        Intent intentURI = getIntent();
        Uri uri;
        if (Intent.ACTION_VIEW.equals(intentURI.getAction())
                && intentURI != null) {
            uri = intentURI.getData();
            if (uri != null)
                friendID.setText(uri.getHost());
        }
    }

    /*
    * method is outside so that the intent can be passed this object
     */
    private void scanIntent() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.initiateScan();
    }

    public void addFriend(View view) {
        Context context = getApplicationContext();
        CharSequence text = "Friend Added";
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context, text, duration);

        /* Send intent to ToxService */
        EditText friendID = (EditText) findViewById(R.id.addfriend_key);
        EditText friendMessage = (EditText) findViewById(R.id.addfriend_message);

        /*validates key*/
        if(validateFriendKey(friendID.getText().toString())){
            toast.show();
        }else{
            toast = Toast.makeText(context, getResources().getString(R.string.invalid_friend_ID), Toast.LENGTH_SHORT);
            toast.show();
            return;
        }

        /* This is a temporary work around until jToxcore stops cutting off the last byte of a string.
         * As it is now, if you send an empty message it will cause jToxcore to throw an exception
         * so simply send a single character instead to stop this.
         */
        String message = friendMessage.getText().toString();

        String[] friendData = { friendID.getText().toString(), message};

        Intent addFriend = new Intent(this, ToxService.class);
        addFriend.setAction(Constants.ADD_FRIEND);
        addFriend.putExtra("friendData", friendData);
        this.startService(addFriend);

        AntoxDB db = new AntoxDB(getApplicationContext());
        db.addFriend(friendID.getText().toString(), "Friend Request Sent");
        db.close();

        Intent update = new Intent(Constants.BROADCAST_ACTION);
        update.putExtra("action", Constants.UPDATE);
        LocalBroadcastManager.getInstance(this).sendBroadcast(update);

        Intent i = new Intent();
        setResult(RESULT_OK,i);

        // Close activity
        finish();
    }
    /*
    * handle intent to read a friend QR code
    * */
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (scanResult != null) {
            if(scanResult.getContents()!=null){
                EditText addFriendKey = (EditText)findViewById(R.id.addfriend_key);
                String friendKey = (scanResult.getContents().contains("tox://")? scanResult.getContents().substring(6):scanResult.getContents());
                if(validateFriendKey(friendKey)){
                    addFriendKey.setText(friendKey);
                }else{
                    Context context = getApplicationContext();
                    Toast toast = Toast.makeText(context, getResources().getString(R.string.invalid_friend_ID), Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        }
    }

    private boolean validateFriendKey(String friendKey) {
        if(friendKey.length()!=76 || friendKey.matches("[[:xdigit:]]")){
            return false;
        }
        int x=0;
        for(int i=0;i<friendKey.length();i+=4){
            x=x^Integer.valueOf(friendKey.substring(i, i+4),16);
        }
        if(x!=0)
            return false;
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.add_friend, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // This ID represents the Home or Up button. In the case of this
                // activity, the Up button is shown. Use NavUtils to allow users
                // to navigate up one level in the application structure. For
                // more details, see the Navigation pattern on Android Design:
                //
                // http://developer.android.com/design/patterns/navigation.html#up-vs-back
                //
                NavUtils.navigateUpFromSameTask(this);
                return true;
            //scanQR button to call the barcode reader app
            case R.id.scanFriend:
                scanIntent();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

}
