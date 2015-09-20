package chat.tox.antox.adapters

import java.util

import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.Log
import android.view.{Gravity, LayoutInflater, View, ViewGroup}
import android.widget.Filter.FilterResults
import android.widget.{BaseAdapter, Filter, Filterable, ImageView, TextView}
import chat.tox.antox.R
import chat.tox.antox.adapters.ContactListAdapter._
import chat.tox.antox.data.State
import chat.tox.antox.fragments.ContactItemType
import chat.tox.antox.tox.ToxSingleton
import chat.tox.antox.utils.{IconColor, _}
import chat.tox.antox.wrapper.ToxKey
import de.hdodenhof.circleimageview.CircleImageView

import scala.collection.JavaConversions._

object ContactListAdapter {

  private class ViewHolder {

    var firstText: TextView = _

    var secondText: TextView = _

    var icon: TextView = _

    var favorite: ImageView = _

    var avatar: CircleImageView = _

    var countText: TextView = _

    var timeText: TextView = _
  }
}

class ContactListAdapter(private var context: Context) extends BaseAdapter with Filterable {

  private val mDataOriginal: util.ArrayList[LeftPaneItem] = new util.ArrayList[LeftPaneItem]()

  private var mData: util.ArrayList[LeftPaneItem] = new util.ArrayList[LeftPaneItem]()

  private val mInflater: LayoutInflater = context.asInstanceOf[Activity].getLayoutInflater

  var mFilter: Filter = _

  def addItem(item: LeftPaneItem) {
    mData.add(item)
    mDataOriginal.add(item)
    notifyDataSetChanged()
  }

  def insert(index: Int, item: LeftPaneItem): Unit = {
    mData.insert(index, item)
    mDataOriginal.insert(index, item)
    notifyDataSetChanged()
  }

  override def getItemViewType(position: Int): Int = {
    val `type` = getItem(position).viewType
    `type`.id
  }

  override def getViewTypeCount: Int = ContactItemType.values.size

  override def getCount: Int = mData.size

  override def getItem(position: Int): LeftPaneItem = mData.get(position)

  def getKey(position: Int): ToxKey = getItem(position).key

  override def getItemId(position: Int): Long = position

  override def getView(position: Int, convertView: View, parent: ViewGroup): View = {
    var holder: ViewHolder = null
    var newConvertView: View = convertView
    val `type` = ContactItemType(getItemViewType(position))
    if (newConvertView == null) {
      holder = new ViewHolder()
      `type` match {
        case ContactItemType.FRIEND_REQUEST | ContactItemType.GROUP_INVITE =>
          newConvertView = mInflater.inflate(R.layout.friendrequest_list_item, null)
          holder.firstText = newConvertView.findViewById(R.id.request_key).asInstanceOf[TextView]
          holder.secondText = newConvertView.findViewById(R.id.request_message).asInstanceOf[TextView]

        case ContactItemType.FRIEND | ContactItemType.GROUP =>
          newConvertView = mInflater.inflate(R.layout.contact_list_item, null)
          holder.firstText = newConvertView.findViewById(R.id.contact_name).asInstanceOf[TextView]
          holder.secondText = newConvertView.findViewById(R.id.contact_status).asInstanceOf[TextView]
          holder.icon = newConvertView.findViewById(R.id.icon).asInstanceOf[TextView]
          holder.favorite = newConvertView.findViewById(R.id.star).asInstanceOf[ImageView]
          holder.avatar = newConvertView.findViewById(R.id.avatar).asInstanceOf[CircleImageView]
          holder.countText = newConvertView.findViewById(R.id.unread_messages_count).asInstanceOf[TextView]
          holder.timeText = newConvertView.findViewById(R.id.last_message_timestamp).asInstanceOf[TextView]
      }
      newConvertView.setTag(holder)
    } else {
      holder = newConvertView.getTag.asInstanceOf[ViewHolder]
    }
    val item = getItem(position)
    holder.firstText.setText(item.first)
    holder.firstText.setTextColor(context.getResources.getColor(R.color.black))

    if (item.second != "") holder.secondText.setText(item.second) else holder.firstText.setGravity(Gravity.CENTER_VERTICAL)

    if (`type` == ContactItemType.FRIEND || `type` == ContactItemType.GROUP) {
      if (item.count > 0) {
        holder.countText.setVisibility(View.VISIBLE)
        //limit unread counter to 99
        holder.countText.setText(java.lang.Integer.toString(
          if (item.count > Constants.UNREAD_COUNT_LIMIT) Constants.UNREAD_COUNT_LIMIT else item.count))
      } else {
        holder.countText.setVisibility(View.GONE)
      }
      holder.timeText.setText(TimestampUtils.prettyTimestamp(item.timestamp, isChat = false))


      holder.avatar.setImageResource(R.drawable.default_avatar)

      item.image.foreach(img => BitmapManager.load(img, holder.avatar, isAvatar = true))

      val drawable = context.getResources.getDrawable(IconColor.iconDrawable(item.isOnline, item.status))
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
        holder.icon.setBackground(drawable)
      } else {
        holder.icon.setBackgroundDrawable(drawable)
      }
      if (item.favorite) {
        holder.favorite.setVisibility(View.VISIBLE)
      } else {
        holder.favorite.setVisibility(View.GONE)
      }
    }
    if (holder.timeText != null) {
      holder.timeText.setTextColor(context.getResources.getColor(R.color.grey_dark))
    }

    val acceptButton = newConvertView.findViewById(R.id.accept).asInstanceOf[ImageView]
    val rejectButton = newConvertView.findViewById(R.id.reject).asInstanceOf[ImageView]

    if (`type` == ContactItemType.FRIEND_REQUEST) {
      createFriendRequestClickHandlers(item.key, acceptButton, rejectButton)
    } else if (`type` == ContactItemType.GROUP_INVITE) {
      createGroupInviteClickHandlers(item.key, acceptButton, rejectButton)
    }

    newConvertView
  }

  def createFriendRequestClickHandlers(key: ToxKey, acceptButton: ImageView, rejectButton: ImageView): Unit = {
    acceptButton.setOnClickListener(new View.OnClickListener() {
      override def onClick(view: View) {
        Log.d("OnClick", "Accepting Friend: " + key)
        val db = State.db
        db.addFriend(key, "", "", context.getResources.getString(R.string.friend_accepted_default_status))
        db.deleteFriendRequest(key)
        try {
          ToxSingleton.tox.addFriendNoRequest(key)
          ToxSingleton.save()
        } catch {
          case e: Exception =>
        }
      }
    })
    rejectButton.setOnClickListener(new View.OnClickListener() {
      override def onClick(view: View) {
        Log.d("OnClick", "Rejecting Friend: " + key)
        val db = State.db
        db.deleteFriendRequest(key)
      }
    })
  }

  def createGroupInviteClickHandlers(groupKey: ToxKey, acceptButton: ImageView, rejectButton: ImageView): Unit = {
    acceptButton.setOnClickListener(new View.OnClickListener() {
      override def onClick(view: View) {
        Log.d("OnClick", "Joining Group: " + groupKey)
        val db = State.db

        db.groupInvites.first.subscribe(invites => {
          try {
            val inviteData = invites.filter(groupInvite => groupInvite.groupKey == groupKey).head.data
            ToxSingleton.tox.acceptGroupInvite(inviteData)
            ToxSingleton.save()
          } catch {
            case e: Exception => Log.e("ContactListAdapter", "exception", e)
          }
        })

        db.addGroup(groupKey, UiUtils.trimId(groupKey), "")
        db.deleteGroupInvite(groupKey)
      }
    })
    rejectButton.setOnClickListener(new View.OnClickListener() {
      override def onClick(view: View) {
        Log.d("OnClick", "Joining Group: " + groupKey)
        val db = State.db
        db.deleteGroupInvite(groupKey)
      }
    })
  }

  override def getFilter: Filter = {
    if (mFilter == null) {
      mFilter = new Filter() {

        protected override def performFiltering(constraint: CharSequence): FilterResults = {
          val filterResults = new FilterResults()
          if (mDataOriginal != null) {
            if (constraint == "" || constraint == null) {
              filterResults.values = mDataOriginal
              filterResults.count = mDataOriginal.size
            } else {
              mData = mDataOriginal
              val tempList1 = new util.ArrayList[LeftPaneItem]()
              val tempList2 = new util.ArrayList[LeftPaneItem]()
              val length = mData.size
              var i = 0
              while (i < length) {
                val item = mData.get(i)
                if (item.first.toUpperCase.startsWith(constraint.toString.toUpperCase)) tempList1.add(item) else if (item.first.toLowerCase.contains(constraint.toString.toLowerCase)) tempList2.add(item)
                i += 1
              }
              tempList1.addAll(tempList2)
              filterResults.values = tempList1
              filterResults.count = tempList1.size
            }
          }
          filterResults
        }

        protected override def publishResults(contraint: CharSequence, results: FilterResults) {
          mData = results.values.asInstanceOf[util.ArrayList[LeftPaneItem]]
          if (results.count > 0) {
            notifyDataSetChanged()
          } else {
            notifyDataSetInvalidated()
          }
        }
      }
    }
    mFilter
  }
}
