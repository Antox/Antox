package chat.tox.antox.activities

import java.io.{File, FileNotFoundException, FileOutputStream, IOException}
import java.util.Random

import android.content.DialogInterface.OnClickListener
import android.content.{Context, DialogInterface, Intent, SharedPreferences}
import android.graphics.{Bitmap, BitmapFactory}
import android.net.Uri
import android.os.{Bundle, Environment}
import android.preference.Preference.OnPreferenceClickListener
import android.preference.{ListPreference, Preference, PreferenceManager}
import android.support.v7.app.AlertDialog
import android.support.v7.app.AlertDialog.Builder
import android.view.{MenuItem, View}
import android.widget.{ImageButton, Toast}
import chat.tox.QR.{Contents, QRCodeEncode}
import chat.tox.antox.R
import chat.tox.antox.activities.ProfileSettingsActivity._
import chat.tox.antox.data.State
import chat.tox.antox.fragments.AvatarDialog
import chat.tox.antox.theme.ThemeManager
import chat.tox.antox.tox.ToxSingleton
import chat.tox.antox.wrapper.UserStatus
import com.github.angads25.filepicker.controller.DialogSelectionListener
import com.github.angads25.filepicker.model.{DialogConfigs, DialogProperties}
import com.github.angads25.filepicker.view.FilePickerDialog
import com.google.zxing.{BarcodeFormat, WriterException}
import im.tox.tox4j.core.data.{ToxNickname, ToxStatusMessage}
import im.tox.tox4j.exceptions.ToxException

object ProfileSettingsActivity {

  private val sBindPreferenceSummaryToValueListener: Preference.OnPreferenceChangeListener = new Preference.OnPreferenceChangeListener() {

    override def onPreferenceChange(preference: Preference, value: AnyRef): Boolean = {
      val stringValue = value.toString
      preference match {
        case lp: ListPreference =>
          val index = lp.findIndexOfValue(stringValue)
          preference.setSummary(if (index >= 0) lp.getEntries()(index) else null)

        case _ =>
          preference.setSummary(stringValue)
      }
      true
    }
  }

  private def bindPreferenceSummaryToValue(preference: Preference) {
    preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener)
    sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, PreferenceManager.getDefaultSharedPreferences(preference.getContext)
      .getString(preference.getKey, ""))
  }
}

class ProfileSettingsActivity extends BetterPreferenceActivity {

  private var avatarDialog: AvatarDialog = _

  override def onCreate(savedInstanceState: Bundle) {

    getDelegate.installViewFactory()
    getDelegate.onCreate(savedInstanceState)
    super.onCreate(savedInstanceState)

    setTitle(getResources.getString(R.string.title_activity_profile_settings))


    getSupportActionBar.setDisplayHomeAsUpEnabled(true)
    ThemeManager.applyTheme(this, getSupportActionBar)

    addPreferencesFromResource(R.xml.pref_profile)

    avatarDialog = new AvatarDialog(ProfileSettingsActivity.this)

    if (savedInstanceState != null) {
      if (savedInstanceState.getBoolean("showing_avatar_dialog", false)) avatarDialog.show()
    }

    bindPreferenceSummaryToValue(findPreference("nickname"))
    bindPreferenceSummaryToValue(findPreference("status"))
    bindPreferenceSummaryToValue(findPreference("status_message"))
    bindPreferenceSummaryToValue(findPreference("tox_id"))
    bindPreferenceSummaryToValue(findPreference("active_account"))


    val passwordPreference = findPreference("password")
    passwordPreference.setOnPreferenceClickListener(new OnPreferenceClickListener {
      override def onPreferenceClick(preference: Preference): Boolean = {
        createPasswordDialog()
        true
      }
    })
    bindPreferenceIfExists(passwordPreference)

    val toxMePreference = findPreference("toxme_info")
    toxMePreference.setOnPreferenceClickListener(new OnPreferenceClickListener {
      override def onPreferenceClick(preference: Preference): Boolean = {
        createToxMeAddressDialog()
        true
      }
    })
    bindPreferenceIfExists(toxMePreference)

    val toxIDPreference = findPreference("tox_id")
    toxIDPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {

      override def onPreferenceClick(preference: Preference): Boolean = {
        createToxIDDialog()
        true
      }
    })

    val avatarPreference = findPreference("avatar")
    avatarPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
      override def onPreferenceClick(preference: Preference): Boolean = {
        avatarDialog.show()
        true
      }
    })

    val exportProfile = findPreference("export")
    val thisActivity = this
    exportProfile.setOnPreferenceClickListener(new OnPreferenceClickListener {
      override def onPreferenceClick(preference: Preference): Boolean = {

        val path = Environment.getExternalStorageDirectory

        val properties: DialogProperties = new DialogProperties()
        properties.selection_mode = DialogConfigs.SINGLE_MODE
        properties.selection_type = DialogConfigs.DIR_SELECT
        properties.root = path
        properties.error_dir = path
        properties.extensions = null
        val dialog: FilePickerDialog = new FilePickerDialog(thisActivity, properties)
        dialog.setTitle(R.string.select_file)

        dialog.setDialogSelectionListener(new DialogSelectionListener() {
          override def onSelectedFilePaths(files: Array[String]) = {
            // files is the array of the paths of files selected by the Application User.
            // since we only want single file selection, use the first entry
            if (files != null) {
              if (files.length > 0) {
                if (files(0) != null) {
                  if (files(0).length > 0) {
                    val directory: File = new File(files(0))
                    onExportDataFileSelected(directory)
                  }
                }
              }
              else {
                onExportDataFileSelected(path)
              }
            }
          }
        })

        dialog.show()
        true
      }
    })

    val deleteAccount = findPreference("delete")
    deleteAccount.setOnPreferenceClickListener(new OnPreferenceClickListener {
      override def onPreferenceClick(preference: Preference): Boolean = {
        val builder = new Builder(ProfileSettingsActivity.this)
        builder.setMessage(R.string.delete_account_dialog_message)

        builder.setTitle(R.string.delete_account_dialog_title)

        builder.setPositiveButton(R.string.delete_account_dialog_confirm, new OnClickListener {
          override def onClick(dialog: DialogInterface, which: Int): Unit = {
            State.deleteActiveAccount(thisActivity)
          }
        })

        builder.setNegativeButton(getString(R.string.delete_account_dialog_cancel), null)

        builder.show()

        true
      }

    })

    val nospamPreference = findPreference("nospam")
    nospamPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
      override def onPreferenceClick(preference: Preference): Boolean = {
        val builder = new Builder(ProfileSettingsActivity.this)
        builder.setMessage(R.string.reset_tox_id_dialog_message)
          .setTitle(R.string.reset_tox_id_dialog_title)

        builder.setPositiveButton(getString(R.string.reset_tox_id_dialog_confirm), new OnClickListener {
          override def onClick(dialog: DialogInterface, which: Int): Unit = {
            try {
              val random = new Random()
              val maxNospam = 1234567890
              val nospam = random.nextInt(maxNospam)
              ToxSingleton.tox.setNospam(nospam)
              val preferences = PreferenceManager.getDefaultSharedPreferences(ProfileSettingsActivity.this)
              val editor = preferences.edit()
              editor.putString("tox_id", ToxSingleton.tox.getAddress.toString)
              editor.apply()

              // Display toast to inform user of successful change
              Toast.makeText(
                getApplicationContext,
                getApplicationContext.getResources.getString(R.string.tox_id_reset),
                Toast.LENGTH_SHORT
              ).show()

            } catch {
              case e: ToxException[_] => e.printStackTrace()
            }
          }
        })

        builder.setNegativeButton(getString(R.string.button_cancel), null)

        builder.show()

        true
      }
    })

  }

  def bindPreferenceIfExists(preference: Preference): AnyVal = {
    if (PreferenceManager.getDefaultSharedPreferences(preference.getContext)
      .getString(preference.getKey, "").isEmpty) {
      getPreferenceScreen.removePreference(preference)
    } else {
      bindPreferenceSummaryToValue(preference)
    }
  }

  def createToxIDDialog() {
    val builder = new AlertDialog.Builder(ProfileSettingsActivity.this)
    val inflater = ProfileSettingsActivity.this.getLayoutInflater
    val view = inflater.inflate(R.layout.dialog_tox_id, null)
    builder.setView(view)
    builder.setPositiveButton(getString(R.string.button_ok), null)
    builder.setNeutralButton(getString(R.string.dialog_tox_id), new DialogInterface.OnClickListener() {

      def onClick(dialogInterface: DialogInterface, ID: Int) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ProfileSettingsActivity.this)
        val clipboard = ProfileSettingsActivity.this.getSystemService(Context.CLIPBOARD_SERVICE).asInstanceOf[android.text.ClipboardManager]
        clipboard.setText(sharedPreferences.getString("tox_id", ""))
      }
    })
    val dir = new File(Environment.getExternalStorageDirectory.getPath + "/Antox/")
    if (!dir.exists) {
      dir.mkdirs()
    }
    val noMedia = new File(Environment.getExternalStorageDirectory.getPath + "/Antox/", ".nomedia")
    if (!noMedia.exists()) {
      try {
        noMedia.createNewFile()
      } catch {
        case e: IOException => e.printStackTrace()
      }
    }

    val file = new File(Environment.getExternalStorageDirectory.getPath + "/Antox/userkey_qr.png")
    val pref = PreferenceManager.getDefaultSharedPreferences(ProfileSettingsActivity.this.getApplicationContext)
    generateQR(pref.getString("tox_id", ""))
    val bmp = BitmapFactory.decodeFile(file.getAbsolutePath)
    val qrCode = view.findViewById(R.id.qr_image).asInstanceOf[ImageButton]
    qrCode.setImageBitmap(bmp)
    qrCode.setOnClickListener(new View.OnClickListener() {

      override def onClick(v: View) {
        val shareIntent = new Intent()
        shareIntent.setAction(Intent.ACTION_SEND)
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(Environment.getExternalStorageDirectory.getPath + "/Antox/userkey_qr.png")))
        shareIntent.setType("image/jpeg")
        view.getContext.startActivity(Intent.createChooser(shareIntent, getResources.getString(R.string.share_with)))
      }
    })
    val dialog = builder.create()
    dialog.show()
  }

  def createCopyToClipboardDialog(prefKey: String, dialogPositiveString: String, dialogNeutralString: String): Unit = {
    val builder = new AlertDialog.Builder(ProfileSettingsActivity.this)
    val pref = PreferenceManager.getDefaultSharedPreferences(ProfileSettingsActivity.this.getApplicationContext)
    builder.setTitle(pref.getString(prefKey, ""))
    builder.setPositiveButton(dialogPositiveString, null)
    builder.setNeutralButton(dialogNeutralString,
      new DialogInterface.OnClickListener() {
        def onClick(dialogInterface: DialogInterface, ID: Int) {
          val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ProfileSettingsActivity.this)
          val clipboard = ProfileSettingsActivity.this.getSystemService(Context.CLIPBOARD_SERVICE).asInstanceOf[android.text.ClipboardManager]
          clipboard.setText(sharedPreferences.getString(prefKey, ""))
        }
      })
    builder.create().show()
  }

  def createToxMeAddressDialog(): Unit = {
    createCopyToClipboardDialog("toxme_info", getString(R.string.button_ok), getString(R.string.dialog_toxme))
  }

  def createPasswordDialog(): Unit = {
    createCopyToClipboardDialog("password", getString(R.string.button_ok), getString(R.string.dialog_password))
  }

  def onExportDataFileSelected(dest: File): Unit = {
    try {
      ToxSingleton.exportDataFile(dest)
      Toast.makeText(getApplicationContext, getResources.getString(R.string.export_success, dest.getPath), Toast.LENGTH_LONG)
        .show()
    } catch {
      case e: Exception =>
        e.printStackTrace()
        Toast.makeText(getApplicationContext, R.string.export_failed, Toast.LENGTH_LONG).show()
    }
  }

  private def generateQR(userKey: String) {
    val qrData = "tox:" + userKey
    val qrCodeSize = 400
    val qrCodeEncoder = new QRCodeEncode(qrData, null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString,
      qrCodeSize)
    var out: FileOutputStream = null
    try {
      val bitmap = qrCodeEncoder.encodeAsBitmap()
      out = new FileOutputStream(Environment.getExternalStorageDirectory.getPath + "/Antox/userkey_qr.png")
      bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
      out.close()
    } catch {
      case e: WriterException => e.printStackTrace()
      case e: FileNotFoundException => e.printStackTrace()
      case e: IOException => e.printStackTrace()
    }
  }

  override def onResume(): Unit = {
    super.onResume()
    getPreferenceScreen.getSharedPreferences.registerOnSharedPreferenceChangeListener(this)
  }

  override def onPause(): Unit = {
    super.onPause()
    getPreferenceScreen.getSharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
  }

  override def onStop(): Unit = {
    super.onStop()
    avatarDialog.close()
  }

  def onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
    val userDb = State.userDb(this)

    key match {
      case "nickname" =>
        val name = sharedPreferences.getString(key, "")
        try {
          ToxSingleton.tox.setName(ToxNickname.unsafeFromValue(name.getBytes))
        } catch {
          case e: ToxException[_] => e.printStackTrace()
        }
        userDb.updateActiveUserDetail(key, name)

      case "password" =>
        val password = sharedPreferences.getString(key, "")
        userDb.updateActiveUserDetail(key, password)

      case "status" =>
        val newStatusString = sharedPreferences.getString(key, "")
        val newStatus = UserStatus.getToxUserStatusFromString(newStatusString)
        try {
          ToxSingleton.tox.setStatus(newStatus)
        } catch {
          case e: ToxException[_] => e.printStackTrace()
        }
        userDb.updateActiveUserDetail(key, newStatusString)

      case "status_message" =>
        val statusMessage = sharedPreferences.getString(key, "")
        try {
          ToxSingleton.tox.setStatusMessage(ToxStatusMessage.unsafeFromValue(sharedPreferences.getString(statusMessage, "").getBytes))
        } catch {
          case e: ToxException[_] => e.printStackTrace()
        }
        userDb.updateActiveUserDetail(key, statusMessage)

      case "logging_enabled" =>
        val loggingEnabled = sharedPreferences.getBoolean(key, true)
        userDb.updateActiveUserDetail(key, loggingEnabled)

      case "avatar" =>
        val avatar = sharedPreferences.getString(key, "")
        userDb.updateActiveUserDetail(key, avatar)

      case _ =>
    }

  }

  override def onActivityResult(requestCode: Int, resultCode: Int, data: Intent): Unit = {
    avatarDialog.onActivityResult(requestCode, resultCode, data)
    avatarDialog.close()
    avatarDialog.show()
  }

  override def onSaveInstanceState(savedInstanceState: Bundle): Unit = {
    super.onSaveInstanceState(savedInstanceState)

    // this is needed to keep the avatar dialog open on rotation
    // the hack is required because PreferenceActivity doesn't allow for dialog fragments
    savedInstanceState.putBoolean("showing_avatar_dialog", avatarDialog.isShowing)
  }

  override def onOptionsItemSelected(item: MenuItem): Boolean = item.getItemId match {
    case android.R.id.home =>
      finish()
      true

  }
}

