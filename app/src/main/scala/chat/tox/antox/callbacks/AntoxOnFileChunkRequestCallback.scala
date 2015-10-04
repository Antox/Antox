package chat.tox.antox.callbacks

import android.content.Context
import android.util.Log
import chat.tox.antox.data.State
import chat.tox.antox.tox.ToxSingleton
import chat.tox.antox.transfer.FileStatus
import chat.tox.antox.utils.AntoxLog
import im.tox.tox4j.core.callbacks.FileChunkRequestCallback

class AntoxOnFileChunkRequestCallback(private var ctx: Context) extends FileChunkRequestCallback[Unit] {

  override def fileChunkRequest(friendNumber: Int, fileNumber: Int, position: Long, length: Int)(state: Unit): Unit = {
    val mFriend = ToxSingleton.getAntoxFriend(friendNumber)
    val mTransfer = State.transfers.get(mFriend.get.key, fileNumber)

    mTransfer match {
      case Some(t) =>
        t.status = FileStatus.INPROGRESS
        mFriend.foreach(friend => {
          if (length <= 0) {
            State.transfers.fileFinished(friend.key, t.fileNumber, ctx)
            State.db.clearFileNumber(friend.key, fileNumber)
          } else {
            val reset = position < t.progress
            val data = t.readData(reset, length)
            data match {
              case Some(d) =>
                ToxSingleton.tox.fileSendChunk(friend.getFriendNumber, fileNumber, t.progress, d)
                if (!reset) t.addToProgress(t.progress + length)
              case None =>
            }
          }

        })
      case None => AntoxLog.debug("Can't find file transfer")
    }
  }
}
