package im.tox.antox.callbacks

import android.content.Context
import im.tox.antox.data.State
import android.util.Log
import im.tox.antox.callbacks.AntoxOnFileReceiveChunkCallback._
import im.tox.antox.tox.{Reactive, ToxSingleton}
import im.tox.antox.utils.{FileStatus, AntoxFriend}
import im.tox.tox4j.core.callbacks.{FileRequestChunkCallback, FileReceiveChunkCallback}

object AntoxOnFileRequestChunkCallback {

  private val TAG = "OnFileRequestChunkCallback"
}

class AntoxOnFileRequestChunkCallback(private var ctx: Context) extends FileRequestChunkCallback {

  override def fileRequestChunk(friendNumber: Int, fileNumber: Int, position: Long, length: Int): Unit = {
    val mFriend = ToxSingleton.getAntoxFriend(friendNumber)
    val mTransfer = State.transfers.get(mFriend.get.getClientId, fileNumber)

    mTransfer match {
      case Some(t) =>
        t.status = FileStatus.INPROGRESS
        mFriend.foreach(friend => {
          println("progress " + t.progress + " assumed progress " + position)
          if (length <= 0) {
            State.db.clearFileNumber(friend.getClientId, fileNumber)
            ToxSingleton.fileFinished(friend.getClientId, t.fileNumber, ctx) //make this on length 0 or whatever TODO
          } else {
            val reset = if (position < t.progress) true else false
            val data = t.readData(reset, length)
            data match {
              case Some(d) =>
                ToxSingleton.tox.fileSendChunk(friend.getFriendnumber, fileNumber, d)
                if (!reset) t.addToProgress(t.progress + length)
              case None =>
            }
          }

        })
      case None => Log.d("OnFileRequestChunkCallback", "Can't find file transfer")
    }
  }
}
