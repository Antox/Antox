package chat.tox.antox.viewholders

import android.view.View
import android.widget.ImageView
import chat.tox.antox.R

class CallEventMessageHolder(val view: View) extends GenericMessageHolder(view) {

  private val prefixedIcon = view.findViewById(R.id.prefixed_icon).asInstanceOf[ImageView]

  def setText(msg: String): Unit = {
    messageText.setText(msg)
  }

  def setPrefixedIcon(imageRes: Int): Unit = {
    prefixedIcon.setImageBitmap(null)
    prefixedIcon.setImageResource(imageRes)
  }

}
