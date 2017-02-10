package chat.tox.antox.activities

import java.util

import android.content.{Context, Intent}
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView.OnScrollListener
import android.support.v7.widget.{LinearLayoutManager, RecyclerView, Toolbar}
import android.text.InputFilter.LengthFilter
import android.text.{Editable, InputFilter, TextWatcher}
import android.view.inputmethod.EditorInfo
import android.view.{KeyEvent, Menu, MenuItem, View}
import android.widget.TextView.OnEditorActionListener
import android.widget.{EditText, TextView}
import chat.tox.antox._
import chat.tox.antox.adapters.ChatMessagesAdapter
import chat.tox.antox.data.State
import chat.tox.antox.theme.ThemeManager
import chat.tox.antox.utils.StringExtensions.RichString
import chat.tox.antox.utils.ViewExtensions.RichView
import chat.tox.antox.utils.{AntoxLog, Constants, KeyboardOptions, Location}
import chat.tox.antox.wrapper.{ContactKey, Message, MessageType}
import im.tox.tox4j.core.enums.ToxMessageType
import jp.wasabeef.recyclerview.animators.LandingAnimator
import rx.lang.scala.schedulers.{AndroidMainThreadScheduler, IOScheduler}
import rx.lang.scala.{Observable, Subscription}

import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer

abstract class GenericChatActivity[KeyType <: ContactKey] extends AppCompatActivity {

  //var ARG_CONTACT_NUMBER: String = "contact_number"
  var toolbar: Toolbar = _
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
  var scrollDateHeader: TextView = _

  var fastScroller: RecyclerViewFastScroller = _
  var conversationDateHeader: ConversationDateHeader = _

  override def onCreate(savedInstanceState: Bundle): Unit = {

    super.onCreate(savedInstanceState)
    overridePendingTransition(R.anim.slide_from_right, R.anim.fade_scale_out)

    setContentView(R.layout.activity_chat)

    val thisActivity = this

    toolbar = findViewById(R.id.chat_toolbar).asInstanceOf[Toolbar]

    toolbar.inflateMenu(R.menu.chat_menu)
    toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp)
    toolbar.setNavigationOnClickListener(new View.OnClickListener {
      override def onClick(v: View): Unit = {
        thisActivity.finish()
      }
    })
    setSupportActionBar(toolbar)

    ThemeManager.applyTheme(this, getSupportActionBar)
    getSupportActionBar.setDisplayShowTitleEnabled(false)

    val extras: Bundle = getIntent.getExtras
    activeKey = getKey(extras.getString("key"))
    fromNotifications = extras.getBoolean("notification", false)

    AntoxLog.debug("key = " + activeKey)

    if (getIntent.getAction == Constants.START_CALL) {
      onClickVoiceCall(Location.Origin)
      finish()
      return
    }

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

    scrollDateHeader = this.findViewById(R.id.scroll_date_header).asInstanceOf[TextView]
    scrollDateHeader.setVisibility(View.GONE)
    conversationDateHeader = new ConversationDateHeader(getApplicationContext, scrollDateHeader)

    // --- enable new fastScroller ---
    // --- enable new fastScroller ---
    // --- enable new fastScroller ---
    fastScroller = this.findViewById(R.id.fast_conversation_scroller).asInstanceOf[RecyclerViewFastScroller]
    fastScroller.setVisibility(View.GONE)
    chatListView.setVerticalScrollBarEnabled(false)
    fastScroller.setRecyclerView(chatListView)
    // --- enable new fastScroller ---
    // --- enable new fastScroller ---
    // --- enable new fastScroller ---


    //    chatListView.setVerticalScrollBarEnabled(true)
    chatListView.addOnScrollListener(new OnScrollListener {

      override def onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int): Unit = {
        if (!recyclerView.canScrollVertically(-1)) {
          onScrolledToTop()
        }

        val itemCount = recyclerView.getAdapter().getItemCount()
        var proportion: Float = 0f
        if (fastScroller.getHandleY() == 0) {
          proportion = 0f
        }
        else {
          if (fastScroller.getHandleY() + fastScroller.getHandleHeight() >= fastScroller.getMyHeight() - RecyclerViewFastScroller.TRACK_SNAP_RANGE) {
            proportion = 1f
          }
          else {
            proportion = fastScroller.getHandleY() / fastScroller.getMyHeight()
          }
        }

        val targetPos = fastScroller.translatedChildPosition(Util.clamp((proportion * itemCount.asInstanceOf[Float]).asInstanceOf[Int], 0, itemCount - 1))
        scrollDateHeader.setText(recyclerView.getAdapter().asInstanceOf[chat.tox.antox.adapters.ChatMessagesAdapter].getBubbleText(targetPos))
      }

      override def onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {

        if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
          conversationDateHeader.show()
        } else if (newState == RecyclerView.SCROLL_STATE_IDLE) {
          conversationDateHeader.hide()
        }

        adapter.setScrolling(!(newState == RecyclerView.SCROLL_STATE_IDLE))
      }

    })

    val sendMessageButton = this.findViewById(R.id.send_message_button)
    sendMessageButton.setOnClickListener(new View.OnClickListener() {
      override def onClick(v: View) {
        onSendMessage()

        setTyping(typing = false)
      }
    })

    messageBox = this.findViewById(R.id.your_message).asInstanceOf[EditText]
    messageBox.setFilters(Array[InputFilter](new LengthFilter(MESSAGE_LENGTH_LIMIT)))
    messageBox.setText(db.getContactUnsentMessage(activeKey))
    messageBox.setOnEditorActionListener(new OnEditorActionListener {
      override def onEditorAction(v: TextView, actionId: Int, event: KeyEvent): Boolean = {
        if (actionId == EditorInfo.IME_ACTION_SEND) {
          onSendMessage()
          setTyping(typing = false)
          return true
        }
        false
      }
    })
    messageBox.setInputType(KeyboardOptions.getInputType(getApplicationContext))
    messageBox.addTextChangedListener(new TextWatcher() {
      override def beforeTextChanged(charSequence: CharSequence, start: Int, count: Int, after: Int) {
        val isTyping = after > 0
        setTyping(isTyping)
      }

      override def onTextChanged(charSequence: CharSequence, start: Int, count: Int, after: Int): Unit = {
        Observable[Boolean](subscriber => {
          db.updateContactUnsentMessage(activeKey, charSequence.toString)
          subscriber.onCompleted()
        }).subscribeOn(IOScheduler()).subscribe()
      }

      override def afterTextChanged(editable: Editable) {
      }
    })

  }

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    getMenuInflater.inflate(R.menu.chat_menu, menu)

    true
  }

  override def onOptionsItemSelected(item: MenuItem): Boolean = {
    super.onOptionsItemSelected(item)

    val maybeItemView = Option(toolbar.findViewById(item.getItemId))
    val clickLocation = maybeItemView.map(_.getCenterLocationOnScreen()).getOrElse(Location.Origin)

    item.getItemId match {
      case R.id.voice_call_button =>
        onClickVoiceCall(clickLocation)
        true

      case R.id.video_call_button =>
        onClickVideoCall(clickLocation)
        true

      case R.id.user_info =>
        onClickInfo(clickLocation)
        true

      case _ =>
        false
    }
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

    adapter.addAll(filterMessageList(messageList))

    if (adapter.getItemCount > 0) scrollDateHeader.setVisibility(View.VISIBLE)
    else scrollDateHeader.setVisibility(View.GONE)

    // This works like TRANSCRIPT_MODE_NORMAL but for RecyclerView
    if (layoutManager.findLastCompletelyVisibleItemPosition() >= chatListView.getAdapter.getItemCount - 2) {
      chatListView.smoothScrollToPosition(chatListView.getAdapter.getItemCount)
    }

    if (chatListView.computeVerticalScrollRange() > chatListView.getHeight) {
      fastScroller.setVisibility(View.VISIBLE)
    }
    else {
      fastScroller.setVisibility(View.GONE)
    }

    AntoxLog.debug("changing chat list cursor")
  }

  def filterMessageList(messageList: Seq[Message]): Seq[Message] = {
    val showCallEvents = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("call_event_logging", true)

    if (!showCallEvents) {
      messageList.filterNot(_.`type` == MessageType.CALL_EVENT)
    } else messageList
  }

  def validateMessageBox(): Option[String] = {
    messageBox.getText.toString.toOption
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

  // zoff
  def getActiveMessageList(takeLast: Int): ArrayBuffer[Message] = {
    try {
      val db = State.db
      db.getMessageList(Some(activeKey), takeLast = takeLast)
    }
    catch {
      case e: Exception => e.printStackTrace()
        null
    }
  }

  override def onPause(): Unit = {
    super.onPause()
    State.chatActive.onNext(false)
    if (isFinishing) overridePendingTransition(R.anim.fade_scale_in, R.anim.slide_to_right)
    messagesSub.unsubscribe()
  }

  override def onBackPressed(): Unit = {
    if (fromNotifications) {
      val main = new Intent(this, classOf[MainActivity])
      main.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
      startActivity(main)
    } else {
      super.onBackPressed()
    }
  }

  //Abstract Methods
  def getKey(key: String): KeyType

  def sendMessage(message: String, messageType: ToxMessageType, context: Context): Unit

  def setTyping(typing: Boolean): Unit

  def onClickVoiceCall(clickLocation: Location): Unit

  def onClickVideoCall(clickLocation: Location): Unit

  def onClickInfo(clickLocation: Location): Unit
}
