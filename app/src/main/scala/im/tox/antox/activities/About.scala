package im.tox.antox.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v7.app.ActionBarActivity
import android.text.Html
import android.text.util.Linkify
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import java.util.regex.Pattern
import im.tox.antox.R
//remove if not needed
import scala.collection.JavaConversions._

class About extends ActionBarActivity {

  protected override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.about)
    getSupportActionBar.setDisplayHomeAsUpEnabled(true)
    val tw = findViewById(R.id.textView).asInstanceOf[TextView]
    val tw10 = findViewById(R.id.textView10).asInstanceOf[TextView]
    val tw11 = findViewById(R.id.textView11).asInstanceOf[TextView]
    val pattern = Pattern.compile("https://github.com/Astonex/Antox")
    Linkify.addLinks(tw10, pattern, "")
    tw11.setText(Html.fromHtml("<a href=\"\">" + getString(R.string.open_source_license) +
      "</a>"))
    var version = "-.-.-"
    try {
      version = getPackageManager.getPackageInfo(getPackageName, 0).versionName
    } catch {
      case e: PackageManager.NameNotFoundException => e.printStackTrace()
    }
    tw.setText(getString(R.string.ver) + " " + version)
  }

  override def onOptionsItemSelected(item: MenuItem): Boolean = item.getItemId match {
    case android.R.id.home =>
      finish()
      true

  }

  def onLicenseClick(view: View) {
    val intent = new Intent(this, classOf[License])
    startActivity(intent)
  }
}
