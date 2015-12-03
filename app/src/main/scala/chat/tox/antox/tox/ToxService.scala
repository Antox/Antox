
package chat.tox.antox.tox

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.preference.PreferenceManager
import chat.tox.antox.av.OngoingCallNotification
import chat.tox.antox.data.State
import chat.tox.antox.utils.AntoxLog
import rx.lang.scala.Subscription

import scala.util.Try

class ToxService extends Service {

  private var serviceThread: Thread = _

  private var keepRunning: Boolean = true

  private val connectionCheckInterval = 10000 //in ms

  private var callNotificationSubscription: Option[Subscription] = None

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

        // This is here to keep this service alive in the background when in a call.
        // In order to do this, we add a notification of the ongoing call and set
        // the service in the foreground when an active call is added.
        // When the call stops the notification is removed and the service is allowed to die.
        callNotificationSubscription =
          Some(State.callManager.activeCallObservable
            .combineLatestWith(State.db.friendInfoList)((activeCalls, friendInfos) => (activeCalls, friendInfos))
            .subscribe(tuple => {
              val (activeCalls, friendInfos) = tuple

              // take the newest call TODO: fix
              val maybeCall = Try(activeCalls.minBy(_.duration)).toOption
              maybeCall match {
                case Some(call) =>
                  // take the friend info in case info in the notification is outdated
                  val maybeFriend = friendInfos.find(_.key == call.contactKey)
                  maybeFriend.foreach { friend =>
                    val callNotification = new OngoingCallNotification(thisService, friend, call)
                    startForeground(callNotification.id, callNotification.build())
                  }
                case None =>
                  // if there is no active call stop foreground and remove the notification
                  stopForeground(true)
              }
            }))

        while (keepRunning) {
          if (!ToxSingleton.isToxConnected(preferences, thisService)) {
            try {
              Thread.sleep(connectionCheckInterval)
            } catch {
              case e: Exception =>
            }
          } else {
            try {
              ToxSingleton.tox.iterate()
              ToxSingleton.toxAv.iterate()

              Thread.sleep(Math.min(ToxSingleton.interval, ToxSingleton.toxAv.interval))
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
    stopForeground(true)
    callNotificationSubscription.foreach(_.unsubscribe())
    ToxSingleton.save()
    ToxSingleton.isInited = false
    AntoxLog.debug("onDestroy() called for Tox service")
  }
}
