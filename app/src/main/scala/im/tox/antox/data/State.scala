package im.tox.antox.data

import im.tox.antox.utils.{CallList, FileTransferManager}

object State {

  private var _chatActive: Boolean = false
  private var _activeKey: Option[String] = None

  val transfers: FileTransferManager = new FileTransferManager()

  var db: AntoxDB = _

  def chatActive = _chatActive

  def chatActive(b: Boolean) = {
    _chatActive = b
  }

  def activeKey = _activeKey

  def activeKey(k: Option[String]) = {
    require(k != null)
    _activeKey = k
  }
}
