package im.tox.antox;

import android.annotation.SuppressLint;
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

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import im.tox.antox.callbacks.AntoxOnMessageCallback;

public class ChatActivity extends Activity {

	public static final String CHAT_ACTIVITY = "im.tox.antox.ChatActivity.CHAT_ACTIVITY";
	private ListView chatListView;
	private int counter = 0;

	ChatMessages chat_messages[] = new ChatMessages[counter];
	ChatMessagesAdapter adapter;

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chat);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			// Show the Up button in the action bar.
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}

		Intent chatIntent = getIntent();
		String friendName = chatIntent
				.getStringExtra(MainActivity.EXTRA_MESSAGE);

		setTitle(friendName);
	}

	public void sendMessage(View view) {
		EditText tmp = (EditText) findViewById(R.id.yourMessage);

		chat_messages = Arrays.copyOf(chat_messages, chat_messages.length + 1);

        SimpleDateFormat time = new SimpleDateFormat("HH:mm");

		chat_messages[counter] = new ChatMessages(tmp.getText().toString(),
				time.format(new Date()),true);

		ChatMessagesAdapter adapter = new ChatMessagesAdapter(this,
				R.layout.chat_message_row, chat_messages);

		chatListView = (ListView) findViewById(R.id.chatMessages);
		chatListView.setAdapter(adapter);

		tmp.setText("");
		counter++;
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

	@Override
	protected void onResume() {
		super.onResume();
		Intent intent = ToxService.getRegisterIntent(this, CHAT_ACTIVITY);

		// The no_chat_partner part needs to be changed in the future
		intent.putExtra(AntoxOnMessageCallback.FRIEND_NUMBER,
				AntoxState.NO_CHAT_PARTNER);
		startService(intent);
	}

	@Override
	protected void onPause() {
		startService(ToxService.getUnRegisterIntent(this, CHAT_ACTIVITY));
		super.onPause();
	}
}
