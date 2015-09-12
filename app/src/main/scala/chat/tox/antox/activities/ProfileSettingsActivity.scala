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
import android.support.v4.content.IntentCompat
import android.support.v7.app.AlertDialog
import android.view.{MenuItem, View}
import android.widget.{ImageButton, Toast}
import chat.tox.QR.{Contents, QRCodeEncode}
import chat.tox.antox.R
import chat.tox.antox.activities.ProfileSettingsActivity._
import chat.tox.antox.data.State
import chat.tox.antox.fragments.AvatarDialog
import chat.tox.antox.theme.ThemeManager
import chat.tox.antox.tox.{ToxDataFile, ToxService, ToxSingleton}
import chat.tox.antox.toxdns.{ToxDNS, ToxData}
import chat.tox.antox.transfer.FileDialog
import chat.tox.antox.transfer.FileDialog.DirectorySelectedListener
import chat.tox.antox.wrapper.UserStatus
import com.google.zxing.{BarcodeFormat, WriterException}
import im.tox.tox4j.exceptions.ToxException

object ProfileSettingsActivity {

  private val sBindPreferenceSummaryToValueListener: Preference.OnPreferenceChangeListener = new Preference.OnPreferenceChangeListener() {

    override def onPreferenceChange(preference: Preference, value: AnyRef): Boolean = {
      val stringValue = value.toString
      if(preference.getKey.equals("dns_info")) {
        preference.setEnabled(true)
      }
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

    getSupportActionBar.setDisplayHomeAsUpEnabled(true)
    ThemeManager.applyTheme(this, getSupportActionBar)

    addPreferencesFromResource(R.xml.pref_profile)

    avatarDialog = new AvatarDialog(ProfileSettingsActivity.this)

    if (savedInstanceState != null) {
      if (savedInstanceState.getBoolean("showing_avatar_dialog", false)) avatarDialog.show()
    }

    bindPreferenceSummaryToValue(findPreference("nickname"))
    val passwordPreference = findPreference("password")
    if (PreferenceManager.getDefaultSharedPreferences(passwordPreference.getContext)
      .getString(passwordPreference.getKey, "").isEmpty) {
      getPreferenceScreen.removePreference(passwordPreference)
    } else {
      bindPreferenceSummaryToValue(passwordPreference)
    }
    bindPreferenceSummaryToValue(findPreference("status"))
    bindPreferenceSummaryToValue(findPreference("status_message"))
    bindPreferenceSummaryToValue(findPreference("tox_id"))
    bindPreferenceSummaryToValue(findPreference("active_account"))
    bindPreferenceSummaryToValue(findPreference("dns_info"))
    val toxIDPreference = findPreference("tox_id")
    toxIDPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

      override def onPreferenceClick(preference: Preference): Boolean = {
        createToxIDDialog()
        true
      }
    })

    val avatarPreference = findPreference("avatar")
    avatarPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      override def onPreferenceClick(preference: Preference): Boolean = {
        avatarDialog.show()
        true
      }
    })

    val exportProfile = findPreference("export")
    val thisActivity = this
    exportProfile.setOnPreferenceClickListener(new OnPreferenceClickListener {
      override def onPreferenceClick(preference: Preference): Boolean = {
        val fileDialog = new FileDialog(thisActivity, Environment.getExternalStorageDirectory, true)
        fileDialog.addDirectoryListener(new DirectorySelectedListener {
          override def directorySelected(directory: File): Unit = {
            onExportDataFileSelected(directory)
          }
        })
        fileDialog.showDialog()
        true
      }
    })

    val deleteAccount = findPreference("delete")
    deleteAccount.setOnPreferenceClickListener(new OnPreferenceClickListener {
      override def onPreferenceClick(preference: Preference): Boolean = {
        val builder = new AlertDialog.Builder(ProfileSettingsActivity.this)
        builder.setMessage(R.string.delete_account_dialog_message)

        builder.setTitle(R.string.delete_account_dialog_title)

        builder.setPositiveButton(R.string.delete_account_dialog_confirm, new OnClickListener {
          override def onClick(dialog: DialogInterface, which: Int): Unit = {

            val userDb = State.userDb(getApplicationContext)
            val userInfo = userDb.getActiveUserDetails
            val dataFile = new ToxDataFile(getApplicationContext, userInfo.profileName)
            val toxData = new ToxData
            toxData.fileBytes = dataFile.loadFile()
            toxData.address = ToxSingleton.tox.getAddress
            val dnsName = userInfo.dnsName
            if (dnsName.domain.isDefined) {
              val observable = ToxDNS.deleteAccount(dnsName, toxData)
              observable.subscribe()
            }
            userDb.deleteActiveUser()
            val startTox = new Intent(getApplicationContext, classOf[ToxService])
            stopService(startTox)
            val loginIntent = new Intent(ProfileSettingsActivity.this, classOf[LoginActivity])
            loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
              Intent.FLAG_ACTIVITY_CLEAR_TOP |
              IntentCompat.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(loginIntent)
            finish()

          }
        })

        builder.setNegativeButton(getString(R.string.delete_account_dialog_cancel), null)

        builder.show()

        true
      }

    })

    val nospamPreference = findPreference("nospam")
    nospamPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      override def onPreferenceClick(preference: Preference): Boolean = {
        val builder = new AlertDialog.Builder(ProfileSettingsActivity.this)
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

    val toxDnsPreference = findPreference("dns_info")
    toxDnsPreference.setEnabled(false)
    toxDnsPreference.setOnPreferenceClickListener(new OnPreferenceClickListener {
      override def onPreferenceClick(preference: Preference): Boolean = {

        true
      }
    })



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
    var file = new File(Environment.getExternalStorageDirectory.getPath + "/Antox/")
    if (!file.exists()) {
      file.mkdirs()
    }
    val noMedia = new File(Environment.getExternalStorageDirectory.getPath + "/Antox/", ".nomedia")
    if (!noMedia.exists()) {
      try {
        noMedia.createNewFile()
      } catch {
        case e: IOException => e.printStackTrace()
      }
    }

    file = new File(Environment.getExternalStorageDirectory.getPath + "/Antox/userkey_qr.png")
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
    builder.create().show()
  }

  def onExportDataFileSelected(dest: File): Unit = {
    try {
      ToxSingleton.exportDataFile(dest)
      Toast.makeText(getApplicationContext, "Exported data file to " + dest.getPath, Toast.LENGTH_LONG)
        .show()
    } catch {
      case e: Exception =>
        e.printStackTrace()
        Toast.makeText(getApplicationContext, "Error: Could not export data file.", Toast.LENGTH_LONG).show()
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
          println("Tox is " + ToxSingleton.tox)
          ToxSingleton.tox.setName(name)
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
          ToxSingleton.tox.setStatusMessage(sharedPreferences.getString(statusMessage, ""))
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

