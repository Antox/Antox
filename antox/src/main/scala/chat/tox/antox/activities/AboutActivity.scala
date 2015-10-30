package chat.tox.antox.activities

import java.util.regex.Pattern

import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.util.Linkify
import android.view.MenuItem
import android.widget.TextView
import chat.tox.antox.R
import chat.tox.antox.theme.ThemeManager

class AboutActivity extends AppCompatActivity {

  protected override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.about)
    getSupportActionBar.setDisplayHomeAsUpEnabled(true)
    ThemeManager.applyTheme(this, getSupportActionBar)

    val tw = findViewById(R.id.version_text).asInstanceOf[TextView]
    val tw10 = findViewById(R.id.source_link).asInstanceOf[TextView]
    val pattern = Pattern.compile("https://github.com/subliun/Antox")
    Linkify.addLinks(tw10, pattern, "")
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
}
