package im.tox.antox.wrapper

import java.io.File

import android.content.Context
import android.os.Environment
import im.tox.antox.utils.StorageType.StorageType
import im.tox.antox.utils.{FileUtil, StorageType, Constants}
import im.tox.antox.utils.StorageType.StorageType
import im.tox.tox4j.core.enums.{ToxFileKind, ToxMessageType}

//Don't change this order (it will break the DB)
/* object FileKind extends Enumeration {
  case class Val(kindId: Int, visible: Boolean) extends super.Val
  val INVALID = Val(-1, visible = false)
  val DATA = Val(0, visible = true)
  val AVATAR = Val(1, visible = false)

  implicit def toxFileKindToFileKind(toxFileKind: Int) = FileKind.values.find(kind => kind.id == toxFileKind).headOption.get
} */

trait Enum[A] {
  trait Value { self: A => }
  val values: List[A]
}

sealed trait FileKind extends FileKind.Value {
  def kindId: Int
  def visible: Boolean
  def storageDir: String //path
  def storageType: StorageType
  def autoAccept: Boolean
  def replaceExisting: Boolean
}

object FileKind extends Enum[FileKind] {
  def fromToxFileKind(toxFileKind: Int): FileKind = {
    for (value <- values) {
      if (value.kindId == toxFileKind){
        return value
      }
    }

    INVALID
  }

  case object INVALID extends FileKind {
    val kindId = -1
    val visible = false
    val storageDir: String = ""
    val autoAccept: Boolean = false
    val storageType: StorageType = StorageType.NONE
    val replaceExisting = false
  }

  case object DATA extends FileKind {
    val kindId = 0
    val visible = true
    val storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), Constants.DOWNLOAD_DIRECTORY).getPath
    def storageType = StorageType.EXTERNAL
    val autoAccept = false
    val replaceExisting = false
  }

  case object AVATAR extends FileKind {
    val kindId = 1
    val visible = false
    val storageDir = Constants.AVATAR_DIRECTORY
    val storageType = StorageType.INTERNAL
    val autoAccept = true
    val replaceExisting = true


    def getAvatarFile(avatarName: String, context: Context): Option[File] = {
      if (avatarName == null || avatarName.equals("") || context == null) None

      val file = new File(FileUtil.getDirectory(AVATAR.storageDir, AVATAR.storageType, context) + "/" + avatarName)
      if (file.exists() && !file.isDirectory) {
        Some(file)
      } else {
        None
      }
    }
  }

  val values = List(INVALID, DATA, AVATAR)
}


