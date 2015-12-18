package chat.tox.antox.callbacks

import android.content.Context
import chat.tox.antox.data.State
import chat.tox.antox.utils.{AntoxLog, AntoxNotificationManager}
import chat.tox.antox.wrapper.{GroupKey, FriendInfo}

object AntoxOnGroupInviteCallback {

}

class AntoxOnGroupInviteCallback(private var ctx: Context) /* extends GroupInviteCallback */ {

  def groupInvite(inviterInfo: FriendInfo, inviteData: Array[Byte]): Unit = {
    val db = State.db
    if (db.isContactBlocked(inviterInfo.key)) return

    val inviteKeyLength = 32
    //val key = new GroupKey(inviteData.slice(0, inviteKeyLength))
    //db.addGroupInvite(key, inviterInfo.key, inviteData)

    AntoxLog.debug("New Group Invite")
    //AntoxNotificationManager.createRequestNotification(key, None, ctx)
  }
}
