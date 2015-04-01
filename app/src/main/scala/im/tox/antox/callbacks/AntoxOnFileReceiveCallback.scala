package im.tox.antox.callbacks

import android.content.Context
import android.util.Log
import im.tox.antox.utils.Constants
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.wrapper.FileKind
import im.tox.tox4j.core.callbacks.FileReceiveCallback
import im.tox.tox4j.core.enums.ToxFileKind

object AntoxOnFileReceiveCallback {
  private val TAG = "OnFileReceiveCallback"
}

class AntoxOnFileReceiveCallback(ctx: Context) extends FileReceiveCallback {
  override def fileReceive(friendNumber: Int, fileNumber: Int, kind: Int, fileSize: Long, filename: Array[Byte]): Unit = {
    if (kind == ToxFileKind.AVATAR && fileSize > Constants.MAX_AVATAR_SIZE) return

    val replaceExisting = kind == ToxFileKind.AVATAR
    val name = if (kind == ToxFileKind.AVATAR) ToxSingleton.getAntoxFriend(friendNumber).get.key else new String(filename)
    val key = ToxSingleton.getAntoxFriend(friendNumber).get.getKey

    ToxSingleton.fileSendRequest(key,
      fileNumber, name, FileKind(kind), fileSize, replaceExisting, ctx)

    if (kind == ToxFileKind.AVATAR) ToxSingleton.acceptFile(key, fileNumber, ctx)
  }
}
