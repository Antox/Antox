package chat.tox.antox.toxdns

import android.util.{Base64, Log}
import chat.tox.antox.toxdns.DNSError.DNSError
import chat.tox.antox.toxdns.ToxDNS.EncryptedRequestAction.EncryptedRequestAction
import chat.tox.antox.toxdns.ToxDNS.PrivacyLevel.PrivacyLevel
import chat.tox.antox.wrapper.ToxAddress
import com.squareup.okhttp.Request.Builder
import com.squareup.okhttp.{MediaType, OkHttpClient, RequestBody}
import org.abstractj.kalium.crypto.Box
import org.abstractj.kalium.encoders.Raw
import org.json.JSONObject
import org.xbill.DNS.{Lookup, TXTRecord, Type}
import rx.lang.scala.Observable
import rx.lang.scala.schedulers.IOScheduler

import scala.language.higherKinds
import scala.util.Try

object ToxDNS {

  val DEFAULT_TOXDNS_DOMAIN = "toxme.io"
  val DEBUG_TAG = "TOX_DNS"

  private def epoch = System.currentTimeMillis() / 1000

 /**
  * Performs a DNS lookup and returns the Tox ID registered to the given dnsName.
  *
  * @param dnsName the dns name to lookup
  * @return the ID or None if the name is not found or no domain is supplied
  */
  def lookup(dnsName: String): Observable[Option[String]] = {
    Observable(subscriber => {
      if (dnsName.contains("@")) {
        val parsedDnsName = DnsName.fromString(dnsName, true)
        val lookup = parsedDnsName.username + "._tox." + parsedDnsName.domain.get
        try {
          val records = new Lookup(lookup, Type.TXT).run()
          val txt = records(0).asInstanceOf[TXTRecord]
          val txtString = txt.toString.substring(txt.toString.indexOf('"'))
          if (txtString.contains("tox1")) {
            val offset = 11
            val key = txtString.substring(offset, offset + ToxAddress.MAX_ADDRESS_LENGTH)
            subscriber.onNext(Some(key))
          }
        } catch {
          case e: Exception =>
            subscriber.onNext(None)
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
      val client = new OkHttpClient()

      val request = new Builder().url(s"https://$dnsDomain/pk").build()
      val response = client.newCall(request).execute()
      val json = new JSONObject(response.body().string())

      Some(json.getString("key"))
    } catch {
      case e: Exception =>
        e.printStackTrace()
        None
    }
  }

  def makeApiURL(dnsDomain: String): String = "https://" + dnsDomain + "/api"

  type Password = String
  type DnsResult[Success] = Either[DNSError, Success]

  /**
   * Different request types for the ToxDNS
   */
  object EncryptedRequestAction extends Enumeration {
    type EncryptedRequestAction = Int
    val REGISTRATION = 1
    val DELETION = 2
  }

  object PrivacyLevel extends Enumeration {
    type PrivacyLevel = Value
    val PRIVATE = Value(0)
    val PUBLIC = Value(1)
  }

  /**
   * Registers a new account on the specified ToxDNS (DnsName.domain)
   *
   * If the service cannot be contacted, the network is down, or some other error occurs,
   * the appropriate RegError is returned.
   *
   * @return ToxDNS request observable that contains password on success, RegError on lookup error
   */
  def registerAccount(name: DnsName, privacyLevel: PrivacyLevel, toxData: ToxData): Observable[DnsResult[Password]] = {
    Observable[DnsResult[Password]](subscriber => {
      val json = new JSONObject
      json.put("tox_id", toxData.address)
      json.put("name", name.username)
      json.put("privacy", privacyLevel.id)
      json.put("bio", "")
      json.put("timestamp", epoch)
      subscriber.onNext(
        makeEncryptedRequest(name, toxData, json, EncryptedRequestAction.REGISTRATION)
        .right
        .map(_.getString("password")))
      subscriber.onCompleted()
    }).subscribeOn(IOScheduler())
  }

  /**
   * Deletes an account on the specified ToxDNS (DnsName.domain)
   *
   * If the service cannot be contacted, the network is down, or some other error occurs,
   * the appropriate RegError is returned.
   *
   * @return ToxDNS request observable that contains a confirmation string on success. RegError on lookup error
   */
  def deleteAccount(name: DnsName, toxData: ToxData): Observable[Option[DNSError]] = {
    Observable[Option[DNSError]](subscriber => {
      val json = new JSONObject
      json.put("public_key", toxData.address.key.toString)
      json.put("timestamp", epoch)
      subscriber.onNext(
        makeEncryptedRequest(name, toxData, json, EncryptedRequestAction.DELETION)
        .left
        .toOption)
      subscriber.onCompleted()
    }).subscribeOn(IOScheduler())
  }

  private def makeEncryptedRequest(name: DnsName, toxData: ToxData, json: JSONObject, action: EncryptedRequestAction) = {
    val apiURL = makeApiURL(name.domain.get)

    encryptRequestJson(name, toxData, json, action)
      .right.flatMap(postJson(_, apiURL))
  }

  private def encryptRequestJson(name: DnsName, toxData: ToxData, requestJson: JSONObject, requestAction: EncryptedRequestAction): DnsResult[JSONObject] = {
    try {
      lookupPublicKey(name.domain.get) match {
        case Some(publicKey) =>
          encryptPayload(requestJson, toxData, publicKey) match {
            case Some(encryptedPayload) =>
              val requestJson = new JSONObject
              requestJson.put("action", requestAction)
              requestJson.put("public_key", toxData.address.key.toString)
              requestJson.put("encrypted", encryptedPayload.payload)
              requestJson.put("nonce", encryptedPayload.nonce)
              Right(requestJson)
            case None =>
              Left(DNSError.KALIUM_LINK_ERROR)
          }
        case None =>
          Left(DNSError.INVALID_DOMAIN)
      }
    } catch {
      case e: Exception =>
        Log.d(DEBUG_TAG, e.getClass.getSimpleName + ": " + e.getMessage)
        Left(DNSError.exception(e))
    }
  }

  final case class EncryptedPayload(payload: String, nonce: String)

  private def encryptPayload(unencryptedPayload: JSONObject, toxData: ToxData, publicKey: String): Option[EncryptedPayload] = {
    try {
      System.load("libkaliumjni.so")
    } catch {
      case e: UnsatisfiedLinkError =>
        return None
    }

    val hexEncoder = new org.abstractj.kalium.encoders.Hex
    val rawEncoder = new Raw
    val dnsPK = publicKey
    val serverPublicKey = hexEncoder.decode(dnsPK)
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
    Some(EncryptedPayload(payload, nonceString))
  }

  private def postJson(requestJson: JSONObject, dnsApiURL: String): DnsResult[JSONObject] = {
    val httpClient = new OkHttpClient()
    try {
      val mediaType = MediaType.parse("application/json; charset=utf-8")
      val requestBody = RequestBody.create(mediaType, requestJson.toString)
      val request = new Builder().url(dnsApiURL).post(requestBody).build()
      val response = httpClient.newCall(request).execute()
      Log.d(DEBUG_TAG, "Response code: " + response.toString)
      val responseJson = new JSONObject(response.body().string())
      val error = Try(DNSError.withName(responseJson.getString("c"))).getOrElse(DNSError.UNKNOWN)

      if (error == DNSError.OK) {
        Right(responseJson)
      } else {
        Left(error)
      }
    } catch {
      case e: Exception =>
        Log.d(DEBUG_TAG, e.getClass.getSimpleName + ": " + e.getMessage)
        Left(DNSError.exception(e))
    }
  }
}