package im.tox.antox.callbacks;

import android.content.Context;
import android.content.Intent;

import im.tox.antox.tox.ToxService;
import im.tox.antox.utils.AntoxFriend;
import im.tox.antox.utils.Constants;
import im.tox.jtoxcore.callbacks.OnMessageCallback;

public class AntoxOnMessageCallback implements OnMessageCallback<AntoxFriend> {
	public static final String TAG = "AntoxOnMessageCallback";
	public static final String MESSAGE = "im.tox.antox.AntoxOnMessageCallback.MESSAGE";
	public static final String KEY = "im.tox.antox.AntoxOnMessageCallback.KEY";
	public static final String FRIEND_NUMBER = "im.tox.antox.AntoxOnMessageCallback.FRIEND_NUMBER";
	public static final String INTENT_ACTION = "im.tox.antox.AntoxOnMessageCallback.INTENT_ACTION";

	private Context ctx;

	public AntoxOnMessageCallback(Context ctx) {
		this.ctx = ctx;
	}

	@Override
	public void execute(AntoxFriend friend, String message) {
		Intent intent = new Intent(this.ctx, ToxService.class);
        intent.setAction(Constants.ON_MESSAGE);
		intent.putExtra(MESSAGE, message);
		intent.putExtra(FRIEND_NUMBER, friend.getFriendnumber());
		intent.putExtra(KEY, friend.getId());
		this.ctx.startService(intent);
	}
}
