package chat.tox.antox;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import chat.tox.antox.data.State;
import chat.tox.antox.tox.ToxService;
import chat.tox.antox.tox.ToxSingleton;

/**
 * Created by zoff on 15.01.2017.
 */

public class AlarmReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent2)
    {
        System.out.println("AlarmReceiver:" + "onReceive");
        System.out.println("AlarmReceiver:" + "isInited=" + ToxSingleton.isInited());
        System.out.println("AlarmReceiver:" + "isBootstrapped=" + State.isBootstrapped());

        Thread t = State.serviceThreadMain();
        System.out.println("AlarmReceiver:" + "1:getPriority=" + t.getPriority());
        System.out.println("AlarmReceiver:" + "1:getState=" + t.getState());
        System.out.println("AlarmReceiver:" + "1:isAlive=" + t.isAlive());
        t.interrupt();
        System.out.println("AlarmReceiver:" + "t interrupted");
        System.out.println("AlarmReceiver:" + "2:getPriority=" + t.getPriority());
        System.out.println("AlarmReceiver:" + "2:getState=" + t.getState());
        System.out.println("AlarmReceiver:" + "2:isAlive=" + t.isAlive());
    }
}