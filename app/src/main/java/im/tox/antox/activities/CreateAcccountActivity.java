package im.tox.antox.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Random;

import im.tox.antox.R;
import im.tox.antox.data.UserDB;
import im.tox.antox.tox.ToxDataFile;
import im.tox.antox.tox.ToxDoService;
import im.tox.antox.utils.AntoxFriendList;
import im.tox.antox.utils.Constants;
import im.tox.jtoxcore.JTox;
import im.tox.jtoxcore.ToxException;
import im.tox.jtoxcore.callbacks.CallbackHandler;

public class CreateAcccountActivity extends ActionBarActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_create_acccount);

        /* Fix for an android 4.1.x bug */
        if(Build.VERSION.SDK_INT != Build.VERSION_CODES.JELLY_BEAN
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            );
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.create_acccount, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onClickAllowSearch(View view) {
        CheckBox checkBox = (CheckBox) findViewById(R.id.create_allow_search);
        TextView bioString = (TextView) findViewById(R.id.create_bio_string);
        EditText bioField = (EditText) findViewById(R.id.create_bio);
        if(checkBox.isChecked()) {
            bioString.setVisibility(View.VISIBLE);
            bioField.setVisibility(View.VISIBLE);
        } else {
            bioString.setVisibility(View.GONE);
            bioField.setVisibility(View.GONE);
        }
    }

    public void onClickRegisterAccount(View view) {
        EditText accountField = (EditText) findViewById(R.id.create_account_name);
        EditText password1Field = (EditText) findViewById(R.id.create_password);
        EditText password2Field = (EditText) findViewById(R.id.create_password_again);
        CheckBox allowSearchField = (CheckBox) findViewById(R.id.create_allow_search);

        String account = accountField.getText().toString();
        String password1 = password1Field.getText().toString();
        String password2 = password2Field.getText().toString();
        boolean allowSearch = allowSearchField.isChecked();

        if(account.equals("") || password1.equals("") || password2.equals("")) {
            Context context = getApplicationContext();
            CharSequence text = getString(R.string.create_must_fill_in);
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        } else {
            if(!password1.equals(password2)) {
                Context context = getApplicationContext();
                CharSequence text = getString(R.string.create_password_bad_match);
                int duration = Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
            } else {
                // Set active account name
                Constants.ACTIVE_DATABASE_NAME = account;

                // Generate a safe random password for encryption (the user entered password is used for logging in)
                final String AB = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ!@#$%^&*()";
                Random rnd = new Random();

                StringBuilder sb = new StringBuilder(32);
                for (int i = 0; i < 32; i++)
                    sb.append(AB.charAt(rnd.nextInt(AB.length())));

                // Add user to DB
                UserDB db = new UserDB(this);
                db.addUser(account, password1);

                // Create a tox data file
                String ID = "";
                byte[] fileBytes = null;
                try {
                    AntoxFriendList antoxFriendList = new AntoxFriendList();
                    CallbackHandler callbackHandler = new CallbackHandler(antoxFriendList);
                    JTox jTox = new JTox(antoxFriendList, callbackHandler);
                    ToxDataFile toxDataFile = new ToxDataFile(this);
                    toxDataFile.saveFile(jTox.save());
                    ID = jTox.getAddress();
                    fileBytes = toxDataFile.loadFile();
                } catch(ToxException e) {
                    Log.d("CreateAccount", e.getMessage());
                }

                // Register Account using toxme.se API
                /*
                long epoch = System.currentTimeMillis();
                int allow = allowSearch ? 1 : 0;
                try {
                    JSONObject unencryptedPayload = new JSONObject();
                    unencryptedPayload.put("tox_id", ID);
                    unencryptedPayload.put("name", account);
                    unencryptedPayload.put("privacy", allow);
                    unencryptedPayload.put("bio", "Antox User");
                    unencryptedPayload.put("timestamp", epoch);

                    /* Encrypt the payload *
                    String toxmepk = "5D72C517DF6AEC54F1E977A6B6F25914EA4CF7277A85027CD9F5196DF17E0B13";

                    Hex hexEncoder = new Hex();
                    byte[] pk = hexEncoder.decode(toxmepk);

                    byte[] sk = new byte[32];
                    System.arraycopy(fileBytes, 52, sk, 0, 32);

                    Box box = new Box(pk, sk);
                    SecureRandom secureRandom = new SecureRandom();
                    byte[] nonce = secureRandom.generateSeed(24);
                    byte[] payloadBytes = box.encrypt(nonce, unencryptedPayload.toString().getBytes());
                    String payload = new String(payloadBytes);
                    String nonceString = new String(nonce);

                    JSONObject json = new JSONObject();
                    json.put("action_id", 1);
                    json.put("public_key", "changeme");
                    json.put("encrypted", payload);
                    json.put("nonce", nonceString);

                    HttpClient httpClient = new DefaultHttpClient();
                    try {
                        HttpPost request = new HttpPost("http://toxme.se/api/");
                        StringEntity params =new StringEntity(json.toString());
                        request.addHeader("content-type", "application/x-www-form-urlencoded");
                        request.setEntity(params);
                        HttpResponse response = httpClient.execute(request);
                        // handle response here...
                        Log.d("Response", response.toString());
                    }catch (Exception ex) {
                        // handle exception here
                        Log.d("CreateAccount", ex.getMessage());
                    } finally {
                        httpClient.getConnectionManager().shutdown();
                    }
                } catch(JSONException e) {
                    Log.d("CreateAcccount", e.getMessage());
                }
                */
                // Login and launch
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("active_account", account);
                editor.putString("nickname", account);
                editor.putString("status", "1");
                editor.putString("status_message", "Hey! I'm using Antox");
                editor.putString("tox_id", ID);
                editor.putBoolean("loggedin", true);
                editor.apply();

                /* Start Tox Service */
                Intent startTox = new Intent(getApplicationContext(), ToxDoService.class);
                getApplicationContext().startService(startTox);

                /* Launch main activity */
                Intent main = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(main);

                finish();
            }
        }
    }

    public void onClickSkipRegistration(View view) {
        /* Temporary */
        /* John please fix this */
        String tmp = "This currently does the same as registering above (no toxme)";
        Context context = getApplicationContext();
        CharSequence text = tmp;
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }
}
