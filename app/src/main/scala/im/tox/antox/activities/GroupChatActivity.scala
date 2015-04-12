package im.tox.antox.activities

import java.io.{File, IOException}
import java.util.Date

import android.app.{Activity, AlertDialog}
import android.content.{CursorLoader, DialogInterface, Intent, SharedPreferences}
import android.database.Cursor
import android.net.Uri
import android.os.{Build, Bundle, Environment}
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.support.v7.app.{ActionBar, ActionBarActivity}
import android.text.{Editable, TextWatcher}
import android.util.Log
import android.view.{Menu, MenuInflater, View}
import android.widget._
import im.tox.antox.transfer.FileDialog
import im.tox.antox.wrapper._
import im.tox.antox.R
import im.tox.antox.adapters.ChatMessagesAdapter
import im.tox.antox.data.AntoxDB
import im.tox.antox.tox.{MessageHelper, Reactive, ToxSingleton}
import im.tox.antox.utils.{Constants, IconColor}
import im.tox.tox4j.exceptions.ToxException
import rx.lang.scala.schedulers.{AndroidMainThreadScheduler, IOScheduler}
import rx.lang.scala.{Observable, Subscription}

import scala.collection.JavaConversions._

class GroupChatActivity extends GenericChatActivity {

  var photoPath: String = null

  override def onCreate(savedInstanceState: Bundle) = {
    super.onCreate(savedInstanceState)
    val extras: Bundle = getIntent.getExtras
    val key = extras.getString("key")
    activeKey = key
    val thisActivity = this

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

    if (mMessage.isDefined) {
      val key = activeKey
      messageBox.setText("")
      MessageHelper.sendGroupMessage(this, key, mMessage.get, None)
    }
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
