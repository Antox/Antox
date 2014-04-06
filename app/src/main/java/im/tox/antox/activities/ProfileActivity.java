package im.tox.antox.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import im.tox.QR.Contents;
import im.tox.QR.QRCodeEncode;
import im.tox.antox.utils.Constants;
import im.tox.antox.R;
import im.tox.antox.tox.ToxService;
import im.tox.antox.utils.UserDetails;
import im.tox.jtoxcore.ToxUserStatus;

/**
 * Profile Activity where the user can change their username, status, and note.

 * @author Mark Winter (Astonex) & David Lohle (Proplex)
 */

public class ProfileActivity extends ActionBarActivity {
    /**
     * Spinner for displaying acceptable statuses (online/away/busy) to the users
     */
    private Spinner statusSpinner;


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

        setContentView(R.layout.activity_profile);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            getSupportActionBar().setIcon(R.drawable.ic_actionbar);
        }
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        statusSpinner = (Spinner) findViewById(R.id.settings_spinner_status);

        /* Add acceptable statuses to the drop down menu */
        String[] statusItems = new String[]{
                getResources().getString(R.string.status_online),
                getResources().getString(R.string.status_away),
                getResources().getString(R.string.status_busy)
        };

        ArrayAdapter<String> statusAdapter = new ArrayAdapter<String>(this,
                R.layout.simple_spinner_item, statusItems);
        statusSpinner.setAdapter(statusAdapter);

		/* Get saved preferences */
        SharedPreferences pref = getSharedPreferences("settings",
                Context.MODE_PRIVATE);

        /* Sets the user key to the saved user key */
        TextView userKey = (TextView) findViewById(R.id.settings_user_key);
        userKey.setText(pref.getString("user_key", ""));

        /* Looks for the userkey qr.png if it doesn't exist then it creates it with the generateQR method.
         * adds onClickListener to the ImageButton to add share the QR
          * */
        ImageButton qrCode = (ImageButton) findViewById(R.id.qr_code);

        File file = new File(Environment.getExternalStorageDirectory().getPath()+"/Antox/");
        if(!file.exists()){
            file.mkdirs();
        }

        File noMedia = new File(Environment.getExternalStorageDirectory().getPath()+"/Antox/",".nomedia");
        if(!noMedia.exists()){
            try {
                noMedia.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        file = new File(Environment.getExternalStorageDirectory().getPath()+"/Antox/userkey_qr.png");
        generateQR(pref.getString("user_key", ""));
        Bitmap bmp = BitmapFactory.decodeFile(file.getAbsolutePath());
        qrCode.setImageBitmap(bmp);
        qrCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(Environment.getExternalStorageDirectory().getPath() + "/Antox/userkey_qr.png")));
                shareIntent.setType("image/jpeg");
                startActivity(Intent.createChooser(shareIntent, getResources().getString(R.string.share_with)));
            }
        });

		/* If the preferences aren't blank, then add them to text fields
         * otherwise it will display the predefined hints in strings.xml
         */

        if (!pref.getString("saved_name_hint", "").equals("")) {
            EditText nameHint = (EditText) findViewById(R.id.settings_name_hint);
            nameHint.setText(pref.getString("saved_name_hint", ""));
        }

        if (!pref.getString("saved_note_hint", "").equals("")) {
            EditText noteHint = (EditText) findViewById(R.id.settings_note_hint);
            noteHint.setText(pref.getString("saved_note_hint", ""));
        }

        if (!pref.getString("saved_status_hint", "").equals("")) {
            String savedStatus = pref.getString("saved_status_hint", "");
            int statusPos = statusAdapter.getPosition(savedStatus);
            statusSpinner.setSelection(statusPos);
        }
    }

    /*
    * generates the QR using the ZXING library (core.jar in libs folder)
     */
    private void generateQR(String userKey) {
        String qrData = "tox://" + userKey;
        int qrCodeSize = 400;
        QRCodeEncode qrCodeEncoder = new QRCodeEncode(qrData, null,
                Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), qrCodeSize);
        FileOutputStream out;
        try {
            Bitmap bitmap = qrCodeEncoder.encodeAsBitmap();
            out = new FileOutputStream(Environment.getExternalStorageDirectory().getPath()+"/antox/userkey_qr.png");
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.close();
        } catch (WriterException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method is called when the user updates their settings. It will check all the text fields
     * to see if they contain default values, and if they don't, save them using SharedPreferences
     *
     * @param view
     */
    public void updateSettings(View view) {
        /**
         * String array to store updated details to be passed by intent to ToxService
         */
        String[] updatedSettings = { null, null, null};

		/* Get all text from the fields */
        EditText nameHintText = (EditText) findViewById(R.id.settings_name_hint);
        EditText noteHintText = (EditText) findViewById(R.id.settings_note_hint);
        //EditText statusHintText = (EditText) findViewById(R.id.settings_status_hint);

		/* Save settings to file */

        SharedPreferences pref = getSharedPreferences("settings",
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();

		/*
		 * If the fields aren't equal to the default strings in strings.xml then
		 * they contain user entered data so they need saving
		 */
        if (!nameHintText.getText().toString().equals(getString(R.id.settings_name_hint))) {
            editor.putString("saved_name_hint", nameHintText.getText().toString());
            UserDetails.username = nameHintText.getText().toString();
        }

        if (!noteHintText.getText().toString().equals(getString(R.id.settings_note_hint))) {
            editor.putString("saved_note_hint", noteHintText.getText().toString());
            UserDetails.note = noteHintText.getText().toString();
        }
        editor.putString("saved_status_hint", statusSpinner.getSelectedItem().toString());
        if (statusSpinner.getSelectedItem().toString().equals(getString(R.string.status_online)))
            UserDetails.status = ToxUserStatus.TOX_USERSTATUS_NONE;
        if (statusSpinner.getSelectedItem().toString().equals(getString(R.string.status_away)))
            UserDetails.status = ToxUserStatus.TOX_USERSTATUS_AWAY;
        if (statusSpinner.getSelectedItem().toString().equals(getString(R.string.status_busy)))
            UserDetails.status = ToxUserStatus.TOX_USERSTATUS_BUSY;

        editor.commit();

        /* Send an intent to ToxService notifying change of settings */
        Intent updateSettings = new Intent(this, ToxService.class);
        updateSettings.setAction(Constants.UPDATE_SETTINGS);
        this.startService(updateSettings);

        Context context = getApplicationContext();
        CharSequence text = getResources().getString(R.string.profile_updated);
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context, text, duration);
        toast.show();

        finish();
    }


}