package im.tox.antox.activities

import java.io.{File, IOException}
import java.text.SimpleDateFormat
import java.util.Date

import android.app.{Activity, AlertDialog}
import android.content.{CursorLoader, DialogInterface, Intent}
import android.net.Uri
import android.os.{Build, Bundle, Environment}
import android.provider.MediaStore
import android.text.{Editable, TextWatcher}
import android.util.Log
import android.view.View
import android.widget._
import de.hdodenhof.circleimageview.CircleImageView
import im.tox.antox.tox.{MessageHelper, Reactive, ToxSingleton}
import im.tox.antox.transfer.FileDialog
import im.tox.antox.utils.{Constants, IconColor}
import im.tox.antox.wrapper.{FileKind, FriendInfo, UserStatus}
import im.tox.antoxnightly.R
import im.tox.tox4j.exceptions.ToxException
import rx.lang.scala.schedulers.{AndroidMainThreadScheduler, IOScheduler}

class ChatActivity extends GenericChatActivity {

  var photoPath: String = null

  override def onCreate(savedInstanceState: Bundle) = {
    super.onCreate(savedInstanceState)
    val extras: Bundle = getIntent.getExtras
    val key = extras.getString("key")
    activeKey = key
    val thisActivity = this

    this.findViewById(R.id.info).setVisibility(View.GONE)

    isTypingBox = this.findViewById(R.id.isTyping).asInstanceOf[TextView]
    statusTextBox = this.findViewById(R.id.chatActiveStatus).asInstanceOf[TextView]

    messageBox = this.findViewById(R.id.yourMessage).asInstanceOf[EditText]
    messageBox.addTextChangedListener(new TextWatcher() {
      override def beforeTextChanged(charSequence: CharSequence, i: Int, i2: Int, i3: Int) {
        val isTyping = (i3 > 0)
        val mFriend = ToxSingleton.getAntoxFriend(key)
        mFriend.foreach(friend => {
          if (friend.isOnline) {
            try {
              ToxSingleton.tox.setTyping(friend.getFriendnumber, isTyping)
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
            ToxSingleton.tox.setTyping(friend.getFriendnumber, typing = false)
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
        ToxSingleton.getAntoxFriend(key).foreach(friend => {
          if (!friend.isOnline) {
            Toast.makeText(thisActivity, getResources.getString(R.string.chat_ft_failed_friend_offline), Toast.LENGTH_SHORT).show()
            return
          }
        })
        
        val builder = new AlertDialog.Builder(thisActivity)
        var items: Array[CharSequence] = null
        items = Array(getResources.getString(R.string.attachment_photo), getResources.getString(R.string.attachment_takephoto), getResources.getString(R.string.attachment_file))
        builder.setItems(items, new DialogInterface.OnClickListener() {

          override def onClick(dialogInterface: DialogInterface, i: Int): Unit = {
            i match {
              case 0 => {
                val intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(intent, Constants.IMAGE_RESULT)
              }
              case 1 => {
                val cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
                val image_name = "Antoxpic " + new SimpleDateFormat("HH:mm:ss").format(new Date()) + " "
                println("image name " + image_name)
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
                val fileDialog = new FileDialog(thisActivity, mPath, false)
                fileDialog.addFileListener(new FileDialog.FileSelectedListener() {
                  def fileSelected(file: File) {
                    ToxSingleton.sendFileSendRequest(file.getPath, activeKey, FileKind.DATA, thisActivity)
                  }
                })
                fileDialog.showDialog()
              }

            }
          }
        })
        builder.create().show()
      }
    })
  }

  override def onResume() = {
    super.onResume()
    ToxSingleton.clearUselessNotifications(activeKey)
    titleSub = Reactive.friendInfoList
      .subscribeOn(IOScheduler())
      .observeOn(AndroidMainThreadScheduler())
      .subscribe(fi => {
        updateDisplayedState(fi)
    })
  }

  private def updateDisplayedState(fi: Array[FriendInfo]): Unit = {
    val thisActivity = this
    val key = activeKey
    val mFriend: Option[FriendInfo] = fi
      .filter(f => f.key == key)
      .headOption
    mFriend match {
      case Some(friend) => {
        thisActivity.setDisplayName(friend.getAliasOrName)

        val avatar = friend.avatar
        val avatarView = this.findViewById(R.id.avatar).asInstanceOf[CircleImageView]
        if (avatar.isDefined && avatar.get.exists()) {
          avatarView.setImageURI(Uri.fromFile(avatar.get))
        } else {
          avatarView.setImageResource(R.color.grey_light)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
          thisActivity.statusIconView.setBackground(thisActivity.getResources
            .getDrawable(IconColor.iconDrawable(friend.online, UserStatus.getToxUserStatusFromString(friend.status))))
        } else {
          thisActivity.statusIconView.setBackgroundDrawable(thisActivity.getResources
            .getDrawable(IconColor.iconDrawable(friend.online, UserStatus.getToxUserStatusFromString(friend.status))))
        }
      }
      case None => {
        thisActivity.setDisplayName("")
      }
    }
  }

  private def sendMessage() {
    Log.d(TAG, "sendMessage")
    val mMessage = validateMessageBox()
    val key = activeKey

    mMessage.foreach(message => {
      messageBox.setText("")
      MessageHelper.sendMessage(this, key, message, None)
    })
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
              ToxSingleton.sendFileSendRequest(filePath, this.activeKey, FileKind.DATA, this)
            } catch {
              case e: Exception => e.printStackTrace()
            }
          }
        }
      }
      if (requestCode == Constants.PHOTO_RESULT) {
        if (photoPath != null) {
          ToxSingleton.sendFileSendRequest(photoPath, this.activeKey, FileKind.DATA, this)
          photoPath = null
        }
      }
    } else {
      Log.d(TAG, "onActivityResult result code not okay, user cancelled")
    }
  }


  def onClickVoiceCallFriend(v: View){}

  def onClickVideoCallFriend(v: View): Unit = {}

  def onClickInfo(v: View): Unit = {}

  override def onPause() = {
    super.onPause()
  }
}
