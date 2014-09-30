
package im.tox.antox.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.ActionBarActivity
import android.util.Base64
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import org.abstractj.kalium.crypto.Box
import org.abstractj.kalium.encoders.Hex
import org.abstractj.kalium.encoders.Raw
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.DefaultHttpClient
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.util.Scanner
import java.util.regex.Matcher
import java.util.regex.Pattern
import im.tox.antox.R
import im.tox.antox.data.UserDB
import im.tox.antox.tox.ToxDataFile
import im.tox.antox.tox.ToxDoService
import im.tox.antox.utils.AntoxFriendList
import im.tox.antox.utils.Options
import im.tox.jtoxcore.JTox
import im.tox.jtoxcore.ToxException
import im.tox.jtoxcore.ToxOptions
import im.tox.jtoxcore.callbacks.CallbackHandler
//remove if not needed
import scala.collection.JavaConversions._

class CreateAcccountActivity extends ActionBarActivity {

  protected override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    getSupportActionBar.hide()
    setContentView(R.layout.activity_create_acccount)
    if (Build.VERSION.SDK_INT != Build.VERSION_CODES.JELLY_BEAN && 
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      getWindow.setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
    }
  }

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    getMenuInflater.inflate(R.menu.create_acccount, menu)
    true
  }

  override def onOptionsItemSelected(item: MenuItem): Boolean = {
    val id = item.getItemId
    if (id == R.id.action_settings) {
      return true
    }
    super.onOptionsItemSelected(item)
  }

  def onClickRegisterAccount(view: View) {
    val accountField = findViewById(R.id.create_account_name).asInstanceOf[EditText]
    val account = accountField.getText.toString
    val pattern = Pattern.compile("\\s")
    val pattern2 = Pattern.compile(File.separator)
    var matcher = pattern.matcher(account)
    val containsSpaces = matcher.find()
    matcher = pattern2.matcher(account)
    val containsFileSeperator = matcher.find()
    if (account == "") {
      val context = getApplicationContext
      val text = getString(R.string.create_must_fill_in)
      val duration = Toast.LENGTH_SHORT
      val toast = Toast.makeText(context, text, duration)
      toast.show()
    } else if (containsSpaces || containsFileSeperator) {
      val context = getApplicationContext
      val text = getString(R.string.create_bad_profile_name)
      val duration = Toast.LENGTH_SHORT
      val toast = Toast.makeText(context, text, duration)
      toast.show()
    } else {
      val db = new UserDB(this)
      db.addUser(account, "")
      db.close()
      var ID = ""
      var fileBytes: Array[Byte] = null
      try {
        val antoxFriendList = new AntoxFriendList()
        val callbackHandler = new CallbackHandler(antoxFriendList)
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val udpEnabled = preferences.getBoolean("enable_udp", false)
        val toxOptions = new ToxOptions(Options.ipv6Enabled, udpEnabled, Options.proxyEnabled)
        val jTox = new JTox(antoxFriendList, callbackHandler, toxOptions)
        val toxDataFile = new ToxDataFile(this, account)
        toxDataFile.saveFile(jTox.save())
        ID = jTox.getAddress
        fileBytes = toxDataFile.loadFile()
      } catch {
        case e: ToxException => Log.d("CreateAccount", e.getMessage)
      }
      val skipRegistration = findViewById(R.id.skip_button).asInstanceOf[CheckBox]
      if (!skipRegistration.isChecked) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = preferences.edit()
        editor.putString("active_account", account)
        editor.putString("nickname", account)
        editor.putString("status", "online")
        editor.putString("status_message", getResources.getString(R.string.pref_default_status_message))
        editor.putString("tox_id", ID)
        editor.putBoolean("loggedin", true)
        editor.apply()
        val startTox = new Intent(getApplicationContext, classOf[ToxDoService])
        getApplicationContext.startService(startTox)
        val main = new Intent(getApplicationContext, classOf[MainActivity])
        startActivity(main)
        setResult(Activity.RESULT_OK)
        finish()
      } else {
        try {
          System.load("/data/data/im.tox.antox/lib/libkaliumjni.so")
        } catch {
          case e: Exception => Log.d("CreateAccount", "System.load() on kalium failed")
        }
        val allow = 0
        val jsonPost = new JSONPost()
        val toxmeThread = new Thread(jsonPost)
        try {
          val unencryptedPayload = new JSONObject()
          unencryptedPayload.put("tox_id", ID)
          unencryptedPayload.put("name", account)
          unencryptedPayload.put("privacy", allow)
          unencryptedPayload.put("bio", "")
          val epoch = System.currentTimeMillis() / 1000
          unencryptedPayload.put("timestamp", epoch)
          val hexEncoder = new Hex()
          val rawEncoder = new Raw()
          val toxmepk = "5D72C517DF6AEC54F1E977A6B6F25914EA4CF7277A85027CD9F5196DF17E0B13"
          val serverPublicKey = hexEncoder.decode(toxmepk)
          val ourSecretKey = Array.ofDim[Byte](32)
          System.arraycopy(fileBytes, 52, ourSecretKey, 0, 32)
          val box = new Box(serverPublicKey, ourSecretKey)
          val random = new org.abstractj.kalium.crypto.Random()
          var nonce = random.randomBytes(24)
          var payloadBytes = box.encrypt(nonce, rawEncoder.decode(unencryptedPayload.toString))
          payloadBytes = Base64.encode(payloadBytes, Base64.NO_WRAP)
          nonce = Base64.encode(nonce, Base64.NO_WRAP)
          val payload = rawEncoder.encode(payloadBytes)
          val nonceString = rawEncoder.encode(nonce)
          val json = new JSONObject()
          json.put("action", 1)
          json.put("public_key", ID.substring(0, 64))
          json.put("encrypted", payload)
          json.put("nonce", nonceString)
          jsonPost.setJSON(json.toString)
          toxmeThread.start()
          toxmeThread.join()
        } catch {
          case e: JSONException => Log.d("CreateAcccount", "JSON Exception " + e.getMessage)
          case e: InterruptedException => 
        }
        var toastMessage = ""
        val context = getApplicationContext
        val duration = Toast.LENGTH_SHORT
        var toast: Toast = null
        val errorCode = jsonPost.getErrorCode
        if ("0" == errorCode) {
          val preferences = PreferenceManager.getDefaultSharedPreferences(this)
          val editor = preferences.edit()
          editor.putString("active_account", account)
          editor.putString("nickname", account)
          editor.putString("status", "1")
          editor.putString("status_message", getResources.getString(R.string.pref_default_status_message))
          editor.putString("tox_id", ID)
          editor.putBoolean("loggedin", true)
          editor.apply()
          val startTox = new Intent(getApplicationContext, classOf[ToxDoService])
          getApplicationContext.startService(startTox)
          val main = new Intent(getApplicationContext, classOf[MainActivity])
          startActivity(main)
          setResult(Activity.RESULT_OK)
          finish()
        } else if ("-25" == errorCode) {
          toastMessage = "This name is already taken"
          toast = Toast.makeText(context, toastMessage, duration)
          toast.show()
        } else if ("-26" == errorCode) {
          toastMessage = "Internal Antox Error. Please restart and try again"
          toast = Toast.makeText(context, toastMessage, duration)
          toast.show()
        } else if ("-4" == errorCode) {
          toastMessage = "You can only register 13 accounts an hour. You have reached this limit"
          toast = Toast.makeText(context, toastMessage, duration)
          toast.show()
        }
      }
    }
  }

  private class JSONPost extends Runnable {

    @volatile private var errorCode: String = "notdone"

    private var finaljson: String = _

    def run() {
      val httpClient = new DefaultHttpClient()
      try {
        val post = new HttpPost("https://toxme.se/api")
        post.setHeader("Content-Type", "application/json")
        post.setEntity(new StringEntity(finaljson.toString))
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
        finaljson = json
      }
    }
  }
}
