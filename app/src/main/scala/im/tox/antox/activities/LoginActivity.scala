package im.tox.antox.activities

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.{Build, Bundle}
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.view.{View, WindowManager}
import android.widget._
import im.tox.antox.data.UserDB
import im.tox.antox.tox.ToxService
import im.tox.antoxnightly.R

class LoginActivity extends AppCompatActivity with AdapterView.OnItemSelectedListener {

  private var profileSelected: String = _

  protected override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_login)
    getSupportActionBar.hide()
    if (Build.VERSION.SDK_INT != Build.VERSION_CODES.JELLY_BEAN &&
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      getWindow.setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
    }
    val preferences = PreferenceManager.getDefaultSharedPreferences(this)
    val db = new UserDB(this)
    if (!db.doUsersExist()) {
      db.close()
      val createAccount = new Intent(getApplicationContext, classOf[CreateAccountActivity])
      startActivity(createAccount)
      finish()
    } else if (preferences.getBoolean("loggedin", false)) {
      db.close()
      val startTox = new Intent(getApplicationContext, classOf[ToxService])
      getApplicationContext.startService(startTox)
      val main = new Intent(getApplicationContext, classOf[MainActivity])
      startActivity(main)
      finish()
    } else {
      val profiles = db.getAllProfiles
      db.close()
      val profileSpinner = findViewById(R.id.login_account_name).asInstanceOf[Spinner]
      val adapter = new ArrayAdapter[String](this, android.R.layout.simple_spinner_dropdown_item, profiles)
      adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
      profileSpinner.setAdapter(adapter)
      profileSpinner.setSelection(0)
      profileSpinner.setOnItemSelectedListener(this)
    }
  }

  def onItemSelected(parent: AdapterView[_], view: View, pos: Int, id: Long) {
    profileSelected = parent.getItemAtPosition(pos).toString

    if (parent.getChildAt(0) != null) // getChildAt(pos) returns a view, or null if non-existant
      parent.getChildAt(0).asInstanceOf[TextView].setTextColor(Color.BLACK)
  }

  def onNothingSelected(parent: AdapterView[_]) {
  }

  def onClickLogin(view: View) {
    val account = profileSelected
    if (account == "") {
      val context = getApplicationContext
      val text = getString(R.string.login_must_fill_in)
      val duration = Toast.LENGTH_SHORT
      val toast = Toast.makeText(context, text, duration)
      toast.show()
    } else {
      val db = new UserDB(this)
      if (db.doesUserExist(account)) {
        val details = db.getUserDetails(account)
        db.close()
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = preferences.edit()
        editor.putBoolean("loggedin", true)
        editor.putString("active_account", account)
        editor.putString("nickname", details.nickname)
        editor.putString("password", details.password)
        editor.putString("status", details.status)
        editor.putString("status_message", details.statusMessage)
        editor.putBoolean("logging_enabled", details.loggingEnabled)
        editor.putString("avatar", details.avatarName)
        editor.apply()
        val startTox = new Intent(getApplicationContext, classOf[ToxService])
        getApplicationContext.startService(startTox)
        val main = new Intent(getApplicationContext, classOf[MainActivity])
        startActivity(main)
        finish()
      } else {
        val context = getApplicationContext
        val text = getString(R.string.login_bad_login)
        val duration = Toast.LENGTH_SHORT
        val toast = Toast.makeText(context, text, duration)
        toast.show()
      }
    }
  }

  def onClickCreateAccount(view: View) {
    val createAccount = new Intent(getApplicationContext, classOf[CreateAccountActivity])
    startActivityForResult(createAccount, 1)
    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
  }

  override def onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
    if (requestCode == 1) {
      if (resultCode == Activity.RESULT_OK) {
        finish()
      }
    }
  }
}
