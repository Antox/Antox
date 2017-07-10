package chat.tox.antox.activities

import java.io.File
import java.util.regex.Pattern

import android.animation.ValueAnimator.AnimatorUpdateListener
import android.animation.{ArgbEvaluator, ObjectAnimator, ValueAnimator}
import android.app.Activity
import android.content.DialogInterface.OnClickListener
import android.content.{DialogInterface, Intent}
import android.graphics.Color
import android.os.{Build, Bundle, Environment}
import android.support.v7.app.{AlertDialog, AppCompatActivity}
import android.text._
import android.view._
import android.widget._
import chat.tox.antox.R
import chat.tox.antox.data.{State, UserDB}
import chat.tox.antox.theme.ThemeManager
import chat.tox.antox.tox.{ToxDataFile, ToxService}
import chat.tox.antox.toxme.ToxMe.PrivacyLevel
import chat.tox.antox.toxme.ToxMeError.ToxMeError
import chat.tox.antox.toxme.{ToxData, ToxMe, ToxMeError, ToxMeName}
import chat.tox.antox.utils._
import chat.tox.antox.wrapper.ToxAddress
import com.github.angads25.filepicker.controller.DialogSelectionListener
import com.github.angads25.filepicker.model.{DialogConfigs, DialogProperties}
import com.github.angads25.filepicker.view.FilePickerDialog
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

    val toxmeCheckBox = findViewById(R.id.toxme).asInstanceOf[CheckBox]
    val toxmeText = findViewById(R.id.toxme_text).asInstanceOf[TextView]

    toxmeText.setOnClickListener(new View.OnClickListener {
      override def onClick(v: View): Unit = {
        toxmeCheckBox.toggle()
        toggleRegisterText()
      }
    })

    toxmeCheckBox.setOnClickListener(new View.OnClickListener {
      override def onClick(v: View): Unit = {
        toggleRegisterText()
      }
    })

    val toxmeHelpButton = findViewById(R.id.toxme_help_button).asInstanceOf[ImageView]
    toxmeHelpButton.setOnClickListener(new View.OnClickListener {
      override def onClick(v: View): Unit = {
        val intent = new Intent(CreateAccountActivity.this, classOf[ToxMeInfoActivity])
        startActivity(intent)
      }
    })

  }


  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    getMenuInflater.inflate(R.menu.create_account, menu)
    true
  }

  override def onOptionsItemSelected(item: MenuItem): Boolean = {
    val id = item.getItemId
    id == R.id.action_settings || super.onOptionsItemSelected(item)
  }

  def toggleRegisterText(): Unit = {
    val registerButton = findViewById(R.id.create_account).asInstanceOf[Button]
    val toxmeCheckBox = findViewById(R.id.toxme).asInstanceOf[CheckBox]
    val fadeOut = ObjectAnimator.ofInt(registerButton, "textColor", getResources.getColor(R.color.white), Color.TRANSPARENT)
    fadeOut.setDuration(300)
    fadeOut.setEvaluator(new ArgbEvaluator)
    fadeOut.start()
    if (toxmeCheckBox.isChecked) {
      registerButton.setText(R.string.create_register_with_toxme)
    }
    else {
      registerButton.setText(R.string.create_register)
    }
    val fadeIn = ObjectAnimator.ofInt(registerButton, "textColor", Color.TRANSPARENT, getResources.getColor(R.color.white))
    fadeIn.setDuration(300)
    fadeIn.setEvaluator(new ArgbEvaluator)
    fadeIn.start()
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

    val importProfileButton = findViewById(R.id.create_account_import).asInstanceOf[Button]
    importProfileButton.setEnabled(true)

    val registerButton = findViewById(R.id.create_account).asInstanceOf[Button]
    registerButton.setEnabled(true)


    val toxmeCheckBox = findViewById(R.id.toxme).asInstanceOf[CheckBox]

    if (toxmeCheckBox.isChecked) {
      registerButton.setText(R.string.create_register_with_toxme)
    }
    else {
      registerButton.setText(R.string.create_register)
    }

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

  def onClickImportProfile(view: View): Unit = {
    val accountField = findViewById(R.id.create_account_name).asInstanceOf[EditText]

    val path = Environment.getExternalStorageDirectory

    val properties: DialogProperties = new DialogProperties()
    properties.selection_mode = DialogConfigs.SINGLE_MODE
    properties.selection_type = DialogConfigs.FILE_SELECT
    properties.root = path
    properties.error_dir = path
    properties.extensions = null
    val dialog: FilePickerDialog = new FilePickerDialog(this, properties)
    dialog.setTitle(R.string.select_file)

    dialog.setDialogSelectionListener(new DialogSelectionListener() {
      override def onSelectedFilePaths(files: Array[String]) = {
        // files is the array of the paths of files selected by the Application User.
        // since we only want single file selection, use the first entry
        if (files != null) {
          if (files.length > 0) {
            if (files(0) != null) {
              if (files(0).length > 0) {
                val filePath: File = new File(files(0))
                onImportFileSelected(Some(filePath), accountField.getText.toString)
              }
            }
          }
        }
      }
    })

    dialog.show()
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
          val importedFile = new File(getFilesDir.getAbsolutePath + "/" + accountName)
          FileUtils.copy(file, importedFile)
          val toxDataFile = new ToxDataFile(this, accountName)
          if (toxDataFile.isEncrypted) {
            AntoxLog.debug("Profile is encrypted")
            val builder = new AlertDialog.Builder(this)
            builder.setTitle(getString(R.string.login_profile_encrypted))
            val input = new EditText(this)
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)
            builder.setView(input)

            builder.setPositiveButton(getString(R.string.button_ok), new OnClickListener {
              override def onClick(dialog: DialogInterface, which: Int): Unit = {
                try {
                  toxDataFile.decrypt(input.getText.toString)
                  createAccount(accountName, State.userDb(getApplicationContext), shouldCreateDataFile = false, shouldRegister = false)
                }
                catch {
                  case e: Exception =>
                    Toast.makeText(getApplicationContext, getString(R.string.login_passphrase_incorrect), Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
              }
            })

            builder.setNegativeButton(getString(R.string.button_cancel), new OnClickListener {
              override def onClick(dialog: DialogInterface, which: Int): Unit = {
                throw new Exception("No password specified.")
              }
            })
            builder.show()
          }
          else createAccount(accountName, State.userDb(this), shouldCreateDataFile = false, shouldRegister = false)
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

    val shouldRegister = findViewById(R.id.toxme).asInstanceOf[CheckBox].isChecked
    createAccount(account, userDb, shouldCreateDataFile = true, shouldRegister)
  }
}