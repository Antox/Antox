package chat.tox.antox.toxme

import android.util.Base64
import chat.tox.antox.toxme.ToxMe.PrivacyLevel.PrivacyLevel
import chat.tox.antox.toxme.ToxMe.RequestAction.EncryptedRequestAction
import chat.tox.antox.toxme.ToxMeError.ToxMeError
import chat.tox.antox.utils.AntoxLog
import com.squareup.okhttp.Request.Builder
import com.squareup.okhttp.{MediaType, OkHttpClient, RequestBody}
import org.abstractj.kalium.crypto.Box
import org.abstractj.kalium.encoders.Raw
import org.json.JSONObject
import org.scaloid.common.LoggerTag
import rx.lang.scala.Observable
import rx.lang.scala.schedulers.IOScheduler

import scala.collection.mutable.ArrayBuffer
import scala.language.higherKinds
import scala.util.Try

object ToxMe {

  val DEFAULT_TOXME_DOMAIN = "toxme.io"
  private val TAG = LoggerTag(getClass.getSimpleName)

  private def epoch = System.currentTimeMillis() / 1000

 /**
  * Performs a lookup and returns the Tox ID registered to the given toxMeName.
  *
  * @param rawToxMeName the ToxMe name to lookup
  * @return the ID or None if the name is not found or no domain is supplied
  */
  def lookup(rawToxMeName: String): Observable[Option[String]] = {
    Observable(subscriber => {
      if (rawToxMeName.contains("@")) {
        val toxMeName = ToxMeName.fromString(rawToxMeName, useToxMe = true)
        try {
          val json = new JSONObject()
          json.put("action", RequestAction.LOOKUP)
          json.put("name", toxMeName.username)

          val response = postJson(json, makeApiURL(toxMeName.domain.get))
          subscriber.onNext(response.right.toOption.map(_.getString("tox_id")))
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

  final case class SearchResult(name: String, bio: String)

  /**
   * Search a ToxMe service for a user
   *
   * @param query The query to search for
   * @param domain The ToxMe api URL
   * @param page The page number
   * @return A sequence of SearchResult
   */
  def search(query: String, domain: String, page: Int = 0): Observable[ToxMeResult[Seq[SearchResult]]] = {
    Observable(subscriber => {
      try {
        val json = new JSONObject()
        json.put("action", RequestAction.SEARCH)
        json.put("name", query)
        json.put("page", page)

        val response = postJson(json, domain)
        var users = ArrayBuffer[SearchResult]()
        response match{
          case Left(error) =>
            subscriber.onNext(Left(error))
          case Right(jsonResult) =>
            val results = jsonResult.getJSONArray("users")
            for(i <- 0 until results.length()) {
              val result = results.getJSONObject(i)
              users += new SearchResult(result.getString("name"), result.getString("bio"))
            }
            subscriber.onNext(Right(users))
        }
      } catch {
        case e: Exception =>
          subscriber.onNext(Left(ToxMeError.exception(e)))
      }
      subscriber.onCompleted()
    })
  }

  /**
   * Performs a https lookup for the given domain to retrieve
   * the service's public key to be used for encrypted requests.
   *
   * If the server does not exist or a network-related error occurs, None is returned.
   *
   * @param domain the domain on which to perform the lookup (e.g. toxme.io)
   * @return the public key of the ToxMe service or None
   */
  def lookupPublicKey(domain: String): Option[String] = {
    try {
      val client = new OkHttpClient()

      val request = new Builder().url(s"https://$domain/pk").build()
      val response = client.newCall(request).execute()
      val json = new JSONObject(response.body().string())

      Some(json.getString("key"))
    } catch {
      case e: Exception =>
        e.printStackTrace()
        None
    }
  }

  def makeApiURL(domain: String): String = "https://" + domain + "/api"

  type Password = String
  type ToxMeResult[Success] = Either[ToxMeError, Success]

  /**
   * Different request types for the ToxMe
   */
  object RequestAction extends Enumeration {
    type EncryptedRequestAction = Int
    val REGISTRATION = 1
    val DELETION = 2
    val LOOKUP = 3
    val REVERSE_LOOKUP = 5
    val SEARCH = 6
  }

  object PrivacyLevel extends Enumeration {
    type PrivacyLevel = Value
    val PRIVATE = Value(0)
    val PUBLIC = Value(1)
  }

  /**
   * Registers a new account on the specified ToxMe (ToxMeName.domain)
   *
   * If the service cannot be contacted, the network is down, or some other error occurs,
   * the appropriate RegError is returned.
   *
   * @return ToxMe request observable that contains password on success, RegError on lookup error
   */
  def registerAccount(name: ToxMeName, privacyLevel: PrivacyLevel, toxData: ToxData): Observable[ToxMeResult[Password]] = {
    Observable[ToxMeResult[Password]](subscriber => {
      val json = new JSONObject
      json.put("tox_id", toxData.address)
      json.put("name", name.username)
      json.put("privacy", privacyLevel.id)
      json.put("bio", "")
      json.put("timestamp", epoch)
      subscriber.onNext(
        makeEncryptedRequest(name, toxData, json, RequestAction.REGISTRATION)
        .right
        .map(_.getString("password")))
      subscriber.onCompleted()
    }).subscribeOn(IOScheduler())
  }

  /**
   * Deletes an account on the specified ToxMe (ToxMeName.domain)
   *
   * If the service cannot be contacted, the network is down, or some other error occurs,
   * the appropriate RegError is returned.
   *
   * @return ToxMe request observable that contains a confirmation string on success. RegError on lookup error
   */
  def deleteAccount(name: ToxMeName, toxData: ToxData): Observable[Option[ToxMeError]] = {
    Observable[Option[ToxMeError]](subscriber => {
      val json = new JSONObject
      json.put("public_key", toxData.address.key.toString)
      json.put("timestamp", epoch)
      subscriber.onNext(
        makeEncryptedRequest(name, toxData, json, RequestAction.DELETION)
        .left
        .toOption)
      subscriber.onCompleted()
    }).subscribeOn(IOScheduler())
  }

  private def makeEncryptedRequest(name: ToxMeName, toxData: ToxData, json: JSONObject, action: EncryptedRequestAction) = {
    val apiURL = makeApiURL(name.domain.get)

    encryptRequestJson(name, toxData, json, action)
      .right.flatMap(postJson(_, apiURL))
  }

  private def encryptRequestJson(name: ToxMeName, toxData: ToxData, requestJson: JSONObject, requestAction: EncryptedRequestAction): ToxMeResult[JSONObject] = {
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
              Left(ToxMeError.KALIUM_LINK_ERROR)
          }
        case None =>
          Left(ToxMeError.INVALID_DOMAIN)
      }
    } catch {
      case e: Exception =>
        AntoxLog.debug(e.getClass.getSimpleName + ": " + e.getMessage)
        Left(ToxMeError.exception(e))
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
    val toxmePk = publicKey
    val serverPublicKey = hexEncoder.decode(toxmePk)
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

  private def postJson(requestJson: JSONObject, toxMeApiUrl: String): ToxMeResult[JSONObject] = {
    val httpClient = new OkHttpClient()
    try {
      val mediaType = MediaType.parse("application/json; charset=utf-8")
      val requestBody = RequestBody.create(mediaType, requestJson.toString)
      val request = new Builder().url(toxMeApiUrl).post(requestBody).build()
      val response = httpClient.newCall(request).execute()
      AntoxLog.debug("Response code: " + response.toString, TAG)
      val responseJson = new JSONObject(response.body().string())
      val error = Try(ToxMeError.withName(responseJson.getString("c"))).getOrElse(ToxMeError.UNKNOWN)

      if (error == ToxMeError.OK) {
        Right(responseJson)
      } else {
        Left(error)
      }
    } catch {
      case e: Exception =>
        AntoxLog.debugException(e.getMessage, e, TAG)
        Left(ToxMeError.exception(e))
    }
  }
}