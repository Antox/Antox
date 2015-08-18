package chat.tox.antox.toxdns

import java.io.UnsupportedEncodingException

import android.util.{Base64, Log}
import chat.tox.antox.toxdns.ToxDNS.RegError.RegError
import com.squareup.okhttp.Request.Builder
import com.squareup.okhttp.{MediaType, OkHttpClient, RequestBody}
import org.abstractj.kalium.crypto.Box
import org.abstractj.kalium.encoders.Raw
import org.json.{JSONException, JSONObject}
import org.xbill.DNS.{Lookup, TXTRecord, Type}
import rx.lang.scala.Observable
import rx.lang.scala.schedulers.IOScheduler

import scala.language.higherKinds
import scala.util.Try

object ToxDNS {

 /**
  * Performs a DNS lookup and returns the Tox ID registered to the given dnsName.
  *
  * @param dnsName the dns name to lookup
  * @return the ID or None if the name is not found
  */
 def lookup(dnsName: String): Observable[Option[String]] = {
    Observable(subscriber => {
      if (dnsName.contains("@")) {
        val parsedDnsName = DnsName.fromString(dnsName)
        val lookup = parsedDnsName.user + "._tox." + parsedDnsName.domain.get
        try {
          val records = new Lookup(lookup, Type.TXT).run()
          val txt = records(0).asInstanceOf[TXTRecord]
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

  /**
   * Performs a DNS lookup for the given dnsDomain to retrieve
   * the service's public key to be used for encrypted toxdns3 requests.
   *
   * If the dns server does not exist or a network-related error occurs, None is returned.
   *
   * @param dnsDomain the domain on which to perform the lookup (e.g. toxme.io)
   * @return the public key of the dns service
   */
  def lookupPublicKey(dnsDomain: String): Option[String] = {
    try {
      val records = new Lookup("_tox." + dnsDomain, Type.TXT).run()
      val txt = records(0).asInstanceOf[TXTRecord]
      Some(txt.toString.substring(txt.toString.indexOf('"')).replace("\"", ""))
    } catch {
      case e: Exception =>
        None
    }
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
    val INVALID_DOMAIN = Value("INVALID_DOMAIN")

    def valueOf(name: String) = values.find(_.toString == name).getOrElse(UNKNOWN)
  }

  type RegistrationResult[Success] = Either[RegError, Success]

  /**
   * Registers a new account on toxme.io with name 'accountName' using the information
   * (toxID, etc) contained in data file 'toxData'.
   *
   * If the service cannot be contacted, the network is down, or some other error occurs,
   * the appropriate RegError is returned.
   *
   * @return toxme request observable that contains password on success, RegError on lookup error
   */
  def registerAccount(name: DnsName, toxData: ToxData): Observable[RegistrationResult[String]] = {
    constructRegistrationRequestJson(name, toxData).flatMap(result =>
      result match {
        case Left(error: RegError) => Observable.just(Left(error))
        case Right(result: JSONObject) => postRegistrationJson(result)
      }
    ).onErrorReturn(_ => Left(RegError.UNKNOWN))
      .subscribeOn(IOScheduler())
  }

  final case class EncryptedPayload(payload: String, nonce: String)

  private def encryptPayload(unencryptedPayload: JSONObject, toxData: ToxData, publicKey: String): EncryptedPayload = {
    val hexEncoder = new org.abstractj.kalium.encoders.Hex
    val rawEncoder = new Raw
    val toxmePK = publicKey
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

  private def constructRegistrationRequestJson(name: DnsName, toxData: ToxData): Observable[RegistrationResult[JSONObject]] = {
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
        unencryptedPayload.put("name", name.user)
        unencryptedPayload.put("privacy", privacy)
        unencryptedPayload.put("bio", "")
        val epoch = System.currentTimeMillis() / 1000
        unencryptedPayload.put("timestamp", epoch)

        lookupPublicKey(name.domain.getOrElse("toxme.io")) match {
          case Some(publicKey) =>
            val encryptedPayload = encryptPayload(unencryptedPayload, toxData, publicKey)

            val requestJson = new JSONObject
            requestJson.put("action", 1)
            requestJson.put("public_key", toxData.ID.substring(0, 64))
            requestJson.put("encrypted", encryptedPayload.payload)
            requestJson.put("nonce", encryptedPayload.nonce)
            subscriber.onNext(Right(requestJson))
          case None =>
            subscriber.onNext(Left(RegError.INVALID_DOMAIN))
        }
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