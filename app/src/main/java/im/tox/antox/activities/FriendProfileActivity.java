package im.tox.antox.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
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
import im.tox.antox.R;
import im.tox.antox.data.AntoxDB;

public class FriendProfileActivity extends ActionBarActivity {

    String friendName = null;
    String friendKey = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_profile);

        friendKey = getIntent().getStringExtra("key");
        AntoxDB db = new AntoxDB(this);
        String[] friendDetails = db.getFriendDetails(friendKey);
        friendName = friendDetails[0];
        String friendAlias = friendDetails[1];
        String friendNote = friendDetails[2];

        setTitle(friendName+"'s Profile");


        EditText editFriendAlias = (EditText) findViewById(R.id.friendAliasText);
        editFriendAlias.setText(friendAlias);

        TextView editFriendNote = (TextView) findViewById(R.id.friendNoteText);
        editFriendNote.setText("\""+friendNote+"\"");

        TextView editFriendKey = (TextView) findViewById(R.id.friendKeyText);
        editFriendKey.setText(friendKey);

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
        file = new File(Environment.getExternalStorageDirectory().getPath()+"/Antox/"+friendName+".png");
        generateQR(friendKey);
        Bitmap bmp = BitmapFactory.decodeFile(file.getAbsolutePath());
        qrCode.setImageBitmap(bmp);
        qrCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(Environment.getExternalStorageDirectory().getPath() + "/Antox/"+friendName+".png")));
                shareIntent.setType("image/jpeg");
                startActivity(Intent.createChooser(shareIntent, getResources().getString(R.string.share_with)));
            }
        });

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            getSupportActionBar().setIcon(R.drawable.ic_actionbar);
        }
    }

    public void updateAlias(View view) {
        AntoxDB db = new AntoxDB(this);
        TextView friendKeyText = (TextView) findViewById(R.id.friendKeyText);
        EditText friendAlias = (EditText) findViewById(R.id.friendAliasText);
        db.updateAlias(friendAlias.getText().toString(), friendKeyText.getText().toString());
        db.close();

        Context context = getApplicationContext();
        CharSequence text = getString(R.string.friend_profile_updated);
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }

    private void generateQR(String userKey) {
        String qrData = "tox://" + userKey;
        int qrCodeSize = 400;
        QRCodeEncode qrCodeEncoder = new QRCodeEncode(qrData, null,
                Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), qrCodeSize);
        FileOutputStream out;
        try {
            Bitmap bitmap = qrCodeEncoder.encodeAsBitmap();
            out = new FileOutputStream(Environment.getExternalStorageDirectory().getPath()+"/Antox/"+friendName+".png");
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
    @SuppressWarnings("deprecation")
    public void copyID(View view)
    {
            Context context=getApplicationContext();
            int sdk = android.os.Build.VERSION.SDK_INT;
            if (sdk < android.os.Build.VERSION_CODES.HONEYCOMB)
            {
                android.text.ClipboardManager clipboard = (android.text.ClipboardManager) context
                        .getSystemService(context.CLIPBOARD_SERVICE);
                clipboard.setText(friendKey);
            }
            else
            {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context
                        .getSystemService(context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData
                        .newPlainText("friendKey", friendKey);
                clipboard.setPrimaryClip(clip);
            }
            Toast.makeText(context,context.getResources().getString(R.string.friend_profile_copied),Toast.LENGTH_SHORT).show();

    }

}
