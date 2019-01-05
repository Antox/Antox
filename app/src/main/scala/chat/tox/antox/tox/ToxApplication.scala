package chat.tox.antox.tox

import android.app.Application
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Build
import chat.tox.antox.utils.ConnectionManager

class ToxApplication extends Application {
  override def onCreate(): Unit = {
    super.onCreate()
    try
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
          val intentFilter = new IntentFilter
          intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
          registerReceiver(new ConnectionManager, intentFilter)
        }
    catch {
      case e: Exception =>
        e.printStackTrace()
    }
  }
}