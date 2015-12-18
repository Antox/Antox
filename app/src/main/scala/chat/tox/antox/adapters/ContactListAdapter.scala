package chat.tox.antox.adapters

import java.util

import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.{Gravity, LayoutInflater, View, ViewGroup}
import android.widget.Filter.FilterResults
import android.widget.{BaseAdapter, Filter, Filterable, ImageView, TextView}
import chat.tox.antox.R
import chat.tox.antox.adapters.ContactListAdapter._
import chat.tox.antox.data.State
import chat.tox.antox.fragments.ContactItemType
import chat.tox.antox.tox.ToxSingleton
import chat.tox.antox.utils.{IconColor, _}
import chat.tox.antox.wrapper.{GroupKey, FriendKey, ContactKey}
import de.hdodenhof.circleimageview.CircleImageView
import rx.lang.scala.Subscription

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

    var imageLoadingSubscription: Option[Subscription] = None
  }
}

class ContactListAdapter(private var context: Context) extends BaseAdapter with Filterable {

  private val originalData: util.ArrayList[LeftPaneItem] = new util.ArrayList[LeftPaneItem]()

  private var data: util.ArrayList[LeftPaneItem] = new util.ArrayList[LeftPaneItem]()

  private val layoutInflater: LayoutInflater = context.asInstanceOf[Activity].getLayoutInflater

  var filter: Filter = _

  def addItem(item: LeftPaneItem) {
    data.add(item)
    originalData.add(item)
    notifyDataSetChanged()
  }

  def insert(index: Int, item: LeftPaneItem): Unit = {
    data.insert(index, item)
    originalData.insert(index, item)
    notifyDataSetChanged()
  }

  override def getItemViewType(position: Int): Int = {
    val `type` = getItem(position).viewType
    `type`.id
  }

  override def getViewTypeCount: Int = ContactItemType.values.size

  override def getCount: Int = data.size

  override def getItem(position: Int): LeftPaneItem = data.get(position)

  def getKey(position: Int): ContactKey = getItem(position).key

  override def getItemId(position: Int): Long = position

  override def getView(position: Int, convertView: View, parent: ViewGroup): View = {
    var holder: ViewHolder = null
    var newConvertView: View = convertView
    val `type` = ContactItemType(getItemViewType(position))
    if (newConvertView == null) {
      holder = new ViewHolder()
      `type` match {
        case ContactItemType.FRIEND_REQUEST | ContactItemType.GROUP_INVITE =>
          newConvertView = layoutInflater.inflate(R.layout.friendrequest_list_item, null)
          holder.firstText = newConvertView.findViewById(R.id.request_key).asInstanceOf[TextView]
          holder.secondText = newConvertView.findViewById(R.id.request_message).asInstanceOf[TextView]

        case ContactItemType.FRIEND | ContactItemType.GROUP =>
          newConvertView = layoutInflater.inflate(R.layout.contact_list_item, null)
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

      holder.imageLoadingSubscription.foreach(_.unsubscribe())

      holder.imageLoadingSubscription =
        item.image match {
          case Some(img) =>
            BitmapManager.getFromCache(isAvatar = true, img) match {
              case Some(bitmap) =>
                holder.avatar.setImageBitmap(bitmap)
                None

              case None =>
                Some(BitmapManager
                  .load(img, isAvatar = true)
                  .subscribe(bitmap => holder.avatar.setImageBitmap(bitmap)))
            }

          case None =>
            holder.avatar.setImageResource(R.drawable.default_avatar)
            None
        }

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
      createFriendRequestClickHandlers(item.key.asInstanceOf[FriendKey], acceptButton, rejectButton)
    } else if (`type` == ContactItemType.GROUP_INVITE) {
      createGroupInviteClickHandlers(item.key.asInstanceOf[GroupKey], acceptButton, rejectButton)
    }

    newConvertView
  }

  def createFriendRequestClickHandlers(key: FriendKey, acceptButton: ImageView, rejectButton: ImageView): Unit = {
    acceptButton.setOnClickListener(new View.OnClickListener() {
      override def onClick(view: View) {
        AntoxLog.debug("Accepting Friend: " + key, AntoxLog.CLICK_TAG)

        val db = State.db
        db.addFriend(key, "", "", context.getResources.getString(R.string.friend_accepted_default_status))

        db.deleteFriendRequest(key)
        AntoxNotificationManager.clearRequestNotification(key)
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
        AntoxLog.debug("Rejecting Friend: " + key, AntoxLog.CLICK_TAG)

        val db = State.db
        db.deleteFriendRequest(key)
      }
    })
  }

  def createGroupInviteClickHandlers(groupKey: GroupKey, acceptButton: ImageView, rejectButton: ImageView): Unit = {
    acceptButton.setOnClickListener(new View.OnClickListener() {
      override def onClick(view: View) {
        AntoxLog.debug("Joining Group: " + groupKey, AntoxLog.CLICK_TAG)
        val db = State.db

        db.groupInvites.first.subscribe(invites => {
          try {
            val inviteData = invites.filter(groupInvite => groupInvite.groupKey == groupKey).head.data
            ToxSingleton.tox.acceptGroupInvite(inviteData)
            ToxSingleton.save()
          } catch {
            case e: Exception => e.printStackTrace()
          }
        })

        db.addGroup(groupKey, UiUtils.trimId(groupKey), "")
        db.deleteGroupInvite(groupKey)
        AntoxNotificationManager.clearRequestNotification(groupKey)
      }
    })
    rejectButton.setOnClickListener(new View.OnClickListener() {
      override def onClick(view: View) {
        AntoxLog.debug("Joining Group: " + groupKey, AntoxLog.CLICK_TAG)
        val db = State.db
        db.deleteGroupInvite(groupKey)
      }
    })
  }

  override def getFilter: Filter = {
    if (filter == null) {
      filter = new Filter() {

        protected override def performFiltering(constraint: CharSequence): FilterResults = {
          val filterResults = new FilterResults()
          if (originalData != null) {
            if (constraint == "" || constraint == null) {
              filterResults.values = originalData
              filterResults.count = originalData.size
            } else {
              data = originalData
              val tempList1 = new util.ArrayList[LeftPaneItem]()
              val tempList2 = new util.ArrayList[LeftPaneItem]()
              val length = data.size
              var i = 0
              while (i < length) {
                val item = data.get(i)
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
          data = results.values.asInstanceOf[util.ArrayList[LeftPaneItem]]
          if (results.count > 0) {
            notifyDataSetChanged()
          } else {
            notifyDataSetInvalidated()
          }
        }
      }
    }
    filter
  }
}
