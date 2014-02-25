package im.tox.antox.callbacks;

import im.tox.antox.AntoxFriend;
import im.tox.antox.ToxService;
import im.tox.jtoxcore.callbacks.OnMessageCallback;
import android.content.Context;
import android.content.Intent;

public class AntoxOnMessageCallback implements OnMessageCallback<AntoxFriend> {
	public static final String TAG = "AntoxOnMessageCallback";
	public static final String MESSAGE = "im.tox.antox.AntoxOnMessageCallback.MESSAGE";
	public static final String NAME = "im.tox.antox.AntoxOnMessageCallback.NAME";
	public static final String FRIEND_NUMBER = "im.tox.antox.AntoxOnMessageCallback.FRIEND_NUMBER";
	public static final String INTENT_ACTION = "im.tox.antox.AntoxOnMessageCallback.INTENT_ACTION";

	private Context ctx;

	public AntoxOnMessageCallback(Context ctx) {
		this.ctx = ctx;
	}

	@Override
	public void execute(AntoxFriend friend, String message) {
		Intent intent = new Intent(INTENT_ACTION);
		intent.putExtra(MESSAGE, message);
		String name;
		if (friend.getNickname() == null || friend.getNickname().isEmpty()) {
			name = friend.getName();
		} else {
			name = friend.getNickname();
		}

		intent.putExtra(FRIEND_NUMBER, friend.getFriendnumber());
		intent.putExtra(NAME, name);
		intent.setClass(this.ctx, ToxService.class);
		this.ctx.startService(intent);
	}
}
