package chat.tox.antox.adapters

import java.io.File
import java.util

import android.content.Context
import android.os.Environment
import android.support.v7.widget.RecyclerView
import android.view.{LayoutInflater, View, ViewGroup}
import chat.tox.antox.R
import chat.tox.antox.utils.{Constants, FileUtils, TimestampUtils}
import chat.tox.antox.viewholders._
import chat.tox.antox.wrapper.{Message, MessageType}

import scala.collection.JavaConversions._

class ChatMessagesAdapter(context: Context, data: util.ArrayList[Message]) extends RecyclerView.Adapter[GenericMessageHolder] {

  private val TEXT = 1
  private val ACTION = 2
  private val FILE = 3
  private val CALL_INFO = 4

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

    try {
    }
    catch {
      case e: Exception => {
        e.printStackTrace()
      }
    }



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

      case CALL_INFO =>
        val callEventHolder = holder.asInstanceOf[CallEventMessageHolder]
        callEventHolder.setText(msg.message)
        callEventHolder.setPrefixedIcon(msg.callEventKind.imageRes)


      case FILE =>
        val fileHolder = holder.asInstanceOf[FileMessageHolder]

        fileHolder.render()


        if (holder.getMessage.isMine) {
          holder.ownMessage()
          // show only filename of file (remove path)
          val split = msg.message.split("/")
          fileHolder.setFileText(split(split.length - 1))
        } else {
          holder.contactMessage()
          // when receiving file there is only filename, no path
          fileHolder.setFileText(msg.message)
        }

        if (msg.sent) {
          if (msg.messageId != -1) {
            fileHolder.showProgressBar()
          } else {
            //FIXME this should be "Failed" - fix the DB bug
            // TODO: zoff
            fileHolder.setProgressText(R.string.file_finished)
            fileHolder.hideCancelButton()
          }
        } else {
          if (msg.messageId != -1) {
            if (msg.isMine) {
              // fileHolder.setProgressText(R.string.file_request_sent) // this removes the progress bar!!
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

          // val extension = getFileExtensionFromUrl(file.getAbsolutePath())
          val isImage = (s"^.+?\\.(${FileUtils.imageExtensions.mkString("|")})" + "$").r.findAllMatchIn(file.getName.toLowerCase).nonEmpty

          // println("FILE LENGTH is " + file.length())
          if (file.exists() && file.length > 0) {
            if (isImage) {
              try {
              }
              catch {
                case e: Exception => {
                  e.printStackTrace()
                }
              }
              fileHolder.setImage(file, true)
            }
            else {
              // also show icon for non image file types
              try {
              }
              catch {
                case e: Exception => {
                  e.printStackTrace()
                }
              }
              fileHolder.setImage(file, false)
            }
          }
        }
    }
  }

  def getHeaderString(position: Int): String = {
    try {
      TimestampUtils.prettyTimestampLong(data.get(position).timestamp)
    } catch {
      case e: Exception =>
        e.printStackTrace()
        " "
    }
  }

  def getBubbleText(position: Int): CharSequence = {
    getHeaderString(position)
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

      case CALL_INFO =>
        val v: View = inflater.inflate(R.layout.chat_message_row_call_event, viewGroup, false)
        new CallEventMessageHolder(v)
    }
  }

  //TODO It would be better to use Ints instead of Enums for MessageType
  override def getItemViewType(pos: Int): Int = {
    val messageType = data.get(pos).`type`
    messageType match {
      case MessageType.MESSAGE | MessageType.GROUP_MESSAGE => TEXT
      case MessageType.ACTION | MessageType.GROUP_ACTION => ACTION
      case MessageType.FILE_TRANSFER => FILE
      case MessageType.CALL_EVENT => CALL_INFO
    }
  }

}
