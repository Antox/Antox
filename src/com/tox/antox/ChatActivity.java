package com.tox.antox;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;

public class ChatActivity extends Activity {

	private ListView chatListView;
	
	ChatMessages chat_messages[];
	ChatMessagesAdapter adapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chat);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Show the Up button in the action bar.
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

		Intent chatIntent = getIntent();
		String friendName = chatIntent.getStringExtra(MainActivity.EXTRA_MESSAGE);
		
		setTitle(friendName);
			
		ChatMessages chat_messages[] = new ChatMessages[]
	    {
				new ChatMessages("this is some chat message", true),
				new ChatMessages("this is another chat message", true),
				new ChatMessages("one more for good measure", true)
		};
		
		
		adapter = new ChatMessagesAdapter(this, 
				R.layout.chat_message_row, chat_messages);
		
		chatListView = (ListView) findViewById(R.id.chatMessages);
		chatListView.setAdapter(adapter);
		
	}

	public void sendMessage(View view)
	{
		EditText tmp = (EditText) findViewById(R.id.yourMessage);
		ChatMessages chat_messages[] = new ChatMessages[]
				{
					new ChatMessages(tmp.toString(), true) 
				};
		adapter.notifyDataSetChanged();
		tmp.setText("");
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.chat, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
