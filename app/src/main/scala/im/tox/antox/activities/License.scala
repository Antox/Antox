package im.tox.antox.activities

import android.os.Bundle
import android.support.v7.app.ActionBarActivity
import android.view.MenuItem
import android.webkit.WebView
import im.tox.antox.R
//remove if not needed

class License extends ActionBarActivity {

  override def onOptionsItemSelected(item: MenuItem): Boolean = item.getItemId match {
    case android.R.id.home =>
      finish()
      true
  }

  override def onCreate(paramBundle: Bundle) {
    super.onCreate(paramBundle)
    setContentView(R.layout.license_menu)
    if (getSupportActionBar != null) getSupportActionBar.setDisplayHomeAsUpEnabled(true)
    val localWebView = findViewById(R.id.webView).asInstanceOf[WebView]
    val webSettings = localWebView.getSettings
    webSettings.setJavaScriptEnabled(false)
    localWebView.loadUrl("file:///android_res/raw/license.html")
  }
}
