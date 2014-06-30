package im.tox.antox.utils;

import java.sql.Timestamp;
import java.text.DateFormat;
import android.text.format.Time;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by ollie on 29/05/14.
 */
public class PrettyTimestamp {
    public static String prettyTimestamp(Timestamp t) {
        Time time = new Time();
        time.set(t.getTime());
        Time startOfDay = new Time();
        startOfDay.setToNow();
        startOfDay.hour = 0;
        startOfDay.minute = 0;
        startOfDay.second = 0;
        Time startOfYear = new Time();
        startOfYear.setToNow();
        startOfYear.hour = 0;
        startOfYear.minute = 0;
        startOfYear.second = 0;
        startOfYear.monthDay = 0;
        startOfYear.month = 0;
        Time startOfTime = new Time();
        startOfTime.set(0);
        if (time.equals(startOfTime)) {
            return "";
        } else if (time.after(startOfDay)) {
            return time.format("%H:%M");
        } else if (time.after(startOfYear)) {
            return time.format("%b %d");
        } else {
            return time.format("%b %d %Y");
        }
    }

    public static String prettyChatTimestamp(Timestamp t) {
        Time time = new Time();
        time.set(t.getTime());
        Time startOfDay = new Time();
        startOfDay.setToNow();
        startOfDay.hour = 0;
        startOfDay.minute = 0;
        startOfDay.second = 0;
        Time startOfYear = new Time();
        startOfYear.setToNow();
        startOfYear.hour = 0;
        startOfYear.minute = 0;
        startOfYear.second = 0;
        startOfYear.monthDay = 0;
        startOfYear.month = 0;
        Time startOfTime = new Time();
        startOfTime.set(0);
        if (time.equals(startOfTime)) {
            return "";
        } else if (time.after(startOfDay)) {
            return time.format("%H:%M");
        } else if (time.after(startOfYear)) {
            return time.format("%b %d, %H:%M");
        } else {
            return time.format("%b %d %Y, %H:%M");
        }
    }
}
