package im.tox.antox.tox;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

public class ToxDoService extends Service {

    private ToxSingleton toxSingleton = ToxSingleton.getInstance();

    public ToxDoService() {
        super();
    }

    private Thread serviceThread;
    private boolean keepRunning = true;

    @Override
    public void onCreate() {
        if(!toxSingleton.isInited) {
            toxSingleton.initTox(getApplicationContext());
            Log.d("ToxDoService", "Initting ToxSingleton");
        }

        keepRunning = true;

        Runnable start = new Runnable() {
            @Override
            public void run() {
                while(keepRunning) {
                    final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    final ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                    final boolean wifiOnly = preferences.getBoolean("wifi_only", true);
                    final NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

                    if(wifiOnly && !mWifi.isConnected()) {

                        try {
                            // Sleep for 10 seconds before checking again
                            Thread.sleep(10000);
                        } catch (Exception e) {
                        }

                    } else {

                        try {
                            Thread.sleep(toxSingleton.jTox.doToxInterval());
                            toxSingleton.jTox.doTox();
                        } catch (Exception e) {
                        }

                    }
                }
            }
        };
        serviceThread = new Thread(start);
        serviceThread.start();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int id) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        keepRunning = false;
        serviceThread.interrupt();
        toxSingleton.isInited = false;
        toxSingleton = null;
        Log.d("ToxDoService", "onDestroy() called");
    }
}
