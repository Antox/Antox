package im.tox.antox.utils

import android.text.format.Time
import java.sql.Timestamp
import java.util.Calendar
//remove if not needed
import scala.collection.JavaConversions._

object PrettyTimestamp {

  def prettyTimestamp(t: Timestamp, isChat: Boolean): String = {
    val time = new Time("UTC")
    val cal = Calendar.getInstance
    cal.setTime(t)
    time.set(cal.get(Calendar.SECOND), cal.get(Calendar.MINUTE), cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.DAY_OF_MONTH),
      cal.get(Calendar.MONTH), cal.get(Calendar.YEAR))
    time.switchTimezone(Time.getCurrentTimezone)
    val startOfDay = new Time(Time.getCurrentTimezone)
    startOfDay.setToNow()
    startOfDay.hour = 0
    startOfDay.minute = 0
    startOfDay.second = 0
    val startOfYear = new Time(Time.getCurrentTimezone)
    startOfYear.setToNow()
    startOfYear.hour = 0
    startOfYear.minute = 0
    startOfYear.second = 0
    startOfYear.monthDay = 0
    startOfYear.month = 0
    val startOfTime = new Time(Time.getCurrentTimezone)
    startOfTime.set(0)
    if (t == new Timestamp(0, 0, 0, 0, 0, 0, 0)) {
      ""
    } else if (isChat) {
      if (time.after(startOfDay)) {
        time.format("%k:%M")
      } else if (time.after(startOfYear)) {
        time.format("%b %d, %k:%M")
      } else {
        time.format("%b %d %Y, %k:%M")
      }
    } else {
      if (time.after(startOfDay)) {
        time.format("%k:%M")
      } else if (time.after(startOfYear)) {
        time.format("%b %d")
      } else {
        time.format("%b %d %Y")
      }
    }
  }
}
