package chat.tox.antox.adapters

import java.io.File
import java.util

import android.app.AlertDialog
import android.content._
import android.graphics.Typeface
import android.net.Uri
import android.os.{Build, Environment}
import android.text.Html
import android.view.View.OnLongClickListener
import android.view.animation.{Animation, AnimationUtils}
import android.view.{Gravity, LayoutInflater, View, ViewGroup}
import android.widget._
import chat.tox.antox.R
import chat.tox.antox.adapters.ChatMessagesAdapter._
import chat.tox.antox.data.State
import chat.tox.antox.utils.{BitmapManager, Constants, TimestampUtils, UiUtils}
import chat.tox.antox.wrapper.{Message, MessageType}
import rx.lang.scala.Observable
import rx.lang.scala.schedulers.IOScheduler

import scala.collection.JavaConversions._
import scala.collection.mutable

object ChatMessagesAdapter {

  class ChatMessagesHolder {

    var row: LinearLayout = _

    var layout: LinearLayout = _

    var background: LinearLayout = _

    var message: TextView = _

    var time: TextView = _

    var imageMessage: ImageView = _

    var imageMessageFrame: FrameLayout = _

    var title: TextView = _

    var progress: ProgressBar = _

    var progressText: TextView = _

    var padding: View = _

    var buttons: LinearLayout = _

    var accept: View = _

    var reject: View = _

    var bubble: LinearLayout = _

    var wrapper: LinearLayout = _

    var sentTriangle: View = _

    var receivedTriangle: View = _
  }

}

class ChatMessagesAdapter(var context: Context, messages: util.ArrayList[Message])
  extends ArrayAdapter[Message](context, R.layout.chat_message_row, messages) {

  var layoutResourceId: Int = R.layout.chat_message_row

  private val density: Int = context.getResources.getDisplayMetrics.density.toInt

  private val anim: Animation = AnimationUtils.loadAnimation(this.context, R.anim.abc_slide_in_bottom)

  private val mInflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE).asInstanceOf[LayoutInflater]

  private val animatedIds: mutable.Set[Int] = mutable.Set(messages.map(_.id):_*)

  override def getView(position: Int, convertView: View, parent: ViewGroup): View = {
    var view: View = null
    var holder: ChatMessagesHolder = null

    val msg = getItem(position)

    val lastMsg: Option[Message] = messages.lift(position - 1)
    val nextMsg: Option[Message] = messages.lift(position + 1)

    if (convertView == null) {
      view = mInflater.inflate(this.layoutResourceId, parent, false)

      holder = new ChatMessagesHolder()
      holder.message = view.findViewById(R.id.message_text).asInstanceOf[TextView]
      holder.layout = view.findViewById(R.id.message_text_layout).asInstanceOf[LinearLayout]
      holder.row = view.findViewById(R.id.message_row_layout).asInstanceOf[LinearLayout]
      holder.background = view.findViewById(R.id.message_text_background).asInstanceOf[LinearLayout]
      holder.time = view.findViewById(R.id.message_text_date).asInstanceOf[TextView]
      holder.title = view.findViewById(R.id.message_title).asInstanceOf[TextView]
      holder.progress = view.findViewById(R.id.file_transfer_progress).asInstanceOf[ProgressBar]
      holder.imageMessage = view.findViewById(R.id.message_sent_photo).asInstanceOf[ImageView]
      holder.imageMessageFrame = view.findViewById(R.id.message_sent_photo_frame).asInstanceOf[FrameLayout]
      holder.progressText = view.findViewById(R.id.file_transfer_progress_text).asInstanceOf[TextView]
      holder.padding = view.findViewById(R.id.file_transfer_padding)
      holder.buttons = view.findViewById(R.id.file_buttons).asInstanceOf[LinearLayout]
      holder.accept = view.findViewById(R.id.file_accept_button)
      holder.reject = view.findViewById(R.id.file_reject_button)
      holder.bubble = view.findViewById(R.id.message_bubble).asInstanceOf[LinearLayout]
      holder.wrapper = view.findViewById(R.id.message_background_wrapper).asInstanceOf[LinearLayout]
      holder.sentTriangle = view.findViewById(R.id.sent_triangle)
      holder.receivedTriangle = view.findViewById(R.id.received_triangle)

      view.setTag(holder)
    } else {
      view = convertView
      holder = view.getTag.asInstanceOf[ChatMessagesHolder]
    }

    view.setLongClickable(true)

    val messageTextSize = 16
    holder.message.setTextSize(messageTextSize)
    holder.message.setVisibility(View.GONE)
    holder.time.setVisibility(View.GONE)
    holder.title.setVisibility(View.GONE)
    holder.progress.setVisibility(View.GONE)
    holder.imageMessage.setVisibility(View.GONE)
    holder.imageMessageFrame.setVisibility(View.GONE)
    holder.progressText.setVisibility(View.GONE)
    holder.padding.setVisibility(View.GONE)
    holder.buttons.setVisibility(View.GONE)
    holder.sentTriangle.setVisibility(View.GONE)
    holder.receivedTriangle.setVisibility(View.GONE)
    setAlpha(holder.bubble, 1f)
    msg.`type` match {
      case MessageType.OWN | MessageType.GROUP_OWN =>
        holder.message.setText(msg.message)
        ownMessage(holder)
        holder.message.setVisibility(View.VISIBLE)
        if (!msg.received) {
          setAlpha(holder.bubble, 0.5f)
        }

      case MessageType.GROUP_PEER =>
        holder.message.setText(msg.message)
        holder.title.setText(msg.senderName)

        // generate name colour from hash to ensure names have consistent colours
        groupMessage(holder, UiUtils.generateColor(msg.senderName.hashCode))
        holder.message.setVisibility(View.VISIBLE)
        if (lastMsg.isEmpty  || msg.senderName != lastMsg.get.senderName) {
          holder.title.setVisibility(View.VISIBLE)
        }
        if (!msg.received) {
          setAlpha(holder.bubble, 0.5f)
        }

      case MessageType.FRIEND =>
        holder.message.setText(msg.message)
        contactMessage(holder)
        holder.message.setVisibility(View.VISIBLE)

      case MessageType.FILE_TRANSFER | MessageType.FILE_TRANSFER_FRIEND =>
        if (msg.`type` == MessageType.FILE_TRANSFER) {
          ownMessage(holder)
          val split = msg.message.split("/")
          holder.message.setText(split(split.length - 1))
          holder.message.setVisibility(View.VISIBLE)
        } else {
          contactMessage(holder)
          holder.message.setText(msg.message)
          holder.message.setVisibility(View.VISIBLE)
        }
        holder.title.setVisibility(View.VISIBLE)
        holder.title.setText(R.string.chat_file_transfer)
        if (msg.received) {
          holder.progressText.setText(context.getResources.getString(R.string.file_finished))
          holder.progressText.setVisibility(View.VISIBLE)
        } else {
          if (msg.sent) {
            if (msg.messageId != -1) {
              holder.progress.setVisibility(View.VISIBLE)
              holder.progress.setMax(msg.size)
              holder.progress.setProgress(State.transfers.getProgress(msg.id).toInt)
              val updateRate = 500
              val mProgress = State.transfers.getProgressSinceXAgo(msg.id, updateRate)
              val bytesPerSecond = mProgress match {
                case Some(p) => ((p._1 * 1000) / p._2).toInt
                case None => 0
              }
              if (bytesPerSecond != 0) {
                val secondsToComplete = msg.size / bytesPerSecond
                holder.progressText.setText(java.lang.Integer.toString(bytesPerSecond / 1024) + " KiB/s, " +
                  context.getResources.getString(R.string.file_time_remaining, secondsToComplete.toString))
              } else {
                holder.progressText.setText(java.lang.Integer.toString(bytesPerSecond / 1024) + " KiB/s")
              }
              holder.progressText.setVisibility(View.VISIBLE)
            } else {
              //FIXME this should be "Failed" - fix the DB bug
              holder.progressText.setText(context.getResources.getString(R.string.file_finished))
              holder.progressText.setVisibility(View.VISIBLE)
            }
          } else {
            if (msg.messageId != -1) {
              if (msg.isMine) {
                holder.progressText.setText(context.getResources.getString(R.string.file_request_sent))
              } else {
                holder.progressText.setText("")
                holder.buttons.setVisibility(View.VISIBLE)
                holder.accept.setOnClickListener(new View.OnClickListener() {

                  override def onClick(view: View) {
                    State.transfers.acceptFile(msg.key, msg.messageId, context)
                  }
                })
                holder.reject.setOnClickListener(new View.OnClickListener() {

                  override def onClick(view: View) {
                    State.transfers.rejectFile(msg.key, msg.messageId, context)
                  }
                })
              }
            } else {
              holder.progressText.setText(context.getResources.getString(R.string.file_rejected))
            }
            holder.progressText.setVisibility(View.VISIBLE)
          }
        }
        if (msg.received || msg.isMine) {
          val file: File =
            if (msg.message.contains("/")) {
              new File(msg.message)
            } else {
              val f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                Constants.DOWNLOAD_DIRECTORY)
              new File(f.getAbsolutePath + "/" + msg.message)
            }

          if (file.exists()) {
            val okFileExtensions = Array("jpg", "png", "gif", "jpeg")
            for (extension <- okFileExtensions) {
              if (file.getName.toLowerCase.endsWith(extension)) {
                // Set a placeholder in the image in case bitmap needs to be loaded from disk
                if (msg.isMine) {
                  holder.imageMessage.setImageResource(R.drawable.sent)
                } else {
                  holder.imageMessage.setImageResource(R.drawable.received)
                }

                BitmapManager.load(file, holder.imageMessage, isAvatar = false)
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
                holder.message.setVisibility(View.GONE)
                holder.title.setVisibility(View.GONE)
                holder.progressText.setVisibility(View.GONE)
              }
            }
          }
        }

      case MessageType.ACTION =>
        actionMessage(holder)
        holder.message.setText(Html.fromHtml("<b>" + msg.senderName + "</b> ") + msg.message)

    }

    val messageTimeSeparation = 60 // the amount of time between messages needed for them to show a timestamp
    val delayedMessage =
      nextMsg
        .map(nextMessage => (nextMessage.timestamp.getTime - msg.timestamp.getTime) / 1000)
        .map(_ > messageTimeSeparation + 1).getOrElse(true)

    val differentSender = nextMsg.exists(_.senderName != msg.senderName)

    if (nextMsg.isEmpty || differentSender || delayedMessage) {
      holder.time.setText(TimestampUtils.prettyTimestamp(msg.timestamp, isChat = true))
      holder.time.setVisibility(View.VISIBLE)
    } else {
      holder.time.setVisibility(View.GONE)
    }

    if (!animatedIds.contains(msg.id)) {
      holder.row.startAnimation(anim)
      animatedIds += msg.id
    }

    holder.background.setOnLongClickListener(new OnLongClickListener() {

      override def onLongClick(view: View): Boolean = {
        if (msg.`type` == MessageType.OWN || msg.`type` == MessageType.FRIEND) {
          val items = Array[CharSequence](context.getResources.getString(R.string.message_copy), context.getResources.getString(R.string.message_delete))
          new AlertDialog.Builder(context).setCancelable(true).setItems(items, new DialogInterface.OnClickListener() {

            def onClick(dialog: DialogInterface, index: Int): Unit = index match {
              case 0 =>
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE).asInstanceOf[ClipboardManager]
                clipboard.setPrimaryClip(ClipData.newPlainText(null, msg.message))

              case 1 =>
                Observable[Boolean](subscriber => {
                  val db = State.db
                  db.deleteMessage(msg.id)
                  subscriber.onCompleted()
                }).subscribeOn(IOScheduler()).subscribe()
            }

          }).create().show()
        }
        else {
          val items = Array[CharSequence](context.getResources.getString(R.string.message_delete))
          new AlertDialog.Builder(context).setCancelable(true).setItems(items, new DialogInterface.OnClickListener() {

            def onClick(dialog: DialogInterface, index: Int): Unit = index match {
              case 0 =>
                Observable[Boolean](subscriber => {
                  val db = State.db
                  db.deleteMessage(msg.id)
                  subscriber.onCompleted()
                }).subscribeOn(IOScheduler()).subscribe()

            }
          }).create().show()
        }
        true
      }
    })

    view
  }

  override def getViewTypeCount: Int = MessageType.values.size

  //override def newDropDownView(context: Context, cursor: Cursor, parent: ViewGroup): View = super.newDropDownView(context, cursor, parent)

  //utility method to set view's alpha on honeycomb+ devices,
  //does nothing on pre-honeycomb devices because setAlpha is unsupported
  private def setAlpha(view: View, value: Float): Unit = {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
      //do nothing
    } else {
      view.setAlpha(value)
    }
  }

  private def shouldGreentext(message: String): Boolean = {
    message.startsWith(">")
  }

  private def ownMessage(holder: ChatMessagesHolder) {
    holder.time.setGravity(Gravity.RIGHT)
    holder.layout.setGravity(Gravity.RIGHT)
    holder.sentTriangle.setVisibility(View.VISIBLE)
    if (shouldGreentext(holder.message.getText.toString)) {
      holder.message.setTextColor(context.getResources.getColor(R.color.green_light))
    } else {
      holder.message.setTextColor(context.getResources.getColor(R.color.white))
    }

    holder.row.setGravity(Gravity.RIGHT)
    holder.wrapper.setGravity(Gravity.RIGHT)
    holder.background.setBackgroundDrawable(context.getResources.getDrawable(R.drawable.conversation_item_sent_shape))
    holder.background.setPadding(8 * density, 8 * density, 8 * density, 8 * density)
  }

  private def groupMessage(holder: ChatMessagesHolder, nameColor: Int) {
    contactMessage(holder)
    holder.title.setTextColor(nameColor)
    holder.title.setTypeface(holder.title.getTypeface, Typeface.BOLD)
  }

  private def contactMessage(holder: ChatMessagesHolder) {
    if (shouldGreentext(holder.message.getText.toString)) {
      holder.message.setTextColor(context.getResources.getColor(R.color.green))
    } else {
      holder.message.setTextColor(context.getResources.getColor(R.color.black))
    }
    holder.receivedTriangle.setVisibility(View.VISIBLE)
    holder.time.setGravity(Gravity.LEFT)
    holder.layout.setGravity(Gravity.LEFT)
    holder.row.setGravity(Gravity.LEFT)
    holder.wrapper.setGravity(Gravity.LEFT)
    holder.background.setBackgroundDrawable(context.getResources.getDrawable(R.drawable.conversation_item_received_shape))
    holder.background.setPadding(8 * density, 8 * density, 8 * density, 8 * density)
  }

  private def actionMessage(holder: ChatMessagesHolder) {
    holder.message.setVisibility(View.VISIBLE)
    val actionMessageFontSize = 18
    holder.message.setTextSize(actionMessageFontSize)
    holder.message.setTextColor(context.getResources.getColor(R.color.black))
    holder.time.setGravity(Gravity.CENTER)
    holder.layout.setGravity(Gravity.CENTER)
    holder.row.setGravity(Gravity.CENTER)
    holder.wrapper.setGravity(Gravity.CENTER)
    holder.background.setBackgroundColor(context.getResources.getColor(R.color.white_absolute))
    holder.background.setPadding(8 * density, 8 * density, 8 * density, 8 * density)
    holder.time.setVisibility(View.VISIBLE)
  }
}
