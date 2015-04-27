
package im.tox.antox.activities

import android.os.{Build, Bundle}
import android.support.v7.app.ActionBarActivity
import android.widget.{EditText, TextView}
import im.tox.antox.R
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.utils.UIUtils

class GroupProfileActivity extends ActionBarActivity {

  var groupName: String = null

  var groupId: String = null

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_group_profile)
    groupId = getIntent.getStringExtra("key")

    val group = ToxSingleton.getGroup(groupId)
    setTitle(getResources.getString(R.string.title_activity_group_profile))


      findViewById(R.id.group_name).asInstanceOf[TextView].setText(if (group.name != null) {
        group.name
      } else {
        UIUtils.trimIDForDisplay(group.id)
      })

    findViewById(R.id.group_status_message).asInstanceOf[EditText].setText(group.topic)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
      getSupportActionBar.setIcon(R.drawable.ic_actionbar)
    }
  }

  override def onBackPressed() {
    super.onBackPressed()
  }
}
