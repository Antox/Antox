package chat.tox.antox.adapters

import java.io.File
import java.util

import android.content.Context
import android.os.Environment
import android.support.v7.widget.RecyclerView
import android.view.{LayoutInflater, View, ViewGroup}
import chat.tox.antox.R
import chat.tox.antox.utils.{Constants, FileUtils}
import chat.tox.antox.viewholders._
import chat.tox.antox.wrapper.{Message, MessageType}

import scala.collection.JavaConversions._

class ChatMessagesAdapter(context: Context, data: util.ArrayList[Message]) extends RecyclerView.Adapter[GenericMessageHolder] {

  private val TEXT = 1
  private val ACTION = 2
  private val FILE = 3

  private var scrolling: Boolean = false

  def add(msg: Message) {
    data.add(msg)
    notifyDataSetChanged()
  }

  def addAll(list: Seq[Message]) {
    data.addAll(list)
    notifyDataSetChanged()
  }

  def remove(msg: Message) {
    data.remove(msg)
    notifyDataSetChanged()
  }

  def removeAll() {
    data.clear()
    notifyDataSetChanged()
  }

  def setScrolling(scrolling: Boolean) {
    this.scrolling = scrolling
  }

  override def getItemCount: Int = if (data == null) 0 else data.size

  override def onBindViewHolder(holder: GenericMessageHolder, pos: Int): Unit = {
    val msg = data.get(pos)
    val lastMsg: Option[Message] = data.lift(pos - 1)
    val nextMsg: Option[Message] = data.lift(pos + 1)

    holder.setMessage(msg, lastMsg, nextMsg)
    holder.setTimestamp()

    val viewType = getItemViewType(pos)
    viewType match {
      case TEXT =>
        if (holder.getMessage.isMine) {
          holder.ownMessage()
        } else {
          holder.contactMessage()
        }
        val textHolder = holder.asInstanceOf[TextMessageHolder]
        textHolder.setText(msg.message)

      case ACTION =>
        val actionHolder = holder.asInstanceOf[ActionMessageHolder]
        actionHolder.setText(msg.senderName, msg.message)

      case FILE =>
        val fileHolder = holder.asInstanceOf[FileMessageHolder]

        fileHolder.render()

        if (holder.getMessage.isMine) {
          holder.ownMessage()
          val split = msg.message.split("/")
          fileHolder.setFileText(split(split.length - 1))
        } else {
          holder.contactMessage()
          fileHolder.setFileText(msg.message)
        }

        if (msg.sent) {
          if (msg.messageId != -1) {
            fileHolder.showProgressBar()
          } else {
            //FIXME this should be "Failed" - fix the DB bug
            fileHolder.setProgressText(R.string.file_finished)
          }
        } else {
          if (msg.messageId != -1) {
            if (msg.isMine) {
              fileHolder.setProgressText(R.string.file_request_sent)
            } else {
              fileHolder.showFileButtons()
            }
          } else {
            fileHolder.setProgressText(R.string.file_rejected)
          }
        }

        if (msg.received || msg.isMine) {
          val file =
            if (msg.message.contains("/")) {
              new File(msg.message)
            } else {
              val f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                Constants.DOWNLOAD_DIRECTORY)
              new File(f.getAbsolutePath + "/" + msg.message)
            }

          val isImage = (s"^.+?\\.(${FileUtils.imageExtensions.mkString("|")})" + "$").r.findAllMatchIn(file.getName.toLowerCase).nonEmpty

          println("FILE LENGTH is " + file.length())
          if (file.exists() && isImage && file.length > 0) {
            fileHolder.setImage(file)
          }
        }
    }
  }

  override def onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): GenericMessageHolder = {
    val inflater = LayoutInflater.from(viewGroup.getContext)

    viewType match {
      case TEXT =>
        val v: View = inflater.inflate(R.layout.chat_message_row_text, viewGroup, false)
        new TextMessageHolder(v)

      case ACTION =>
        val v: View = inflater.inflate(R.layout.chat_message_row_action, viewGroup, false)
        new ActionMessageHolder(v)

      case FILE =>
        val v: View = inflater.inflate(R.layout.chat_message_row_file, viewGroup, false)
        new FileMessageHolder(v)

    }
  }

  //TODO It would be better to use Ints instead of Enums for MessageType
  override def getItemViewType(pos: Int): Int = {
    val messageType = data.get(pos).`type`
    messageType match {
      case MessageType.MESSAGE | MessageType.GROUP_MESSAGE => TEXT
      case MessageType.ACTION | MessageType.GROUP_ACTION => ACTION
      case MessageType.FILE_TRANSFER => FILE
    }
  }

}
