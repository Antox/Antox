package im.tox.antox.utils

import java.io.File

import android.content.Context
import im.tox.antox.utils.StorageType.StorageType

object FileUtil {
  def getDirectory(path: String, storageType: StorageType, context: Context): File = {
    if (storageType == StorageType.EXTERNAL) {
      new File(path)
    } else {
      context.getDir(path, Context.MODE_PRIVATE)
    }
  }
}