package im.tox.antox.activities

import android.app.Activity
import android.content.{Context, Intent}
import android.net.Uri
import android.os.{Build, Bundle}
import android.preference.PreferenceManager
import android.support.v4.app.{Fragment, FragmentPagerAdapter, FragmentManager, NavUtils}
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.ActionBarActivity
import android.util.Log
import android.view._
import android.widget.{ImageView, EditText, Toast}
import com.astuetz.PagerSlidingTabStrip.CustomTabProvider
import com.balysv.materialripple.MaterialRippleLayout
import im.tox.QR.IntentIntegrator
import im.tox.antox.R
import im.tox.antox.data.AntoxDB
import im.tox.antox.fragments.{ContactsFragment, RecentFragment}
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.utils.{Constants, Hex}
import im.tox.tox4j.exceptions.ToxException
import org.xbill.DNS.{Lookup, TXTRecord, Type}
import rx.lang.scala.Observable
import rx.lang.scala.schedulers.{AndroidMainThreadScheduler, IOScheduler}
//remove if not needed

class AddActivity extends ActionBarActivity {

  var _friendID: String = ""

  var _friendCHECK: String = ""

  var _originalUsername: String = ""

  var context: Context = _

  var text: CharSequence = _

  var duration: Int = Toast.LENGTH_SHORT

  var toast: Toast = _

  var friendID: EditText = _

  var friendMessage: EditText = _

  var friendAlias: EditText = _

  class LeftPagerAdapter(fm: FragmentManager) extends FragmentPagerAdapter(fm) with CustomTabProvider {

    val ICONS: Array[Int] = Array(R.drawable.ic_action_recent_tab, R.drawable.ic_action_contacts_tab)

    override def getCustomTabView(parent: ViewGroup, position: Int): View = {
      val materialRippleLayout: MaterialRippleLayout = LayoutInflater.from(getActivity)
        .inflate(R.layout.custom_tab, parent, false).asInstanceOf[MaterialRippleLayout]
      materialRippleLayout.findViewById(R.id.image).asInstanceOf[ImageView].setImageResource(ICONS(position))
      materialRippleLayout
    }

    override def getPageTitle(position: Int): CharSequence = {
      position match {
        case 0 => return "Recent"
        case _ => return "Contacts"
      }

      null
    }

    override def getItem(pos: Int): Fragment = pos match {
      case 0 => new RecentFragment()
      case _ => new ContactsFragment()
    }

    override def getCount: Int = 2
  }

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)

    overridePendingTransition(R.anim.slide_from_bottom, R.anim.fade_scale_out)

    if (Build.VERSION.SDK_INT != Build.VERSION_CODES.JELLY_BEAN &&
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      getWindow.setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
    }
  }

  override def onPause() = {
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
        getFragment
      }
    }
  }

  private def validateFriendKey(friendKey: String): Boolean = {
    if (friendKey.length != 76 || friendKey.matches("[[:xdigit:]]")) {
      return false
    }
    var x = 0
    try {
      var i = 0
      while (i < friendKey.length) {
        x = x ^
          java.lang.Integer.valueOf(friendKey.substring(i, i + 4), 16)
        i += 4
      }
    } catch {
      case e: NumberFormatException => return false
    }
    x == 0
  }

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    getMenuInflater.inflate(R.menu.add_friend, menu)
    true
  }

  override def onOptionsItemSelected(item: MenuItem): Boolean = {
    item.getItemId match {
      case android.R.id.home =>
        NavUtils.navigateUpFromSameTask(this)
        true

      case R.id.scanFriend => scanIntent()
    }
    return super.onOptionsItemSelected(item)
  }

  private def DNSLookup(input: String): Observable[(String, Option[String])] = {
    Observable(subscriber => {
      var user: String = null
      var domain: String = null
      var lookup: String = null
      if (!input.contains("@")) {
        user = input
        domain = "toxme.se"
        lookup = user + "._tox." + domain
      } else {
        user = input.substring(0, input.indexOf("@"))
        domain = input.substring(input.indexOf("@") + 1)
        lookup = user + "._tox." + domain
      }
      var txt: TXTRecord = null
      try {
        val records = new Lookup(lookup, Type.TXT).run()
        txt = records(0).asInstanceOf[TXTRecord]
        val txtString = txt.toString.substring(txt.toString.indexOf('"'))
        if (txtString.contains("tox1")) {
          val key = txtString.substring(11, 11 + 76)
          subscriber.onNext((key, None))
        }
      } catch {
        case e: Exception => e.printStackTrace()
      }
      subscriber.onCompleted()
    })
  }
}
