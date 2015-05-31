
package im.tox.antox.activities

import java.io.File

import android.content.Intent
import android.net.Uri
import android.os.{Build, Bundle}
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.{EditText, TextView, Toast}
import de.hdodenhof.circleimageview.CircleImageView
import im.tox.antox.data.AntoxDB
import im.tox.antoxnightly.R

class FriendProfileActivity extends AppCompatActivity {

  var friendKey: String = null

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)

    setContentView(R.layout.activity_friend_profile)

    friendKey = getIntent.getStringExtra("key")
    val db = new AntoxDB(this)
    val friendNote = db.getFriendStatusMessage(friendKey)

    setTitle(getResources.getString(R.string.friend_profile_title, getIntent.getStringExtra("name")))

    val editFriendAlias = findViewById(R.id.friendAliasText).asInstanceOf[EditText]
    editFriendAlias.setText(getIntent.getStringExtra("name"))

    val editFriendNote = findViewById(R.id.friendNoteText).asInstanceOf[TextView]
    editFriendNote.setText("\"" + friendNote + "\"")

    val avatar = getIntent.getSerializableExtra("avatar").asInstanceOf[Option[File]]
    avatar.foreach(avatar => {
      val avatarHolder = findViewById(R.id.avatar).asInstanceOf[CircleImageView]
      avatarHolder.setImageURI(Uri.fromFile(avatar))
    })

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
      getSupportActionBar.setIcon(R.drawable.ic_actionbar)
    }
  }

  def updateAlias(view: View) {
    val db = new AntoxDB(this)
    val friendAlias = findViewById(R.id.friendAliasText).asInstanceOf[EditText]
    db.updateAlias(friendAlias.getText.toString, friendKey)
    db.close()
    val context = getApplicationContext
    val text = getString(R.string.friend_profile_updated)
    val duration = Toast.LENGTH_SHORT
    val toast = Toast.makeText(context, text, duration)
    toast.show()
  }

  override def onBackPressed() {
    super.onBackPressed()
    val intent = new Intent(FriendProfileActivity.this, classOf[MainActivity])
    intent.addCategory(Intent.CATEGORY_HOME)
    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    FriendProfileActivity.this.startActivity(intent)
    finish()
  }
}
