package im.tox.antox.callbacks

import android.content.Context
import android.util.Log
import im.tox.antox.tox.ToxSingleton
import im.tox.tox4j.core.callbacks.FileReceiveCallback
import im.tox.tox4j.core.enums.ToxFileKind

object AntoxOnFileReceiveCallback {
  private val TAG = "OnFileReceiveCallback"
}

class AntoxOnFileReceiveCallback(ctx: Context) extends FileReceiveCallback {
  override def fileReceive(friendNumber: Int, fileNumber: Int, kind: ToxFileKind, fileSize: Long, filename: Array[Byte]): Unit = {
    if (kind == ToxFileKind.DATA) {
      ToxSingleton.fileSendRequest(ToxSingleton.getAntoxFriend(friendNumber).get.getAddress, 
        fileNumber, new String(filename), fileSize, ctx)
    }
  }
}
