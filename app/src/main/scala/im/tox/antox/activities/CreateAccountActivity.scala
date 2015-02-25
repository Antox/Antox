package im.tox.antox.activities

import java.io.{File, IOException, UnsupportedEncodingException}
import java.util.Scanner
import java.util.regex.Pattern

import android.app.Activity
import android.content.Intent
import android.os.{Environment, Build, Bundle}
import android.preference.PreferenceManager
import android.support.v7.app.ActionBarActivity
import android.util.{Base64, Log}
import android.view.{Menu, MenuItem, View, WindowManager}
import android.widget.{EditText, Toast}
import im.tox.antoxnightly.R
import im.tox.antox.data.UserDB
import im.tox.antox.tox.{ToxSingleton, ToxDataFile, ToxDoService}
import im.tox.antox.utils.{FileUtils, FileDialog, Options}
import im.tox.tox4j.ToxCoreImpl
import im.tox.tox4j.core.ToxOptions
import im.tox.tox4j.exceptions.ToxException
import org.abstractj.kalium.crypto.Box
import org.abstractj.kalium.encoders.{Hex, Raw}
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.DefaultHttpClient
import org.json.{JSONException, JSONObject}

import scala.beans.BeanProperty

class CreateAccountActivity extends ActionBarActivity {

  protected override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    getSupportActionBar.hide()
    setContentView(R.layout.activity_create_account)
    if (Build.VERSION.SDK_INT != Build.VERSION_CODES.JELLY_BEAN &&
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      getWindow.setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
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
    editor.putString("status", "1")
    editor.putString("status_message", getResources.getString(R.string.pref_default_status_message))
    editor.putString("tox_id", toxID)
    editor.putBoolean("loggedin", true)
    editor.apply()

    // Start the activity
    val startTox = new Intent(getApplicationContext, classOf[ToxDoService])
    getApplicationContext.startService(startTox)
    val main = new Intent(getApplicationContext, classOf[MainActivity])
    startActivity(main)
    setResult(Activity.RESULT_OK)

    finish()
  }

  class ToxData {
    @BeanProperty
    var fileBytes: Array[Byte] = null
    @BeanProperty
    var ID: String = _
  }

  def createToxData(accountName: String): ToxData = {
    val toxData = new ToxData
    val toxOptions = new ToxOptions()
    toxOptions.setIpv6Enabled(Options.ipv6Enabled)
    toxOptions.setUdpEnabled(Options.udpEnabled)
    val tox = new ToxCoreImpl(toxOptions)
    val toxDataFile = new ToxDataFile(this, accountName)
    toxDataFile.saveFile(tox.save())
    toxData.ID = im.tox.antox.utils.Hex.bytesToHexString(tox.getAddress)
    toxData.fileBytes = toxDataFile.loadFile()
    toxData
  }

  def loadToxData(fileName: String): ToxData = {
    val toxData = new ToxData
    val toxOptions = new ToxOptions()
    toxOptions.setIpv6Enabled(Options.ipv6Enabled)
    toxOptions.setUdpEnabled(Options.udpEnabled)
    val toxDataFile = new ToxDataFile(this, fileName)
    val tox = new ToxCoreImpl(toxOptions, toxDataFile.loadFile())
    toxData.ID = im.tox.antox.utils.Hex.bytesToHexString(tox.getAddress)
    toxData.fileBytes = toxDataFile.loadFile()
    toxData
  }

  //db is expected to be open and is closed at the end of the method
  def createAccount(accountName: String, db: UserDB, createDataFile: Boolean, shouldRegister: Boolean): Unit = {
    try {
    if (!validAccountName(accountName)) {
      showBadAccountNameError()
    } else if (db.doesUserExist(accountName)) {
      val context = getApplicationContext
      val text = getString(R.string.create_profile_exists)
      val duration = Toast.LENGTH_LONG
      val toast = Toast.makeText(context, text, duration)
      toast.show()
    } else {
      db.addUser(accountName, "")
      var toxData = new ToxData

      if (createDataFile) {
        // Create tox data save file
        try {
          toxData = createToxData(accountName)
        } catch {
          case e: ToxException => Log.d("CreateAccount", "Failed creating tox data save file")
        }
      } else {
        toxData = loadToxData(accountName)
      }

      if (shouldRegister) {
        // Register on toxme.se
        try {
          System.load("/data/data/im.tox.antox/lib/libkaliumjni.so")
        } catch {
          case e: Exception => Log.d("CreateAccount", "System.load() on kalium failed")
        }

        val allow = 0
        val jsonPost = new JSONPost
        val toxmeThread = new Thread(jsonPost)

        try {
          val unencryptedPayload = new JSONObject
          unencryptedPayload.put("tox_id", toxData.ID)
          unencryptedPayload.put("name", accountName)
          unencryptedPayload.put("privacy", allow)
          unencryptedPayload.put("bio", "")
          val epoch = System.currentTimeMillis() / 1000
          unencryptedPayload.put("timestamp", epoch)
          val hexEncoder = new Hex
          val rawEncoder = new Raw
          val toxmePK = "5D72C517DF6AEC54F1E977A6B6F25914EA4CF7277A85027CD9F5196DF17E0B13"
          val serverPublicKey = hexEncoder.decode(toxmePK)
          val ourSecretKey = Array.ofDim[Byte](32)
          System.arraycopy(toxData.fileBytes, 52, ourSecretKey, 0, 32)
          val box = new Box(serverPublicKey, ourSecretKey)
          val random = new org.abstractj.kalium.crypto.Random()
          var nonce = random.randomBytes(24)
          var payloadBytes = box.encrypt(nonce, rawEncoder.decode(unencryptedPayload.toString))
          payloadBytes = Base64.encode(payloadBytes, Base64.NO_WRAP)
          nonce = Base64.encode(nonce, Base64.NO_WRAP)
          val payload = rawEncoder.encode(payloadBytes)
          val nonceString = rawEncoder.encode(nonce)
          val json = new JSONObject
          json.put("action", 1)
          json.put("public_key", toxData.ID.substring(0, 64))
          json.put("encrypted", payload)
          json.put("nonce", nonceString)
          jsonPost.setJSON(json.toString)
          toxmeThread.start()
          toxmeThread.join()
        } catch {
          case e: JSONException => Log.d("CreateAccount", "JSON Exception " + e.getMessage)
          case e: InterruptedException =>
        }

        val toastMessage = jsonPost.getErrorCode match {
          case "0" =>
            db.updateUserDetail(accountName, "password", jsonPost.getPassword)
            saveAccountAndStartMain(accountName, jsonPost.getPassword, toxData.ID)
            null
          case "-25" => getString(R.string.create_account_exists)
          case "-26" => getString(R.string.create_account_internal_error)
          case "-4" => getString(R.string.create_account_reached_registration_limit)
          case _ => getString(R.string.create_account_unknown_error) + jsonPost.getErrorCode
        }

        if (toastMessage != null) {
          val context = getApplicationContext
          val duration = Toast.LENGTH_SHORT
          Toast.makeText(context, toastMessage, duration).show()
        }

      } else {
        saveAccountAndStartMain(accountName, "", toxData.ID)
      }
    }
    } finally {
      db.close()
    }
  }

  def onClickRegisterIncogAccount(view: View) {
    val accountField = findViewById(R.id.create_account_name).asInstanceOf[EditText]
    val account = accountField.getText.toString

    val db = new UserDB(this)
    createAccount(account, db, createDataFile = true, shouldRegister = false)
  }

  def onClickImportProfile(view: View): Unit = {
    val accountField = findViewById(R.id.create_account_name).asInstanceOf[EditText]

    val path = new File(Environment.getExternalStorageDirectory + "//DIR//")
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
      case Some(selectedFile) =>
        if (!selectedFile.getName.contains(".tox")) {
          val context = getApplicationContext
          val duration = Toast.LENGTH_SHORT
          Toast.makeText(context, getResources.getString(R.string.import_profile_invalid_file), duration).show()
          return
        }

        val accountName =
          if (accountFieldName.isEmpty) {
            selectedFile.getName.replace(".tox", "")
          } else {
            accountFieldName
          }

          val toxDataFile = new File(getFilesDir.getAbsolutePath + "/" + accountName)
          FileUtils.copy(selectedFile, toxDataFile)
          createAccount(accountName, new UserDB(this), createDataFile = false, shouldRegister = false)

      case None => throw new Exception("Could not load data file.")
    }
  }

  def onClickRegisterAccount(view: View) {
    val accountField = findViewById(R.id.create_account_name).asInstanceOf[EditText]
    val account = accountField.getText.toString

    val db = new UserDB(this)

    createAccount(account, db, createDataFile = true, shouldRegister = true)
  }

  private class JSONPost extends Runnable {

    @volatile private var errorCode: String = "notdone"
    @volatile private var password: String = ""
    
    private var finalJson: String = _

    def run() {
      val httpClient = new DefaultHttpClient()
      try {
        val post = new HttpPost("https://toxme.se/api")
        post.setHeader("Content-Type", "application/json")
        post.setEntity(new StringEntity(finalJson.toString))
        val response = httpClient.execute(post)
        Log.d("CreateAccount", "Response code: " + response.toString)
        val entity = response.getEntity
        val in = new Scanner(entity.getContent)
        while (in.hasNext) {
          val responseString = in.next()
          Log.d("CreateAccount", "Response: " + responseString)
          if (responseString.contains("\"c\":")) {
            errorCode = in.next()
            errorCode = errorCode.replaceAll("\"", "")
            errorCode = errorCode.replaceAll(",", "")
            errorCode = errorCode.replaceAll("\\}", "")
            Log.d("CreateAccount", "Error Code: " + errorCode)
          }

          if (responseString.contains("\"password\":")) {
            password = in.next()
            password = password.replaceAll("\"", "")
            password = password.replaceAll(",", "")
            password = password.replaceAll("\\}", "")
            Log.d("CreateAccount", "Password: " + password)
          }
        }
        in.close()
      } catch {
        case e: UnsupportedEncodingException => Log.d("CreateAccount", "Unsupported Encoding Exception: " + e.getMessage)
        case e: IOException => Log.d("CreateAccount", "IOException: " + e.getMessage)
      } finally {
        httpClient.getConnectionManager.shutdown()
      }
    }

    def getErrorCode: String = synchronized {
      errorCode
    }
    
    def getPassword: String = synchronized {
      password
    }

    def setJSON(json: String) {
      synchronized {
        finalJson = json
      }
    }
  }
}
