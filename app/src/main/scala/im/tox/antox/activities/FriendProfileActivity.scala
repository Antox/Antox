
package im.tox.antox.activities

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v7.app.ActionBarActivity
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import im.tox.antox.R
import im.tox.antox.data.AntoxDB
//remove if not needed
import scala.collection.JavaConversions._

class FriendProfileActivity extends ActionBarActivity {

  var friendName: String = null

  var friendKey: String = null

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_friend_profile)
    friendKey = getIntent.getStringExtra("key")
    val db = new AntoxDB(this)
    val friendDetails = db.getFriendDetails(friendKey)
    friendName = friendDetails(0)
    val friendAlias = friendDetails(1)
    val friendNote = friendDetails(2)
    if (friendAlias == "") setTitle(getResources.getString(R.string.friend_profile_title, friendName)) else setTitle(getResources.getString(R.string.friend_profile_title,
      friendAlias))
    val editFriendAlias = findViewById(R.id.friendAliasText).asInstanceOf[EditText]
    editFriendAlias.setText(friendAlias)
    val editFriendNote = findViewById(R.id.friendNoteText).asInstanceOf[TextView]
    editFriendNote.setText("\"" + friendNote + "\"")
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
