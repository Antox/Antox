package im.tox.antox.activities

import java.io.{File, IOException, UnsupportedEncodingException}
import java.util.Scanner
import java.util.regex.Pattern

import android.app.Activity
import android.content.Intent
import android.os.{Build, Bundle}
import android.preference.PreferenceManager
import android.support.v7.app.ActionBarActivity
import android.util.{Base64, Log}
import android.view.{Menu, MenuItem, View, WindowManager}
import android.widget.{EditText, Toast}
import im.tox.antox.R
import im.tox.antox.data.UserDB
import im.tox.antox.tox.{ToxDataFile, ToxDoService}
import im.tox.antox.utils.{AntoxFriendList, Options}
import im.tox.jtoxcore.{JTox, ToxException, ToxOptions}
import im.tox.jtoxcore.callbacks.CallbackHandler
import org.abstractj.kalium.crypto.Box
import org.abstractj.kalium.encoders.{Hex, Raw}
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.DefaultHttpClient
import org.json.{JSONException, JSONObject}

import scala.beans.BeanProperty

//remove if not needed

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

  def saveAccountAndStartMain(accountName: String, toxID: String) {
    // Save preferences
    val preferences = PreferenceManager.getDefaultSharedPreferences(this)
    val editor = preferences.edit()
    editor.putString("active_account", accountName)
    editor.putString("nickname", accountName)
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

    val antoxFriendList = new AntoxFriendList()
    val callbackHandler = new CallbackHandler(antoxFriendList)
    val toxOptions = new ToxOptions(Options.ipv6Enabled, Options.udpEnabled, Options.proxyEnabled)
    val jTox = new JTox(antoxFriendList, callbackHandler, toxOptions)
    val toxDataFile = new ToxDataFile(this, accountName)
    toxDataFile.saveFile(jTox.save())
    toxData.ID = jTox.getAddress
    toxData.fileBytes = toxDataFile.loadFile()
    toxData
  }

  def onClickRegisterIncogAccount(view: View) {
    val accountField = findViewById(R.id.create_account_name).asInstanceOf[EditText]
    val account = accountField.getText.toString

    val db = new UserDB(this)

    if (!validAccountName(account)) {
      showBadAccountNameError()
    } else if (db.login(account)) {
      /* Check if user exists. This login() function simply checks if the username is in the database */
      val context = getApplicationContext
      val text = getString(R.string.create_profile_exists)
      val duration = Toast.LENGTH_LONG
      val toast = Toast.makeText(context, text, duration)
      toast.show()
    } else {
      // Add user to db
      val db = new UserDB(this)
      db.addUser(account, "")
      db.close()

      try {
        val toxData = createToxData(account)

        saveAccountAndStartMain(account, toxData.ID)
      } catch {
        case e: ToxException => Log.d("CreateAccount", "Failed creating tox data save file")
      }
    }
  }

  def onClickRegisterAccount(view: View) {
    val accountField = findViewById(R.id.create_account_name).asInstanceOf[EditText]
    val account = accountField.getText.toString

    if (!validAccountName(account)) {
      showBadAccountNameError()
    } else {
      // Add user to db
      val db = new UserDB(this)
      db.addUser(account, "")
      db.close()

      // Create tox data save file
      var toxData = new ToxData

      try {
        toxData = createToxData(account)
      } catch {
        case e: ToxException => Log.d("CreateAccount", "Failed creating tox data save file")
      }

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
        unencryptedPayload.put("name", account)
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
          saveAccountAndStartMain(account, toxData.ID)
          null
        case "-25" => "This name is already taken"
        case "-26" => "Internal Antox Error. Please restart and try again"
        case "-4" => "You can only register 13 accounts an hour. You have reached this limit"
      }

      if (toastMessage != null) {
        val context = getApplicationContext
        val duration = Toast.LENGTH_SHORT
        Toast.makeText(context, toastMessage, duration).show()
      }
    }
  }

  private class JSONPost extends Runnable {

    @volatile private var errorCode: String = "notdone"

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
        }
        in.close()
      } catch {
        case e: UnsupportedEncodingException => Log.d("CreateAccount", "Unsupported Encoding Exception: " + e.getMessage)
        case e: IOException => Log.d("CreateAccount", "IOException: " + e.getMessage)
      } finally {
        httpClient.getConnectionManager.shutdown()
      }
    }

    def getErrorCode(): String = synchronized {
      errorCode
    }

    def setJSON(json: String) {
      synchronized {
        finalJson = json
      }
    }
  }
}
