package im.tox.antox.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.NavUtils;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.TXTRecord;
import org.xbill.DNS.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import im.tox.QR.IntentIntegrator;
import im.tox.QR.IntentResult;
import im.tox.antox.data.AntoxDB;

import im.tox.antox.fragments.PinDialogFragment;
import im.tox.antox.utils.Constants;
import im.tox.antox.R;
import im.tox.antox.tox.ToxService;
import im.tox.antox.utils.DhtNode;
import im.tox.antox.utils.GroupItem;

/**
 * Activity to allow the user to add a friend. Also as a URI handler to automatically insert public
 * keys from tox:// links. See AndroidManifest.xml for more information on the URI handler.
 *
 * @author Mark Winter (Astonex)
 */

public class AddFriendActivity extends ActionBarActivity implements PinDialogFragment.PinDialogListener {

    String _friendID = "";
    String _friendCHECK = "";
    String _originalUsername = "";

    boolean isV2 = false;

    Context context;
    CharSequence text;
    int duration = Toast.LENGTH_SHORT;
    Toast toast;

    EditText friendID;
    EditText friendMessage;
    EditText friendAlias;
    Spinner  friendGroup;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Fix for an android 4.1.x bug */
        if(Build.VERSION.SDK_INT != Build.VERSION_CODES.JELLY_BEAN
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            );
        }

        setContentView(R.layout.activity_add_friend);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            getSupportActionBar().setIcon(R.drawable.ic_actionbar);
        }

        // Check to see if user is connected to dht first

        if(!DhtNode.connected) {
            AlertDialog notConnectedAlertDialog = new AlertDialog.Builder(this).create();
            notConnectedAlertDialog.setTitle(R.string.addfriend_no_internet);
            notConnectedAlertDialog.setMessage(getString(R.string.addfriend_no_internet_text));
            notConnectedAlertDialog.setIcon(R.drawable.ic_launcher);
            notConnectedAlertDialog.setButton(getString(R.string.button_ok), new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });

            notConnectedAlertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    finish();
                }
            });
            notConnectedAlertDialog.show();
        }

        //set the spinner
        friendGroup = (Spinner) findViewById(R.id.spinner_add_friend_group);
        ArrayList<String> spinnerArray = new ArrayList<String>();
        spinnerArray.add(getResources().getString(R.string.manage_groups_friends));
        SharedPreferences sharedPreferences = getSharedPreferences("groups", Context.MODE_PRIVATE);
        if (!sharedPreferences.getAll().isEmpty()) {
            Map<String,?> keys = sharedPreferences.getAll();

            for(Map.Entry<String,?> entry : keys.entrySet()){
                String groupName = entry.getValue().toString();
                spinnerArray.add(groupName);
            }

        }
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, spinnerArray);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        friendGroup.setAdapter(spinnerAdapter);

        context = getApplicationContext();
        text = getString(R.string.addfriend_friend_added);

        friendID = (EditText) findViewById(R.id.addfriend_key);
        friendMessage = (EditText) findViewById(R.id.addfriend_message);
        friendAlias = (EditText) findViewById(R.id.addfriend_friendAlias);

        Intent intent = getIntent();
        //If coming from tox uri link
        if (Intent.ACTION_VIEW.equals(intent.getAction())
                && intent != null) {
            EditText friendID = (EditText) findViewById(R.id.addfriend_key);
            Uri uri;
            uri = intent.getData();
            if (uri != null)
                friendID.setText(uri.getHost());
            //TODO: ACCEPT DNS LOOKUPS FROM URI

        } else if (intent.getAction() == "toxv2") {
            //else if it came from toxv2 restart

            friendID.setText(intent.getStringExtra("originalUsername"));
            friendAlias.setText(intent.getStringExtra("alias"));
            friendMessage.setText(intent.getStringExtra("message"));

            if(checkAndSend(intent.getStringExtra("key"), intent.getStringExtra("originalUsername")) == 0) {
                toast = Toast.makeText(context, text, duration);
                toast.show();
            } else if (checkAndSend(intent.getStringExtra("key"), intent.getStringExtra("originalUsername")) == -1) {
                toast = Toast.makeText(context, getResources().getString(R.string.invalid_friend_ID), Toast.LENGTH_SHORT);
                toast.show();
                return;
            } else if (checkAndSend(intent.getStringExtra("key"), intent.getStringExtra("originalUsername")) == -2) {
                toast = Toast.makeText(context, getString(R.string.addfriend_friend_exists), Toast.LENGTH_SHORT);
                toast.show();
            }

            Intent update = new Intent(Constants.BROADCAST_ACTION);
            update.putExtra("action", Constants.UPDATE);
            LocalBroadcastManager.getInstance(this).sendBroadcast(update);
            Intent i = new Intent();
            setResult(RESULT_OK, i);

            // Close activity
            finish();
        }
    }

    private int checkAndSend(String key, String originalUsername) {
        if(validateFriendKey(key)) {
            String ID = key;
            String message = friendMessage.getText().toString();
            String alias = friendAlias.getText().toString();

            String[] friendData = {ID, message, alias};

            AntoxDB db = new AntoxDB(getApplicationContext());
            if (!db.doesFriendExist(friendID.getText().toString())) {
                Intent addFriend = new Intent(this, ToxService.class);
                addFriend.setAction(Constants.ADD_FRIEND);
                addFriend.putExtra("friendData", friendData);
                this.startService(addFriend);

                if (!alias.equals(""))
                    ID = alias;

                String group = friendGroup.getSelectedItem().toString();
                db.addFriend(ID, "Friend Request Sent", alias, originalUsername, group);
            } else {
                return -2;
            }
            db.close();
            return 0;
        } else {
            return -1;
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

        if(friendID.getText().toString().contains("@")) {
            _originalUsername = friendID.getText().toString();
            // Get the first TXT record
            try {
                //.get() is a possible ui lag on very slow internet connections where dns lookup takes a long time
                new DNSLookup().execute(friendID.getText().toString()).get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if(isV2) {
            DialogFragment dialog = new PinDialogFragment();
            Bundle bundle = new Bundle();
            bundle.putString("Enter Friend's Pin", "Enter Friend's Pin");
            dialog.setArguments(bundle);
            dialog.show(getSupportFragmentManager(), "NoticeDialogFragment");
        }

        String finalFriendKey = friendID.getText().toString();

        if(!_friendID.equals(""))
            finalFriendKey = _friendID;

        if(!isV2) {

            if(checkAndSend(finalFriendKey, "") == 0) {
                toast = Toast.makeText(context, text, duration);
                toast.show();
            } else if(checkAndSend(finalFriendKey, "") == -1) {
                toast = Toast.makeText(context, getResources().getString(R.string.invalid_friend_ID), Toast.LENGTH_SHORT);
                toast.show();
                return;
            } else if(checkAndSend(finalFriendKey, "") == -2) {
                toast = Toast.makeText(context, getString(R.string.addfriend_friend_exists), Toast.LENGTH_SHORT);
                toast.show();
            }

            Intent update = new Intent(Constants.BROADCAST_ACTION);
            update.putExtra("action", Constants.UPDATE);
            LocalBroadcastManager.getInstance(this).sendBroadcast(update);
            Intent i = new Intent();
            setResult(RESULT_OK, i);

            finish();
        }
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog, String pin) {
        pin = pin + "==";
        //Base64 to Bytes
        try {
            byte[] decoded = Base64.decode(pin, Base64.DEFAULT);

            //Bytes to Hex
            StringBuilder sb = new StringBuilder();
            for(byte b: decoded)
                sb.append(String.format("%02x", b&0xff));
            String encodedString = sb.toString();

            //Finally set the correct ID to add
            _friendID = _friendID + encodedString + _friendCHECK;

            //Restart activity with info needed
            Intent restart = new Intent(this, AddFriendActivity.class);
            restart.putExtra("key", _friendID);
            restart.putExtra("alias", friendAlias.getText().toString());
            restart.putExtra("message", friendMessage.getText().toString());
            restart.putExtra("originalUsername", _originalUsername);
            restart.setAction("toxv2");
            startActivity(restart);

            finish();

        } catch (IllegalArgumentException e) {
            Context context = getApplicationContext();
            CharSequence text = getString(R.string.addfriend_invalid_pin);
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
            e.printStackTrace();
        }

    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {}

    /*
    * handle intent to read a friend QR code
    * */
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (scanResult != null) {
            if (scanResult.getContents() != null) {
                EditText addFriendKey = (EditText) findViewById(R.id.addfriend_key);
                String friendKey = (scanResult.getContents().contains("tox://") ? scanResult.getContents().substring(6) : scanResult.getContents());
                if (validateFriendKey(friendKey)) {
                    addFriendKey.setText(friendKey);
                } else {
                    Context context = getApplicationContext();
                    Toast toast = Toast.makeText(context, getResources().getString(R.string.invalid_friend_ID), Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        }
    }

    private boolean validateFriendKey(String friendKey) {
        if (friendKey.length() != 76 || friendKey.matches("[[:xdigit:]]")) {
            return false;
        }
        int x = 0;
        try {
            for (int i = 0; i < friendKey.length(); i += 4) {
                x = x ^ Integer.valueOf(friendKey.substring(i, i + 4), 16);
            }
        }
        catch (NumberFormatException e) {
            return false;
        }
        return x == 0;
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

    private class DNSLookup extends AsyncTask<String, Void, Void> {
        protected Void doInBackground(String... params) {

            String user = params[0].substring(0, params[0].indexOf("@"));
            String domain = params[0].substring(params[0].indexOf("@")+1);
            String lookup = user + "._tox." + domain;
            Log.d("DNSLOOKUP", lookup);

            TXTRecord txt = null;
            try {
                Record[] records = new Lookup(lookup, Type.TXT).run();
                txt = (TXTRecord) records[0];
            } catch (Exception e) {
                e.printStackTrace();
            }

            if(txt != null) {
                String txtString = txt.toString().substring(txt.toString().indexOf('"'));
                Log.d("DNSLOOKUP", txtString);

                if(txtString.contains("tox1")) {
                    String key = txtString.substring(11, txtString.length()-1);
                    Log.d("DNSLOOKUP", "V1KEY: " + key);
                    _friendID = key;

                } else if (txtString.contains("tox2")) {
                    isV2 = true;
                    String key = txtString.substring(12, 12+64);
                    String check = txtString.substring(12+64+7,12+64+7+4);
                    Log.d("DNSLOOKUP", "V2KEY: " + key);
                    Log.d("DNSLOOKUP", "V2CHECK: " + check);
                    _friendID = key;
                    _friendCHECK = check;
                }
            }

            return null;
        }
    }

}
