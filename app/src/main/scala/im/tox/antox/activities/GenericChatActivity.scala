package im.tox.antox.activities

import java.util

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.{ActionBar, ActionBarActivity}
import android.util.Log
import android.view.{Menu, MenuInflater, View}
import android.widget._
import im.tox.antoxnightly.R
import im.tox.antox.adapters.ChatMessagesAdapter
import im.tox.antox.data.AntoxDB
import im.tox.antox.tox.{Reactive, ToxSingleton}
import im.tox.antox.wrapper.Message
import rx.lang.scala.schedulers.{AndroidMainThreadScheduler, IOScheduler}
import rx.lang.scala.{Observable, Subscription}

import scala.concurrent.duration._

abstract class GenericChatActivity extends ActionBarActivity {
  val TAG: String = "im.tox.antox.activities.ChatActivity"
  //var ARG_CONTACT_NUMBER: String = "contact_number"
  var adapter: ChatMessagesAdapter = null
  var messageBox: EditText = null
  var isTypingBox: TextView = null
  var statusTextBox: TextView = null
  var chatListView: ListView = null
  var displayNameView: TextView = null
  var statusIconView: View = null
  var avatarActionView: View = null
  var messagesSub: Subscription = null
  var progressSub: Subscription = null
  var titleSub: Subscription = null
  var activeKey: String = null
  var scrolling: Boolean = false
  var antoxDB: AntoxDB = null

  val MESSAGE_LENGTH_LIMIT = 1367 * 100

  override def onCreate(savedInstanceState: Bundle) = {
    super.onCreate(savedInstanceState)
    overridePendingTransition(R.anim.slide_from_right, R.anim.fade_scale_out)
    setContentView(R.layout.activity_chat)
    val actionBar = getSupportActionBar
    val avatarView = getLayoutInflater.inflate(R.layout.avatar_actionview, null)
    actionBar.setCustomView(avatarView)
    actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM)
    val extras: Bundle = getIntent.getExtras
    val key = extras.getString("key")
    activeKey = key
    val thisActivity = this
    Log.d(TAG, "key = " + key)
    val preferences = PreferenceManager.getDefaultSharedPreferences(this)
    adapter = new ChatMessagesAdapter(this, getMessageList, antoxDB.getMessageIds(key, preferences.getBoolean("action_messages", false)))
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
  }

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    // Inflate the menu items for use in the action bar
    val inflater: MenuInflater = getMenuInflater
    inflater.inflate(R.menu.chat_activity, menu)
    super.onCreateOptionsMenu(menu)
  }

  def setDisplayName(name: String) = {
    this.displayNameView.setText(name)
  }

  override def onResume() = {
    super.onResume()
    Reactive.activeKey.onNext(Some(activeKey))
    Reactive.chatActive.onNext(true)
    val antoxDB = new AntoxDB(getApplicationContext)
    antoxDB.markIncomingMessagesRead(activeKey)
    ToxSingleton.updateMessages(getApplicationContext)
    messagesSub = Reactive.updatedMessages.subscribe(x => {
      Log.d(TAG, "Messages updated")
      updateChat()
      antoxDB.close()
    })
    progressSub = Observable.interval(500 milliseconds)
      .observeOn(AndroidMainThreadScheduler())
      .subscribe(x => {
      if (!scrolling) {
        updateProgress()
      }
    })
  }

  def updateChat() = {
    val observable: Observable[util.ArrayList[Message]] = Observable((observer) => {
      val cursor: util.ArrayList[Message] = getMessageList
      observer.onNext(cursor)
      observer.onCompleted()
    })
    observable
      .subscribeOn(IOScheduler())
      .observeOn(AndroidMainThreadScheduler())
      .subscribe((messageList: util.ArrayList[Message]) => {
      //FIXME make this more efficient
      adapter.setNotifyOnChange(false)
      adapter.clear()
      adapter.addAll(messageList)
      adapter.notifyDataSetChanged()
      Log.d(TAG, "changing chat list cursor")
    })
    Log.d("ChatFragment", "new key: " + activeKey)
  }

  private def updateProgress() {
    val start = chatListView.getFirstVisiblePosition
    val end = chatListView.getLastVisiblePosition
    for (i <- start to end) {
      val view = chatListView.getChildAt(i - start)
      chatListView.getAdapter.getView(i, view, chatListView)
    }
  }

  def validateMessageBox(): Option[String] = {
    if (messageBox.getText != null && messageBox.getText.toString.length() == 0) {
      return None
    }

    //limit to 100 max length messages
    if (messageBox.getText.length() > MESSAGE_LENGTH_LIMIT) {
      Toast.makeText(this, getResources.getString(R.string.chat_message_too_long), Toast.LENGTH_LONG)
      return None
    }

    var msg: String = null
    if (messageBox.getText != null) {
      msg = messageBox.getText.toString
    } else {
      msg = ""
    }

    Some(msg)
  }

  def getMessageList: util.ArrayList[Message] = {
    if (antoxDB == null) {
      antoxDB = new AntoxDB(this)
    }
    val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
    val messageList: util.ArrayList[Message] = antoxDB.getMessageList(activeKey, preferences.getBoolean("action_messages", true))
    messageList
  }

  override def onPause() = {
    super.onPause()
    Reactive.chatActive.onNext(false)
    if (isFinishing) overridePendingTransition(R.anim.fade_scale_in, R.anim.slide_to_right)
    messagesSub.unsubscribe()
    progressSub.unsubscribe()
  }
}
