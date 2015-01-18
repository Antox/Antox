package im.tox.antox.callbacks

import android.content.Context
import im.tox.antox.data.State
import android.util.Log
import im.tox.antox.callbacks.AntoxOnFileReceiveChunkCallback._
import im.tox.antox.tox.{Reactive, ToxSingleton}
import im.tox.antox.utils.AntoxFriend
import im.tox.tox4j.core.callbacks.FileReceiveChunkCallback

object AntoxOnFileReceiveChunkCallback {

  private val TAG = "OnFileReceiveChunkCallback"
}

class AntoxOnFileReceiveChunkCallback(private var ctx: Context) extends FileReceiveChunkCallback {

  override def fileReceiveChunk(friendNumber: Int, fileNumber: Int, position: Long, data: Array[Byte]): Unit = {
    val address = ToxSingleton.getAntoxFriend(friendNumber).get.getAddress
    val size = State.transfers.get(address, fileNumber).get.size
    println("file data received at pos " + position + " out of " + size + " with data length " + data.length)
    
    if (position == size) {
      ToxSingleton.fileFinished(address, fileNumber, ctx)
      Reactive.updatedMessages.onNext(true)
    } else {
      ToxSingleton.receiveFileData(address, fileNumber, data, ctx)
    }
  }
}
