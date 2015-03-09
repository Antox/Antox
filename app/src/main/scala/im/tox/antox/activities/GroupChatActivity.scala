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
import im.tox.antox.wrapper.{Message, UserStatus, FriendInfo}
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
    Log.d(TAG, "key = " + key)
    val preferences = PreferenceManager.getDefaultSharedPreferences(this)
    adapter = new ChatMessagesAdapter(this, getCursor, antoxDB.getMessageIds(key, preferences.getBoolean("action_messages", false)))
    displayNameView = this.findViewById(R.id.displayName).asInstanceOf[TextView]
    statusIconView = this.findViewById(R.id.icon)
    avatarActionView = this.findViewById(R.id.avatarActionView)
    avatarActionView.setOnClickListener(new View.OnClickListener() {
      override def onClick(v: View) {
        thisActivity.finish()
      }
    })
    chatListView = this.findViewById(R.id.chatMessages).asInstanceOf[ListView]
    chatListView.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_NORMAL)
    chatListView.setStackFromBottom(true)
    chatListView.setAdapter(adapter)
    chatListView.setOnScrollListener(new AbsListView.OnScrollListener() {

      override def onScrollStateChanged(view: AbsListView, scrollState: Int) {
        scrolling = !(scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE)
      }

      override def onScroll(view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {

      }

    })
    isTypingBox = this.findViewById(R.id.isTyping).asInstanceOf[TextView]
    statusTextBox = this.findViewById(R.id.chatActiveStatus).asInstanceOf[TextView]

    messageBox = this.findViewById(R.id.yourMessage).asInstanceOf[EditText]

    val b = this.findViewById(R.id.sendMessageButton)
    b.setOnClickListener(new View.OnClickListener() {
      override def onClick(v: View) {
        sendMessage()
      }
    })
  }

  override def onResume() = {
    super.onResume()
    val thisActivity = this
  }

  private def sendMessage() {
    Log.d(TAG, "sendMessage")
    if (messageBox.getText != null && messageBox.getText.toString.length() == 0) {
      return
    }

    //limit to 100 max length messages
    if (messageBox.getText.length() > MESSAGE_LENGTH_LIMIT) {
      Toast.makeText(this, getResources.getString(R.string.chat_message_too_long), Toast.LENGTH_LONG)
      return
    }

    var msg: String = null
    if (messageBox.getText != null) {
      msg = messageBox.getText.toString
    } else {
      msg = ""
    }

    val db = new AntoxDB(this)
    db.open(false)
    for (message: Message <- db.getMessageList(activeKey, true)) {
      println("message of type " + message.`type` + " with content " + message.message + " active key " + activeKey)
    }

    val key = activeKey
    messageBox.setText("")
    MessageHelper.sendGroupMessage(this, key, msg, None)
    db.close()
  }

  override def onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
    super.onActivityResult(requestCode, resultCode, data)
    if (resultCode == Activity.RESULT_OK) {
      if (requestCode == Constants.IMAGE_RESULT) {
        val uri = data.getData
        val filePathColumn = Array(MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DISPLAY_NAME)
        val loader = new CursorLoader(this, uri, filePathColumn, null, null, null)
        val cursor = loader.loadInBackground()
        if (cursor != null) {
          if (cursor.moveToFirst()) {
            val columnIndex = cursor.getColumnIndexOrThrow(filePathColumn(0))
            val filePath = cursor.getString(columnIndex)
            val fileNameIndex = cursor.getColumnIndexOrThrow(filePathColumn(1))
            val fileName = cursor.getString(fileNameIndex)
            try {
              ToxSingleton.sendFileSendRequest(filePath, this.activeKey, this)
            } catch {
              case e: Exception => e.printStackTrace()
            }
          }
        }
      }
      if (requestCode == Constants.PHOTO_RESULT) {
        if (photoPath != null) {
          ToxSingleton.sendFileSendRequest(photoPath, this.activeKey, this)
          photoPath = null
        }
      }
    } else {
      Log.d(TAG, "onActivityResult resut code not okay, user cancelled")
    }
  }


  def onClickVoiceCallFriend(v: View){
    println("This button (Audio Call) doesn't work yet.")
  }

  def onClickVideoCallFriend(v: View): Unit = {
    println("This button (Video Call) doesn't work yet.")
  }

  override def onPause() = {
    super.onPause()
  }
}
