package chat.tox.antox.callbacks

import android.content.Context
import chat.tox.antox.activities.ChatActivity
import chat.tox.antox.data.State
import chat.tox.antox.tox.ToxSingleton
import chat.tox.antox.utils.{AntoxNotificationManager, Constants}
import chat.tox.antox.wrapper.FileKind.AVATAR
import chat.tox.antox.wrapper.{FileKind, FriendInfo}
import im.tox.tox4j.core.data.ToxFilename
import im.tox.tox4j.core.enums.ToxFileControl

class AntoxOnFileRecvCallback(ctx: Context) {
  def fileRecv(friendInfo: FriendInfo,
               fileNumber: Int,
               toxFileKind: Int,
               fileSize: Long,
               filename: ToxFilename)(state: Unit): Unit = {
    val kind: FileKind = FileKind.fromToxFileKind(toxFileKind)

    val name =
      if (kind == FileKind.AVATAR) {
        friendInfo.key.toString
      } else {
        filename.toString
      }


    if (State.getBatterySavingMode()) {
      if (kind == FileKind.AVATAR) {
        // cancel all incoming Avatar FTs in battery saving mode
        ToxSingleton.tox.fileControl(friendInfo.key, fileNumber, ToxFileControl.CANCEL)
        return
      }
    }


    if (kind == FileKind.AVATAR) {
      if (fileSize > Constants.MAX_AVATAR_SIZE) {
        return
      } else if (fileSize == 0) {
        ToxSingleton.tox.fileControl(friendInfo.key, fileNumber, ToxFileControl.CANCEL)

        val db = State.db
        friendInfo.avatar.foreach(_.delete())
        db.updateFriendAvatar(friendInfo.key, None)
        return
      }

      val fileId = ToxSingleton.tox.fileGetFileId(friendInfo.key, fileNumber).toString
      val avatarFile = AVATAR.getAvatarFile(name, ctx).orNull

      if (avatarFile != null) {
        val storedFileId = ToxSingleton.tox.hash(avatarFile).orNull
        if (fileId.equals(storedFileId)) {
          ToxSingleton.tox.fileControl(friendInfo.key, fileNumber, ToxFileControl.CANCEL)
          return
        }
      }
    }

    val chatActive = State.isChatActive(friendInfo.key)
    State.transfers.fileIncomingRequest(friendInfo.key, friendInfo.name, chatActive, fileNumber, name, kind, fileSize, kind.replaceExisting, ctx)

    if (!chatActive) {
      val db = State.db
      try {
        val unreadCount = db.getUnreadCounts(friendInfo.key)
        AntoxNotificationManager.createMessageNotification(ctx, classOf[ChatActivity], friendInfo, new String("Incoming File ..."), unreadCount)
      }
      catch {
        case e: Exception => e.printStackTrace()
      }
    }



    if (kind.autoAccept) {
      State.transfers.acceptFile(friendInfo.key, fileNumber, ctx)
    }
    else {



      if (State.getAutoAcceptFt() == true) {
        State.transfers.acceptFile(friendInfo.key, fileNumber, ctx)
      }
      else {
      }
    }


  }
}
