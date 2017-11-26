package chat.tox.antox;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import chat.tox.antox.utils.AntoxLog;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;
import rx.plugins.RxJavaErrorHandler;
import rx.plugins.RxJavaPlugins;

public class SplashActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks
{
    final static int RC_ANTOX_PERM = 146;
    final static String[] perms = {Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.VIBRATE, Manifest.permission.RECEIVE_BOOT_COMPLETED, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WAKE_LOCK, Manifest.permission.MODIFY_AUDIO_SETTINGS};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            RxJavaPlugins.getInstance().registerErrorHandler(new RxJavaErrorHandler() {
                @Override
                public void handleError(Throwable e) {
                    e.printStackTrace();
                    super.handleError(e);
                }
            });
        } catch (Exception e) {
            // don't worry about this, this is just annoying
            AntoxLog.debug("Registered another error handler when we didn't need to.", AntoxLog.DEFAULT_TAG());
        }

        getPermissions();
    }

    public void startAntox()
    {
        Intent intent = new Intent(this, chat.tox.antox.activities.LoginActivity.class);
        startActivity(intent);
        finish();
    }

    public void getPermissions()
    {
        if (EasyPermissions.hasPermissions(this, perms))
        {
            // already have granted all permissions
            startAntox();
            return;
        }

        // List<String> perms2 = new ArrayList<String>(Arrays.asList(perms));
        if (EasyPermissions.hasPermissions(this, perms))
        {
            // already have granted all permissions
            startAntox();
            return;
        }
        else
        {
            // Ask for permissions
            EasyPermissions.requestPermissions(this, getString(R.string.request_permissions_text), RC_ANTOX_PERM, perms);
        }
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms2)
    {
        // Some permissions have been granted
        if (EasyPermissions.hasPermissions(this, perms))
        {
            // already have granted all permissions
            startAntox();
            return;
        }
        else
        {
            getPermissions();
        }
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms2)
    {
        // (Optional) Check whether the user denied any permissions and checked "NEVER ASK AGAIN."
        // This will display a dialog directing them to enable the permission in app settings.
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms2))
        {
            new AppSettingsDialog.Builder(this).build().show();
        }
        else
        {
            getPermissions();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE)
        {
            // returned from settings activity
            getPermissions();
        }
    }

    static
    {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }
}