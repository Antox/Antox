package chat.tox.antox.utils

import java.util

import android.content.{BroadcastReceiver, Context, Intent}
import android.net.ConnectivityManager
import chat.tox.antox.tox.ToxSingleton

import scala.collection.JavaConversions._

trait ConnectionTypeChangeListener {
  //only called when network is connected
  def connectionTypeChange(connectionType: Int): Unit
}

object ConnectionManager {

  private val listenerList = new util.ArrayList[ConnectionTypeChangeListener]()

  private var lastConnectionType: Option[Int] = None

  def addConnectionTypeChangeListener(listener: ConnectionTypeChangeListener): Unit = {
    listenerList.add(listener)
  }

  def getConnctionType(context: Context): Int = {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                                     .asInstanceOf[ConnectivityManager]
    connectivityManager.getActiveNetworkInfo.getType
  }

  def isNetworkAvailable(context: Context): Boolean = {
    val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE).asInstanceOf[ConnectivityManager]
    val networkInfo = connMgr.getActiveNetworkInfo

    networkInfo != null && networkInfo.isConnected
  }
}

class ConnectionManager extends BroadcastReceiver {
  override def onReceive(context: Context, intent: Intent) {
    if (ConnectionManager.isNetworkAvailable(context)) {
      val connectionType = ConnectionManager.getConnctionType(context)
      if (ConnectionManager.lastConnectionType.isEmpty || connectionType != ConnectionManager.lastConnectionType.get) {
        for (listener <- ConnectionManager.listenerList) {
          listener.connectionTypeChange(connectionType)
        }
        ConnectionManager.lastConnectionType = Some(connectionType)
      }

      if (ToxSingleton.dhtNodes.length == 0) {
        ToxSingleton.updateDhtNodes(context)
      }
    }
  }
}
