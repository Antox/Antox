package im.tox.antox.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import android.widget.ImageView
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStream
import BitmapManager._
//remove if not needed
import scala.collection.JavaConversions._

object BitmapManager {

  private var mMemoryCache: LruCache[String, Bitmap] = _

  private def getBitmapFromMemCache(key: String): Bitmap = mMemoryCache.get(key)

  private def addBitmapToMemoryCache(key: String, bitmap: Bitmap) {
    if (getBitmapFromMemCache(key) == null) mMemoryCache.put(key, bitmap)
  }

  private def calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int = {
    val height = options.outHeight
    val width = options.outWidth
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
      val halfHeight = height / 2
      val halfWidth = width / 2
      while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
        inSampleSize *= 2
      }
    }
    inSampleSize
  }

  def checkValidImage(file: File): Boolean = {
    var fis: FileInputStream = null
    try {
      fis = new FileInputStream(file)
      val byteArr = decodeBytes(fis)
      val options = new BitmapFactory.Options()
      options.inJustDecodeBounds = true
      BitmapFactory.decodeByteArray(byteArr, 0, byteArr.length, options)
      if (options.outWidth > 0 && options.outHeight > 0) true else false
    } catch {
      case e: FileNotFoundException => {
        Log.d("BitMapManager", "File not found when trying to be used for FileInputStream in checkValidImage")
        e.printStackTrace()
        false
      }
    }
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
      case e: Exception => {
        e.printStackTrace()
        null
      }
    }
  }

  def loadBitmap(file: File, id: Int, imageView: ImageView) {
    val imageKey = String.valueOf(id)
    var bitmap = getBitmapFromMemCache(imageKey)
    if (bitmap != null) {
      Log.d("BitmapManager", "Found image in cache")
      imageView.setImageBitmap(bitmap)
    } else {
      Log.d("BitmapManager", "Image not in cache")
      var fis: FileInputStream = null
      try {
        fis = new FileInputStream(file)
        val byteArr = decodeBytes(fis)
        val options = new BitmapFactory.Options()
        options.inJustDecodeBounds = true
        bitmap = BitmapFactory.decodeByteArray(byteArr, 0, byteArr.length, options)
        options.inSampleSize = calculateInSampleSize(options, 100, 100)
        options.inPurgeable = true
        options.inInputShareable = true
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.RGB_565
        bitmap = BitmapFactory.decodeByteArray(byteArr, 0, byteArr.length, options)
        imageView.setImageBitmap(bitmap)
        addBitmapToMemoryCache(imageKey, bitmap)
      } catch {
        case e: FileNotFoundException => {
          Log.d("BitMapManager", "File not found when trying to be used for FileInputStream")
          e.printStackTrace()
        }
      }
    }
  }
}

class BitmapManager {

  val maxMemory = (Runtime.getRuntime.maxMemory() / 1024).toInt

  val cacheSize = maxMemory / 8

  mMemoryCache = new LruCache[String, Bitmap](cacheSize) {

    protected override def sizeOf(key: String, bitmap: Bitmap): Int = bitmap.getByteCount / 1024
  }
}
