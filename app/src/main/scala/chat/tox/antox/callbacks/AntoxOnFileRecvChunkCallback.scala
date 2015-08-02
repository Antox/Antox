package chat.tox.antox.callbacks

import android.content.Context
import chat.tox.antox.data.State
import chat.tox.antox.tox.ToxSingleton
import im.tox.tox4j.core.callbacks.FileRecvChunkCallback

class AntoxOnFileRecvChunkCallback(private var ctx: Context) extends FileRecvChunkCallback[Unit] {

  override def fileRecvChunk(friendNumber: Int, fileNumber: Int, position: Long, data: Array[Byte])(state: Unit): Unit = {
    val key = ToxSingleton.getAntoxFriend(friendNumber).get.getKey
    val size = State.transfers.get(key, fileNumber).get.size
    
    if (position == size) {
      State.transfers.fileFinished(key, fileNumber, ctx)
    } else {
      State.transfers.receiveFileData(key, fileNumber, data, ctx)
    }
  }
}
