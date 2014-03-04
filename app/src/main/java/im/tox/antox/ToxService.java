package im.tox.antox;

import im.tox.antox.callbacks.AntoxOnActionCallback;
import im.tox.antox.callbacks.AntoxOnConnectionStatusCallback;
import im.tox.antox.callbacks.AntoxOnFriendRequestCallback;
import im.tox.antox.callbacks.AntoxOnMessageCallback;
import im.tox.antox.callbacks.AntoxOnNameChangeCallback;
import im.tox.antox.callbacks.AntoxOnReadReceiptCallback;
import im.tox.antox.callbacks.AntoxOnStatusMessageCallback;
import im.tox.antox.callbacks.AntoxOnUserStatusCallback;
import im.tox.jtoxcore.FriendExistsException;
import im.tox.jtoxcore.FriendList;
import im.tox.jtoxcore.JTox;
import im.tox.jtoxcore.ToxException;
import im.tox.jtoxcore.ToxUserStatus;
import im.tox.jtoxcore.callbacks.CallbackHandler;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class ToxService extends IntentService {

    private static final String TAG = "im.tox.antox.ToxService";


	
	public ToxService() {
		super("ToxService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		AntoxState state = AntoxState.getInstance();
        ToxSingleton toxSingleton = ToxSingleton.getInstance();
		ArrayList<String> boundActivities = state.getBoundActivities();

        if (!intent.getAction().equals(Constants.DO_TOX)) {
            Log.d(TAG, "Got intent action: " + intent.getAction());
        }
		if (intent.getAction().equals(Constants.REGISTER)) {
			String name = intent.getStringExtra(Constants.REGISTER_NAME);
			boundActivities.add(name);

			if (name.equals(ChatActivity.CHAT_ACTIVITY)) {
				state.setActiveChatPartner(intent.getIntExtra(
						AntoxOnMessageCallback.FRIEND_NUMBER,
						AntoxState.NO_CHAT_PARTNER));
			}
		} else if (intent.getAction().equals(Constants.UNREGISTER)) {
			String name = intent.getStringExtra(Constants.REGISTER_NAME);
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
		} else if (intent.getAction().equals(Constants.START_TOX)) {
            try {
                AntoxOnMessageCallback antoxOnMessageCallback = new AntoxOnMessageCallback(getApplicationContext());
                AntoxOnFriendRequestCallback antoxOnFriendRequestCallback = new AntoxOnFriendRequestCallback(getApplicationContext());
                AntoxOnActionCallback antoxOnActionCallback = new AntoxOnActionCallback(getApplicationContext());
                AntoxOnConnectionStatusCallback antoxOnConnectionStatusCallback = new AntoxOnConnectionStatusCallback(getApplicationContext());
                AntoxOnNameChangeCallback antoxOnNameChangeCallback = new AntoxOnNameChangeCallback(getApplicationContext());
                AntoxOnReadReceiptCallback antoxOnReadReceiptCallback = new AntoxOnReadReceiptCallback(getApplicationContext());
                AntoxOnStatusMessageCallback antoxOnStatusMessageCallback = new AntoxOnStatusMessageCallback(getApplicationContext());
                AntoxOnUserStatusCallback antoxOnUserStatusCallback = new AntoxOnUserStatusCallback(getApplicationContext());

                toxSingleton.callbackHandler.registerOnMessageCallback(antoxOnMessageCallback);
                toxSingleton.callbackHandler.registerOnFriendRequestCallback(antoxOnFriendRequestCallback);
                toxSingleton.callbackHandler.registerOnActionCallback(antoxOnActionCallback);
                toxSingleton.callbackHandler.registerOnConnectionStatusCallback(antoxOnConnectionStatusCallback);
                toxSingleton.callbackHandler.registerOnNameChangeCallback(antoxOnNameChangeCallback);
                toxSingleton.callbackHandler.registerOnReadReceiptCallback(antoxOnReadReceiptCallback);
                toxSingleton.callbackHandler.registerOnStatusMessageCallback(antoxOnStatusMessageCallback);
                toxSingleton.callbackHandler.registerOnUserStatusCallback(antoxOnUserStatusCallback);

                SharedPreferences settingsPref = getSharedPreferences("settings", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = settingsPref.edit();
                editor.putString("user_key", toxSingleton.jTox.getAddress());
                editor.commit();

                try {
                    toxSingleton.jTox.bootstrap(DhtNode.ipv4, Integer.parseInt(DhtNode.port), DhtNode.key);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            } catch (ToxException e) {
                Log.d(TAG, e.getError().toString());
                e.printStackTrace();
            }
        } else if (intent.getAction().equals(Constants.DO_TOX)) {
            try {
                toxSingleton.jTox.doTox();
                if(toxSingleton.jTox.isConnected()) {
                    //Intent localIntent = new Intent(Constants.BROADCAST_ACTION)
                    //       .putExtra(Constants.CONNECTED_STATUS, "connected");
                    //LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
                }
            } catch (ToxException e) {
                Log.d(TAG, e.getError().toString());
                e.printStackTrace();
            }

        } else if (intent.getAction().equals(Constants.ADD_FRIEND)) {
            try {
                String[] friendData = intent.getStringArrayExtra("friendData");
                toxSingleton.jTox.addFriend(friendData[0], friendData[1]);
            } catch (FriendExistsException e) {
                e.printStackTrace();
            } catch (ToxException e) {
                e.printStackTrace();
            }
        } else if (intent.getAction().equals(Constants.UPDATE_SETTINGS)) {
            String[] newSettings = intent.getStringArrayExtra("newSettings");

            /* If not empty, update the users settings which is passed in intent from SettingsActivity */
            try {
                if(!newSettings[0].equals(""))
                    toxSingleton.jTox.setName(newSettings[0]);

                if(!newSettings[1].equals("")) {
                    if(newSettings[1].equals("away"))
                        toxSingleton.jTox.setUserStatus(ToxUserStatus.TOX_USERSTATUS_AWAY);
                    else if(newSettings[1].equals("busy"))
                        toxSingleton.jTox.setUserStatus(ToxUserStatus.TOX_USERSTATUS_BUSY);
                    else
                        toxSingleton.jTox.setUserStatus(ToxUserStatus.TOX_USERSTATUS_NONE);
                }

                if(!newSettings[2].equals(""))
                    toxSingleton.jTox.setStatusMessage(newSettings[2]);
            } catch (ToxException e) {
                e.printStackTrace();
            }

        } else if (intent.getAction().equals(Constants.FRIEND_LIST)) {
            Log.d(TAG, "Constants.FRIEND_LIST");

        } else if (intent.getAction().equals(Constants.FRIEND_REQUEST)) {
            Log.d(TAG, "Constants.FRIEND_REQUEST");
            Intent notify = new Intent(Constants.BROADCAST_ACTION);
            notify.putExtra("action", Constants.FRIEND_REQUEST);
            notify.putExtra("key", intent.getStringExtra(AntoxOnFriendRequestCallback.FRIEND_KEY));
            notify.putExtra("message", intent.getStringExtra(AntoxOnFriendRequestCallback.FRIEND_MESSAGE));
            LocalBroadcastManager.getInstance(this).sendBroadcast(notify);
            /* Update friends list */
            Intent updateFriends = new Intent(this, ToxService.class);
            updateFriends.setAction(Constants.FRIEND_LIST);
            this.startService(updateFriends);

        } else if (intent.getAction().equals(Constants.CONNECTED_STATUS)) {
            Log.d(TAG, "Constants.CONNECTION_STATUS");
        }
	}

	public static Intent getRegisterIntent(Context ctx, String name) {
		Intent intent = new Intent(ctx, ToxService.class);
		intent.setAction(Constants.REGISTER);
		intent.putExtra(Constants.REGISTER_NAME, name);

		return intent;
	}

	public static Intent getUnRegisterIntent(Context ctx, String name) {
		Intent intent = new Intent(ctx, ToxService.class);
		intent.setAction(Constants.UNREGISTER);
		intent.putExtra(Constants.REGISTER_NAME, name);

		return intent;
	}
}
