package im.tox.antox.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import im.tox.antox.R;
import im.tox.antox.utils.FriendInfo;
import im.tox.antox.utils.IconColor;
import im.tox.antox.utils.PrettyTimestamp;

public class RecentAdapter extends ArrayAdapter<FriendInfo> {
	Context context;
	int layoutResourceId;
	ArrayList<FriendInfo> data = null;

	public RecentAdapter(Context context, int layoutResourceId,
                         ArrayList<FriendInfo> data) {
		super(context, layoutResourceId, data);
		this.layoutResourceId = layoutResourceId;
		this.context = context;
		this.data = data;
	}

	@Override
	public int getCount() {
		return data.size();
	}

	@Override
	public FriendInfo getItem(int position) {
		return data.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		FriendsListHolder holder = null;

		if (row == null) {
			LayoutInflater inflater = ((Activity) context).getLayoutInflater();
			row = inflater.inflate(layoutResourceId, parent, false);
			holder = new FriendsListHolder();
			holder.icon = (TextView) row.findViewById(R.id.icon);
			holder.friendName = (TextView) row.findViewById(R.id.friend_name);
			holder.friendStatus = (TextView) row
					.findViewById(R.id.friend_status);
            holder.timestamp = (TextView) row.findViewById(R.id.last_message_timestamp);
            holder.unreadCount = (TextView) row.findViewById(R.id.unread_messages_count);
			row.setTag(holder);
		} else {
			holder = (FriendsListHolder) row.getTag();
		}

		FriendInfo friend = data.get(position);
		holder.friendName.setText(friend.friendName);
		holder.friendStatus.setText(friend.lastMessage);
        holder.unreadCount.setText(Integer.toString(friend.unreadCount));
        holder.timestamp.setText(PrettyTimestamp.prettyTimestamp(friend.lastMessageTimestamp));
        holder.icon.setBackgroundColor(Color.parseColor(IconColor.iconColor(friend.icon)));
        if (friend.unreadCount == 0) {
            holder.unreadCount.setVisibility(View.GONE);
        } else {
            holder.unreadCount.setVisibility(View.VISIBLE);
        }
		return row;
	}

	static class FriendsListHolder {
		TextView icon;
		TextView friendName;
		TextView friendStatus;
        TextView timestamp;
        TextView unreadCount;
	}

}
