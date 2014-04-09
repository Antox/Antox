package im.tox.antox.utils;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import im.tox.antox.activities.MainActivity;
import im.tox.antox.tox.ToxDoService;

/**
 * Created by Dragos Corlatescu on 05.04.2014.
 */
public class ConnectionChangeReceiver extends BroadcastReceiver {

    private static final String TAG = "im.tox.antox.utils.ConnectionChangeReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            if (DhtNode.ipv4.size() == 0) {
                new DHTNodeDetails(context).execute();
            }
        }
    }

    private class DHTNodeDetails extends AsyncTask<Void, Void, Void> {
        final String[] nodeDetails = new String[7];
        private Context context;

        public DHTNodeDetails(Context context) {
            this.context = context;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                // Connect to the web site
                Document document = Jsoup.connect("http://wiki.tox.im/Nodes").timeout(10000).get();
                Elements nodeRows = document.getElementsByTag("tr");

                for(Element nodeRow : nodeRows) {
                    Elements nodeElements = nodeRow.getElementsByTag("td");
                    int c = 0;
                    for(Element nodeElement : nodeElements)
                        nodeDetails[c++]=nodeElement.text();


                    if(nodeDetails[6]!=null && nodeDetails[6].equals("WORK")) {
                        DhtNode.ipv4.add(nodeDetails[0]);
                        DhtNode.ipv6.add(nodeDetails[1]);
                        DhtNode.port.add(nodeDetails[2]);
                        DhtNode.key.add(nodeDetails[3]);
                        DhtNode.owner.add(nodeDetails[4]);
                        DhtNode.location.add(nodeDetails[5]);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            Log.d(TAG, "About to ping servers...");
            /**
             * Ping servers to find quickest connection
             */
            long shortestTime = 99999;
            int pos = -1;
            Socket socket = null;
            Log.d(TAG, "DhtNode size: " + DhtNode.ipv4.size());
            for(int i = 0;i < DhtNode.ipv4.size(); i++) {
                try {
                    long currentTime = System.currentTimeMillis();
                    boolean reachable = InetAddress.getByName(DhtNode.ipv4.get(i)).isReachable(400);
                    long elapsedTime = System.currentTimeMillis() - currentTime;
                    if (reachable && (elapsedTime < shortestTime)) {
                        shortestTime = elapsedTime;
                        pos = i;
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    e.printStackTrace();
                }
            }

            if(socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

             /* Move quickest node to front of list */
            if(pos != -1) {
                DhtNode.ipv4.add(0, DhtNode.ipv4.get(pos));
                DhtNode.ipv6.add(0, DhtNode.ipv6.get(pos));
                DhtNode.port.add(0, DhtNode.port.get(pos));
                DhtNode.key.add(0, DhtNode.key.get(pos));
                DhtNode.owner.add(0, DhtNode.owner.get(pos));
                DhtNode.location.add(0, DhtNode.location.get(pos));
                Log.d(TAG, "DHT Nodes have been sorted");
                DhtNode.sorted = true;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result)
        {
            /**
             * There is a chance that downloading finishes later than the bootstrapping call in the
             * ToxService, because both are in separate threads. In that case to make sure the nodes
             * are bootstrapped we restart the ToxService
             */
            if(!DhtNode.connected)
            {
                Log.d(TAG, "Restarting START_TOX as DhtNode.connected returned false");
                Intent restart = new Intent(context, ToxDoService.class);
                restart.setAction(Constants.START_TOX);
                context.startService(restart);
            }

            /* Restart intent if it was connected before nodes were sorted */
            if(DhtNode.connected && !DhtNode.sorted) {
                Log.d(TAG, "Restarting START_TOX as DhtNode.sorted was false");
                Intent restart = new Intent(context, ToxDoService.class);
                restart.setAction(Constants.START_TOX);
                context.startService(restart);
            }
        }
    }
}
