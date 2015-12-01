package chat.tox.antox.callbacks

import android.content.Context
import chat.tox.antox.data.State
import chat.tox.antox.wrapper.FriendInfo

class AntoxOnFileRecvChunkCallback(private var ctx: Context) {

  def fileRecvChunk(friendInfo: FriendInfo, fileNumber: Int, position: Long, data: Array[Byte])(state: Unit): Unit = {
    val size = State.transfers.get(friendInfo.key, fileNumber).get.size
    
    if (position == size) {
      State.transfers.fileFinished(friendInfo.key, fileNumber, ctx)
    } else {
      State.transfers.receiveFileData(friendInfo.key, fileNumber, data, ctx)
    }
  }
}
