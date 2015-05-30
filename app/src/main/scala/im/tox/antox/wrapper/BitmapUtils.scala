package im.tox.antox.wrapper

import android.graphics.Bitmap

object BitmapUtils {
  implicit class RichBitmap(bitmap: Bitmap) {
    //bitmap.getByteCount doesn't exist in Android 2.3
    def getSizeInBytes: Long = {
      bitmap.getRowBytes * bitmap.getHeight
    }
  }
}
