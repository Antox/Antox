package chat.tox.antox.utils

import android.content.{BroadcastReceiver, Context, Intent, IntentFilter}
import android.os.Handler
import rx.lang.scala.{Observer, Subscription}

final class OnSubscribeBroadcastRegister(context: Context,
                                         intentFilter: IntentFilter,
                                         broadcastPermission: String,
                                         schedulerHandler: Handler) {

  def call(subscriber: Observer[_ >: Intent]): Subscription = {
    val broadcastReceiver = new BroadcastReceiver {
      override def onReceive(context: Context, intent: Intent): Unit = {
        subscriber.onNext(intent)
      }
    }

    context.registerReceiver(broadcastReceiver, intentFilter, broadcastPermission, schedulerHandler)
    Subscription(context.unregisterReceiver(broadcastReceiver))
  }
}