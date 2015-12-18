
package chat.tox.antox.tox

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.preference.PreferenceManager
import chat.tox.antox.callbacks.ToxCallbackListener
import chat.tox.antox.utils.AntoxLog

class ToxService extends Service() {

  private var serviceThread: Thread = _

  private var keepRunning: Boolean = true

  private val connectionCheckInterval = 10000 //in ms

  override def onCreate() {
    if (!ToxSingleton.isInited) {
      ToxSingleton.initTox(getApplicationContext)
      AntoxLog.debug("Initting ToxSingleton")
    }

    keepRunning = true
    val thisService = this
    val start = new Runnable() {

      override def run() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext)
        val toxCallbackListener = new ToxCallbackListener(thisService)

        while (keepRunning) {
          if (!ToxSingleton.isToxConnected(preferences, thisService)) {
            try {
              Thread.sleep(connectionCheckInterval)
            } catch {
              case e: Exception =>
            }
          } else {
            try {
              Thread.sleep(ToxSingleton.interval)
              ToxSingleton.tox.iterate(toxCallbackListener)
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
    ToxSingleton.save()
    ToxSingleton.isInited = false
    AntoxLog.debug("onDestroy() called for Tox service")
  }
}
