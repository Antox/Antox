package im.tox.antox.callbacks

import android.content.Context
import android.util.Log
import im.tox.antox.data.State
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.transfer.FileStatus
import im.tox.tox4j.core.callbacks.FileRequestChunkCallback

object AntoxOnFileRequestChunkCallback {

  private val TAG = "OnFileRequestChunkCallback"
}

class AntoxOnFileRequestChunkCallback(private var ctx: Context) extends FileRequestChunkCallback {

  override def fileRequestChunk(friendNumber: Int, fileNumber: Int, position: Long, length: Int): Unit = {
    val mFriend = ToxSingleton.getAntoxFriend(friendNumber)
    val mTransfer = State.transfers.get(mFriend.get.getKey, fileNumber)

    mTransfer match {
      case Some(t) =>
        t.status = FileStatus.INPROGRESS
        mFriend.foreach(friend => {
          if (length <= 0) {
            State.db.clearFileNumber(friend.getKey, fileNumber)
            State.transfers.fileFinished(friend.getKey, t.fileNumber, ctx)
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
      case None => Log.d("OnFileRequestChunkCallback", "Can't find file transfer")
    }
  }
}
