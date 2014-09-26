package im.tox.antox.utils;

import android.text.format.Time;

import java.sql.Timestamp;
import java.util.Calendar;

/**
 * Created by ollie on 29/05/14.
 */
public class PrettyTimestamp {
    public static String prettyTimestamp(Timestamp t, boolean isChat) {
        //Set time in UTC time
        Time time = new Time("UTC");
        Calendar cal = Calendar.getInstance();
        cal.setTime(t);
        time.set(cal.get(Calendar.SECOND), cal.get(Calendar.MINUTE), cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH), cal.get(Calendar.YEAR));
        //Switch to device timezone
        time.switchTimezone(Time.getCurrentTimezone());
        Time startOfDay = new Time(Time.getCurrentTimezone());
        startOfDay.setToNow();
        startOfDay.hour = 0;
        startOfDay.minute = 0;
        startOfDay.second = 0;
        Time startOfYear = new Time(Time.getCurrentTimezone());
        startOfYear.setToNow();
        startOfYear.hour = 0;
        startOfYear.minute = 0;
        startOfYear.second = 0;
        startOfYear.monthDay = 0;
        startOfYear.month = 0;
        Time startOfTime = new Time(Time.getCurrentTimezone());
        startOfTime.set(0);
        if (t.equals(new Timestamp(0,0,0,0,0,0,0))) {
            return "";
        } else if (isChat) {
            if (time.after(startOfDay)) {
                return time.format("%H:%M");
            } else if (time.after(startOfYear)) {
                return time.format("%b %d, %H:%M");
            } else {
                return time.format("%b %d %Y, %H:%M");
            }
        } else {
            if (time.after(startOfDay)) {
                return time.format("%H:%M");
            } else if (time.after(startOfYear)) {
                return time.format("%b %d");
            } else {
                return time.format("%b %d %Y");
            }
        }
    }
}
