package im.tox.antox.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import im.tox.antox.tox.ToxSingleton;

/**
 * Created by Dragos Corlatescu on 05.04.2014.
 */
public class ConnectionChangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            if (DhtNodes.ipv4.size() == 0) {
                new DownloadNodes(context).execute();
                ToxSingleton toxSingleton = ToxSingleton.getInstance();
                // Bootstrap again
                for (int i = 0; i < DhtNodes.ipv4.size(); i++) {
                    try {
                        toxSingleton.jTox.bootstrap(DhtNodes.ipv4.get(i), Integer.parseInt(DhtNodes.port.get(i)), DhtNodes.key.get(i));
                    } catch (Exception e) {
                    }
                }
            }
        }
    }
}
