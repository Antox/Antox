package chat.tox.antox.av

object CallEndReason extends Enumeration {
  type CallEndReason = Value
  val Normal, Missed, Unanswered, Error = Value
}
