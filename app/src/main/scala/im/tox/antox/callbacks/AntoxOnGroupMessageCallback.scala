package im.tox.antox.callbacks

import android.app.{Notification, PendingIntent}
import android.content.{Context, Intent}
import android.preference.PreferenceManager
import android.support.v4.app.{NotificationCompat, TaskStackBuilder}
import android.util.Log
import im.tox.antox.R
import im.tox.antox.activities.MainActivity
import im.tox.antox.callbacks.AntoxOnMessageCallback._
import im.tox.antox.data.{AntoxDB, State}
import im.tox.antox.tox.{MessageHelper, ToxSingleton}
import im.tox.antox.utils.{Hex, Constants}
import im.tox.antox.wrapper.MessageType
import im.tox.tox4j.core.callbacks.{FriendMessageCallback}

class AntoxOnGroupMessageCallback(private var ctx: Context) /* extends GroupMessageCallback */ {

  //override
  def groupMessage(groupNumber: Int, peerNumber: Int, timeDelta: Int, message: Array[Byte]): Unit = {
    println("new group message callback for id " + ToxSingleton.getGroupList.getGroup(groupNumber).id)
    MessageHelper.handleGroupMessage(ctx, groupNumber, peerNumber, ToxSingleton.getGroupList.getGroup(groupNumber).id,
                                      new String(message, "UTF-8"), MessageType.GROUP_PEER)
  }
}
