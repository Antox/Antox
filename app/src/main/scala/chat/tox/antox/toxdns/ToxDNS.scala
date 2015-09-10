package chat.tox.antox.toxdns

import java.io.UnsupportedEncodingException

import android.util.{Base64, Log}
import chat.tox.antox.toxdns.ToxDNS.DNSError.DNSError
import chat.tox.antox.toxdns.ToxDNS.RequestType.RequestType
import chat.tox.antox.wrapper.ToxAddress
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

  val DEFAULT_TOXDNS_DOMAIN = "toxme.io"
  val DEBUG_TAG = "TOX_DNS"
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
            val offset = 11
            val key = txtString.substring(offset, offset + ToxAddress.MAX_ADDRESS_LENGTH)
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


  def APIof(dnsDomain: String): String = "https://" + dnsDomain + "/api"

  //ToxDNS Error
  object DNSError extends Enumeration {
    type DNSError = Value
    val OK = Value("0")
    val METHOD_UNSUPPORTED = Value("-1")
    val NOTSECURE = Value("-2")
    val BAD_PAYLOAD = Value("-3")
    val NAME_TAKEN = Value("-25")
    val DUPE_ID = Value("-26")
    val UNKNOWN_NAME = Value("-30")
    val INVALID_ID = Value("-31")
    val LOOKUP_FAILED = Value("-41")
    val NO_USER = Value("-42")
    val LOOKUP_INTERNAL = Value("-43")
    val RATE_LIMIT = Value("-4")
    val UNKNOWN = Value("")
    val KALIUM_LINK_ERROR = Value("KALIUM")
    val INVALID_DOMAIN = Value("INVALID_DOMAIN")
    val INTERNAL = Value("INTERNAL")

    def valueOf(name: String) = values.find(_.toString == name).getOrElse(UNKNOWN)

    def getDescription(dnsError: DNSError) = dnsError match {
      case OK => "OK"
      case METHOD_UNSUPPORTED => "Client didn't POST to /api"
      case NOTSECURE => "Client is not using a secure connection"
      case BAD_PAYLOAD => "Bad encrypted payload (not encrypted with DNS key)"
      case NAME_TAKEN => "Name is taken"
      case DUPE_ID => "The public key given is bound to a name already"
      case UNKNOWN_NAME => "Name not found"
      case INVALID_ID => "Sent invalid data in place of an ID"
      case LOOKUP_FAILED => "Lookup failed because of an error on the other domain's side."
      case NO_USER => "Lookup failed because that user doesn't exist on the domain"
      case LOOKUP_INTERNAL => "Lookup failed because of a DNS server error"
      case RATE_LIMIT => "Client is publishing IDs too fast"
      case UNKNOWN => "Unknown error"
      case KALIUM_LINK_ERROR => "Kalium link error"
      case INVALID_DOMAIN => "Invalid Tox DNS domain"
      case INTERNAL => "Internal error"
        "Unknown error " + dnsError.toString
    }
  }

  final case class EncryptedPayload(payload: String, nonce: String)

  private def encryptPayload(unencryptedPayload: JSONObject, toxData: ToxData, publicKey: String): EncryptedPayload = {
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
    EncryptedPayload(payload, nonceString)
  }

  type Result[Success] = Either[DNSError, Success]

  /**
   * Registers a new account on the specified ToxDNS (DnsName.domain)
   *
   * If the service cannot be contacted, the network is down, or some other error occurs,
   * the appropriate RegError is returned.
   *
   * @return ToxDNS request observable that contains password on success, RegError on lookup error
   */
  def registerAccount(name: DnsName, toxData: ToxData): Observable[Result[String]] = {
    constructRequestJson(name, toxData, RequestType.REGISTRATION_REQUEST).flatMap(result =>
      result match {
        case Left(error: DNSError) => Observable.just(Left(error))
        case Right(result: JSONObject) => postJson(result, APIof(name.domain.getOrElse(DEFAULT_TOXDNS_DOMAIN)), RequestType.REGISTRATION_REQUEST)
      }
    ).onErrorReturn(_ => Left(DNSError.UNKNOWN))
      .subscribeOn(IOScheduler())
  }

  /**
   * Deletes an account on the specified ToxDNS (DnsName.domain)
   *
   * If the service cannot be contacted, the network is down, or some other error occurs,
   * the appropriate RegError is returned.
   *
   * @return ToxDNS request observable that contains a confirmation string on success. RegError on lookup error
   */
  def deleteAccount(name: DnsName, toxData: ToxData): Observable[Result[String]] = {
    constructRequestJson(name, toxData, RequestType.DELETION_REQUEST).flatMap(result =>
      result match {
        case Left(error: DNSError) => Observable.just(Left(error))
        case Right(result: JSONObject) => postJson(result, APIof(name.domain.getOrElse(DEFAULT_TOXDNS_DOMAIN)), RequestType.REGISTRATION_REQUEST)
      }
    ).onErrorReturn(_ => Left(DNSError.UNKNOWN))
      .subscribeOn(IOScheduler())
  }

  /**
   * Different request types for the ToxDNS
   */
  object RequestType extends Enumeration {
    type RequestType = Int
    val REGISTRATION_REQUEST = 1
    val DELETION_REQUEST = 2
  }

  private def constructRequestJson(name: DnsName, toxData: ToxData, requestType: RequestType): Observable[Result[JSONObject]] = {
    Observable(subscriber => {
      try {
        System.load("libkaliumjni.so")
      } catch {
        case e: UnsatisfiedLinkError =>
          subscriber.onNext(Left(DNSError.KALIUM_LINK_ERROR))
      }
      try {
        val privacy = 0
        val epoch = System.currentTimeMillis() / 1000

        val unencryptedPayload = requestType match {
          case RequestType.REGISTRATION_REQUEST =>
            val json = new JSONObject
            json.put("tox_id", toxData.address)
            json.put("name", name.user)
            json.put("privacy", privacy)
            json.put("bio", "")
            json.put("timestamp", epoch)
            json
          case RequestType.DELETION_REQUEST =>
            val json = new JSONObject
            json.put("public_key",toxData.address.key.toString)
            json.put("timestamp", epoch)
            json
        }

        lookupPublicKey(name.domain.getOrElse(DEFAULT_TOXDNS_DOMAIN)) match {
          case Some(publicKey) =>
            val encryptedPayload = encryptPayload(unencryptedPayload, toxData, publicKey)

            val requestJson = new JSONObject
            requestJson.put("action", 1)
            requestJson.put("public_key", toxData.address.key.toString)
            requestJson.put("encrypted", encryptedPayload.payload)
            requestJson.put("nonce", encryptedPayload.nonce)
            subscriber.onNext(Right(requestJson))
          case None =>
            subscriber.onNext(Left(DNSError.INVALID_DOMAIN))
        }
      } catch {
        case e: JSONException =>
          Log.d(DEBUG_TAG, "JSON Exception " + e.getMessage)
          subscriber.onError(e)
      }
      subscriber.onCompleted()
    })
  }

  private def postJson(requestJson: JSONObject, apiURL: String, requestType: RequestType): Observable[Result[String]] = {
    Observable(subscriber => {
      val httpClient = new OkHttpClient()
      try {
        val mediaType = MediaType.parse("application/json; charset=utf-8")
        val requestBody = RequestBody.create(mediaType, requestJson.toString)
        val request = new Builder().url(apiURL).post(requestBody).build()
        val response = httpClient.newCall(request).execute()
        Log.d(DEBUG_TAG, "Response code: " + response.toString)
        val json = new JSONObject(response.body().string())
        val errorCode = json.getString("c")
        val error = Try(DNSError.withName(errorCode)).getOrElse(DNSError.UNKNOWN)
        if (error == DNSError.OK) {

          val result = requestType match {
            case RequestType.REGISTRATION_REQUEST => json.getString ("password")
            case RequestType.DELETION_REQUEST => "Deleted successfully"
          }
          subscriber.onNext(Right(result))
        } else {
          subscriber.onNext(Left(error))
        }
      } catch {
        case e: UnsupportedEncodingException =>
          Log.d(DEBUG_TAG, "Unsupported Encoding Exception: " + e.getMessage)
          subscriber.onError(e)
        case e: Exception =>
          Log.d(DEBUG_TAG, "IOException: " + e.getMessage)
          subscriber.onError(e)
      }

      subscriber.onCompleted()
    })
  }
}