package im.tox.antox.wrapper

import im.tox.tox4j.core.enums.{ToxFileKind, ToxMessageType}

//Don't change this order (it will break the DB)
object FileKind extends Enumeration {
  type FileKind = Value
  val INVALID = Value(-1)
  val DATA = Value(0)
  val AVATAR = Value(1)
}
