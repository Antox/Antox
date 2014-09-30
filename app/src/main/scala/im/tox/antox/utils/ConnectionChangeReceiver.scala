package im.tox.antox.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
//remove if not needed
import scala.collection.JavaConversions._

class ConnectionChangeReceiver extends BroadcastReceiver {

  override def onReceive(context: Context, intent: Intent) {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE).asInstanceOf[ConnectivityManager]
    val networkInfo = connectivityManager.getActiveNetworkInfo
    if (networkInfo != null && networkInfo.isConnected) {
      if (DhtNode.ipv4.size == 0) {
        new DHTNodeDetails(context).execute()
      }
    }
  }
}
