package chat.tox.antox.activities

import java.util

import android.content.{Context, SharedPreferences}
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.{ActionBar, AppCompatActivity}
import android.text.InputFilter.LengthFilter
import android.text.{Editable, InputFilter, TextWatcher}
import android.util.Log
import android.view.{Menu, MenuInflater, View}
import android.widget._
import chat.tox.antox.R
import chat.tox.antox.adapters.ChatMessagesAdapter
import chat.tox.antox.data.State
import chat.tox.antox.theme.ThemeManager
import chat.tox.antox.tox.Reactive
import chat.tox.antox.utils.Constants
import chat.tox.antox.wrapper.{Message, ToxKey}
import rx.lang.scala.schedulers.AndroidMainThreadScheduler
import rx.lang.scala.{Observable, Subscription}

import scala.collection.JavaConversions
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._

abstract class GenericChatActivity extends AppCompatActivity {
  val TAG: String = "ChatActivity"
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
  var activeKey: ToxKey = null
  var scrolling: Boolean = false

  val MESSAGE_LENGTH_LIMIT = Constants.MAX_MESSAGE_LENGTH * 64

  override def onCreate(savedInstanceState: Bundle) = {
    super.onCreate(savedInstanceState)
    overridePendingTransition(R.anim.slide_from_right, R.anim.fade_scale_out)
    setContentView(R.layout.activity_chat)
    val actionBar = getSupportActionBar
    val avatarView = getLayoutInflater.inflate(R.layout.avatar_actionview, null)
    actionBar.setCustomView(avatarView)
    actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM)
    ThemeManager.applyTheme(this, getSupportActionBar)

    val extras: Bundle = getIntent.getExtras
    activeKey = new ToxKey(extras.getString("key"))
    val thisActivity = this
    Log.d(TAG, "key = " + activeKey)
    val preferences = PreferenceManager.getDefaultSharedPreferences(this)
    val db = State.db
    adapter = new ChatMessagesAdapter(this, new util.ArrayList(JavaConversions.mutableSeqAsJavaList(getMessageList)), db.getMessageIds(Some(activeKey), preferences.getBoolean("action_messages", false)))
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

    val b = this.findViewById(R.id.sendMessageButton)
    b.setOnClickListener(new View.OnClickListener() {
      override def onClick(v: View) {
        onSendMessage()
        setTyping(typing = false)
      }
    })

    messageBox = this.findViewById(R.id.yourMessage).asInstanceOf[EditText]
    messageBox.setFilters(Array[InputFilter](new LengthFilter(MESSAGE_LENGTH_LIMIT)))
    messageBox.setText(db.getContactUnsentMessage(activeKey))
    messageBox.addTextChangedListener(new TextWatcher() {
      override def beforeTextChanged(charSequence: CharSequence, start: Int, count: Int, after: Int) {
        val isTyping = after > 0
        setTyping(isTyping)
      }

      override def onTextChanged(charSequence: CharSequence, start: Int, count: Int, after: Int): Unit = {
        db.updateContactUnsentMessage(activeKey, charSequence.toString)
      }

      override def afterTextChanged(editable: Editable) {
      }
    })

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
    val db = State.db
    db.markIncomingMessagesRead(activeKey)
    messagesSub = getMessageObservable
      .observeOn(AndroidMainThreadScheduler())
      .subscribe(messageList => {
      Log.d(TAG, "Messages updated")
      updateChat(messageList)
    })
    progressSub = Observable.interval(500 milliseconds)
      .observeOn(AndroidMainThreadScheduler())
      .subscribe(x => {
      if (!scrolling) {
        updateProgress()
      }
    })
  }

  def updateChat(messageList: Seq[Message]) = {
    //FIXME make this more efficient
    adapter.setNotifyOnChange(false)
    adapter.clear()
    //add all is not available on api 10
    for (message <- messageList) {
      adapter.add(message)
    }
    adapter.notifyDataSetChanged()
    Log.d(TAG, "changing chat list cursor")
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

    var msg: String = null
    if (messageBox.getText != null) {
      msg = messageBox.getText.toString
    } else {
      msg = ""
    }

    Some(msg)
  }

  private def onSendMessage() {
    Log.d(TAG, "sendMessage")
    val mMessage = validateMessageBox()

    mMessage.foreach(rawMessage => {
      messageBox.setText("")
      val meMessagePrefix = "/me "
      val isAction = rawMessage.startsWith(meMessagePrefix)
      val message =
        if (isAction) {
          rawMessage.replaceFirst(meMessagePrefix, "")
        } else {
          rawMessage
        }
      sendMessage(message, isAction, this)
    })
  }

  def getMessageObservable: Observable[ArrayBuffer[Message]] = {
    val db = State.db
    val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
    db.messageListObservable(Some(activeKey), preferences.getBoolean("action_messages", true))
  }

  def getMessageList: ArrayBuffer[Message] = {
    val db = State.db
    val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
    db.getMessageList(Some(activeKey), preferences.getBoolean("action_messages", true))
  }

  override def onPause() = {
    super.onPause()
    Reactive.chatActive.onNext(false)
    if (isFinishing) overridePendingTransition(R.anim.fade_scale_in, R.anim.slide_to_right)
    messagesSub.unsubscribe()
    progressSub.unsubscribe()
  }

  //Abstract Methods
  def sendMessage(message: String, isAction: Boolean, context: Context): Unit

  def setTyping(typing: Boolean): Unit
}
