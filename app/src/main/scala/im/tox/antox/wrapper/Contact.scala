package im.tox.antox.wrapper

trait Contact {
  def sendMessage(message: String): Int
  def sendAction(action: String): Int
}
