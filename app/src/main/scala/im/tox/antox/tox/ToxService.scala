
package im.tox.antox.tox

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.preference.PreferenceManager
import android.util.Log
import im.tox.antox.activities.{ChatActivity, CallActivity}
import im.tox.antox.callbacks.AntoxOnCallCallback
import im.tox.antox.wrapper.Friend
import rx.Observer
import rx.lang.scala.schedulers.{AndroidMainThreadScheduler, IOScheduler}

class ToxService extends Service() {

  private var serviceThread: Thread = _

  private var keepRunning: Boolean = true

  override def onCreate() {
    if (!ToxSingleton.isInited) {
      ToxSingleton.initTox(getApplicationContext)
      Log.d("ToxService", "Initting ToxSingleton")
    }

    keepRunning = true
    val thisService = this
    val start = new Runnable() {

      override def run() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext)
        while (keepRunning) {
          if (1 == 0 && !ToxSingleton.isToxConnected(preferences, thisService)) {
            try {
              Thread.sleep(10000)
            } catch {
              case e: Exception =>
            }
          } else {
            try {
              ToxSingleton.tox.iterate()
              Thread.sleep(ToxSingleton.interval)
            } catch {
              case e: Exception =>
            }
          }
        }
      }
    }

    new Thread(new Runnable {
      override def run(): Unit = {
        while (keepRunning) {
          ToxSingleton.toxAv.iterate()
          Thread.sleep(0)
        }
      }
    }).start()

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
    Log.d("ToxService", "onDestroy() called")
  }
}
