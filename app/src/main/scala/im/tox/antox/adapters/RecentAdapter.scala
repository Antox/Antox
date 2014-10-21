package im.tox.antox.adapters

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Build
import android.support.v4.widget.ResourceCursorAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import java.sql.Timestamp
import im.tox.antox.R
import im.tox.antox.activities.ChatActivity
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.utils.IconColor
import im.tox.antox.utils.PrettyTimestamp
import im.tox.antox.utils.UserStatus
import RecentAdapter._

object RecentAdapter {

  class FriendsListHolder {

    var icon: TextView = _

    var friendName: TextView = _

    var friendStatus: TextView = _

    var timestamp: TextView = _

    var unreadCount: TextView = _
  }
}

class RecentAdapter(var context: Context, c: Cursor) extends ResourceCursorAdapter(context, R.layout.contact_list_item,
  c, 0) {

  var layoutResourceId: Int = R.layout.contact_list_item

  private var mInflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE).asInstanceOf[LayoutInflater]

  override def newView(context: Context, cursor: Cursor, parent: ViewGroup): View = {
    mInflater.inflate(this.layoutResourceId, parent, false)
  }

  override def bindView(view: View, context: Context, cursor: Cursor) {
    val tox_key = cursor.getString(0)
    val username = cursor.getString(1)
    val online = cursor.getInt(2) != 0
    val status = cursor.getString(3)
    val time = Timestamp.valueOf(cursor.getString(4))
    val message = cursor.getString(5)
    val unreadCount = cursor.getInt(6)
    val holder = new FriendsListHolder()
    holder.icon = view.findViewById(R.id.icon).asInstanceOf[TextView]
    holder.friendName = view.findViewById(R.id.friend_name).asInstanceOf[TextView]
    holder.friendStatus = view.findViewById(R.id.friend_status).asInstanceOf[TextView]
    holder.timestamp = view.findViewById(R.id.last_message_timestamp).asInstanceOf[TextView]
    holder.unreadCount = view.findViewById(R.id.unread_messages_count).asInstanceOf[TextView]
    holder.friendName.setText(username)
    holder.friendStatus.setText(message)
    holder.unreadCount.setText(java.lang.Integer.toString(unreadCount))
    holder.timestamp.setText(PrettyTimestamp.prettyTimestamp(time, false))
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      holder.icon.setBackground(context.getResources.getDrawable(IconColor.iconDrawable(online, UserStatus.getToxUserStatusFromString(status))))
    } else {
      holder.icon.setBackgroundDrawable(context.getResources.getDrawable(IconColor.iconDrawable(online, UserStatus.getToxUserStatusFromString(status))))
    }
    if (unreadCount == 0) {
      holder.unreadCount.setVisibility(View.GONE)
    } else {
      holder.unreadCount.setVisibility(View.VISIBLE)
    }
    view.setOnClickListener(new View.OnClickListener() {

      override def onClick(view: View) {
        if (tox_key != "") {
          ToxSingleton.changeActiveKey(tox_key)
          val intent = new Intent(context, classOf[ChatActivity])
          intent.putExtra("key", tox_key)
          context.startActivity(intent)
        }
      }
    })
  }
}
