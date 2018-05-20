package chat.tox.antox.wrapper

import java.io.File

import android.content.Context
import android.os.Environment
import chat.tox.antox.utils.StorageType.StorageType
import chat.tox.antox.utils.{Constants, FileUtils, StorageType}

trait Enum[A] {

  trait Value {
    self: A =>
  }

  val values: List[A]
}

sealed trait FileKind extends FileKind.Value {
  def kindId: Int

  def visible: Boolean

  protected def rawStorageDirectory: String

  protected def storageType: StorageType

  def getStorageDir(context: Context): File = {
    val dir = FileUtils.getDirectory(rawStorageDirectory, storageType, context)
    dir.mkdirs()
    dir
  }

  def autoAccept: Boolean

  def replaceExisting: Boolean
}

object FileKind extends Enum[FileKind] {
  def fromToxFileKind(toxFileKind: Int): FileKind =
    values.find(_.kindId == toxFileKind).getOrElse(INVALID)

  case object INVALID extends FileKind {
    val kindId = -1
    val visible = true
    val rawStorageDirectory: String = ""
    val autoAccept: Boolean = false
    val storageType: StorageType = StorageType.NONE
    val replaceExisting = false
  }

  case object DATA extends FileKind {
    val kindId = 0
    val visible = true
    val rawStorageDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), Constants.DOWNLOAD_DIRECTORY).getPath

    def storageType: StorageType = StorageType.EXTERNAL

    val autoAccept = false
    val replaceExisting = false
  }

  case object AVATAR extends FileKind {
    val kindId = 1
    val visible = false
    val rawStorageDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
      + "/" + Constants.DOWNLOAD_DIRECTORY, Constants.AVATAR_DIRECTORY).getPath
    val storageType = StorageType.EXTERNAL
    val autoAccept = true
    val replaceExisting = true


    def getAvatarFile(avatarName: String, context: Context): Option[File] =
      if (avatarName != null && avatarName.nonEmpty && context != null) {
        val file = new File(AVATAR.getStorageDir(context), avatarName)
        if (file.exists() && !file.isDirectory) {
          Some(file)
        } else {
          None
        }
      } else {
        None
      }
  }

  val values = List(INVALID, DATA, AVATAR)
}


