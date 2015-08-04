package chat.tox.antox.activities

import java.io.File
import java.util.regex.Pattern

import android.app.Activity
import android.content.Intent
import android.os.{Build, Bundle, Environment}
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.{Menu, MenuItem, View, WindowManager}
import android.widget.{EditText, Toast}
import chat.tox.antox.R
import chat.tox.antox.data.{State, UserDB}
import chat.tox.antox.theme.ThemeManager
import chat.tox.antox.tox.{ToxDataFile, ToxService}
import chat.tox.antox.toxdns.ToxDNS.RegError
import chat.tox.antox.toxdns.{ToxDNS, ToxData}
import chat.tox.antox.transfer.FileDialog
import chat.tox.antox.utils._
import im.tox.tox4j.core.exceptions.ToxNewException
import im.tox.tox4j.core.options.SaveDataOptions.ToxSave
import im.tox.tox4j.core.options.ToxOptions
import im.tox.tox4j.exceptions.ToxException
import im.tox.tox4j.impl.jni.ToxCoreImpl

class CreateAccountActivity extends AppCompatActivity {

  protected override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    getSupportActionBar.hide()
    ThemeManager.applyTheme(this, getSupportActionBar)

    setContentView(R.layout.activity_create_account)
    if (Build.VERSION.SDK_INT != Build.VERSION_CODES.JELLY_BEAN &&
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      getWindow.setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      getWindow.setStatusBarColor(getResources.getColor(R.color.material_blue_grey_950))
    }
  }

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    getMenuInflater.inflate(R.menu.create_account, menu)
    true
  }

  override def onOptionsItemSelected(item: MenuItem): Boolean = {
    val id = item.getItemId
    if (id == R.id.action_settings) {
      return true
    }
    super.onOptionsItemSelected(item)
  }

  def validAccountName(account: String): Boolean = {
    val pattern = Pattern.compile("\\s")
    val pattern2 = Pattern.compile(File.separator)
    var matcher = pattern.matcher(account)
    val containsSpaces = matcher.find()
    matcher = pattern2.matcher(account)
    val containsFileSeparator = matcher.find()

    if (account == "" || containsSpaces || containsFileSeparator)
      return false

    true
  }

  def showBadAccountNameError(): Unit = {
    val context = getApplicationContext
    val text = getString(R.string.create_bad_profile_name)
    val duration = Toast.LENGTH_SHORT
    val toast = Toast.makeText(context, text, duration)
    toast.show()
  }

  def saveAccountAndStartMain(accountName: String, password: String, toxID: String) {
    // Save preferences
    val preferences = PreferenceManager.getDefaultSharedPreferences(this)
    val editor = preferences.edit()
    editor.putString("active_account", accountName)
    editor.putString("nickname", accountName)
    editor.putString("password", password)
    editor.putString("status", "online")
    editor.putString("status_message", getResources.getString(R.string.pref_default_status_message))
    editor.putString("tox_id", toxID)
    editor.putBoolean("logging_enabled", true)
    editor.putBoolean("loggedin", true)
    editor.putBoolean("autostart", true)
    editor.apply()

    // Start the activity
    val startTox = new Intent(getApplicationContext, classOf[ToxService])
    getApplicationContext.startService(startTox)
    val main = new Intent(getApplicationContext, classOf[MainActivity])
    startActivity(main)
    setResult(Activity.RESULT_OK)

    finish()
  }

  def createToxData(accountName: String): ToxData = {
    val toxData = new ToxData
    val toxOptions = new ToxOptions(Options.ipv6Enabled, Options.udpEnabled)
    val tox = new ToxCoreImpl(toxOptions)
    val toxDataFile = new ToxDataFile(this, accountName)
    toxDataFile.saveFile(tox.getSavedata)
    toxData.ID = chat.tox.antox.utils.Hex.bytesToHexString(tox.getAddress)
    toxData.fileBytes = toxDataFile.loadFile()
    toxData
  }

  def loadToxData(fileName: String): Option[ToxData] = {
    val toxData = new ToxData
    val toxDataFile = new ToxDataFile(this, fileName)
    val toxOptions = new ToxOptions(
      Options.ipv6Enabled,
      Options.udpEnabled,
      saveData = ToxSave(toxDataFile.loadFile()))

    try {
      val tox = new ToxCoreImpl(toxOptions)
      toxData.ID = chat.tox.antox.utils.Hex.bytesToHexString(tox.getAddress)
      toxData.fileBytes = toxDataFile.loadFile()

      Option(toxData)
    } catch {
      case error: ToxNewException =>
        if (error.code == ToxNewException.Code.LOAD_ENCRYPTED)
          Toast.makeText(
            getBaseContext,
            getString(R.string.create_account_encrypted_profile_error),
            Toast.LENGTH_SHORT
          ).show()
        else
          Toast.makeText(
            getBaseContext,
            getString(R.string.create_account_load_profile_unknown),
            Toast.LENGTH_SHORT
          ).show()

        None
    }
  }

  def createAccount(accountName: String, userDb: UserDB, createDataFile: Boolean, shouldRegister: Boolean): Unit = {
    try {
      if (!validAccountName(accountName)) {
        showBadAccountNameError()
      } else if (userDb.doesUserExist(accountName)) {
        val context = getApplicationContext
        val text = getString(R.string.create_profile_exists)
        val duration = Toast.LENGTH_LONG
        val toast = Toast.makeText(context, text, duration)
        toast.show()
      } else {
        var toxData = new ToxData

        if (createDataFile) {
          // Create tox data save file
          try {
            toxData = createToxData(accountName)
          } catch {
            case e: ToxException[_] => Log.d("CreateAccount", "Failed creating tox data save file")
          }
        } else {
          val result = loadToxData(accountName)

          result match {
            case Some(data) =>
              toxData = data

            // If None is returned then failed to load data so exit the createAccount function
            case None =>
              return
          }
        }

        var successful = true
        var accountPassword: String = ""
        if (shouldRegister) {
          // Register on toxme.se
          val registerResult = ToxDNS.registerAccount(accountName, toxData)

          val toastMessage = registerResult match {
            case Left(error) =>
              successful = false
              error match {
                case RegError.NAME_TAKEN => getString(R.string.create_account_exists)
                case RegError.INTERNAL => getString(R.string.create_account_internal_error)
                case RegError.REGISTRATION_LIMIT_REACHED => getString(R.string.create_account_reached_registration_limit)
                case RegError.KALIUM_LINK_ERROR => getString(R.string.create_account_kalium_link_error)
                case _ => getString(R.string.create_account_unknown_error)
              }
            case Right(password) =>
              successful = true
              accountPassword = password
              null
          }

          if (toastMessage != null) {
            val context = getApplicationContext
            val duration = Toast.LENGTH_LONG
            Toast.makeText(context, toastMessage, duration).show()
          }
        }

        if (successful) {
          userDb.addUser(accountName, "")
          userDb.updateUserDetail(accountName, "password", accountPassword)

          saveAccountAndStartMain(accountName, accountPassword, toxData.ID)
        }
      }
    } finally {
    }
  }

  def onClickRegisterIncogAccount(view: View) {
    val accountField = findViewById(R.id.create_account_name).asInstanceOf[EditText]
    val account = accountField.getText.toString

    val userDb = State.userDb
    createAccount(account, userDb, createDataFile = true, shouldRegister = false)
  }

  def onClickImportProfile(view: View): Unit = {
    val accountField = findViewById(R.id.create_account_name).asInstanceOf[EditText]

    val path = new File(Environment.getExternalStorageDirectory + "//DIR//")

    //prompt the user to select the profile to import
    val fileDialog = new FileDialog(this, path, false)
    fileDialog.addFileListener(new FileDialog.FileSelectedListener() {
      def fileSelected(file: File) {
        onImportFileSelected(Some(file), accountField.getText.toString)
      }
    })
    fileDialog.showDialog()
  }

  def onImportFileSelected(selectedFile: Option[File], accountFieldName: String): Unit = {
    selectedFile match {
      case Some(file) =>
        if (!file.getName.contains(".tox")) {
          val context = getApplicationContext
          val duration = Toast.LENGTH_SHORT
          Toast.makeText(context, getResources.getString(R.string.import_profile_invalid_file), duration).show()
          return
        }

        val accountName =
          if (accountFieldName.isEmpty) {
            file.getName.replace(".tox", "")
          } else {
            accountFieldName
          }

        if (validAccountName(accountName)) {
          val toxDataFile = new File(getFilesDir.getAbsolutePath + "/" + accountName)
          FileUtils.copy(file, toxDataFile)
          createAccount(accountName, State.userDb, createDataFile = false, shouldRegister = false)
        } else {
          showBadAccountNameError()
        }

      case None => throw new Exception("Could not load data file.")
    }
  }

  def onClickRegisterAccount(view: View) {
    val accountField = findViewById(R.id.create_account_name).asInstanceOf[EditText]
    val account = accountField.getText.toString

    val userDb = State.userDb

    createAccount(account, userDb, createDataFile = true, shouldRegister = true)
  }
}