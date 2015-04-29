package im.tox.antox.activities

import android.content.Intent
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
    val extras: Bundle = getIntent.getExtras
    val key = extras.getString("key")
    activeKey = key

    isTypingBox = this.findViewById(R.id.isTyping).asInstanceOf[TextView]
    statusTextBox = this.findViewById(R.id.chatActiveStatus).asInstanceOf[TextView]

    messageBox = this.findViewById(R.id.yourMessage).asInstanceOf[EditText]

    val b = this.findViewById(R.id.sendMessageButton)
    b.setOnClickListener(new View.OnClickListener() {
      override def onClick(v: View) {
        sendMessage()
      }
    })

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
        .filter(groupInfo => groupInfo.id == id)
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

  private def sendMessage() {
    Log.d(TAG, "sendMessage")
    val mMessage = validateMessageBox()
    val key = activeKey

    mMessage.foreach(message => {
      messageBox.setText("")
      MessageHelper.sendGroupMessage(this, key, message, None)
    })
  }

  def onClickVoiceCallFriend(v: View){
    println("This button (Audio Call) doesn't work yet.")
  }

  def onClickVideoCallFriend(v: View): Unit = {
    println("This button (Video Call) doesn't work yet.")
  }

  def onClickInfo(v: View): Unit = {
    val profile = new Intent(this, classOf[GroupProfileActivity])
    profile.putExtra("key", activeKey)
    startActivity(profile)
  }

  override def onPause() = {
    super.onPause()
  }
}
