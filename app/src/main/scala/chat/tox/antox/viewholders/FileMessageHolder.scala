package chat.tox.antox.viewholders

import android.view.View
import android.widget.{LinearLayout, ImageView}
import chat.tox.antox.R

class FileMessageHolder(val view: View) extends GenericMessageHolder(view) {

  protected val imageMessage = view.findViewById(R.id.message_sent_photo).asInstanceOf[ImageView]

  protected val fileButtons = view.findViewById(R.id.file_buttons).asInstanceOf[LinearLayout]

  def setImage(): Unit = {
  }

}
