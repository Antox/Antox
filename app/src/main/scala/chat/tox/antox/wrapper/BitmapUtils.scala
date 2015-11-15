package chat.tox.antox.wrapper

import android.graphics._
import android.media.ThumbnailUtils

object BitmapUtils {
  implicit class RichBitmap(bitmap: Bitmap) {
    //bitmap.getByteCount doesn't exist in Android 2.3
    def getSizeInBytes: Long = {
      bitmap.getRowBytes * bitmap.getHeight
    }
  }

  def getCroppedBitmap(bitmap: Bitmap, recycle: Boolean = true): Bitmap = {
    val min = math.min(bitmap.getWidth,bitmap.getHeight)
    val output = ThumbnailUtils.extractThumbnail(bitmap,min,min)
    if (recycle) bitmap.recycle()
    output
  }
}
