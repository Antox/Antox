package im.tox.antox.activities

import android.content.{Context, Intent}
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget._
import im.tox.antox.tox.{MessageHelper, Reactive}
import im.tox.antox.wrapper._
import im.tox.antoxnightly.R
import rx.lang.scala.schedulers.{AndroidMainThreadScheduler, IOScheduler}

class GroupChatActivity extends GenericChatActivity {

  var photoPath: String = null

  override def onCreate(savedInstanceState: Bundle) = {
    super.onCreate(savedInstanceState)

    statusIconView.setVisibility(View.GONE)
  }

  override def onResume() = {
    super.onResume()
    val thisActivity = this
    titleSub = Reactive.groupInfoList
      .subscribeOn(IOScheduler())
      .observeOn(AndroidMainThreadScheduler())
      .subscribe(groupInfo => {
      val id = activeKey
      val mGroup: Option[GroupInfo] = groupInfo
        .filter(groupInfo => groupInfo.key == id)
        .headOption
      mGroup match {
        case Some(group) => {
          thisActivity.setDisplayName(group.getAliasOrName)
        }
        case None => {
          thisActivity.setDisplayName("")
        }
      }
    })
  }

  def onClickVoiceCallFriend(v: View): Unit = {}

  def onClickVideoCallFriend(v: View): Unit = {}

  def onClickInfo(v: View): Unit = {
    val profile = new Intent(this, classOf[GroupProfileActivity])
    profile.putExtra("key", activeKey)
    startActivity(profile)
  }

  override def onPause() = {
    super.onPause()
  }

  override def sendMessage(message: String, isAction: Boolean, activeKey: String, context: Context): Unit = {
    MessageHelper.sendGroupMessage(context, activeKey, message, isAction, None)
  }

  override def setTyping(typing: Boolean, activeKey: String): Unit = {
    //Not yet implemented in toxcore
  }
}
