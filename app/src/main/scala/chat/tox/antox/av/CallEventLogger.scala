package chat.tox.antox.av

import android.content.Context
import chat.tox.antox.data.{State, CallEventKind}
import chat.tox.antox.tox.ToxSingleton
import chat.tox.antox.utils.TimestampUtils
import chat.tox.antox.wrapper.FriendKey
import im.tox.tox4j.core.data.ToxNickname
import rx.lang.scala.subscriptions.CompositeSubscription

case class CallEventLogger(call: Call, context: Context) {
  var subscriptions: CompositeSubscription = CompositeSubscription()

  /**
   * Log a call event to the db.
   *
   * @param callEventKind kind of call event
   * @param extraInfo string appended to the standard call event message
   */
  def addCallEvent(callEventKind: CallEventKind, extraInfo: String = ""): Unit = {
    val (senderKey, senderName) =
      if (call.incoming) {
        val senderKey = call.contactKey // if the call is incoming, the sender key will always be contactKey
        val senderInfo = State.db.getFriendInfo(senderKey.asInstanceOf[FriendKey])
        val senderName = ToxNickname.unsafeFromValue(senderInfo.getDisplayName.getBytes)
        (senderKey, senderName)
      } else {
        (ToxSingleton.tox.getSelfKey, ToxSingleton.tox.getName)
      }

    val message = callEventKind.message(context) + extraInfo

    State.db.addCallEventMessage(call.contactKey, senderKey, senderName, message, State.isChatActive(call.contactKey), callEventKind)
  }

  def startLogging(): Unit = {
    subscriptions +=
      call.ringingObservable.subscribe(ringing => {
        if (ringing) {
          if (call.incoming) {
            addCallEvent(CallEventKind.Incoming)
          } else {
            addCallEvent(CallEventKind.Outgoing)
          }
        } else {
          addCallEvent(CallEventKind.Answered)
        }
      })

    subscriptions +=
      call.callEndedObservable.subscribe(reason => {
        import CallEndReason._

        reason match {
          case Normal | Error =>
            val duration = TimestampUtils.formatDuration(call.duration.toSeconds)
            addCallEvent(CallEventKind.Ended, s" ($duration)")
          case Missed =>
            addCallEvent(CallEventKind.Missed)
          case Unanswered =>
            addCallEvent(CallEventKind.Unanswered)
        }
      })
  }

  def stopLogging(): Unit = {
    subscriptions.unsubscribe()
  }
}
