package im.tox.antox.callbacks

import android.content.Context
import im.tox.antox.data.State
import im.tox.antox.tox.ToxSingleton
import im.tox.tox4j.core.callbacks.FileReceiveChunkCallback

object AntoxOnFileReceiveChunkCallback {

  private val TAG = "OnFileReceiveChunkCallback"
}

class AntoxOnFileReceiveChunkCallback(private var ctx: Context) extends FileReceiveChunkCallback {

  override def fileReceiveChunk(friendNumber: Int, fileNumber: Int, position: Long, data: Array[Byte]): Unit = {
    val key = ToxSingleton.getAntoxFriend(friendNumber).get.getKey
    val size = State.transfers.get(key, fileNumber).get.size
    
    if (position == size) {
      State.transfers.fileFinished(key, fileNumber, ctx)
      ToxSingleton.updateMessages(ctx)
    } else {
      State.transfers.receiveFileData(key, fileNumber, data, ctx)
    }
  }
}
