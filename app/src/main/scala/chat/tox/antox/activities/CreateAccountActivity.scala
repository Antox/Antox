package chat.tox.antox.activities

import java.io.File
import java.util.regex.Pattern

import android.animation.ValueAnimator.AnimatorUpdateListener
import android.animation.{ArgbEvaluator, ValueAnimator}
import android.app.Activity
import android.content.Intent
import android.os.{Build, Bundle, Environment}
import android.support.v7.app.AppCompatActivity
import android.view.{Menu, MenuItem, View, WindowManager}
import android.widget.{Button, EditText, ProgressBar, Toast}
import chat.tox.antox.R
import chat.tox.antox.data.{State, UserDB}
import chat.tox.antox.theme.ThemeManager
import chat.tox.antox.tox.{ToxDataFile, ToxService}
import chat.tox.antox.toxme.ToxMe.PrivacyLevel
import chat.tox.antox.toxme.ToxMeError.ToxMeError
import chat.tox.antox.toxme.{ToxData, ToxMe, ToxMeError, ToxMeName}
import chat.tox.antox.transfer.FileDialog
import chat.tox.antox.utils._
import chat.tox.antox.wrapper.ToxAddress
import im.tox.tox4j.core.exceptions.ToxNewException
import im.tox.tox4j.core.options.SaveDataOptions.ToxSave
import im.tox.tox4j.core.options.ToxOptions
import im.tox.tox4j.exceptions.ToxException
import im.tox.tox4j.impl.jni.ToxCoreImpl
import rx.lang.scala.Observable
import rx.lang.scala.schedulers.AndroidMainThreadScheduler


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

    !(account == "" || containsSpaces || containsFileSeparator)
  }

  def showBadAccountNameError(): Unit = {
    val context = getApplicationContext
    val text = getString(R.string.create_bad_profile_name)
    val duration = Toast.LENGTH_SHORT
    val toast = Toast.makeText(context, text, duration)
    toast.show()
  }

  def loginAndStartMain(accountName: String, password: String) {
    val userDb = State.userDb(this)
    State.login(accountName, this)
    userDb.updateActiveUserDetail(DatabaseConstants.COLUMN_NAME_PASSWORD, password)

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
    toxData.address = new ToxAddress(tox.getAddress.value)
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
      toxData.address = new ToxAddress(tox.getAddress.value)
      toxData.fileBytes = toxDataFile.loadFile()

      Option(toxData)
    } catch {
      case error: ToxNewException =>
        if (error.code == ToxNewException.Code.LOAD_ENCRYPTED) {
          Toast.makeText(
            getBaseContext,
            getString(R.string.create_account_encrypted_profile_error),
            Toast.LENGTH_SHORT
          ).show()
        } else {
          Toast.makeText(
            getBaseContext,
            getString(R.string.create_account_load_profile_unknown),
            Toast.LENGTH_SHORT
          ).show()
        }

        None
    }
  }


  def disableRegisterButton(): Unit = {
    //prevent user from registering some other way while trying to register
    val registerIncognitoButton = findViewById(R.id.create_account_incog).asInstanceOf[Button]
    registerIncognitoButton.setEnabled(false)

    val importProfileButton = findViewById(R.id.create_account_import).asInstanceOf[Button]
    importProfileButton.setEnabled(false)

    val registerButton = findViewById(R.id.create_account).asInstanceOf[Button]
    registerButton.setText(getResources.getText(R.string.create_registering))
    registerButton.setEnabled(false)

    //only animate on 2.3+ because animation was added in 3.0
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      val colorFrom = getResources.getColor(R.color.brand_secondary)
      val colorTo = getResources.getColor(R.color.brand_secondary_darker)
      val colorAnimation = ValueAnimator.ofObject(
        new ArgbEvaluator(),
        colorFrom.asInstanceOf[java.lang.Integer],
        colorTo.asInstanceOf[java.lang.Integer])

      colorAnimation.addUpdateListener(new AnimatorUpdateListener {
        override def onAnimationUpdate(animation: ValueAnimator): Unit = {
          registerButton.setBackgroundColor(animation.getAnimatedValue.asInstanceOf[Int])
        }
      })
      colorAnimation.start()
    }

    val progressBar = findViewById(R.id.login_progress_bar).asInstanceOf[ProgressBar]
    progressBar.setVisibility(View.VISIBLE)
  }

  def enableRegisterButton(): Unit = {
    val registerIncognitoButton = findViewById(R.id.create_account_incog).asInstanceOf[Button]
    registerIncognitoButton.setEnabled(true)

    val importProfileButton = findViewById(R.id.create_account_import).asInstanceOf[Button]
    importProfileButton.setEnabled(true)

    val registerButton = findViewById(R.id.create_account).asInstanceOf[Button]
    registerButton.setEnabled(true)
    registerButton.setText(getResources.getText(R.string.create_register))
    registerButton.setBackgroundColor(getResources.getColor(R.color.brand_secondary))

    val progressBar = findViewById(R.id.login_progress_bar).asInstanceOf[ProgressBar]
    progressBar.setVisibility(View.GONE)
  }

  def createAccount(rawAccountName: String, userDb: UserDB, shouldCreateDataFile: Boolean, shouldRegister: Boolean): Unit = {
    val toxMeName = ToxMeName.fromString(rawAccountName, shouldRegister)
    if (!validAccountName(toxMeName.username)) {
      showBadAccountNameError()
    } else if (userDb.doesUserExist(toxMeName.username)) {
      val context = getApplicationContext
      val text = getString(R.string.create_profile_exists)
      val duration = Toast.LENGTH_LONG
      val toast = Toast.makeText(context, text, duration)
      toast.show()
    } else {
      disableRegisterButton()

      val toxData =
        if (shouldCreateDataFile) {
          // Create tox data save file
          try {
            Some(createToxData(toxMeName.username))
          } catch {
            case e: ToxException[_] =>
              AntoxLog.debug("Failed creating tox data save file")
              None
          }
        } else {
          loadToxData(toxMeName.username)
        }

      toxData match {
        case Some(data) =>
          val observable =
            if (shouldRegister) {
              // Register acccount
              if (ConnectionManager.isNetworkAvailable(this)) {
                ToxMe.registerAccount(toxMeName, PrivacyLevel.PUBLIC, data)
              } else {
                // fail if there is no connection
                Observable.just(Left(ToxMeError.CONNECTION_ERROR))
              }

            } else {
              //succeed with empty password
              Observable.just(Right(""))
            }

          observable
            .observeOn(AndroidMainThreadScheduler())
            .subscribe(result => {
            onRegistrationResult(toxMeName, data, result)
          }, error => {
            AntoxLog.debug("Unexpected error registering account.")
            error.printStackTrace()
          })

        case None =>
          enableRegisterButton()
      }
    }
  }

  def onRegistrationResult(toxMeName: ToxMeName, toxData: ToxData, result: Either[ToxMeError, String]): Unit = {
    var successful = true
    var accountPassword = ""
    val toastMessage: Option[String] = result match {
      case Left(error) =>
        successful = false
        Some(error match {
          case ToxMeError.NAME_TAKEN => getString(R.string.create_account_exists)
          case ToxMeError.INTERNAL => getString(R.string.create_account_internal_error)
          case ToxMeError.RATE_LIMIT => getString(R.string.create_account_reached_registration_limit)
          case ToxMeError.KALIUM_LINK_ERROR => getString(R.string.create_account_kalium_link_error)
          case ToxMeError.INVALID_DOMAIN => getString(R.string.create_account_invalid_domain)
          case ToxMeError.CONNECTION_ERROR => getString(R.string.create_account_connection_error)
          case _ => getString(R.string.create_account_unknown_error)
        })
      case Right(password) =>
        successful = true
        accountPassword = password
        None
    }

    if (successful) {
      State.userDb(this).addUser(toxMeName, toxData.address, "")
      loginAndStartMain(toxMeName.username, accountPassword)
    } else {
      toastMessage.foreach(message => {
        val context = getApplicationContext
        val duration = Toast.LENGTH_LONG
        Toast.makeText(context, message, duration).show()
      })
    }

    enableRegisterButton()
  }

  def onClickRegisterIncogAccount(view: View) {
    val accountField = findViewById(R.id.create_account_name).asInstanceOf[EditText]
    val account = accountField.getText.toString

    val userDb = State.userDb(this)
    createAccount(account, userDb, shouldCreateDataFile = true, shouldRegister = false)
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
          createAccount(accountName, State.userDb(this), shouldCreateDataFile = false, shouldRegister = false)
        } else {
          showBadAccountNameError()
        }

      case None => throw new Exception("Could not load data file.")
    }
  }

  def onClickRegisterAccount(view: View) {
    val accountField = findViewById(R.id.create_account_name).asInstanceOf[EditText]
    val account = accountField.getText.toString

    val userDb = State.userDb(this)

    createAccount(account, userDb, shouldCreateDataFile = true, shouldRegister = true)
  }
}