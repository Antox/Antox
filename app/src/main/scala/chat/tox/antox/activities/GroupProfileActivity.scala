
package chat.tox.antox.activities

import android.os.{Build, Bundle}
import android.support.v7.app.AppCompatActivity
import android.widget.{EditText, TextView}
import chat.tox.antox.R
import chat.tox.antox.theme.ThemeManager
import chat.tox.antox.tox.ToxSingleton
import chat.tox.antox.utils.UiUtils
import chat.tox.antox.wrapper.{GroupKey, ToxKey}

class GroupProfileActivity extends AppCompatActivity {

  var groupName: String = null

  var groupKey: GroupKey = _

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_group_profile)
    groupKey = new GroupKey(getIntent.getStringExtra("key"))

    ThemeManager.applyTheme(this, getSupportActionBar)

    val group = ToxSingleton.getGroup(groupKey)
    setTitle(getResources.getString(R.string.title_activity_group_profile))


      findViewById(R.id.group_name).asInstanceOf[TextView].setText(if (group.name != null) {
        group.name
      } else {
        UiUtils.trimId(group.key)
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
