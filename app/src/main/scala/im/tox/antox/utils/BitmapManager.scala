package im.tox.antox.utils

import java.io.{File, FileInputStream, FileNotFoundException, InputStream}
import java.util

import android.graphics.{Bitmap, BitmapFactory}
import android.util.{Log, LruCache}
import android.widget.ImageView
import im.tox.antox.utils.BitmapManager._
import im.tox.antox.wrapper.BitmapUtils.RichBitmap

object BitmapManager {

  private var mMemoryCache: LruCache[String, Bitmap] = _
  private val bitmapValidMap = new util.HashMap[String, Boolean]()

  private def getBitmapFromMemCache(key: String): Option[Bitmap] = if (mMemoryCache != null) Option(mMemoryCache.get(key)) else None

  private def isBitmapValid(key: String): Boolean = Option(bitmapValidMap.get(key)).getOrElse(false)

  private def addBitmapToMemoryCache(key: String, valid: Boolean, bitmap: Bitmap) {
    if (getBitmapFromMemCache(key).isEmpty && mMemoryCache != null) {
      mMemoryCache.put(key, bitmap)
      bitmapValidMap.put(key, valid)
    }
  }

  private def calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int): Int = {
    val width = options.outWidth
    var inSampleSize = 1
    if (width > reqWidth) {
      val halfWidth = width / 2
      while ((halfWidth / inSampleSize) > reqWidth) {
        inSampleSize *= 2
      }
    }
    inSampleSize
  }

  private def checkValidImage(byteArr: Array[Byte]): Boolean = {
    val options = new BitmapFactory.Options()
    options.inJustDecodeBounds = true
    BitmapFactory.decodeByteArray(byteArr, 0, byteArr.length, options)

    options.outWidth > 0 && options.outHeight > 0
  }

  private def decodeBytes(inputStream: InputStream): Array[Byte] = {
    var byteArr = Array.ofDim[Byte](0)
    val buffer = Array.ofDim[Byte](1024)
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

  def loadBitmap(file: File, id: Int, imageView: ImageView) {
    val imageKey = String.valueOf(id)
    getBitmapFromMemCache(imageKey) match {
      case Some(bitmap) =>
        if (isBitmapValid(imageKey)) {
          imageView.setImageBitmap(bitmap)
        }
      case None =>
        var fis: FileInputStream = null
        try {
          fis = new FileInputStream(file)
          val byteArr = decodeBytes(fis)
          val options = new BitmapFactory.Options()
          options.inJustDecodeBounds = true
          var bitmap = BitmapFactory.decodeByteArray(byteArr, 0, byteArr.length, options)
          options.inSampleSize = calculateInSampleSize(options, 200)
          options.inJustDecodeBounds = false
          options.inPreferredConfig = Bitmap.Config.RGB_565
          bitmap = BitmapFactory.decodeByteArray(byteArr, 0, byteArr.length, options)
          addBitmapToMemoryCache(imageKey, checkValidImage(byteArr), bitmap)
          if (isBitmapValid(imageKey)) {
            imageView.setImageBitmap(bitmap)
          }
        } catch {
          case e: FileNotFoundException =>
            Log.d("BitMapManager", "File not found when trying to be used for FileInputStream")
            e.printStackTrace()
        }
    }
  }
}

class BitmapManager {

  val maxMemory = (Runtime.getRuntime.maxMemory() / 1024).toInt

  val cacheSize = maxMemory / 8

  mMemoryCache = new LruCache[String, Bitmap](cacheSize) {

    protected override def sizeOf(key: String, bitmap: Bitmap): Int =
      bitmap.getSizeInBytes.asInstanceOf[Int] / 1024
  }
}
