package im.tox.antox.callbacks

import android.content.Context
import im.tox.antox.data.{AntoxDB, State}
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.utils.Constants
import im.tox.antox.wrapper.FileKind
import im.tox.antox.wrapper.FileKind.AVATAR
import im.tox.tox4j.core.callbacks.FileReceiveCallback
import im.tox.tox4j.core.enums.ToxFileControl

object AntoxOnFileReceiveCallback {
  private val TAG = "OnFileReceiveCallback"
}

class AntoxOnFileReceiveCallback(ctx: Context) extends FileReceiveCallback {
  override def fileReceive(friendNumber: Int, fileNumber: Int, toxFileKind: Int, fileSize: Long, filename: Array[Byte]): Unit = {
    val kind: FileKind = FileKind.fromToxFileKind(toxFileKind)
    val key = ToxSingleton.getAntoxFriend(friendNumber).get.key

    val name =
      if (kind == FileKind.AVATAR) {
        key
      } else {
        new String(filename)
      }

    if (kind == FileKind.AVATAR) {
      if (fileSize > Constants.MAX_AVATAR_SIZE) {
        return
      } else if (fileSize == 0) {
        ToxSingleton.tox.fileControl(friendNumber, fileNumber, ToxFileControl.CANCEL)
        ToxSingleton.getAntoxFriend(friendNumber).get.deleteAvatar()
        val db = new AntoxDB(ctx)
        db.updateFriendAvatar(key, "")
        db.close()
        ToxSingleton.updateContactsList(ctx)
        ToxSingleton.updateMessages(ctx)
        return
      }

      val fileId = ToxSingleton.tox.fileGetFileId(friendNumber, fileNumber).toString
      val avatarFile = AVATAR.getAvatarFile(name, ctx).orNull

      if (avatarFile != null) {
        val storedFileId = ToxSingleton.tox.hash(avatarFile).orNull
        if (fileId.equals(storedFileId)) {
          ToxSingleton.tox.fileControl(friendNumber, fileNumber, ToxFileControl.CANCEL)
          return
        }
      }
    }

    State.transfers.fileSendRequest(key,
      fileNumber, name, kind, fileSize, kind.replaceExisting, ctx)

    if (kind.autoAccept) State.transfers.acceptFile(key, fileNumber, ctx)
  }
}
