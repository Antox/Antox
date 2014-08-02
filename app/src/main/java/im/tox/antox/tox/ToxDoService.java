package im.tox.antox.tox;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import im.tox.jtoxcore.ToxException;

public class ToxDoService extends Service {

    private static final String TAG = "im.tox.antox.tox.ToxDoService";

    private ToxScheduleTaskExecutor toxScheduleTaskExecutor = new ToxScheduleTaskExecutor(1);

    private ToxSingleton toxSingleton = ToxSingleton.getInstance();

    public ToxDoService() {
        super();
    }

    private Thread serviceThread;

    @Override
    public void onCreate() {
        if(!toxSingleton.isInited) {
            toxSingleton.initTox(getApplicationContext());
            Log.d("ToxDoService", "Initting ToxSingleton");
        }

        Runnable start = new Runnable() {
            @Override
            public void run() {
                final DoTox doTox = new DoTox();
                toxScheduleTaskExecutor.scheduleAtFixedRate(doTox, 0, 50, TimeUnit.MILLISECONDS);
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
        toxScheduleTaskExecutor.shutdownNow();
        toxScheduleTaskExecutor = null;
        toxSingleton.isInited = false;
        toxSingleton = null;
        serviceThread.interrupt();
        Log.d("ToxDoService", "onDestroy() called");
        super.onDestroy();
    }

    /* Extend the scheduler to have it restart itself on any exceptions */
    private class ToxScheduleTaskExecutor extends ScheduledThreadPoolExecutor {

        public ToxScheduleTaskExecutor(int size) {
            super(1);
        }

        @Override
        public ScheduledFuture scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
            return super.scheduleAtFixedRate(wrapRunnable(command), initialDelay, period, unit);
        }

        @Override
        public ScheduledFuture scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
            return super.scheduleWithFixedDelay(wrapRunnable(command), initialDelay, delay, unit);
        }

        private Runnable wrapRunnable(Runnable command) {
            return new LogOnExceptionRunnable(command);
        }

        private class LogOnExceptionRunnable implements Runnable {
            private Runnable theRunnable;

            public LogOnExceptionRunnable(Runnable theRunnable) {
                super();
                this.theRunnable = theRunnable;
            }

            @Override
            public void run() {
                try {
                    theRunnable.run();
                } catch (Exception e) {
                    Log.e(TAG, "Executor has caught an exception");
                    e.printStackTrace();
                    toxScheduleTaskExecutor.scheduleAtFixedRate(new DoTox(), 0, 50, TimeUnit.MILLISECONDS);
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private class DoTox implements Runnable {
        @Override
        public void run() {
            /* Praise the sun */
            try {
                toxSingleton.jTox.doTox();
            } catch (ToxException e) {
                Log.e(TAG, e.getError().toString());
                e.printStackTrace();
            }
        }
    }
}
