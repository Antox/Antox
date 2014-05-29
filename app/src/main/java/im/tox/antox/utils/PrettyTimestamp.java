package im.tox.antox.utils;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by ollie on 29/05/14.
 */
public class PrettyTimestamp {
    public static String prettyTimestamp(Timestamp t) {
        String tString = t.toString();

        try {
            //Set the date format.
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
            //Get the Date in UTC format.
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = dateFormat.parse(tString);

            //Adapt the date to the local timestamp.
            dateFormat.setTimeZone(TimeZone.getDefault());
            tString = dateFormat.format(date).toString();
        }
        catch (Exception e) {
            tString = t.toString();
        }

        java.util.Date date= new java.util.Date();
        Timestamp current = new Timestamp(date.getTime());
        String output;
        String month = "";
        if (current.toString().substring(0,10).equals(tString.substring(0,10))){
            output = tString.substring(11,16);
        } else if (tString.substring(0,10).equals("1899-12-31")) {
            output = "";
        } else {
            switch (Integer.parseInt(tString.substring(5,7))) {
                case 1:
                    month = "Jan";
                    break;
                case 2:
                    month = "Feb";
                    break;
                case 3:
                    month = "Mar";
                    break;
                case 4:
                    month = "Apr";
                    break;
                case 5:
                    month = "May";
                    break;
                case 6:
                    month = "Jun";
                    break;
                case 7:
                    month = "Jul";
                    break;
                case 8:
                    month = "Aug";
                    break;
                case 9:
                    month = "Sep";
                    break;
                case 10:
                    month = "Oct";
                    break;
                case 11:
                    month = "Nov";
                    break;
                case 12:
                    month = "Dec";
                    break;
            }
            output = month + " " + tString.substring(8,10);
        }
        return output;
    }
}
