package im.tox.antox.tox;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import im.tox.jtoxcore.ToxException;

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

        Runnable start = new Runnable() {
            @Override
            public void run() {
                long lastTime = System.currentTimeMillis();
                while(keepRunning) {
                    long currentTime = System.currentTimeMillis();
                    try {
                        if (currentTime - lastTime > toxSingleton.jTox.doToxInterval()) {
                            lastTime = currentTime;
                            toxSingleton.jTox.doTox();
                        }
                    } catch (ToxException e) {
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
