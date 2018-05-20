
package chat.tox.antox.tox

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.preference.PreferenceManager
import chat.tox.antox.av.CallService
import chat.tox.antox.callbacks.{AntoxOnSelfConnectionStatusCallback, ToxCallbackListener, ToxavCallbackListener}
import chat.tox.antox.utils.AntoxLog
import im.tox.tox4j.core.enums.ToxConnection
import im.tox.tox4j.impl.jni.ToxJniLog
import rx.lang.scala.schedulers.AndroidMainThreadScheduler
import rx.lang.scala.{Observable, Subscription}
import scala.concurrent.duration._

class ToxService extends Service {

  private var serviceThread: Thread = _

  private var keepRunning: Boolean = true

  private val connectionCheckInterval = 10000 //in ms

  private val reconnectionIntervalSeconds = 60

  private var callService: CallService = _

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

        callService = new CallService(thisService)
        callService.start()

        val toxCallbackListener = new ToxCallbackListener(thisService)
        val toxAvCallbackListener = new ToxavCallbackListener(thisService)

        var reconnection: Subscription = null

        val connectionSubscription = AntoxOnSelfConnectionStatusCallback.connectionStatusSubject
          .observeOn(AndroidMainThreadScheduler())
          .distinctUntilChanged
          .subscribe(toxConnection => {
            if (toxConnection != ToxConnection.NONE) {
              if (reconnection != null && !reconnection.isUnsubscribed) {
                reconnection.unsubscribe()
              }
              AntoxLog.debug("Tox connected. Stopping reconnection")
            } else {
              reconnection = Observable
                .interval(reconnectionIntervalSeconds seconds)
                .subscribe(x => {
                    AntoxLog.debug("Reconnecting")
                    Observable[Boolean](_ => ToxSingleton.bootstrap(getApplicationContext)).subscribe()
                  })
              AntoxLog.debug(s"Tox disconnected. Scheduled reconnection every $reconnectionIntervalSeconds seconds")
            }
          })

        var ticks = 0
        while (keepRunning) {
          if (!ToxSingleton.isToxConnected(preferences, thisService)) {
            try {
              Thread.sleep(connectionCheckInterval)
            } catch {
              case e: Exception =>
            }
          } else {
            try {
              ToxSingleton.tox.iterate(toxCallbackListener)
              ToxSingleton.toxAv.iterate(toxAvCallbackListener)

              if (ticks % 100 == 0) {
                println(ToxJniLog().entries.filter(_.name == "tox4j_video_receive_frame_cb").map(_.elapsedNanos).toList.map(nanos => s" elapsed nanos video cb: $nanos").mkString("\n"))
              }

              Thread.sleep(Math.min(ToxSingleton.interval, ToxSingleton.toxAv.interval))
              ticks += 1
            } catch {
              case e: Exception =>
                e.printStackTrace()
            }
          }
        }

        connectionSubscription.unsubscribe()
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
    serviceThread.join()

    callService.destroy()

    ToxSingleton.save()
    ToxSingleton.isInited = false
    ToxSingleton.tox.close()
    AntoxLog.debug("onDestroy() called for Tox service")
  }
}
