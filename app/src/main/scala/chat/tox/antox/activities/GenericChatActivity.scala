package chat.tox.antox.activities

import java.util

import android.content.{Intent, Context}
import android.os.Bundle
import android.support.v7.app.{ActionBar, AppCompatActivity}
import android.support.v7.widget.RecyclerView.OnScrollListener
import android.support.v7.widget.{LinearLayoutManager, RecyclerView}
import android.text.InputFilter.LengthFilter
import android.text.{Editable, InputFilter, TextWatcher}
import android.view.{Menu, MenuInflater, View}
import android.widget._
import chat.tox.antox.R
import chat.tox.antox.adapters.ChatMessagesAdapter
import chat.tox.antox.data.State
import chat.tox.antox.theme.ThemeManager
import chat.tox.antox.utils.{AntoxLog, Constants}
import chat.tox.antox.wrapper.{ContactKey, Message}
import im.tox.tox4j.core.enums.ToxMessageType
import jp.wasabeef.recyclerview.animators.LandingAnimator
import rx.lang.scala.schedulers.{IOScheduler, AndroidMainThreadScheduler}
import rx.lang.scala.{Observable, Subscription}

import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer

abstract class GenericChatActivity[KeyType <: ContactKey] extends AppCompatActivity {

  //var ARG_CONTACT_NUMBER: String = "contact_number"
  var adapter: ChatMessagesAdapter = _
  var messageBox: EditText = _
  var isTypingBox: TextView = _
  var statusTextBox: TextView = _
  var chatListView: RecyclerView = _
  var displayNameView: TextView = _
  var statusIconView: View = _
  var avatarActionView: View = _
  var messagesSub: Subscription = _
  var titleSub: Subscription = _
  var activeKey: KeyType = _
  var scrolling: Boolean = false
  val layoutManager = new LinearLayoutManager(this)

  var fromNotifications: Boolean = false

  val MESSAGE_LENGTH_LIMIT = Constants.MAX_MESSAGE_LENGTH * 64

  val defaultMessagePageSize = 50
  var numMessagesShown = defaultMessagePageSize

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
    activeKey = getKey(extras.getString("key"))
    fromNotifications = extras.getBoolean("notification", false)
    val thisActivity = this

    AntoxLog.debug("key = " + activeKey)

    val db = State.db
    adapter = new ChatMessagesAdapter(this,
      new util.ArrayList(mutableSeqAsJavaList(getActiveMessageList(numMessagesShown))))

    displayNameView = this.findViewById(R.id.displayName).asInstanceOf[TextView]
    statusIconView = this.findViewById(R.id.icon)
    avatarActionView = this.findViewById(R.id.avatarActionView)
    avatarActionView.setOnClickListener(new View.OnClickListener() {
      override def onClick(v: View) {
        thisActivity.finish()
      }
    })

    layoutManager.setStackFromEnd(true)

    chatListView = this.findViewById(R.id.chat_messages).asInstanceOf[RecyclerView]
    chatListView.setLayoutManager(layoutManager)
    chatListView.setAdapter(adapter)
    chatListView.setItemAnimator(new LandingAnimator())
    chatListView.setVerticalScrollBarEnabled(true)
    chatListView.addOnScrollListener(new OnScrollListener {

      override def onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int): Unit = {
        if (!recyclerView.canScrollVertically(-1)) {
          onScrolledToTop()
        }
      }

      override def onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        adapter.setScrolling(!(newState == RecyclerView.SCROLL_STATE_IDLE))
      }

    })

    val b = this.findViewById(R.id.send_message_button)
    b.setOnClickListener(new View.OnClickListener() {
      override def onClick(v: View) {
        onSendMessage()

        setTyping(typing = false)
      }
    })

    messageBox = this.findViewById(R.id.your_message).asInstanceOf[EditText]
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
    State.activeKey.onNext(Some(activeKey))
    State.chatActive.onNext(true)

    val db = State.db
    db.markIncomingMessagesRead(activeKey)

    messagesSub =
      getActiveMessagesUpdatedObservable
        .observeOn(AndroidMainThreadScheduler())
        .subscribe(_ => {
          AntoxLog.debug("Messages updated")
          updateChat(getActiveMessageList(numMessagesShown))
        })
  }

  def updateChat(messageList: Seq[Message]): Unit = {
    //FIXME make this more efficient
    adapter.removeAll()

    for (message <- messageList) {
      adapter.add(message)
    }

    // This works like TRANSCRIPT_MODE_NORMAL but for RecyclerView
    if (layoutManager.findLastCompletelyVisibleItemPosition() >= chatListView.getAdapter.getItemCount - 2) {
      chatListView.smoothScrollToPosition(chatListView.getAdapter.getItemCount)
    }
    AntoxLog.debug("changing chat list cursor")
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

  private def onScrolledToTop(): Unit = {
    numMessagesShown += defaultMessagePageSize
    Observable[Seq[Message]](subscriber => {
      subscriber.onNext(getActiveMessageList(numMessagesShown))
      subscriber.onCompleted()
    }).subscribeOn(IOScheduler())
      .observeOn(AndroidMainThreadScheduler())
      .subscribe(updateChat(_))
  }

  private def onSendMessage() {
    AntoxLog.debug("sendMessage")
    val mMessage = validateMessageBox()

    mMessage.foreach(rawMessage => {
      messageBox.setText("")
      val meMessagePrefix = "/me "
      val messageType = if (rawMessage.startsWith(meMessagePrefix)) ToxMessageType.ACTION else ToxMessageType.NORMAL
      val message =
        if (messageType == ToxMessageType.ACTION) {
          rawMessage.replaceFirst(meMessagePrefix, "")
        } else {
          rawMessage
        }
      sendMessage(message, messageType, this)
    })
  }

  def getActiveMessagesUpdatedObservable: Observable[Int] = {
    val db = State.db
    db.messageListUpdatedObservable(Some(activeKey))
  }

  def getActiveMessageList(takeLast: Int): ArrayBuffer[Message] = {
    val db = State.db
    db.getMessageList(Some(activeKey), takeLast = takeLast)
  }

  override def onPause(): Unit = {
    super.onPause()
    State.chatActive.onNext(false)
    if (isFinishing) overridePendingTransition(R.anim.fade_scale_in, R.anim.slide_to_right)
    messagesSub.unsubscribe()
  }

  override def onBackPressed(): Unit = {
    if(fromNotifications) {
      val main = new Intent(this, classOf[MainActivity])
      main.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
      startActivity(main)
    }
    else super.onBackPressed()
  }

  //Abstract Methods
  def getKey(key: String): KeyType

  def sendMessage(message: String, messageType: ToxMessageType, context: Context): Unit

  def setTyping(typing: Boolean): Unit
}
