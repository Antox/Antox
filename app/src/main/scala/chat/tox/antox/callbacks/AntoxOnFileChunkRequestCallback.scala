package chat.tox.antox.callbacks

import android.content.Context
import chat.tox.antox.data.State
import chat.tox.antox.tox.ToxSingleton
import chat.tox.antox.transfer.FileStatus
import chat.tox.antox.utils.AntoxLog
import chat.tox.antox.wrapper.FriendInfo

class AntoxOnFileChunkRequestCallback(private var ctx: Context) {

  def fileChunkRequest(friendInfo: FriendInfo, fileNumber: Int, position: Long, length: Int)(state: Unit): Unit = {
    val mTransfer = State.transfers.get(friendInfo.key, fileNumber)


    mTransfer match {
      case Some(t) =>
        t.status = FileStatus.IN_PROGRESS
        if (length <= 0) {
          State.transfers.fileFinished(friendInfo.key, t.fileNumber, ctx)
          State.db.clearFileNumber(friendInfo.key, fileNumber)
        } else {
          val data = t.readData(position, t.progress, length)
          data match {
            case Some(d) =>
              try {
                ToxSingleton.tox.fileSendChunk(friendInfo.key, fileNumber, position, d)
              } catch {
                case e: Exception =>
                  e.printStackTrace()
              }
              t.addToProgress(position + length)
            case None =>
          }
        }

      case None => AntoxLog.debug("Can't find file transfer")
    }
  }
}
