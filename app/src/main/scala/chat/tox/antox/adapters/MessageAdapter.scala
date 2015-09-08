package chat.tox.antox.adapters

import java.util

import android.support.v7.widget.RecyclerView
import android.view.{LayoutInflater, View, ViewGroup}
import chat.tox.antox.R
import chat.tox.antox.utils.TimestampUtils
import chat.tox.antox.viewholders.{ActionMessageHolder, FileMessageHolder, GenericMessageHolder, TextMessageHolder}
import chat.tox.antox.wrapper.{Message, MessageType}

class MessageAdapter extends RecyclerView.Adapter[GenericMessageHolder] {

  protected var data: util.ArrayList[Message] = new util.ArrayList[Message]

  private val TEXT = 1
  private val ACTION = 2
  private val FILE = 3

  def add(msg: Message) {
    data.add(msg)
    notifyDataSetChanged()
  }

  def addAll(list: util.ArrayList[Message]) {
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

  override def getItemCount: Int = if (data == null) 0 else data.size

  override def onBindViewHolder(holder: GenericMessageHolder, pos: Int): Unit = {
    val message = data.get(pos)
    holder.setMessage(message)
    holder.setTimestamp(TimestampUtils.prettyTimestamp(message.timestamp, isChat = true))

    if (holder.getMessage.isMine) {
      holder.ownMessage()
    } else {
      holder.contactMessage()
    }

    val viewType = getItemViewType(pos)
    viewType match {
      case TEXT =>
        val textHolder = holder.asInstanceOf[TextMessageHolder]
        textHolder.setText(message.message)

      case ACTION =>
        val actionHolder = holder.asInstanceOf[ActionMessageHolder]
        actionHolder.setText(message.senderName, message.message)

      case FILE =>
        val fileHolder = holder.asInstanceOf[FileMessageHolder]
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
      case MessageType.OWN | MessageType.GROUP_OWN | MessageType.FRIEND | MessageType.GROUP_PEER => TEXT
      case MessageType.ACTION | MessageType.GROUP_ACTION => ACTION
      case MessageType.FILE_TRANSFER | MessageType.FILE_TRANSFER_FRIEND => FILE
    }
  }

}
