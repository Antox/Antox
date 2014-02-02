package com.tox.antox;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class FriendsListAdapter extends ArrayAdapter<FriendsList> {
	Context context;
	int layoutResourceId;
	FriendsList data[] = null;
	
	public FriendsListAdapter(Context context, int layoutResourceId, FriendsList[] data)
	{
		super(context, layoutResourceId, data);
		this.layoutResourceId = layoutResourceId;
		this.context = context;
		this.data = data;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		View row = convertView;
		FriendsListHolder holder = null;
		
		if(row == null)
		{
			LayoutInflater inflater = ((Activity)context).getLayoutInflater();
			row = inflater.inflate(layoutResourceId, parent, false);
			holder = new FriendsListHolder();
			holder.imgIcon = (ImageView)row.findViewById(R.id.imgIcon);
			holder.friendName = (TextView)row.findViewById(R.id.friend_name);
			holder.friendStatus = (TextView)row.findViewById(R.id.friend_status);
			row.setTag(holder);
		}
		else
		{
			holder = (FriendsListHolder)row.getTag();
		}
		
		FriendsList friendsList = data[position];
		holder.friendName.setText(friendsList.friendName);
		holder.imgIcon.setImageResource(friendsList.icon);
		holder.friendStatus.setText(friendsList.friendStatus);
		
		return row;
	}
	
	static class FriendsListHolder
	{
		ImageView imgIcon;
		TextView friendName;
		TextView friendStatus;
	}
}
