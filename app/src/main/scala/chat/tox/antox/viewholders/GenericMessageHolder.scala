package chat.tox.antox.viewholders

import android.app.AlertDialog
import android.content.{ClipData, ClipboardManager, Context, DialogInterface}
import android.support.v7.widget.RecyclerView
import android.view.View.OnLongClickListener
import android.view.{Gravity, View}
import android.widget.{LinearLayout, TextView}
import chat.tox.antox.R
import chat.tox.antox.data.State
import chat.tox.antox.wrapper.{Message, MessageType}
import rx.lang.scala.Observable
import rx.lang.scala.schedulers.IOScheduler

abstract class GenericMessageHolder(val v: View) extends RecyclerView.ViewHolder(v) with OnLongClickListener {

  protected val bubble = v.findViewById(R.id.message_bubble).asInstanceOf[LinearLayout]

  protected val background = v.findViewById(R.id.message_text_background).asInstanceOf[LinearLayout]

  protected val row = v.findViewById(R.id.message_row_layout).asInstanceOf[LinearLayout]

  protected var time: TextView = v.findViewById(R.id.message_text_date).asInstanceOf[TextView]

  protected val sentTriangle = v.findViewById(R.id.sent_triangle)

  protected val receivedTriangle = v.findViewById(R.id.received_triangle)

  protected var message: Message = _

  private val density: Int = v.getContext.getResources.getDisplayMetrics.density.toInt

  def setMessage(message: Message): Unit = {
    this.message = message
    bubble.setOnLongClickListener(this)
  }

  def getMessage: Message = message

  def setTimestamp(date: String): Unit = {
    time.setText(date)
  }

  def ownMessage() {
    val context = v.getContext
    sentTriangle.setVisibility(View.VISIBLE)
    receivedTriangle.setVisibility(View.GONE)
    row.setGravity(Gravity.RIGHT)
    //Set extra padding to the left of the bubble
    bubble.setPadding(48 * density,0,0,0)
    background.setBackgroundDrawable(context.getResources.getDrawable(R.drawable.conversation_item_sent_shape))
  }

  def contactMessage() {
    val context = v.getContext
    receivedTriangle.setVisibility(View.VISIBLE)
    sentTriangle.setVisibility(View.GONE)
    row.setGravity(Gravity.LEFT)
    //Set extra padding to the right of the bubble
    bubble.setPadding(0,0,48 * density,0)
    background.setBackgroundDrawable(context.getResources.getDrawable(R.drawable.conversation_item_received_shape))
  }

  override def onLongClick(view: View): Boolean = {
    val context = view.getContext
    if (message.`type` == MessageType.OWN || message.`type` == MessageType.FRIEND) {
      val items = Array[CharSequence](context.getResources.getString(R.string.message_copy), context.getResources.getString(R.string.message_delete))
      new AlertDialog.Builder(context).setCancelable(true).setItems(items, new DialogInterface.OnClickListener() {

        def onClick(dialog: DialogInterface, index: Int): Unit = index match {
          case 0 =>
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE).asInstanceOf[ClipboardManager]
            clipboard.setPrimaryClip(ClipData.newPlainText(null, message.message))

          case 1 =>
            Observable[Boolean](subscriber => {
              val db = State.db
              db.deleteMessage(message.id)
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
              db.deleteMessage(message.id)
              subscriber.onCompleted()
            }).subscribeOn(IOScheduler()).subscribe()

        }
      }).create().show()
    }
    true
  }
}
