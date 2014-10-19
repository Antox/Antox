package im.tox.antox.data

import im.tox.antox.utils.FileTransferManager
import im.tox.antox.utils.CallManager

object State {

  private var _chatActive: Boolean = false
  private var _activeKey: Option[String] = None

  val transfers: FileTransferManager = new FileTransferManager()
  val calls: CallManager = new CallManager()

  var db: AntoxDB = _

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
