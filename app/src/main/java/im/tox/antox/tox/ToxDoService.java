package im.tox.antox.tox;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.sql.Time;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import im.tox.jtoxcore.ToxException;

public class ToxDoService extends Service {

    private static final String TAG = "im.tox.antox.tox.ToxDoService";

    private ToxSingleton toxSingleton = ToxSingleton.getInstance();
    ;

    public ToxDoService() {
        super();
    }

    @Override
    public void onCreate() {
        Thread t = new Thread() {
            public void run() {
                toxSingleton.initTox(getApplicationContext());
            }
        };
        t.start();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int id) {
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    while(!toxSingleton.isInited) {
                        // Wait till singleton is inited
                    }
                    long after = System.currentTimeMillis();
                    int interval = toxSingleton.jTox.doToxInterval();
                    while(true) {
                        long current = System.currentTimeMillis();
                        if(current - after >= interval) {
                            toxSingleton.jTox.doTox();
                            interval = toxSingleton.jTox.doToxInterval();
                            after = System.currentTimeMillis();
                        }
                    }
                } catch (ToxException e) {
                }
            }
        };
        t.start();

        return START_STICKY;
    }
}
