
package im.tox.antox.activities

import java.io.File

import android.content.Intent
import android.os.{Build, Bundle}
import android.support.v7.app.AppCompatActivity
import android.text.{Editable, TextWatcher}
import android.widget.{EditText, TextView}
import de.hdodenhof.circleimageview.CircleImageView
import im.tox.antox.data.AntoxDB
import im.tox.antox.utils.BitmapManager
import im.tox.antoxnightly.R

class FriendProfileActivity extends AppCompatActivity {

  var friendKey: String = null
  var nickChanged: Boolean = false

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)

    setContentView(R.layout.activity_friend_profile)

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
      getSupportActionBar.setIcon(R.drawable.ic_actionbar)
    }

    friendKey = getIntent.getStringExtra("key")
    val db = new AntoxDB(this)
    val friendNote = db.getFriendStatusMessage(friendKey)

    setTitle(getResources.getString(R.string.friend_profile_title, getIntent.getStringExtra("name")))

    val editFriendAlias = findViewById(R.id.friendAlias).asInstanceOf[EditText]
    editFriendAlias.setText(getIntent.getStringExtra("name"))

    editFriendAlias.addTextChangedListener(new TextWatcher() {
      override def afterTextChanged(s: Editable) {
        /* Set nick changed to true in order to save change in onPause() */
        nickChanged = true

        /* Update title to reflect new nick */
        setTitle(getResources.getString(R.string.friend_profile_title, editFriendAlias.getText.toString))
      }

      override def beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

      override def onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    })

    // Set cursor to end of edit text field
    editFriendAlias.setSelection(editFriendAlias.length(), editFriendAlias.length())

    val editFriendNote = findViewById(R.id.friendNoteText).asInstanceOf[TextView]
    editFriendNote.setText("\"" + friendNote + "\"")

    val avatar = getIntent.getSerializableExtra("avatar").asInstanceOf[Option[File]]
    avatar.foreach(avatar => {
      val avatarHolder = findViewById(R.id.avatar).asInstanceOf[CircleImageView]
      BitmapManager.load(avatar, avatarHolder, isAvatar = true)
    })
  }

  override def onBackPressed() {
    super.onBackPressed()
    val intent = new Intent(FriendProfileActivity.this, classOf[MainActivity])
    intent.addCategory(Intent.CATEGORY_HOME)
    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    FriendProfileActivity.this.startActivity(intent)
    finish()
  }

  /**
   * Override onPause() in order to save any nickname changes
   */
  override def onPause() {
    super.onPause()

    /* Update friend alias after text has been changed */
    if (nickChanged) {
      val editFriendAlias = findViewById(R.id.friendAlias).asInstanceOf[EditText]
      val db = new AntoxDB(getApplicationContext)
      db.updateAlias(editFriendAlias.getText.toString, friendKey)
    }
  }
}
