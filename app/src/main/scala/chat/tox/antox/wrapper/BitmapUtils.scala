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
    val output = Bitmap.createBitmap(bitmap.getWidth,
      bitmap.getHeight, Bitmap.Config.ARGB_8888)
    val canvas = new Canvas(output)

    val color = Color.RED
    val paint = new Paint()
    val rect = new Rect(0, 0, bitmap.getWidth, bitmap.getHeight)
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
    val min = math.min(bitmap.getWidth,bitmap.getHeight)
    val output = ThumbnailUtils.extractThumbnail(bitmap,min,min)
    if (recycle) bitmap.recycle()
    output
  }
}
