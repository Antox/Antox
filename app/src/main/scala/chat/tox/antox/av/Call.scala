package chat.tox.antox.av

import rx.lang.scala.Subscription

object Call {
  private val TAG = this.getClass.getSimpleName
}

class Call(val id: Integer, 
  val audioBitRate: Int,
  val videoBitRate: Int,
  val subscription: Subscription) {
    val playAudio = new PlayAudio()
  }
