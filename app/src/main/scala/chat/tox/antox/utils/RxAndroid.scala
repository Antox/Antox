package chat.tox.antox.utils

import android.content.{Context, Intent, IntentFilter}
import android.os.Handler
import rx.lang.scala.Observable

object RxAndroid {

  /**
    * Create Observable that wraps BroadcastReceiver and emits received intents.
    *
    * @param filter Selects the Intent broadcasts to be received.
    */
  def fromBroadcast(context: Context, filter: IntentFilter): Observable[Intent] = {
    Observable.create(new OnSubscribeBroadcastRegister(context, filter, null, null).call)
  }

  /**
    * Create Observable that wraps BroadcastReceiver and emits received intents.
    *
    * @param filter              Selects the Intent broadcasts to be received.
    * @param broadcastPermission String naming a permissions that a
    *                            broadcaster must hold in order to send an Intent to you.  If null,
    *                            no permission is required.
    * @param schedulerHandler    Handler identifying the thread that will receive
    *                            the Intent.  If null, the main thread of the process will be used.
    */
  def fromBroadcast(context: Context, filter: IntentFilter, broadcastPermission: String, schedulerHandler: Handler): Observable[Intent] = {
    Observable.create(new OnSubscribeBroadcastRegister(context, filter, broadcastPermission, schedulerHandler).call)
  }
}
