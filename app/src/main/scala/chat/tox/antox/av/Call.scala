package chat.tox.antox.av

import rx.lang.scala.Subscription

class Call(val id: Integer, 
  val audioBitRate: Int,
  val videoBitRate: Int,
  val subscription: Subscription) {
    val playAudio = new PlayAudio()
  }
