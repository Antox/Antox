package chat.tox.antox.callbacks

import android.content.Context
import android.util.Log
import chat.tox.antox.data.State
import chat.tox.antox.tox.{MessageHelper, ToxSingleton}
import chat.tox.antox.wrapper.ToxKey

object AntoxOnGroupInviteCallback {

}

class AntoxOnGroupInviteCallback(private var ctx: Context) /* extends GroupInviteCallback */ {

  def groupInvite(friendNumber: Int, inviteData: Array[Byte]): Unit = {
    val db = State.db
    val inviter = ToxSingleton.getAntoxFriend(friendNumber).get
    if (db.isContactBlocked(inviter.getKey)) return

    val inviteKeyLength = 32
    val key = new ToxKey(inviteData.slice(0, inviteKeyLength))
    Log.d("GroupInviteCallback", "invite key is " + key)
    db.addGroupInvite(key, inviter.getName, inviteData)

    Log.d("GroupInviteCallback", "")
    MessageHelper.createRequestNotification(None, ctx)
  }
}
