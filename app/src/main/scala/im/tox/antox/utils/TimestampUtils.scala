package im.tox.antox.utils

import java.sql.Timestamp
import java.util.Calendar

import android.text.format.Time

object TimestampUtils {

  val EMPTY_TIMESTAMP = new Timestamp(0, 0, 0, 0, 0, 0, 0)

  val startOfDay = new Time(Time.getCurrentTimezone)
  val startOfYear = new Time(Time.getCurrentTimezone)

  val startOfTime = new Time(Time.getCurrentTimezone)
  startOfTime.set(0)

  def prettyTimestamp(t: Timestamp, isChat: Boolean): String = {
    val cal = Calendar.getInstance
    cal.setTime(t)

    val time = new Time("UTC")
    time.set(cal.get(Calendar.SECOND), cal.get(Calendar.MINUTE), cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.DAY_OF_MONTH),
      cal.get(Calendar.MONTH), cal.get(Calendar.YEAR))
    time.switchTimezone(Time.getCurrentTimezone)

    startOfDay.setToNow()
    startOfDay.switchTimezone(Time.getCurrentTimezone)
    startOfDay.hour = 0
    startOfDay.minute = 0
    startOfDay.second = 0

    startOfYear.setToNow()
    startOfYear.switchTimezone(Time.getCurrentTimezone)
    startOfYear.hour = 0
    startOfYear.minute = 0
    startOfYear.second = 0
    startOfYear.monthDay = 0
    startOfYear.month = 0

    if (t == emptyTimestamp()) {
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

  /**
   * Returns an empty (all fields in constructor set to 0) timstamp.
   * DO NOT MODIFY THE TIMESTAMP
   *
   * @return an empty timestamp
   */
  def emptyTimestamp(): Timestamp = {
    EMPTY_TIMESTAMP
  }
}
