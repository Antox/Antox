package chat.tox.antox.utils

import java.io.{File, FileInputStream, FileNotFoundException, InputStream}

import android.graphics.BitmapFactory.{Options => BitmapOptions}
import android.graphics.{Bitmap, BitmapFactory}
import android.support.v4.util.LruCache
import chat.tox.antox.utils.BitmapManager._
import chat.tox.antox.wrapper.BitmapUtils.RichBitmap
import org.scaloid.common._
import rx.lang.scala.Observable
import rx.lang.scala.schedulers.{AndroidMainThreadScheduler, IOScheduler}

import scala.collection.mutable
import scala.util.Try

object BitmapManager {

  private final case class ImageKey(key: String) extends AnyVal

  // Use a LRU Cache for storing inlined bitmap images in chats
  private var memoryCache: LruCache[ImageKey, Bitmap] = _

  // Use a separate hashmap for avatars as they are all needed most of the time
  private val avatarCache = new mutable.HashMap[ImageKey, Bitmap]

  // Hashmap used for storing whether a cached avatar is valid or needs to be updated because a contact
  // has updated their avatar - contacts' avatars are stored under the name of their public key
  private val avatarValid = new mutable.HashMap[ImageKey, Boolean]()

  private val TAG = LoggerTag(getClass.getSimpleName)

  private def getImageKey(file: File): ImageKey = ImageKey(file.getAbsolutePath)

  def getFromCache(isAvatar: Boolean, file: File): Option[Bitmap] = {
    getFromCache(isAvatar, getImageKey(file))
  }

  private def getFromCache(isAvatar: Boolean, key: ImageKey): Option[Bitmap] = {
    if (isAvatar) {
      getAvatarFromCache(key)
    } else {
      getBitmapFromMemCache(key)
    }
  }

  private def getBitmapFromMemCache(key: ImageKey): Option[Bitmap] = {
    Try(Option(memoryCache.get(key))).toOption.flatten
  }

  private def getAvatarFromCache(key: ImageKey): Option[Bitmap] = {
    if (isAvatarValid(key)) {
      avatarCache.get(key)
    } else {
      None
    }
  }

  private def isAvatarValid(key: ImageKey): Boolean = {
    avatarValid.getOrElse(key, false)
  }

  private def addBitmapToMemoryCache(key: ImageKey, bitmap: Bitmap) {
    if (memoryCache != null && getBitmapFromMemCache(key).isEmpty) {
      try {
        memoryCache.put(key, bitmap)
      }
      catch {
        case e: Exception =>
          e.printStackTrace()
      }
    }
  }

  private def addAvatarToCache(key: ImageKey, bitmap: Bitmap) {
    avatarCache.put(key, bitmap) // will overwrite any previous value for key
    avatarValid.put(key, true)
  }

  def setAvatarInvalid(file: File) {
    avatarValid.put(getImageKey(file), false)
  }

  def calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int): Int = {
    val width = options.outWidth
    var inSampleSize = 1

    if (width > reqWidth) {
      val halfWidth = width / 2
      while ((halfWidth / inSampleSize) > reqWidth) {
        inSampleSize *= 2
      }
    }
    AntoxLog.debug("Using a sample size of " + inSampleSize, TAG)

    inSampleSize
  }

  /**
    * Will decode the byte Array and proceed to return true if the bitmap is valid.
    */
  def decodeAndCheck(byteArr: Array[Byte], options: BitmapOptions): Boolean = {
    options.inJustDecodeBounds = true
    BitmapFactory.decodeByteArray(byteArr, 0, byteArr.length, options)

    options.outWidth > 0 && options.outHeight > 0
  }

  /**
    * Reads in bytes from the given stream and returns them in an array
    */
  private def getBytesFromStream(inputStream: InputStream): Array[Byte] = {
    var byteArr = Array.ofDim[Byte](0)
    val buffer = Array.ofDim[Byte](2 ^ 10)
    var len: Int = 0
    var count = 0

    try {
      len = inputStream.read(buffer)
      while (len > -1) {
        if (len != 0) {
          if (count + len > byteArr.length) {
            val newbuf = Array.ofDim[Byte]((count + len) * 2)
            System.arraycopy(byteArr, 0, newbuf, 0, count)
            byteArr = newbuf
          }
          System.arraycopy(buffer, 0, byteArr, count, len)
          count += len
        }
        len = inputStream.read(buffer)
      }
      byteArr
    } catch {
      case e: Exception =>
        e.printStackTrace()
        null
    }
  }

  /**
    * Will load the bitmap from file, decode it and then return a potentially downsampled bitmap
    * ready to be displayed
    */
  private def decodeBitmap(file: File, imageKey: ImageKey, isAvatar: Boolean): Bitmap = {
    var fis: FileInputStream = null

    try {


      // Get a stream to the file
      fis = new FileInputStream(file)

      // Get the bytes from the image file
      val byteArr = getBytesFromStream(fis)

      val options = new BitmapFactory.Options()

      if (decodeAndCheck(byteArr, options)) {
        options.inSampleSize = calculateInSampleSize(options, 200)
        options.inPreferredConfig = Bitmap.Config.RGB_565
        options.inJustDecodeBounds = false

        val bitmap = BitmapFactory.decodeByteArray(byteArr, 0, byteArr.length, options)

        if (isAvatar) {
          addAvatarToCache(imageKey, bitmap)
        } else {
          addBitmapToMemoryCache(imageKey, bitmap)
        }

        bitmap
      } else {
        null
      }
    } catch {
      case e: FileNotFoundException =>
        AntoxLog.debug("File not found when trying to be used for FileInputStream", TAG)
        e.printStackTrace()
        null
    } finally {
      if (fis != null) {
        fis.close()
      }
    }
  }

  def load(file: File, isAvatar: Boolean): Observable[Bitmap] = {
    Observable[Bitmap](sub => {
      sub.onNext(loadBlocking(file, isAvatar))

      sub.onCompleted()
    }).subscribeOn(IOScheduler())
      .observeOn(AndroidMainThreadScheduler())
  }

  def loadBlocking(file: File, isAvatar: Boolean): Bitmap = {
    val imageKey = getImageKey(file)
    AntoxLog.debug(imageKey.toString, TAG)

    getFromCache(isAvatar, imageKey) match {
      case Some(bitmap) =>
        AntoxLog.debug("Loading Bitmap image from cache", TAG)
        bitmap

      case None =>
        AntoxLog.debug("Decoding Bitmap image", TAG)
        decodeBitmap(file, imageKey, isAvatar)
    }
  }
}

class BitmapManager {
  val maxMemory = (Runtime.getRuntime.maxMemory() / 1024).toInt
  val cacheSize = maxMemory / 8

  memoryCache = new LruCache[ImageKey, Bitmap](cacheSize) {
    // Measure size in KB instead of number of items
    protected override def sizeOf(key: ImageKey, bitmap: Bitmap): Int =
      bitmap.getSizeInBytes.asInstanceOf[Int] / 1024
  }
}
