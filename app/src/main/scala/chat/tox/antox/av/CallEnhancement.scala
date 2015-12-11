package chat.tox.antox.av

import rx.lang.scala.subscriptions.CompositeSubscription

trait CallEnhancement {
  def call: Call

  val subscriptions: CompositeSubscription = CompositeSubscription()
}
