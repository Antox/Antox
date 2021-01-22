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

  def getCircleBitmap(bitmap: Bitmap, recycle: Boolean = true): Bitmap = {

    var w = 10
    try {
      w = bitmap.getWidth
    }
    catch {
      case e: Exception => {
        e.printStackTrace()
        w = 10
      }
    }

    var h = 10
    try {
      h = bitmap.getWidth
    }
    catch {
      case e: Exception => {
        e.printStackTrace()
        h = 10
      }
    }

    val output = Bitmap.createBitmap(w,
      h, Bitmap.Config.ARGB_8888)
    val canvas = new Canvas(output)

    val color = Color.RED
    val paint = new Paint()

    var rect: Rect = null
    // zoff //
    try {
      rect = new Rect(0, 0, bitmap.getWidth, bitmap.getHeight)
    }
    catch {
      case e: Exception => {
        e.printStackTrace()
        rect = new Rect(0, 0, 10, 10)
      }
    }
    val rectF = new RectF(rect)

    paint.setAntiAlias(true)
    canvas.drawARGB(0, 0, 0, 0)
    paint.setColor(color)
    canvas.drawOval(rectF, paint)

    paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN))
    canvas.drawBitmap(bitmap, rect, rect, paint)

    bitmap.recycle()

    output
  }

  def getCroppedBitmap(bitmap: Bitmap, recycle: Boolean = true): Bitmap = {

    var min = 10
    try {
      min = math.min(bitmap.getWidth, bitmap.getHeight)
    }
    catch {
      case e: Exception => {
        e.printStackTrace()
        min = 10
      }
    }

    val output = ThumbnailUtils.extractThumbnail(bitmap, min, min)
    if (recycle) bitmap.recycle()
    output
  }
}
