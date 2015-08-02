package chat.tox.antox.activities

import android.content.{Context, Intent}
import android.os.Bundle
import android.view.View
import chat.tox.antox.data.State
import chat.tox.antox.tox.MessageHelper
import chat.tox.antox.wrapper._
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
    val db = State.db
    titleSub = db.groupInfoList
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
    profile.putExtra("key", activeKey.toString)
    startActivity(profile)
  }

  override def onPause() = {
    super.onPause()
  }

  override def sendMessage(message: String, isAction: Boolean, context: Context): Unit = {
    MessageHelper.sendGroupMessage(context, activeKey, message, isAction, None)
  }

  override def setTyping(typing: Boolean): Unit = {
    //Not yet implemented in toxcore
  }
}
