package chat.tox.antox.activities

import android.content.{Context, Intent}
import android.net.Uri
import android.os.{Build, Bundle}
import android.support.v4.app.NavUtils
import android.support.v7.app.AppCompatActivity
import android.view.{Menu, MenuItem, WindowManager}
import chat.tox.QR.IntentIntegrator
import chat.tox.antox.R
import chat.tox.antox.fragments.{AddPaneFragment, InputableID}
import chat.tox.antox.theme.ThemeManager

class AddActivity extends AppCompatActivity {

  var context: Context = _

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)

    overridePendingTransition(R.anim.slide_from_bottom, R.anim.fade_scale_out)

    if (Build.VERSION.SDK_INT != Build.VERSION_CODES.JELLY_BEAN &&
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      getWindow.setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
    }

    context = getApplicationContext

    setTitle(getResources.getString(R.string.title_activity_add))
    setContentView(R.layout.activity_add)
    ThemeManager.applyTheme(this, getSupportActionBar)

    val intent = getIntent
    if (Intent.ACTION_VIEW == intent.getAction && intent != null) {
      // Handle incoming tox uri links
      val uri = intent.getData
      if (uri != null) {
        getSupportFragmentManager.findFragmentById(R.id.fragment_add_pane)
          .asInstanceOf[AddPaneFragment].getSelectedFragment
      }
    }
  }

  override def onPause(): Unit = {
    super.onPause()
    if (isFinishing) overridePendingTransition(R.anim.fade_scale_in, R.anim.slide_to_bottom)
  }

  private def scanIntent() {
    val integrator = new IntentIntegrator(this)
    integrator.initiateScan()
  }

  override def onActivityResult(requestCode: Int, resultCode: Int, intent: Intent) {
    val scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent)
    if (scanResult != null) {
      if (scanResult.getContents != null) {
        getSupportFragmentManager
          .findFragmentById(R.id.fragment_add_pane)
          .asInstanceOf[AddPaneFragment]
          .getSelectedFragment
          .asInstanceOf[InputableID]
          .inputID(scanResult.getContents)
      }
    }
  }

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    getMenuInflater.inflate(R.menu.add_friend, menu)
    true
  }

  override def onOptionsItemSelected(item: MenuItem): Boolean = {
    item.getItemId match {
      case android.R.id.home =>
        NavUtils.navigateUpFromSameTask(this)

      case R.id.scanFriend => scanIntent()
    }
    super.onOptionsItemSelected(item)
  }
}
