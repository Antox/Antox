package chat.tox.antox.viewholders

import android.support.v7.widget.RecyclerView
import android.view.{Gravity, View}
import android.widget.{LinearLayout, TextView}
import chat.tox.antox.R
import chat.tox.antox.wrapper.Message

abstract class GenericMessageHolder(val v: View) extends RecyclerView.ViewHolder(v) {

  val row = v.findViewById(R.id.message_row_layout).asInstanceOf[LinearLayout]

  protected val bubble = v.findViewById(R.id.message_bubble).asInstanceOf[LinearLayout]

  protected val background = v.findViewById(R.id.message_text_background).asInstanceOf[LinearLayout]

  protected val messageText = v.findViewById(R.id.message_text).asInstanceOf[TextView]

  protected val time = v.findViewById(R.id.message_text_date).asInstanceOf[TextView]

  protected val sentTriangle = v.findViewById(R.id.sent_triangle)

  protected val receivedTriangle = v.findViewById(R.id.received_triangle)

  protected var message: Message = _

  protected val context = v.getContext

  private val density: Int = v.getContext.getResources.getDisplayMetrics.density.toInt

  def setMessage(message: Message): Unit = {
    this.message = message
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
    bubble.setPadding(48 * density, 0, 0, 0)
    background.setBackgroundDrawable(context.getResources.getDrawable(R.drawable.conversation_item_sent_shape))
  }

  def contactMessage() {
    val context = v.getContext
    receivedTriangle.setVisibility(View.VISIBLE)
    sentTriangle.setVisibility(View.GONE)
    row.setGravity(Gravity.LEFT)
    //Set extra padding to the right of the bubble
    bubble.setPadding(0, 0, 48 * density, 0)
    background.setBackgroundDrawable(context.getResources.getDrawable(R.drawable.conversation_item_received_shape))
  }

}
