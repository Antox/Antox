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
import android.widget.{AbsListView, EditText, ListView, TextView}
import im.tox.antox.R
import im.tox.antox.adapters.ChatMessagesAdapter
import im.tox.antox.data.AntoxDB
import im.tox.antox.tox.{Methods, Reactive, ToxSingleton}
import im.tox.antox.utils.{Constants, FileDialog, FriendInfo, IconColor, UserStatus}
import im.tox.jtoxcore.ToxException
import rx.lang.scala.{Observable, Subscription}
import rx.lang.scala.schedulers.{AndroidMainThreadScheduler, IOScheduler}
import scala.concurrent.duration._

class ChatActivity extends ActionBarActivity {
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
  //var activeKeySub: Subscription
  var titleSub: Subscription = null
  //var typingSub: Subscription
  //var chatMessages: ArrayList<ChatMessages>
  var activeKey: String = null
  var scrolling: Boolean = false
  var antoxDB: AntoxDB = null
  var photoPath: String = null

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
    adapter = new ChatMessagesAdapter(this, getCursor(), antoxDB.getMessageIds(key, preferences.getBoolean("action_messages", false)))
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
    messageBox.addTextChangedListener(new TextWatcher() {
      override def beforeTextChanged(charSequence: CharSequence, i: Int, i2: Int, i3: Int) {
        val isTyping = (i3 > 0)
        val mFriend = ToxSingleton.getAntoxFriend(key)
        mFriend.foreach(friend => {
          if (friend.isOnline()) {
            try {
              ToxSingleton.jTox.sendIsTyping(friend.getFriendnumber(), isTyping)
            } catch {
              case te: ToxException => {
              }
              case e: Exception => {
              }
            }
          }
        })
      }

      override def onTextChanged(charSequence: CharSequence, i: Int, i2: Int, i3: Int) {
      }

      override def afterTextChanged(editable: Editable) {
      }
    })

    messageBox.setOnFocusChangeListener(new View.OnFocusChangeListener() {
      override def onFocusChange(v: View, hasFocus: Boolean) {
        //chatListView.setSelection(adapter.getCount() - 1)
      }
    })

    val b = this.findViewById(R.id.sendMessageButton)
    b.setOnClickListener(new View.OnClickListener() {
      override def onClick(v: View) {
        sendMessage()
        val mFriend = ToxSingleton.getAntoxFriend(key)
        mFriend.foreach(friend => {
          try {
            ToxSingleton.jTox.sendIsTyping(friend.getFriendnumber(), false)
          } catch {
            case te: ToxException => {
            }
            case e: Exception => {
            }
          }
        })
      }
    })

    val attachmentButton = this.findViewById(R.id.attachmentButton)

    attachmentButton.setOnClickListener(new View.OnClickListener() {

      override def onClick(v: View) {
        val builder = new AlertDialog.Builder(thisActivity)
        var items: Array[CharSequence] = null
        items = Array(getResources.getString(R.string.attachment_photo), getResources.getString(R.string.attachment_takephoto), getResources.getString(R.string.attachment_file))
        builder.setItems(items, new DialogInterface.OnClickListener() {

          override def onClick(dialogInterface: DialogInterface, i: Int) = i match {
            case 0 => {
              var intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
              startActivityForResult(intent, Constants.IMAGE_RESULT)
            }
            case 1 => {
              val cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
              val image_name = "Antoxpic" + new Date().toString
              val storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
              try {
                val file = File.createTempFile(image_name, ".jpg", storageDir)
                val imageUri = Uri.fromFile(file)
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
                photoPath = file.getAbsolutePath
                startActivityForResult(cameraIntent, Constants.PHOTO_RESULT)
              } catch {
                case e: IOException => e.printStackTrace()
              }
            }
            case 2 => {
              val mPath = new File(Environment.getExternalStorageDirectory + "//DIR//")
              val fileDialog = new FileDialog(thisActivity, mPath)
              fileDialog.addFileListener(new FileDialog.FileSelectedListener() {
                def fileSelected(file: File) {
                  ToxSingleton.sendFileSendRequest(file.getPath, activeKey, thisActivity)
                }
              })
              fileDialog.showDialog()
            }

          }
        })
        builder.create().show()
      }
    })
  }

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    // Inflate the menu items for use in the action bar
    val inflater: MenuInflater = getMenuInflater()
    inflater.inflate(R.menu.chat_activity, menu)
    super.onCreateOptionsMenu(menu)
  }

  def setDisplayName(name: String) = {
    this.displayNameView.setText(name)
  }

  override def onResume() = {
    super.onResume()
    val thisActivity = this
    Reactive.activeKey.onNext(Some(activeKey))
    Reactive.chatActive.onNext(true)
    val antoxDB = new AntoxDB(getApplicationContext())
    antoxDB.markIncomingMessagesRead(activeKey)
    ToxSingleton.clearUselessNotifications(activeKey)
    ToxSingleton.updateMessages(getApplicationContext())
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
    titleSub = Reactive.friendInfoList
      .subscribeOn(IOScheduler())
      .observeOn(AndroidMainThreadScheduler())
      .subscribe(fi => {
        val key = activeKey
        val mFriend: Option[FriendInfo] = fi
          .filter(f => f.friendKey == key)
          .headOption
        mFriend match {
          case Some(friend) => {
            if (friend.alias != "") {
              thisActivity.setDisplayName(friend.alias)
            } else {
              thisActivity.setDisplayName(friend.friendName)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
              thisActivity.statusIconView.setBackground(thisActivity.getResources.getDrawable(IconColor.iconDrawable(friend.isOnline, UserStatus.getToxUserStatusFromString(friend.friendStatus))))
            } else {
              thisActivity.statusIconView.setBackgroundDrawable(thisActivity.getResources.getDrawable(IconColor.iconDrawable(friend.isOnline, UserStatus.getToxUserStatusFromString(friend.friendStatus))))
            }
          }
          case None => {
            thisActivity.setDisplayName("")
          }
        }
      })
  }

  private def sendMessage() {
    Log.d(TAG, "sendMessage")
    if (messageBox.getText() != null && messageBox.getText().toString().length() == 0) {
      return
    }
    var msg: String = null
    if (messageBox.getText() != null) {
      msg = messageBox.getText().toString()
    } else {
      msg = ""
    }
    val key = activeKey
    messageBox.setText("")
    Methods.sendMessage(this, key, msg, None)
  }

  def updateChat() = {
    val observable: Observable[Cursor] = Observable((observer) => {
      val cursor: Cursor = getCursor()
      observer.onNext(cursor)
      observer.onCompleted()
    })
    observable
      .subscribeOn(IOScheduler())
      .observeOn(AndroidMainThreadScheduler())
      .subscribe((cursor: Cursor) => {
        adapter.changeCursor(cursor)
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

  def getCursor(): Cursor = {
    if (antoxDB == null) {
      antoxDB = new AntoxDB(this)
    }
    val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
    val cursor: Cursor = antoxDB.getMessageCursor(activeKey, preferences.getBoolean("action_messages", true))
    return cursor
  }

  override def onPause() = {
    super.onPause()
    Reactive.chatActive.onNext(false)
    if (isFinishing()) overridePendingTransition(R.anim.fade_scale_in, R.anim.slide_to_right);
    messagesSub.unsubscribe()
    titleSub.unsubscribe()
    progressSub.unsubscribe()
  }
}
