package chat.tox.antox;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.multidex.MultiDex;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import chat.tox.antox.data.State;

/**
 * Created by zoff99 on 05.01.2017.
 */
public class MainApplication extends Application
{
    String last_stack_trace_as_string = "";
    int i = 0;
    int crashes = 0;
    long last_crash_time = 0L;
    long prevlast_crash_time = 0L;
    int randnum = -1;


    @Override
    public void onCreate()
    {
        randnum = (int) (Math.random() * 1000d);

        System.out.println("MainApplication:" + randnum + ":" + "onCreate");
        super.onCreate();

        crashes = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext()).getInt("crashes", 0);

        if (crashes > 10000)
        {
            crashes = 0;
            PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext()).edit().putInt("crashes", crashes).commit();
        }

        System.out.println("MainApplication:" + randnum + ":" + "crashes[load]=" + crashes);
        last_crash_time = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext()).getLong("last_crash_time", 0);
        System.out.println("MainApplication:" + randnum + ":" + "last_crash_time[load]=" + last_crash_time);
        prevlast_crash_time = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext()).getLong("prevlast_crash_time", 0);
        System.out.println("MainApplication:" + randnum + ":" + "prevlast_crash_time[load]=" + prevlast_crash_time);

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler()
        {
            @Override
            public void uncaughtException(Thread thread, Throwable e)
            {
                handleUncaughtException(thread, e);
            }
        });

    }

    @Override
    protected void attachBaseContext(Context base)
    {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    private String grabLogcat()
    {
        try
        {
            // grep -r 'Log\.' *|sed -e 's#^.*Log..("##'|grep -v TAG|sed -e 's#",.*$##'|sort |uniq

            final Process process = Runtime.getRuntime().exec("logcat -d -v threadtime");

            final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            final StringBuilder log = new StringBuilder();
            final String separator = System.getProperty("line.separator");

            String line;
            while ((line = bufferedReader.readLine()) != null)
            {
                log.append(line);
                log.append(separator);
            }

            if ((log.length() < 100) || (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT))
            {
                // some problems with the params?
                final Process process2 = Runtime.getRuntime().exec("logcat -d");
                final BufferedReader bufferedReader2 = new BufferedReader(new InputStreamReader(process2.getInputStream()));
                final StringBuilder log2 = new StringBuilder();

                String line2;
                while ((line2 = bufferedReader2.readLine()) != null)
                {
                    log2.append(line2);
                    log2.append(separator);
                }

                return log2.toString();
            }
            else
            {
                return log.toString();
            }
        }
        catch (IOException ioe)
        {
            System.out.println("MainApplication:" + randnum + ":" + "IOException when trying to read logcat.");
            return null;
        }
        catch (Exception e)
        {
            System.out.println("MainApplication:" + randnum + ":" + "Exception when trying to read logcat.");
            return null;
        }
    }


    void save_error_msg() throws IOException
    {

        String log_detailed = grabLogcat();

        try
        {
            // also save to crash file ----
            Calendar c = Calendar.getInstance();
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss");
            String formattedDate = df.format(c.getTime());
            File myDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Antox"); //new File("/sdcard/Antox/");
            myDir.mkdirs();
            File myFile = new File(myDir.getAbsolutePath() + "/crash_" + formattedDate + ".txt");
            System.out.println("MainApplication:" + randnum + ":" + "crash file=" + myFile.getAbsolutePath());
            myFile.createNewFile();
            FileOutputStream fOut = new FileOutputStream(myFile);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            myOutWriter.append("Errormesage:\n" + last_stack_trace_as_string + "\n\n===================================\n\n" + log_detailed);
            myOutWriter.close();
            fOut.close();
            // also save to crash file ----
        }
        catch (Exception e)
        {
        }
    }

    private void handleUncaughtException(Thread thread, Throwable e)
    {
        last_stack_trace_as_string = e.getMessage();
        boolean stack_trace_ok = false;

        try
        {
            Writer writer = new StringWriter();
            PrintWriter printWriter = new PrintWriter(writer);
            e.printStackTrace(printWriter);
            last_stack_trace_as_string = writer.toString();

            System.out.println("MainApplication:" + randnum + ":" + "stack trace ok");
            stack_trace_ok = true;
        }
        catch (Exception ee)
        {
        }
        catch (OutOfMemoryError ex2)
        {
            System.out.println("MainApplication:" + randnum + ":" + "stack trace *error*");
        }

        if (!stack_trace_ok)
        {
            try
            {
                last_stack_trace_as_string = Log.getStackTraceString(e);
                System.out.println("MainApplication:" + randnum + ":" + "stack trace ok (addon 1)");
                stack_trace_ok = true;
            }
            catch (Exception ee)
            {
            }
            catch (OutOfMemoryError ex2)
            {
                System.out.println("MainApplication:" + randnum + ":" + "stack trace *error* (addon 1)");
            }
        }

        crashes++;
        PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext()).edit().putInt("crashes", crashes).commit();

        try
        {
            save_error_msg();
        }
        catch (Exception ee)
        {
        }
        catch (OutOfMemoryError ex2)
        {
        }

        System.out.println("MainApplication:" + randnum + ":" + "crashes[set]=" + crashes);
        System.out.println("MainApplication:" + randnum + ":" + "?:" + (prevlast_crash_time + (60 * 1000)) + " < " + System.currentTimeMillis());
        System.out.println("MainApplication:" + randnum + ":" + "?:" + (System.currentTimeMillis() - (prevlast_crash_time + (60 * 1000))));


        // stop service or the app will restart with strange state! ---------
        try
        {
            State.shutdown(getBaseContext());
        }
        catch (Exception e2)
        {
            e2.printStackTrace();
        }
        // stop service or the app will restart with strange state! ---------

        ActivityManager am = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
        ComponentName componentInfo = taskInfo.get(0).topActivity;
        System.out.println("MainApplication:" + randnum + ":" + "componentInfo=" + componentInfo + " class=" + componentInfo.getClassName());

        try
        {
            State.MainToxService().onDestroy();
        }
        catch (Exception e3)
        {
            e3.printStackTrace();
        }

        Intent intent = new Intent(this, chat.tox.antox.CrashActivity.class);
        System.out.println("MainApplication:" + randnum + ":" + "xx1 intent(1)=" + intent);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        System.out.println("MainApplication:" + randnum + ":" + "xx1 intent(2)=" + intent);
        startActivity(intent);
        System.out.println("MainApplication:" + randnum + ":" + "xx2");
        android.os.Process.killProcess(android.os.Process.myPid());
        System.out.println("MainApplication:" + randnum + ":" + "xx3");
        System.exit(2);
        System.out.println("MainApplication:" + randnum + ":" + "xx4");

    }
}
