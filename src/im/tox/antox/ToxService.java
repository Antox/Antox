package im.tox.antox;

import im.tox.antox.callbacks.AntoxOnMessageCallback;

import java.util.ArrayList;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class ToxService extends IntentService {

	private static final String TAG = "im.tox.antox.ToxService";
	private static final String REGISTER = "im.tox.antox.REGISTER";
	private static final String UNREGISTER = "im.tox.antox.UNREGISTER";
	private static final String REGISTER_NAME = "im.tox.antox.REGISTER_NAME";

	public ToxService() {
		super("ToxService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		AntoxState state = AntoxState.getInstance();
		ArrayList<String> boundActivities = state.getBoundActivities();

		Log.d(TAG, "Got intent action: " + intent.getAction());
		if (intent.getAction().equals(REGISTER)) {
			String name = intent.getStringExtra(REGISTER_NAME);
			boundActivities.add(name);

			if (name.equals(ChatActivity.CHAT_ACTIVITY)) {
				state.setActiveChatPartner(intent.getIntExtra(
						AntoxOnMessageCallback.FRIEND_NUMBER,
						AntoxState.NO_CHAT_PARTNER));
			}
		} else if (intent.getAction().equals(UNREGISTER)) {
			String name = intent.getStringExtra(REGISTER_NAME);
			state.getBoundActivities().remove(name);

			if (name.equals(ChatActivity.CHAT_ACTIVITY)) {
				state.setActiveChatPartner(AntoxState.NO_CHAT_PARTNER);
			}
		} else if (intent.getAction().equals(
				AntoxOnMessageCallback.INTENT_ACTION)) {
			if (state.getBoundActivities().contains(ChatActivity.CHAT_ACTIVITY)) {
				int friendNumber = intent.getIntExtra(
						AntoxOnMessageCallback.FRIEND_NUMBER,
						AntoxState.NO_CHAT_PARTNER);
				if (friendNumber != AntoxState.NO_CHAT_PARTNER
						&& friendNumber == state.getActiveChatPartner()) {
					// Send intent
				} else {
					// Send notification
					String message = intent
							.getStringExtra(AntoxOnMessageCallback.MESSAGE);
					String name = intent
							.getStringExtra(AntoxOnMessageCallback.NAME);
					NotificationCompat.Builder nb = new NotificationCompat.Builder(
							getApplicationContext())
							.setContentTitle("Message from: " + name)
							.setContentText(message)
							.setSmallIcon(R.drawable.ic_action_new);

					Notification mn = nb.build();
					NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
					nm.notify(0, mn);
				}
			}
		}
	}

	public static Intent getRegisterIntent(Context ctx, String name) {
		Intent intent = new Intent(ctx, ToxService.class);
		intent.setAction(REGISTER);
		intent.putExtra(REGISTER_NAME, name);

		return intent;
	}

	public static Intent getUnRegisterIntent(Context ctx, String name) {
		Intent intent = new Intent(ctx, ToxService.class);
		intent.setAction(UNREGISTER);
		intent.putExtra(REGISTER_NAME, name);

		return intent;
	}
}
