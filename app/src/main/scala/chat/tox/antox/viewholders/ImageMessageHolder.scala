package chat.tox.antox.viewholders

import android.view.View
import android.widget.ImageView
import chat.tox.antox.R

class ImageMessageHolder(var view: View) extends GenericMessageHolder(view) {

  protected val imageMessage = view.findViewById(R.id.message_sent_photo).asInstanceOf[ImageView]
}
