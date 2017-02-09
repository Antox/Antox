package chat.tox.antox.activities

import android.app.SearchManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity

class ShareActivity extends AppCompatActivity {
  override protected def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
  }

  override protected def onNewIntent(intent: Intent) {
    if (Intent.ACTION_SEARCH == intent.getAction) {
      val query: String = intent.getStringExtra(SearchManager.QUERY)
    }
    else {
      val data: Uri = intent.getData
      val dataString: String = intent.getDataString
      val shareWith: String = dataString.substring(dataString.lastIndexOf('/') + 1)
    }
  }
}