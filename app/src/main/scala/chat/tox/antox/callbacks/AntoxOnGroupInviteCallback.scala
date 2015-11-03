package chat.tox.antox.callbacks

import android.content.Context
import android.util.Log
import chat.tox.antox.data.State
import chat.tox.antox.tox.{MessageHelper, ToxSingleton}
import chat.tox.antox.utils.{AntoxNotificationManager, GroupKey, AntoxLog}
import chat.tox.antox.wrapper.{FriendInfo, ToxKey}

object AntoxOnGroupInviteCallback {

}

class AntoxOnGroupInviteCallback(private var ctx: Context) /* extends GroupInviteCallback */ {

  def groupInvite(inviterInfo: FriendInfo, inviteData: Array[Byte]): Unit = {
    val db = State.db
    if (db.isContactBlocked(inviterInfo.key)) return

    val inviteKeyLength = 32
    val key = new GroupKey(inviteData.slice(0, inviteKeyLength))
    db.addGroupInvite(key, inviterInfo.name, inviteData)

    AntoxLog.debug("New Group Invite")
    AntoxNotificationManager.createRequestNotification(key, None, ctx)
  }
}
