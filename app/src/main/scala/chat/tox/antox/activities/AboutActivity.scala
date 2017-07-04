package chat.tox.antox.activities

import java.util.regex.Pattern

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

    setTitle(getResources.getString(R.string.about))
    getSupportActionBar.setDisplayHomeAsUpEnabled(true)
    ThemeManager.applyTheme(this, getSupportActionBar)

    val versionTextView = findViewById(R.id.version_text).asInstanceOf[TextView]
    val sourceURLTextView = findViewById(R.id.source_link).asInstanceOf[TextView]

    // Make the source URL a clickable link
    val pattern = Pattern.compile("https://github.com/Antox/Antox")
    Linkify.addLinks(sourceURLTextView, pattern, "")

    // Fetch the app version and set it in the versionTextView, falling back to a blank version if unable to
    var version_name = "-.-.-"
    var version_code = 0
    try {
      version_name = getPackageManager.getPackageInfo(getPackageName, 0).versionName
    } catch {
      case e: Exception => e.printStackTrace()
    }

    try {
      version_code = getPackageManager.getPackageInfo(getPackageName, 0).versionCode
    } catch {
      case e: Exception => e.printStackTrace()
    }

    versionTextView.setText(getString(R.string.ver) + " " + version_name + " (" + version_code + ")")
  }

  override def onOptionsItemSelected(item: MenuItem): Boolean = item.getItemId match {
    case android.R.id.home =>
      finish()
      true
  }
}