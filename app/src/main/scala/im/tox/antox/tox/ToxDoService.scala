
package im.tox.antox.tox

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.IBinder
//import android.os.PowerManager.WakeLock
import android.os.PowerManager
import android.preference.PreferenceManager
import android.util.Log
//remove if not needed
import scala.collection.JavaConversions._

class ToxDoService extends Service() {

  private var serviceThread: Thread = _

  private var keepRunning: Boolean = true

  override def onCreate() {
    if (!ToxSingleton.isInited) {
      ToxSingleton.initTox(getApplicationContext)
      Log.d("ToxDoService", "Initting ToxSingleton")
    }
    keepRunning = true
    val start = new Runnable() {

      override def run() {
        while (keepRunning) {
          val preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext)
          val connManager = getSystemService(Context.CONNECTIVITY_SERVICE).asInstanceOf[ConnectivityManager]
          val wifiOnly = preferences.getBoolean("wifi_only", true)
          val mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
          if (wifiOnly && !mWifi.isConnected) {
            try {
              Thread.sleep(10000)
            } catch {
              case e: Exception => 
            }
          } else {
            try {
              Thread.sleep(ToxSingleton.jTox.doToxInterval())
              var powerManager = getSystemService(Context.POWER_SERVICE).asInstanceOf[PowerManager]
              var wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakelockTag")
              wakeLock.acquire()
              ToxSingleton.jTox.doTox()
              wakeLock.release()
            } catch {
              case e: Exception => 
            }
          }
        }
      }
    }
    serviceThread = new Thread(start)
    serviceThread.start()
  }

  override def onBind(intent: Intent): IBinder = null

  override def onStartCommand(intent: Intent, flags: Int, id: Int): Int = Service.START_STICKY

  override def onDestroy() {
    super.onDestroy()
    keepRunning = false
    serviceThread.interrupt()
    ToxSingleton.isInited = false
    Log.d("ToxDoService", "onDestroy() called")
  }
}
