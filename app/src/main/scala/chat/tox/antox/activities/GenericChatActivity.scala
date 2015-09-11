package chat.tox.antox.activities

import java.util

import android.content.Context
import android.os.Bundle
import android.support.v7.app.{ActionBar, AppCompatActivity}
import android.support.v7.widget.RecyclerView.OnScrollListener
import android.support.v7.widget.{LinearLayoutManager, RecyclerView}
import android.text.InputFilter.LengthFilter
import android.text.{Editable, InputFilter, TextWatcher}
import android.util.Log
import android.view.{Menu, MenuInflater, View}
import android.widget._
import chat.tox.antox.R
import chat.tox.antox.adapters.MessageAdapter
import chat.tox.antox.data.State
import chat.tox.antox.theme.ThemeManager
import chat.tox.antox.tox.Reactive
import chat.tox.antox.utils.Constants
import chat.tox.antox.wrapper.{Message, ToxKey}
import rx.lang.scala.schedulers.AndroidMainThreadScheduler
import rx.lang.scala.{Observable, Subscription}

import scala.collection.JavaConversions
import scala.collection.mutable.ArrayBuffer

abstract class GenericChatActivity extends AppCompatActivity {
  val TAG: String = "ChatActivity"
  //var ARG_CONTACT_NUMBER: String = "contact_number"
  var adapter: MessageAdapter = null
  var messageBox: EditText = null
  var isTypingBox: TextView = null
  var statusTextBox: TextView = null
  var chatListView: RecyclerView = null
  var displayNameView: TextView = null
  var statusIconView: View = null
  var avatarActionView: View = null
  var messagesSub: Subscription = null
  var titleSub: Subscription = null
  var activeKey: ToxKey = null
  var scrolling: Boolean = false
  val layoutManager = new LinearLayoutManager(this)

  val MESSAGE_LENGTH_LIMIT = Constants.MAX_MESSAGE_LENGTH * 64

  override def onCreate(savedInstanceState: Bundle): Unit = {
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

    val db = State.db
    adapter = new MessageAdapter(this, new util.ArrayList(JavaConversions.mutableSeqAsJavaList(getActiveMessageList)))

    displayNameView = this.findViewById(R.id.displayName).asInstanceOf[TextView]
    statusIconView = this.findViewById(R.id.icon)
    avatarActionView = this.findViewById(R.id.avatarActionView)
    avatarActionView.setOnClickListener(new View.OnClickListener() {
      override def onClick(v: View) {
        thisActivity.finish()
      }
    })

    layoutManager.setStackFromEnd(true)
    chatListView = this.findViewById(R.id.chatMessages).asInstanceOf[RecyclerView]
    chatListView.setLayoutManager(layoutManager)
    chatListView.setAdapter(adapter)
    chatListView.setVerticalScrollBarEnabled(true)
    chatListView.addOnScrollListener(new OnScrollListener {
      override def onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        adapter.setScrolling(!(newState == RecyclerView.SCROLL_STATE_IDLE))
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

  def setDisplayName(name: String): Unit = {
    this.displayNameView.setText(name)
  }

  override def onResume(): Unit = {
    super.onResume()
    Reactive.activeKey.onNext(Some(activeKey))
    Reactive.chatActive.onNext(true)
    val db = State.db
    db.markIncomingMessagesRead(activeKey)
    messagesSub = getActiveMessageObservable
      .observeOn(AndroidMainThreadScheduler())
      .subscribe(messageList => {
      Log.d(TAG, "Messages updated")
      updateChat(messageList)
    })
  }

  def updateChat(messageList: Seq[Message]): Unit = {
    //FIXME make this more efficient
    adapter.removeAll()
    //add all is not available on api 10
    for (message <- messageList) {
      adapter.add(message)
    }
    if (layoutManager.findLastCompletelyVisibleItemPosition() >= chatListView.getAdapter.getItemCount - 2) {
      chatListView.smoothScrollToPosition(chatListView.getAdapter.getItemCount)
    }
    Log.d(TAG, "changing chat list cursor")
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

  def getActiveMessageObservable: Observable[ArrayBuffer[Message]] = {
    val db = State.db
    db.messageListObservable(Some(activeKey))
  }

  def getActiveMessageList: ArrayBuffer[Message] = {
    val db = State.db
    db.getMessageList(Some(activeKey))
  }

  override def onPause(): Unit = {
    super.onPause()
    Reactive.chatActive.onNext(false)
    if (isFinishing) overridePendingTransition(R.anim.fade_scale_in, R.anim.slide_to_right)
    messagesSub.unsubscribe()
  }

  //Abstract Methods
  def sendMessage(message: String, isAction: Boolean, context: Context): Unit

  def setTyping(typing: Boolean): Unit
}
