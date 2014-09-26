package im.tox.antox.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.abstractj.kalium.crypto.Box;
import org.abstractj.kalium.encoders.Hex;
import org.abstractj.kalium.encoders.Raw;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import im.tox.antox.R;
import im.tox.antox.data.UserDB;
import im.tox.antox.tox.ToxDataFile;
import im.tox.antox.tox.ToxDoService;
import im.tox.antox.utils.AntoxFriendList;
import im.tox.antox.utils.Options;
import im.tox.jtoxcore.JTox;
import im.tox.jtoxcore.ToxException;
import im.tox.jtoxcore.ToxOptions;
import im.tox.jtoxcore.callbacks.CallbackHandler;

public class CreateAcccountActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_create_acccount);

        /* Fix for an android 4.1.x bug */
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.JELLY_BEAN
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

    public void onClickRegisterIncogAccount(View view) {
        EditText accountField = (EditText) findViewById(R.id.create_account_name);

        String account = accountField.getText().toString();

        Pattern pattern = Pattern.compile("\\s");
        Pattern pattern2 = Pattern.compile(File.separator);
        Matcher matcher = pattern.matcher(account);
        boolean containsSpaces = matcher.find();
        matcher = pattern2.matcher(account);
        boolean containsFileSeperator = matcher.find();

        if (account.equals("")) {
            Context context = getApplicationContext();
            CharSequence text = getString(R.string.create_must_fill_in);
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        } else if (containsSpaces || containsFileSeperator) {
            Context context = getApplicationContext();
            CharSequence text = getString(R.string.create_bad_profile_name);
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        } else {
            // Add user to DB
            UserDB db = new UserDB(this);
            db.addUser(account, "");
            db.close();

            // Create a tox data file
            String ID = "";
            byte[] fileBytes = null;
            try {
                AntoxFriendList antoxFriendList = new AntoxFriendList();
                CallbackHandler callbackHandler = new CallbackHandler(antoxFriendList);
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                boolean udpEnabled = preferences.getBoolean("enable_udp", false);
                ToxOptions toxOptions = new ToxOptions(Options.ipv6Enabled, udpEnabled, Options.proxyEnabled);
                JTox jTox = new JTox(antoxFriendList, callbackHandler, toxOptions);
                ToxDataFile toxDataFile = new ToxDataFile(this, account);
                toxDataFile.saveFile(jTox.save());
                ID = jTox.getAddress();
                fileBytes = toxDataFile.loadFile();
            } catch (ToxException e) {
                Log.d("CreateAccount", e.getMessage());
            }
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("active_account", account);
            editor.putString("nickname", account);
            editor.putString("status", "online");
            editor.putString("status_message", getResources().getString(R.string.pref_default_status_message));
            editor.putString("tox_id", ID);
            editor.putBoolean("loggedin", true);
            editor.apply();

                            /* Start Tox Service */
            Intent startTox = new Intent(getApplicationContext(), ToxDoService.class);
            getApplicationContext().startService(startTox);

                            /* Launch main activity */
            Intent main = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(main);

            setResult(RESULT_OK);
            finish();
        }
    }


    public void onClickRegisterAccount(View view) {
        EditText accountField = (EditText) findViewById(R.id.create_account_name);

        String account = accountField.getText().toString();

        Pattern pattern = Pattern.compile("\\s");
        Pattern pattern2 = Pattern.compile(File.separator);
        Matcher matcher = pattern.matcher(account);
        boolean containsSpaces = matcher.find();
        matcher = pattern2.matcher(account);
        boolean containsFileSeperator = matcher.find();

        if (account.equals("")) {
            Context context = getApplicationContext();
            CharSequence text = getString(R.string.create_must_fill_in);
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        } else if (containsSpaces || containsFileSeperator) {
            Context context = getApplicationContext();
            CharSequence text = getString(R.string.create_bad_profile_name);
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        } else {
            // Add user to DB
            UserDB db = new UserDB(this);
            db.addUser(account, "");
            db.close();

            // Create a tox data file
            String ID = "";
            byte[] fileBytes = null;
            try {
                AntoxFriendList antoxFriendList = new AntoxFriendList();
                CallbackHandler callbackHandler = new CallbackHandler(antoxFriendList);
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                boolean udpEnabled = preferences.getBoolean("enable_udp", false);
                ToxOptions toxOptions = new ToxOptions(Options.ipv6Enabled, udpEnabled, Options.proxyEnabled);
                JTox jTox = new JTox(antoxFriendList, callbackHandler, toxOptions);
                ToxDataFile toxDataFile = new ToxDataFile(this, account);
                toxDataFile.saveFile(jTox.save());
                ID = jTox.getAddress();
                fileBytes = toxDataFile.loadFile();
            } catch (ToxException e) {
                Log.d("CreateAccount", e.getMessage());
            }
                    /* Register Account using toxme.se API */
            try {
                System.load("/data/data/im.tox.antox/lib/libkaliumjni.so");
            } catch (Exception e) {
                Log.d("CreateAccount", "System.load() on kalium failed");
            }

            int allow = 0;
            JSONPost jsonPost = new JSONPost();
            Thread toxmeThread = new Thread(jsonPost);
            try {
                JSONObject unencryptedPayload = new JSONObject();
                unencryptedPayload.put("tox_id", ID);
                unencryptedPayload.put("name", account);
                unencryptedPayload.put("privacy", allow);
                unencryptedPayload.put("bio", "");
                long epoch = System.currentTimeMillis() / 1000;
                unencryptedPayload.put("timestamp", epoch);

                Hex hexEncoder = new Hex();
                Raw rawEncoder = new Raw();

                String toxmepk = "5D72C517DF6AEC54F1E977A6B6F25914EA4CF7277A85027CD9F5196DF17E0B13";
                byte[] serverPublicKey = hexEncoder.decode(toxmepk);
                byte[] ourSecretKey = new byte[32];
                System.arraycopy(fileBytes, 52, ourSecretKey, 0, 32);

                Box box = new Box(serverPublicKey, ourSecretKey);
                org.abstractj.kalium.crypto.Random random = new org.abstractj.kalium.crypto.Random();
                byte[] nonce = random.randomBytes(24);
                byte[] payloadBytes = box.encrypt(nonce, rawEncoder.decode(unencryptedPayload.toString()));
                // Encode payload and nonce to base64
                payloadBytes = Base64.encode(payloadBytes, Base64.NO_WRAP);
                nonce = Base64.encode(nonce, Base64.NO_WRAP);

                String payload = rawEncoder.encode(payloadBytes);
                String nonceString = rawEncoder.encode(nonce);

                JSONObject json = new JSONObject();
                json.put("action", 1);
                json.put("public_key", ID.substring(0, 64));
                json.put("encrypted", payload);
                json.put("nonce", nonceString);

                jsonPost.setJSON(json.toString());
                toxmeThread.start();
                toxmeThread.join();
            } catch (JSONException e) {
                Log.d("CreateAcccount", "JSON Exception " + e.getMessage());
            } catch (InterruptedException e) {
            }

            String toastMessage = "";
            Context context = getApplicationContext();
            int duration = Toast.LENGTH_SHORT;
            Toast toast;

            switch (jsonPost.getErrorCode()) {
                case "0":
                    // Login and launch
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString("active_account", account);
                    editor.putString("nickname", account);
                    editor.putString("status", "1");
                    editor.putString("status_message", getResources().getString(R.string.pref_default_status_message));
                    editor.putString("tox_id", ID);
                    editor.putBoolean("loggedin", true);
                    editor.apply();

                            /* Start Tox Service */
                    Intent startTox = new Intent(getApplicationContext(), ToxDoService.class);
                    getApplicationContext().startService(startTox);

                            /* Launch main activity */
                    Intent main = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(main);

                    setResult(RESULT_OK);
                    finish();
                    break;

                case "-25": // Name alrady taken
                    toastMessage = "This name is already taken";
                    toast = Toast.makeText(context, toastMessage, duration);
                    toast.show();
                    break;

                case "-26":
                    // ID already bound to a name
                    toastMessage = "Internal Antox Error. Please restart and try again";
                    toast = Toast.makeText(context, toastMessage, duration);
                    toast.show();
                    break;

                case "-4":
                    // Rate limited
                    toastMessage = "You can only register 13 accounts an hour. You have reached this limit";
                    toast = Toast.makeText(context, toastMessage, duration);
                    toast.show();
                    break;
            }
        }

    }

    private class JSONPost implements Runnable {
        private volatile String errorCode = "notdone";
        private String finaljson;

        public JSONPost() {

        }

        public void run() {
            HttpClient httpClient = new DefaultHttpClient();
            try {
                HttpPost post = new HttpPost("https://toxme.se/api");
                post.setHeader("Content-Type", "application/json");
                post.setEntity(new StringEntity(finaljson.toString()));
                HttpResponse response = httpClient.execute(post);
                Log.d("CreateAccount", "Response code: " + response.toString());
                HttpEntity entity = response.getEntity();
                Scanner in = new Scanner(entity.getContent());
                while (in.hasNext()) {
                    String responseString = in.next();
                    Log.d("CreateAccount", "Response: " + responseString);
                    if (responseString.contains("\"c\":")) {
                        errorCode = in.next();
                        errorCode = errorCode.replaceAll("\"", "");
                        errorCode = errorCode.replaceAll(",", "");
                        errorCode = errorCode.replaceAll("\\}", "");
                        Log.d("CreateAccount", "Error Code: " + errorCode);
                    }
                }
                in.close();
            } catch (UnsupportedEncodingException e) {
                Log.d("CreateAccount", "Unsupported Encoding Exception: " + e.getMessage());
            } catch (IOException e) {
                Log.d("CreateAccount", "IOException: " + e.getMessage());
            } finally {
                httpClient.getConnectionManager().shutdown();
            }
        }

        public synchronized String getErrorCode() {
            return errorCode;
        }

        public synchronized void setJSON(String json) {
            finaljson = json;
        }
    }
}
