package im.tox.antox.adapters

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.AsyncTask
import android.os.Environment
import android.support.v4.widget.ResourceCursorAdapter
import android.text.ClipboardManager
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import java.io.File
import java.sql.Timestamp
import java.util.HashSet
import im.tox.antox.R
import im.tox.antox.data.AntoxDB
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.utils.BitmapManager
import im.tox.antox.utils.ChatMessages
import im.tox.antox.utils.Constants
import im.tox.antox.utils.PrettyTimestamp
import im.tox.antox.utils.Tuple
import ChatMessagesAdapter._
//remove if not needed
import scala.collection.JavaConversions._

object ChatMessagesAdapter {

  class ChatMessagesHolder {

    var row: LinearLayout = _

    var layout: LinearLayout = _

    var background: LinearLayout = _

    var message: TextView = _

    var time: TextView = _

    var sent: ImageView = _

    var received: ImageView = _

    var imageMessage: ImageView = _

    var imageMessageFrame: FrameLayout = _

    var title: TextView = _

    var progress: ProgressBar = _

    var progressText: TextView = _

    var padding: View = _

    var buttons: LinearLayout = _

    var accept: View = _

    var reject: View = _
  }
}

class ChatMessagesAdapter(var context: Context, c: Cursor, ids: HashSet[Integer])
    extends ResourceCursorAdapter(context, R.layout.chat_message_row, c, 0) {

  var layoutResourceId: Int = R.layout.chat_message_row

  private var density: Int = context.getResources.getDisplayMetrics.density.toInt

  private var paddingscale: Int = 8

  private var anim: Animation = AnimationUtils.loadAnimation(this.context, R.anim.abc_slide_in_bottom)

  private var mInflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE).asInstanceOf[LayoutInflater]

  private var animatedIds: HashSet[Integer] = ids

  override def newView(context: Context, cursor: Cursor, parent: ViewGroup): View = {
    mInflater.inflate(this.layoutResourceId, parent, false)
  }

  override def bindView(view: View, context: Context, cursor: Cursor) {
    val id = cursor.getInt(0)
    val time = Timestamp.valueOf(cursor.getString(1))
    val message_id = cursor.getInt(2)
    val k = cursor.getString(3)
    val m = cursor.getString(4)
    val received = cursor.getInt(5) > 0
    val read = cursor.getInt(6) > 0
    val sent = cursor.getInt(7) > 0
    val size = cursor.getInt(8)
    val messageType = cursor.getInt(9)
    val msg = new ChatMessages(id, message_id, m, time, received, sent, size, messageType)
    val holder = new ChatMessagesHolder()
    holder.message = view.findViewById(R.id.message_text).asInstanceOf[TextView]
    holder.layout = view.findViewById(R.id.message_text_layout).asInstanceOf[LinearLayout]
    holder.row = view.findViewById(R.id.message_row_layout).asInstanceOf[LinearLayout]
    holder.background = view.findViewById(R.id.message_text_background).asInstanceOf[LinearLayout]
    holder.time = view.findViewById(R.id.message_text_date).asInstanceOf[TextView]
    holder.sent = view.findViewById(R.id.chat_row_sent).asInstanceOf[ImageView]
    holder.received = view.findViewById(R.id.chat_row_received).asInstanceOf[ImageView]
    holder.title = view.findViewById(R.id.message_title).asInstanceOf[TextView]
    holder.progress = view.findViewById(R.id.file_transfer_progress).asInstanceOf[ProgressBar]
    holder.imageMessage = view.findViewById(R.id.message_sent_photo).asInstanceOf[ImageView]
    holder.imageMessageFrame = view.findViewById(R.id.message_sent_photo_frame).asInstanceOf[FrameLayout]
    holder.progressText = view.findViewById(R.id.file_transfer_progress_text).asInstanceOf[TextView]
    holder.padding = view.findViewById(R.id.file_transfer_padding).asInstanceOf[View]
    holder.buttons = view.findViewById(R.id.file_buttons).asInstanceOf[LinearLayout]
    holder.accept = view.findViewById(R.id.file_accept_button).asInstanceOf[View]
    holder.reject = view.findViewById(R.id.file_reject_button).asInstanceOf[View]
    holder.message.setTextSize(16)
    holder.message.setVisibility(View.GONE)
    holder.time.setVisibility(View.GONE)
    holder.sent.setVisibility(View.GONE)
    holder.received.setVisibility(View.GONE)
    holder.title.setVisibility(View.GONE)
    holder.progress.setVisibility(View.GONE)
    holder.imageMessage.setVisibility(View.GONE)
    holder.imageMessageFrame.setVisibility(View.GONE)
    holder.progressText.setVisibility(View.GONE)
    holder.padding.setVisibility(View.GONE)
    holder.buttons.setVisibility(View.GONE)
    messageType match {
      case Constants.MESSAGE_TYPE_OWN => 
        ownMessage(holder)
        holder.message.setText(msg.message)
        holder.message.setVisibility(View.VISIBLE)
        if (msg.sent) {
          if (msg.received) {
            holder.received.setVisibility(View.VISIBLE)
          } else {
            holder.sent.setVisibility(View.VISIBLE)
          }
        }

      case Constants.MESSAGE_TYPE_FRIEND => 
        friendMessage(holder)
        holder.message.setText(msg.message)
        holder.message.setVisibility(View.VISIBLE)

      case Constants.MESSAGE_TYPE_FILE_TRANSFER | Constants.MESSAGE_TYPE_FILE_TRANSFER_FRIEND => 
        ToxSingleton.fileSizeMap.put(id, size)
        if (messageType == Constants.MESSAGE_TYPE_FILE_TRANSFER) {
          ownMessage(holder)
          val split = msg.message.split("/")
          holder.message.setText(split(split.length - 1))
          holder.message.setVisibility(View.VISIBLE)
        } else {
          friendMessage(holder)
          holder.message.setText(msg.message)
          holder.message.setVisibility(View.VISIBLE)
        }
        holder.title.setVisibility(View.VISIBLE)
        holder.title.setText(R.string.chat_file_transfer)
        if (msg.received) {
          holder.progressText.setText("Finished")
          holder.progressText.setVisibility(View.VISIBLE)
        } else {
          if (msg.sent) {
            if (msg.message_id != -1) {
              holder.progress.setVisibility(View.VISIBLE)
              holder.progress.setMax(msg.size)
              holder.progress.setProgress(ToxSingleton.getProgress(msg.id))
              val progress = ToxSingleton.getProgressSinceXAgo(msg.id, 5000)
              var bytesPerSecond: Int = 0
              bytesPerSecond = if (progress != null) ((progress.x * 1000).toLong / progress.y).toInt else 0
              if (bytesPerSecond != 0) {
                val secondsToComplete = msg.size / bytesPerSecond
                holder.progressText.setText(java.lang.Integer.toString(bytesPerSecond / 1024) + " KiB/s, " + 
                  secondsToComplete + 
                  " seconds left")
              } else {
                holder.progressText.setText(java.lang.Integer.toString(bytesPerSecond / 1024) + " KiB/s")
              }
              holder.progressText.setVisibility(View.VISIBLE)
            } else {
              holder.progressText.setText("Failed")
              holder.progressText.setVisibility(View.VISIBLE)
            }
          } else {
            if (msg.message_id != -1) {
              if (msg.isMine) {
                holder.progressText.setText("Sent filesending request")
              } else {
                holder.progressText.setText("")
                holder.buttons.setVisibility(View.VISIBLE)
                holder.accept.setOnClickListener(new View.OnClickListener() {

                  override def onClick(view: View) {
                    ToxSingleton.acceptFile(k, message_id, context)
                  }
                })
                holder.reject.setOnClickListener(new View.OnClickListener() {

                  override def onClick(view: View) {
                    ToxSingleton.rejectFile(k, message_id, context)
                  }
                })
              }
            } else {
              holder.progressText.setText("Rejected")
            }
            holder.progressText.setVisibility(View.VISIBLE)
          }
        }
        if (msg.received || msg.isMine) {
          var f: File = null
          if (msg.message.contains("/")) {
            f = new File(msg.message)
          } else {
            f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), 
              Constants.DOWNLOAD_DIRECTORY)
            f = new File(f.getAbsolutePath + "/" + msg.message)
          }
          if (f.exists()) {
            val file = f
            val okFileExtensions = Array("jpg", "png", "gif", "jpeg")
            for (extension <- okFileExtensions) {
              Log.d("ChatMessagesAdapter", file.getName.toLowerCase())
              Log.d("ChatMessagesAdapter", extension)
              if (file.getName.toLowerCase().endsWith(extension)) {
                Log.d("ChatMessagesAdapter", "true")
                if (BitmapManager.checkValidImage(file)) {
                  BitmapManager.loadBitmap(file, file.getPath.hashCode, holder.imageMessage)
                  holder.imageMessage.setVisibility(View.VISIBLE)
                  holder.imageMessageFrame.setVisibility(View.VISIBLE)
                  holder.imageMessage.setOnClickListener(new View.OnClickListener() {

                    def onClick(v: View) {
                      val i = new Intent()
                      i.setAction(android.content.Intent.ACTION_VIEW)
                      i.setDataAndType(Uri.fromFile(file), "image/*")
                      ChatMessagesAdapter.this.context.startActivity(i)
                    }
                  })
                  if (msg.received) {
                    holder.message.setVisibility(View.GONE)
                    holder.title.setVisibility(View.GONE)
                    holder.progressText.setVisibility(View.GONE)
                  } else {
                    holder.padding.setVisibility(View.VISIBLE)
                  }
                }
                //break
              }
            }
          }
        }

      case Constants.MESSAGE_TYPE_ACTION => 
        holder.time.setGravity(Gravity.CENTER)
        holder.layout.setGravity(Gravity.CENTER)
        holder.message.setTextColor(context.getResources.getColor(R.color.gray_darker))
        holder.row.setGravity(Gravity.CENTER)
        holder.background.setBackgroundColor(context.getResources.getColor(R.color.white_absolute))
        holder.message.setTextSize(12)

    }
    holder.time.setText(PrettyTimestamp.prettyTimestamp(msg.time, true))
    holder.time.setVisibility(View.VISIBLE)
    if (!animatedIds.contains(id)) {
      holder.row.startAnimation(anim)
      animatedIds.add(id)
    }
    holder.row.setOnLongClickListener(new View.OnLongClickListener() {

      override def onLongClick(view: View): Boolean = {
        if (messageType == Constants.MESSAGE_TYPE_OWN || messageType == Constants.MESSAGE_TYPE_FRIEND) {
          val builder = new AlertDialog.Builder(context)
          val items = Array[CharSequence](context.getResources.getString(R.string.message_copy), context.getResources.getString(R.string.message_delete))
          builder.setCancelable(true).setItems(items, new DialogInterface.OnClickListener() {

            def onClick(dialog: DialogInterface, index: Int) = index match {
              case 0 => 
                var clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE).asInstanceOf[ClipboardManager]
                clipboard.setText(m)

              case 1 => 
                class DeleteMessage extends AsyncTask[Void, Void, Void] {

                  protected override def doInBackground(params: Void*): Void = {
                    val antoxDB = new AntoxDB(context.getApplicationContext)
                    antoxDB.deleteMessage(id)
                    antoxDB.close()
                    return null
                  }

                  protected override def onPostExecute(result: Void) {
                    ToxSingleton.updateMessages(context)
                  }
                }
                new DeleteMessage().execute()

            }
          })
          val alert = builder.create()
          alert.show()
        } else {
          val builder = new AlertDialog.Builder(context)
          val items = Array[CharSequence](context.getResources.getString(R.string.message_delete))
          builder.setCancelable(true).setItems(items, new DialogInterface.OnClickListener() {

            def onClick(dialog: DialogInterface, index: Int) = index match {
              case 0 => 
                class DeleteMessage extends AsyncTask[Void, Void, Void] {

                  protected override def doInBackground(params: Void*): Void = {
                    val antoxDB = new AntoxDB(context.getApplicationContext)
                    antoxDB.deleteMessage(id)
                    antoxDB.close()
                    return null
                  }

                  protected override def onPostExecute(result: Void) {
                    ToxSingleton.updateMessages(context)
                  }
                }
                new DeleteMessage().execute()

            }
          })
          val alert = builder.create()
          alert.show()
        }
        true
      }
    })
  }

  override def getViewTypeCount(): Int = 4

  private def ownMessage(holder: ChatMessagesHolder) {
    holder.time.setGravity(Gravity.RIGHT)
    holder.layout.setGravity(Gravity.RIGHT)
    holder.message.setTextColor(context.getResources.getColor(R.color.white_absolute))
    holder.row.setGravity(Gravity.RIGHT)
    holder.background.setBackgroundDrawable(context.getResources.getDrawable(R.drawable.chatright))
    holder.background.setPadding(1 * density * paddingscale, 1 * density * paddingscale, 6 * density + 1 * density * paddingscale, 
      1 * density * paddingscale)
  }

  private def friendMessage(holder: ChatMessagesHolder) {
    holder.message.setTextColor(context.getResources.getColor(R.color.black))
    holder.background.setBackgroundDrawable(context.getResources.getDrawable(R.drawable.chatleft))
    holder.background.setPadding(6 * density + 1 * density * paddingscale, 1 * density * paddingscale, 
      1 * density * paddingscale, 1 * density * paddingscale)
    holder.time.setGravity(Gravity.LEFT)
    holder.layout.setGravity(Gravity.LEFT)
    holder.row.setGravity(Gravity.LEFT)
  }
}
