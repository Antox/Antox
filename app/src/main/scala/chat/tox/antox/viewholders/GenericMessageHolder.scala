package chat.tox.antox.viewholders

import android.graphics.drawable.{GradientDrawable, LayerDrawable, RotateDrawable}
import android.os.Build
import android.support.v7.widget.RecyclerView
import android.view.{Gravity, View}
import android.widget.{LinearLayout, TextView}
import chat.tox.antox.R
import chat.tox.antox.theme.ThemeManager
import chat.tox.antox.utils.TimestampUtils
import chat.tox.antox.wrapper.Message

abstract class GenericMessageHolder(val v: View) extends RecyclerView.ViewHolder(v) {

  val row = v.findViewById(R.id.message_row_layout).asInstanceOf[LinearLayout]

  protected val bubble = v.findViewById(R.id.message_bubble).asInstanceOf[LinearLayout]

  protected val background = v.findViewById(R.id.message_text_background).asInstanceOf[LinearLayout]

  protected val messageText = v.findViewById(R.id.message_text).asInstanceOf[TextView]

  protected val time = v.findViewById(R.id.message_text_date).asInstanceOf[TextView]

  protected val sentTriangle = v.findViewById(R.id.sent_triangle).asInstanceOf[View]

  protected val receivedTriangle = v.findViewById(R.id.received_triangle).asInstanceOf[View]

  protected var msg: Message = _

  protected var lastMsg: Option[Message] = _

  protected var nextMsg: Option[Message] = _

  protected val context = v.getContext

  protected val backgroundViews = List(background, receivedTriangle, sentTriangle)

  private val density: Int = v.getContext.getResources.getDisplayMetrics.density.toInt

  def setMessage(message: Message, lastMsg: Option[Message], nextMsg: Option[Message]): Unit = {
    this.msg = message
    this.lastMsg = lastMsg
    this.nextMsg = nextMsg
  }

  def getMessage: Message = msg

  def setTimestamp(): Unit = {
    val messageTimeSeparation = 60 // the amount of time between messages needed for them to show a timestamp
    val delayedMessage =
      nextMsg
        .map(nextMessage => (nextMessage.timestamp.getTime - msg.timestamp.getTime) / 1000)
        .forall(_ > messageTimeSeparation + 1)

    val differentSender = nextMsg.exists(_.senderName != msg.senderName)

    if (nextMsg.isEmpty || differentSender || delayedMessage) {
      time.setText(TimestampUtils.prettyTimestamp(msg.timestamp, isChat = true))
      time.setVisibility(View.VISIBLE)
    } else {
      time.setVisibility(View.GONE)
    }
  }

  def ownMessage() {
    val context = v.getContext
    sentTriangle.setVisibility(View.VISIBLE)
    receivedTriangle.setVisibility(View.GONE)
    row.setGravity(Gravity.RIGHT)
    //Set extra padding to the left of the bubble
    bubble.setPadding(48 * density, 0, 0, 0)
    toggleReceived()

    val drawable = context.getResources.getDrawable(R.drawable.conversation_item_sent_shape).asInstanceOf[LayerDrawable]
    val shape = drawable.findDrawableByLayerId(R.id.sent_shape).asInstanceOf[GradientDrawable]
    shape.setColor(ThemeManager.primaryColor)

    val drawableTriangle = context.getResources.getDrawable(R.drawable.conversation_item_sent_triangle_shape).asInstanceOf[LayerDrawable]
    val shapeRotateTriangle = drawableTriangle.findDrawableByLayerId(R.id.sent_triangle_shape).asInstanceOf[RotateDrawable]
    val shapeTriangle = shapeRotateTriangle.getDrawable.asInstanceOf[GradientDrawable]
    shapeTriangle.setColor(ThemeManager.primaryColor)

    background.setBackgroundDrawable(drawable)
    sentTriangle.setBackgroundDrawable(shapeRotateTriangle)
  }

  def contactMessage() {
    val context = v.getContext
    receivedTriangle.setVisibility(View.VISIBLE)
    sentTriangle.setVisibility(View.GONE)
    row.setGravity(Gravity.LEFT)
    //Set extra padding to the right of the bubble
    bubble.setPadding(0, 0, 48 * density, 0)
    toggleReceived()
    background.setBackgroundDrawable(context.getResources.getDrawable(R.drawable.conversation_item_received_shape))
  }

  protected def toggleReceived(): Unit = {
    if (msg.received) {
      setAlphaCompat(bubble, 1f)
    } else {
      setAlphaCompat(bubble, 0.5f)
    }
  }

  //utility method to set view's alpha on honeycomb+ devices,
  //does nothing on pre-honeycomb devices because setAlpha is unsupported
  protected def setAlphaCompat(view: View, value: Float): Unit = {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
      //do nothing
    } else {
      view.setAlpha(value)
    }
  }
}
