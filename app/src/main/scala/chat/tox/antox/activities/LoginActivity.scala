package chat.tox.antox.activities

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.{Build, Bundle}
import android.preference.PreferenceManager
import android.support.v4.content.IntentCompat
import android.support.v7.app.AppCompatActivity
import android.view.{View, WindowManager}
import android.widget._
import chat.tox.antox.R
import chat.tox.antox.data.State
import chat.tox.antox.tox.ToxService
import chat.tox.antox.utils.AntoxNotificationManager

import scala.collection.JavaConversions._

class LoginActivity extends AppCompatActivity with AdapterView.OnItemSelectedListener {

  private var profileSelected: String = _

  protected override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_login)
    getSupportActionBar.hide()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      getWindow.setStatusBarColor(getResources.getColor(R.color.black))
    }

    if (Build.VERSION.SDK_INT != Build.VERSION_CODES.JELLY_BEAN &&
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      getWindow.setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
    }

    val preferences = PreferenceManager.getDefaultSharedPreferences(this)
    val userDb = State.userDb(this)

    // If there are no users stored in the DB then start CreateAccount Activity
    if (userDb.numUsers() == 0) {
      val createAccount = new Intent(getApplicationContext, classOf[CreateAccountActivity])
      createAccount.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
        Intent.FLAG_ACTIVITY_CLEAR_TOP |
        IntentCompat.FLAG_ACTIVITY_CLEAR_TASK)
      startActivity(createAccount)
      finish()
    } else if (userDb.loggedIn) {
      // Else If the user is already logged in then start the Tox Service and the MainActivity
      val startTox = new Intent(getApplicationContext, classOf[ToxService])
      getApplicationContext.startService(startTox)
      val main = new Intent(getApplicationContext, classOf[MainActivity])
      startActivity(main)
      finish()
    } else {
      // Else show UI elements to allow user to login
      val profiles = userDb.getAllProfiles
      val profileSpinner = findViewById(R.id.login_account_name).asInstanceOf[Spinner]
      val adapter = new ArrayAdapter[String](this, android.R.layout.simple_spinner_dropdown_item, profiles)
      adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
      profileSpinner.setAdapter(adapter)
      profileSpinner.setSelection(0)
      profileSpinner.setOnItemSelectedListener(this)
    }
  }

  /** Used by the profileSpinner to change the colour of the selected profile text otherwise
      it is white text on a white background **/
  def onItemSelected(parent: AdapterView[_], view: View, pos: Int, id: Long) {
    profileSelected = parent.getItemAtPosition(pos).toString

    if (parent.getChildAt(0) != null) {
      // getChildAt(pos) returns a view, or null if non-existant
      parent.getChildAt(0).asInstanceOf[TextView].setTextColor(Color.BLACK)
    }
  }

  def onNothingSelected(parent: AdapterView[_]) {
  }

  /** Used by the Login ui button to attempt to login. If successful it starts the ToxService
      and MainActivity before closing this activity. Otherwise it shows errors in the form of
      toasts to the user **/
  def onClickLogin(view: View) {
    val account = profileSelected
    // If the user hasn't selected a profile to login with then show an error via a toast
    if (account == "") {
      val context = getApplicationContext
      val text = getString(R.string.login_must_fill_in)
      val duration = Toast.LENGTH_SHORT
      val toast = Toast.makeText(context, text, duration)
      toast.show()
    } else {
      val userDb = State.userDb(this)
      if (userDb.doesUserExist(account)) {
        val details = userDb.getUserDetails(account)
        State.login(account, this)
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

  /** Used by the Create Account ui button to start the CreateAccountActivity **/
  def onClickCreateAccount(view: View) {
    val createAccount = new Intent(getApplicationContext, classOf[CreateAccountActivity])
    startActivityForResult(createAccount, 1)
    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
  }

  /** Checks to see if a requestCode of 1 is returned from an activity and proceeds to finish this
      activity. Used by the onClickCreateAccount above **/
  override def onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
    if (requestCode == 1 && requestCode == Activity.RESULT_OK)
      finish()
  }
}
