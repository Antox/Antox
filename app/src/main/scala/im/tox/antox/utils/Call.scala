package im.tox.antox.utils

import android.util.Log
import rx.lang.scala.Observable
import rx.lang.scala.Subscription
import im.tox.jtoxcore.ToxCodecSettings
import Call._

object Call {
  private val TAG = "im.tox.antox.utils.Call"
}

class Call(val id: Integer, 
  val codecSettings: ToxCodecSettings,
  val subscription: Subscription) {

  }
