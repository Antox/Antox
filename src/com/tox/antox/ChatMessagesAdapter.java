package com.tox.antox;


import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class ChatMessagesAdapter extends ArrayAdapter<ChatMessages>
{
	Context context;
	int layoutResourceId;
	ChatMessages data[] = null;

	public ChatMessagesAdapter(Context context, int layoutResourceId, ChatMessages[] data) 
	{
		super(context, layoutResourceId, data);
		this.context = context;
		this.layoutResourceId = layoutResourceId;
		this.data = data;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		View row = convertView;
		ChatMessagesHolder holder = null;
		
		if(row == null)
		{
			LayoutInflater inflater = ((Activity)context).getLayoutInflater();
			row = inflater.inflate(layoutResourceId, parent, false);
			holder = new ChatMessagesHolder();
			holder.message = (TextView)row.findViewById(R.id.message_text);
			row.setTag(holder);
		}
		else
		{
			holder = (ChatMessagesHolder)row.getTag();
		}
		
		ChatMessages chatMessages = data[position];
		holder.message.setText(chatMessages.message);
		
		return row;
	}
	
	static class ChatMessagesHolder
	{
		TextView message;
	}

}
