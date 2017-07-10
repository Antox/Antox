package chat.tox.antox.viewholders

import android.app.AlertDialog
import android.content.{ClipData, ClipboardManager, Context, DialogInterface}
import android.graphics.PorterDuff
import android.view.View.{OnLongClickListener, OnTouchListener}
import android.view.{MotionEvent, View}
import android.widget.TextView
import chat.tox.antox.R
import chat.tox.antox.data.State
import chat.tox.antox.utils.UiUtils
import chat.tox.antox.wrapper.MessageType
import rx.lang.scala.Observable
import rx.lang.scala.schedulers.IOScheduler

class TextMessageHolder(val view: View) extends GenericMessageHolder(view) with OnLongClickListener with OnTouchListener {

  protected val messageTitle = view.findViewById(R.id.message_title).asInstanceOf[TextView]
  private var isLongClick = false

  def setText(s: String): Unit = {
    messageText.setText(s)
    messageText.setOnLongClickListener(this)
    messageText.setOnTouchListener(this)

    // Reset the visibility for non-group messages
    messageTitle.setVisibility(View.GONE)
    if (msg.isMine) {
      if (shouldGreentext(s)) {
        messageText.setTextColor(context.getResources.getColor(R.color.green_light))
      } else {
        messageText.setTextColor(context.getResources.getColor(R.color.white))
      }
    } else {
      if (shouldGreentext(s)) {
        messageText.setTextColor(context.getResources.getColor(R.color.green))
      } else {
        messageText.setTextColor(context.getResources.getColor(R.color.black))
      }
      if (msg.`type` == MessageType.GROUP_MESSAGE) {
        groupMessage()
      }
    }
  }

  def groupMessage() {
    messageText.setText(msg.message)
    messageTitle.setText(msg.senderName)
    toggleReceived()
    // generate name colour from hash to ensure names have consistent colours
    UiUtils.generateColor(msg.senderName.hashCode)
    if (lastMsg.isEmpty || msg.senderName != lastMsg.get.senderName) {
      messageTitle.setVisibility(View.VISIBLE)
    }
    messageTitle.setTextColor(UiUtils.generateColor(msg.senderName.hashCode))
    contactMessage()
  }

  private def shouldGreentext(message: String): Boolean = {
    message.startsWith(">")
  }

  override def onLongClick(view: View): Boolean = {
    val items = Array[CharSequence](context.getResources.getString(R.string.message_copy), context.getResources.getString(R.string.message_delete))
    isLongClick = true
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

    true
  }

  override def onTouch(v: View, event: MotionEvent): Boolean = {
    event.getAction match {
      case MotionEvent.ACTION_DOWN =>
        for (view <- backgroundViews) {
          view.getBackground.setColorFilter(0x55000000, PorterDuff.Mode.SRC_ATOP)
          view.invalidate()
        }

      case MotionEvent.ACTION_CANCEL | MotionEvent.ACTION_UP =>
        for (view <- backgroundViews) {
          view.getBackground.clearColorFilter()
          view.invalidate()
        }

      case _ => //do nothing
    }

    //bizarre workaround to prevent long pressing on a link causing the link to open on release
    event.getAction match {
      case MotionEvent.ACTION_DOWN =>
        isLongClick = false
        v.onTouchEvent(event)

      case MotionEvent.ACTION_UP if isLongClick =>
        isLongClick = false
        true // if we're in a long click ignore the release action

      case _ => //do nothing
        v.onTouchEvent(event)
    }
  }
}
