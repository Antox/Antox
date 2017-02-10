package chat.tox.antox;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import chat.tox.antox.data.State;
import chat.tox.antox.tox.ToxService;
import chat.tox.antox.tox.ToxSingleton;

public class AlarmReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent2)
    {

        Thread t = State.serviceThreadMain();
        t.interrupt();
    }
}