package chat.tox.antox.av

import android.content.Context
import android.media.AudioManager
import chat.tox.antox.data.State
import chat.tox.antox.tox.ToxService
import chat.tox.antox.utils.AntoxNotificationManager
import rx.lang.scala.Subscription

import scala.util.Try

class CallService(toxService: ToxService) {

  private var callAddedSubscription: Option[Subscription] = None
  private var callNotificationSubscription: Option[Subscription] = None

  def start(): Unit = {
    val audioManager = toxService.getSystemService(Context.AUDIO_SERVICE).asInstanceOf[AudioManager]

    callAddedSubscription =
      Some(State.callManager.callAddedObservable.subscribe(call => {
        call.enhancements ++= Seq(
          new CallEventLogger(call, toxService),
          new CallSounds(call, toxService),
          new AudioStateManager(call, audioManager)
        )
      }))

    // This is here to keep this service alive in the background when in a call.
    // In order to do this, we add a notification of the ongoing call and set
    // the service in the foreground when an active call is added.
    // When the call stops the notification is removed and the service is allowed to die.
    callNotificationSubscription =
      Some(State.callManager.activeCallObservable
        .combineLatestWith(State.db.friendInfoList)((activeCalls, friendInfoList) => (activeCalls, friendInfoList))
        .subscribe(tuple => {
          val (activeCalls, friendInfoList) = tuple

          // take the newest call TODO: fix
          val maybeCall = findNewestCall(activeCalls)
          maybeCall match {
            case Some(call) =>
              // take the friend info as info in the notification may be outdated
              val maybeFriend = friendInfoList.find(_.key == call.contactKey)
              maybeFriend.foreach { friend =>
                val callNotification = new OngoingCallNotification(toxService, friend, call)
                toxService.startForeground(callNotification.id, callNotification.build())
              }
            case None =>
              // if there is no active call stop foreground and remove the notification
              toxService.stopForeground(true)
          }
        }))

    AntoxNotificationManager.startMonitoringCalls(toxService, State.db)
  }

  def findNewestCall(activeCalls: Iterable[Call]): Option[Call] = {
    Try(activeCalls.minBy(_.duration)).toOption
  }

  def destroy(): Unit = {
    toxService.stopForeground(true)
    toxService.stopSelf()

    AntoxNotificationManager.stopMonitoringCalls()
    callAddedSubscription.foreach(_.unsubscribe())
    callNotificationSubscription.foreach(_.unsubscribe())
  }
}
