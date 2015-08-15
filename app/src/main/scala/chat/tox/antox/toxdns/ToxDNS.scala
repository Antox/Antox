package chat.tox.antox.toxdns

import java.io.{IOException, UnsupportedEncodingException}
import java.util.Scanner
import java.util.concurrent.ThreadPoolExecutor

import android.util.{Base64, Log}
import chat.tox.antox.toxdns.ToxDNS.RegError
import chat.tox.antox.toxdns.ToxDNS.RegError.RegError
import chat.tox.antox.toxdns.ToxDNS.RegError.RegError
import com.squareup.okhttp.Request.Builder
import com.squareup.okhttp.{MediaType, RequestBody, Request, OkHttpClient}
import org.abstractj.kalium.crypto.Box
import org.abstractj.kalium.encoders.Raw
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.DefaultHttpClient
import org.json.{JSONObject, JSONException}
import org.xbill.DNS.{Lookup, TXTRecord, Type}
import rx.Scheduler
import rx.lang.scala.{Subscriber, Observable}
import rx.lang.scala.schedulers.{IOScheduler}

import scala.util.Try
import language.higherKinds

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

  type RegistrationResult[Success] = Either[RegError, Success]

  /**
   * Registers a new account on toxme.io with name 'accountName' using the information
   * (toxID, etc) contained in data file 'toxData'.
   *
   * @return toxme request observable
   */
  def registerAccount(accountName: String, toxData: ToxData): Observable[RegistrationResult[String]] = {
    constructRegistrationRequestJson(accountName, toxData).flatMap(result =>
      result match {
        case Left(error: RegError) => Observable.just(Left(error))
        case Right(result: JSONObject) => postRegistrationJson(result)
      }
    ).onErrorReturn(_ => Left(RegError.UNKNOWN))
      .subscribeOn(IOScheduler())
      .observeOn(IOScheduler())
  }

  final case class EncryptedPayload(payload: String, nonce: String)

  private def encryptPayload(unencryptedPayload: JSONObject, toxData: ToxData): EncryptedPayload = {
    val hexEncoder = new org.abstractj.kalium.encoders.Hex
    val rawEncoder = new Raw
    val toxmePK = "1A39E7A5D5FA9CF155C751570A32E625698A60A55F6D88028F949F66144F4F25"
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
    EncryptedPayload(payload, nonceString)
  }

  private def constructRegistrationRequestJson(accountName: String, toxData: ToxData): Observable[RegistrationResult[JSONObject]] = {
    Observable(subscriber => {
      try {
        System.load("libkaliumjni.so")
      } catch {
        case e: UnsatisfiedLinkError =>
          subscriber.onNext(Left(RegError.KALIUM_LINK_ERROR))
      }

      try {
        val privacy = 0

        val unencryptedPayload = new JSONObject
        unencryptedPayload.put("tox_id", toxData.ID)
        unencryptedPayload.put("name", accountName)
        unencryptedPayload.put("privacy", privacy)
        unencryptedPayload.put("bio", "")
        val epoch = System.currentTimeMillis() / 1000
        unencryptedPayload.put("timestamp", epoch)

        val encryptedPayload = encryptPayload(unencryptedPayload, toxData)

        val requestJson = new JSONObject
        requestJson.put("action", 1)
        requestJson.put("public_key", toxData.ID.substring(0, 64))
        requestJson.put("encrypted", encryptedPayload.payload)
        requestJson.put("nonce", encryptedPayload.nonce)
        subscriber.onNext(Right(requestJson))
      } catch {
        case e: JSONException =>
          Log.d("CreateAccount", "JSON Exception " + e.getMessage)
          subscriber.onError(e)
      }
      subscriber.onCompleted()
    })
  }

  private def postRegistrationJson(requestJson: JSONObject): Observable[RegistrationResult[String]] = {
    Observable(subscriber => {
        val httpClient = new OkHttpClient()
        try {
          val mediaType = MediaType.parse("application/json; charset=utf-8")
          val requestBody = RequestBody.create(mediaType, requestJson.toString)
          val request = new Builder().url("https://toxme.io/api").post(requestBody).build()
          val response = httpClient.newCall(request).execute()

          Log.d("CreateAccount", "Response code: " + response.toString)
          val json = new JSONObject(response.body().string())
          val errorCode = json.getString("c")
          val password = json.getString("password")

          val error = Try(RegError.withName(errorCode)).getOrElse(RegError.UNKNOWN)
          if (error == RegError.SUCCESS) {
            subscriber.onNext(Right(password))
          } else {
            subscriber.onNext(Left(error))
          }
        } catch {
          case e: UnsupportedEncodingException =>
            Log.d("CreateAccount", "Unsupported Encoding Exception: " + e.getMessage)
            subscriber.onError(e)
          case e: Exception =>
            Log.d("CreateAccount", "IOException: " + e.getMessage)
            subscriber.onError(e)
        }

      subscriber.onCompleted()
    })
  }
}