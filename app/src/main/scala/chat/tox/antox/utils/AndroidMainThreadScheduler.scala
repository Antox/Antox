package rx.lang.scala.schedulers

import rx.android.schedulers.AndroidSchedulers
import rx.lang.scala.Scheduler

object AndroidMainThreadScheduler {

  def apply(): AndroidMainThreadScheduler = {
    new AndroidMainThreadScheduler(AndroidSchedulers.mainThread())
  }
}

class AndroidMainThreadScheduler private[scala](val asJavaScheduler: rx.Scheduler) extends Scheduler {}
