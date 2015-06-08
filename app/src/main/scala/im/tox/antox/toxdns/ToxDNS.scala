package im.tox.antox.toxdns

import java.io.{IOException, UnsupportedEncodingException}
import java.util.Scanner

import android.util.{Base64, Log}
import im.tox.antox.toxdns.ToxDNS.RegError.RegError
import org.abstractj.kalium.crypto.Box
import org.abstractj.kalium.encoders.Raw
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.DefaultHttpClient
import org.json.{JSONException, JSONObject}
import org.xbill.DNS.{Lookup, TXTRecord, Type}
import rx.lang.scala.Observable

import scala.util.Try

object ToxDNS {

 /**
  * Performs a DNS lookup and returns the Tox ID registered to the given DNSName.
  *
  * @param DNSName the dns name to lookup
  * @return the ID or None if the name is not found
  */
 def lookup(DNSName: String): Observable[Option[String]] = {
    Observable(subscriber => {
      var user: String = null
      var domain: String = null
      var lookup: String = null

      if (DNSName.contains("@")) {
        user = DNSName.substring(0, DNSName.indexOf("@"))
        domain = DNSName.substring(DNSName.indexOf("@") + 1)

        lookup = user + "._tox." + domain
        var txt: TXTRecord = null
        try {
          val records = new Lookup(lookup, Type.TXT).run()
          txt = records(0).asInstanceOf[TXTRecord]
          val txtString = txt.toString.substring(txt.toString.indexOf('"'))
          if (txtString.contains("tox1")) {
            val key = txtString.substring(11, 11 + 76)
            subscriber.onNext(Some(key))
          }
        } catch {
          case e: Exception => subscriber.onNext(None)
        }
      } else {
        subscriber.onNext(None)
      }
      subscriber.onCompleted()
    })
  }

  //ToxDNS Registration Error
  object RegError extends Enumeration {
    type RegError = Value
    val SUCCESS = Value("0")
    val REGISTRATION_LIMIT_REACHED = Value("-4")
    val NAME_TAKEN = Value("-25")
    val INTERNAL = Value("-26")
    val UNKNOWN = Value("")
    val KALIUM_LINK_ERROR = Value("KALIUM")

    def valueOf(name: String) = values.find(_.toString == name).getOrElse(UNKNOWN)
  }

  /**
   *  Registers a new account on toxme.se with name 'accountName' using the information
   *  (toxID, etc) contained in data file 'toxData'.
   *
   *  @return tuple: (toxme password, errorCode)
   */
  def registerAccount(accountName: String, toxData: ToxData): Either[RegError, String] = {
    try {
      System.load("libkaliumjni.so")

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
        val hexEncoder = new org.abstractj.kalium.encoders.Hex
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

      if (Try(RegError.withName(jsonPost.getErrorCode)).getOrElse(RegError.UNKNOWN) == RegError.SUCCESS) {
        Right(jsonPost.getPassword)
      } else {
        Left(RegError.valueOf(jsonPost.getErrorCode))
      }
    } catch {
      case e: UnsatisfiedLinkError => Left(RegError.KALIUM_LINK_ERROR)
    }
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