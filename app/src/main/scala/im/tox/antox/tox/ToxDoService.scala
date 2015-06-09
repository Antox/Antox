
package im.tox.antox.tox

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.preference.PreferenceManager
import android.util.Log

class ToxDoService extends Service() {

  private var serviceThread: Thread = _

  private var keepRunning: Boolean = true

  private val fastIteartion = 50
  private val slowIteration = 1000

  override def onCreate() {
    if (!ToxSingleton.isInited) {
      ToxSingleton.initTox(getApplicationContext)
      Log.d("ToxDoService", "Initting ToxSingleton")
    }
    keepRunning = true
    val thisService = this
    val start = new Runnable() {

      override def run() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext)
        while (keepRunning) {

          if (!ToxSingleton.isToxConnected(preferences, thisService)) {
            try {
              Thread.sleep(10000)
            } catch {
              case e: Exception =>
            }
          } else {
            try {
              if (ToxSingleton.isTransferring)
                Thread.sleep(fastIteartion)
              else
                Thread.sleep(slowIteration)

              Log.d("ToxDoService", "Trasnferrring: " + ToxSingleton.isTransferring)

              ToxSingleton.tox.iteration()
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
    Log.d("ToxDoService", "onDestroy() called")
  }
}
