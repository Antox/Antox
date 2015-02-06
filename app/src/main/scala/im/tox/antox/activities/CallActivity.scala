package im.tox.antox.activities

import android.app.Activity
import android.os.{Build, Bundle}
import android.view.{MenuItem, WindowManager}
import im.tox.antoxnightly.R

import scala.beans.BeanProperty

class CallActivity extends Activity {

  val friendNumber = _

  protected override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_call)
    if (Build.VERSION.SDK_INT != Build.VERSION_CODES.JELLY_BEAN &&
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      getWindow.setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
    }
  }

  def onCallCanceled() {
    finish()
  }
}
