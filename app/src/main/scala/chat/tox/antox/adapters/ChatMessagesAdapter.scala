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
    System.out.println("ChatMessagesAdapter:add")
    data.add(msg)
    notifyDataSetChanged()
  }

  def addAll(list: Seq[Message]) {
    System.out.println("ChatMessagesAdapter:addAll")
    data.addAll(list)
    notifyDataSetChanged()
  }

  def remove(msg: Message) {
    System.out.println("ChatMessagesAdapter:remove")
    data.remove(msg)
    notifyDataSetChanged()
  }

  def removeAll() {
    System.out.println("ChatMessagesAdapter:removeAll")
    data.clear()
    notifyDataSetChanged()
  }

  def setScrolling(scrolling: Boolean) {
    System.out.println("ChatMessagesAdapter:setScrolling")
    this.scrolling = scrolling
  }

  override def getItemCount: Int = if (data == null) 0 else data.size

  override def onBindViewHolder(holder: GenericMessageHolder, pos: Int): Unit = {
    val msg = data.get(pos)
    val lastMsg: Option[Message] = data.lift(pos - 1)
    val nextMsg: Option[Message] = data.lift(pos + 1)

    try {
      System.out.println("onBindViewHolder:1:pos=" + pos + " msg=" + msg.message.substring(0, Math.min(8, msg.message.length())) + " view=" + holder)
    }
    catch {
      case e: Exception => {
        e.printStackTrace()
        System.out.println("onBindViewHolder:1:pos=" + pos + " msg=NULL view=" + holder)
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

        System.out.println("CMA:" + "holder.getMessage.isMine=" + holder.getMessage.isMine + " msg.messageId=" + msg.messageId)
        System.out.println("CMA:" + "msg.sent=" + msg.sent + " msg.isMine=" + msg.isMine + " msg.received=" + msg.received)

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
            System.out.println("CMA:" + "fileHolder.showProgressBar:1")
            fileHolder.showProgressBar()
          } else {
            //FIXME this should be "Failed" - fix the DB bug
            // TODO: zoff
            System.out.println("CMA:" + "fileHolder.setProgressText:file_finished")
            fileHolder.setProgressText(R.string.file_finished)
            fileHolder.hideCancelButton()
          }
        } else {
          if (msg.messageId != -1) {
            if (msg.isMine) {
              System.out.println("CMA:" + "fileHolder.setProgressText:file_request_sent")
              // fileHolder.setProgressText(R.string.file_request_sent) // this removes the progress bar!!
            } else {
              System.out.println("CMA:" + "fileHolder.showFileButtons")
              fileHolder.showFileButtons()
            }
          } else {
            System.out.println("CMA:" + "fileHolder.setProgressText:file_rejected")
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
                System.out.println("setImage:1:imageMessage:" + file.getName.substring(0, Math.min(40, file.getName.length())))
              }
              catch {
                case e: Exception => {
                  e.printStackTrace()
                  System.out.println("setImage:1:imageMessage:" + "NULL")
                }
              }
              fileHolder.setImage(file, true)
            }
            else {
              // also show icon for non image file types
              try {
                System.out.println("setImage:2:imageMessage:" + file.getName.substring(0, Math.min(40, file.getName.length())));
              }
              catch {
                case e: Exception => {
                  e.printStackTrace()
                  System.out.println("setImage:2:imageMessage:" + file.getName)
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
      return TimestampUtils.prettyTimestampLong(data.get(position).timestamp)
    }
    catch {
      case e: Exception => {
        e.printStackTrace()
        return " "
      }
    }
  }

  def getBubbleText(position: Int): CharSequence = {
    getHeaderString(position)
  }

  override def onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): GenericMessageHolder = {
    val inflater = LayoutInflater.from(viewGroup.getContext)

    System.out.println("onCreateViewHolder:type=" + viewType)

    viewType match {
      case TEXT =>
        System.out.println("onCreateViewHolder:TEXT")
        val v: View = inflater.inflate(R.layout.chat_message_row_text, viewGroup, false)
        new TextMessageHolder(v)

      case ACTION =>
        System.out.println("onCreateViewHolder:ACTION")
        val v: View = inflater.inflate(R.layout.chat_message_row_action, viewGroup, false)
        new ActionMessageHolder(v)

      case FILE =>
        System.out.println("onCreateViewHolder:FILE")
        val v: View = inflater.inflate(R.layout.chat_message_row_file, viewGroup, false)
        new FileMessageHolder(v)

      case CALL_INFO =>
        System.out.println("onCreateViewHolder:CALL_INFO")
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
