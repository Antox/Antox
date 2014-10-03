package im.tox.antox.data

object State {

  private var _chatActive: Boolean = false
  private var _activeKey: Option[String] = None

  def chatActive = _chatActive

  def chatActive(b: Boolean) = {
    require(b != null)
    _chatActive = b
  }

  def activeKey = _activeKey

  def activeKey(k: Option[String]) = {
    require(k != null)
    _activeKey = k
  }
}
