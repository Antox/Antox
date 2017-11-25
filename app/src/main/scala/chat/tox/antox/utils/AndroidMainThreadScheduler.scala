package rx.lang.scala.schedulers

import rx.android.schedulers.AndroidSchedulers
import rx.lang.scala.Scheduler
import rx.plugins.{RxJavaErrorHandler, RxJavaPlugins}

object AndroidMainThreadScheduler {

  RxJavaPlugins.getInstance.registerErrorHandler(new RxJavaErrorHandler() {
    override def handleError(e: Throwable): Unit = {
      e.printStackTrace()
      super.handleError(e)
    }
  })

  def apply(): AndroidMainThreadScheduler = {
    new AndroidMainThreadScheduler(AndroidSchedulers.mainThread())
  }
}

class AndroidMainThreadScheduler private[scala](val asJavaScheduler: rx.Scheduler) extends Scheduler {}
