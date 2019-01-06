package chat.tox.antox.tox

import android.app.Application
import android.arch.lifecycle.{DefaultLifecycleObserver, ProcessLifecycleOwner}
import android.content.{Context, IntentFilter}
import android.net.ConnectivityManager
import android.os.Build
import android.support.annotation.NonNull
import chat.tox.antox.utils.ConnectionManager

object ToxApplication {
  def getInstance(context: Context): ToxApplication = context.getApplicationContext.asInstanceOf[ToxApplication]
}

class ToxApplication extends Application with DefaultLifecycleObserver {

  private var isAppVisible = false

  def onStart(@NonNull owner: Nothing): Unit = {
    isAppVisible = true
  }

  def onStop(@NonNull owner: Nothing): Unit = {
    isAppVisible = false
  }

  def getAppVisible: Boolean = isAppVisible

  override def onCreate(): Unit = {
    super.onCreate()
    ProcessLifecycleOwner.get.getLifecycle.addObserver(this)
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