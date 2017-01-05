package chat.tox.antox;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.preference.PreferenceManager;
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

/**
 * Created by zoff99 on 05.01.2017.
 */
public class MainApplication extends Application
{
    static String last_stack_trace_as_string = "";
    static int i = 0;

    @Override
    public void onCreate()
    {
        System.out.println("MainApplication:" + "onCreate");
        super.onCreate();

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler()
        {
            @Override
            public void uncaughtException(Thread thread, Throwable e)
            {
                handleUncaughtException(thread, e);
            }
        });

    }

    private static String grabLogcat()
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
            System.out.println("MainApplication:" + "IOException when trying to read logcat.");
            return null;
        }
        catch (Exception e)
        {
            System.out.println("MainApplication:" + "Exception when trying to read logcat.");
            return null;
        }
    }


    void save_error_msg() throws IOException
    {

        String log_detailed = grabLogcat();

        PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext()).edit().putString("last_crash_text", last_stack_trace_as_string).commit();
        System.out.println("MainApplication:" + "save_error_msg=" + last_stack_trace_as_string + "\n" + log_detailed);

        try
        {
            // also save to crash file ----
            Calendar c = Calendar.getInstance();
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss");
            String formattedDate = df.format(c.getTime());
            // *TODO* hardcoded path --> bad!!
            File myDir = new File("/sdcard/Antox/");
            myDir.mkdirs();
            // *TODO* hardcoded path --> bad!!
            File myFile = new File("/sdcard/Antox/crash_" + formattedDate +".txt");
            System.out.println("MainApplication:" + "crash file=" + myFile.getAbsolutePath());
            myFile.createNewFile();
            FileOutputStream fOut = new FileOutputStream(myFile);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            myOutWriter.append(last_stack_trace_as_string + "\n" + log_detailed);
            myOutWriter.close();
            fOut.close();
            // also save to crash file ----
        }
        catch(Exception e)
        {
            // e.printStackTrace();
        }
    }

//    static void restore_error_msg(Context c)
//    {
//        last_stack_trace_as_string = PreferenceManager.getDefaultSharedPreferences(c).getString("last_crash_text", "");
//        System.out.println("MainApplication:" + "restore_error_msg=" + last_stack_trace_as_string);
//    }

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

            System.out.println("MainApplication:" + "stack trace ok");
            stack_trace_ok = true;
        }
        catch (Exception ee)
        {
        }
        catch (OutOfMemoryError ex2)
        {
            System.out.println("MainApplication:" + "stack trace *error*");
        }

        if (!stack_trace_ok)
        {
            try
            {
                last_stack_trace_as_string = Log.getStackTraceString(e);
                System.out.println("MainApplication:" + "stack trace ok (addon 1)");
                stack_trace_ok = true;
            }
            catch (Exception ee)
            {
            }
            catch (OutOfMemoryError ex2)
            {
                System.out.println("MainApplication:" + "stack trace *error* (addon 1)");
            }
        }

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

        // System.out.println("MainApplication:" + "handleUncaughtException" + " thread.name=" + thread.getName() + " thread.id=" + thread.getId() + " Ex=" + last_stack_trace_as_string.replace("\n", " xxx "));

        PendingIntent intent = PendingIntent.getActivity(getBaseContext(), 0, new Intent(getApplicationContext(), chat.tox.antox.activities.LoginActivity.class), Intent.FLAG_ACTIVITY_NEW_TASK);

        AlarmManager mgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 2000, intent); // restart app after 2 second delay

        System.exit(2);
    }
}
