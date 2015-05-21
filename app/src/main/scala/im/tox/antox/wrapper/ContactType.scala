package im.tox.antox.wrapper

//Don't change this order (it will break the DB)
object ContactType extends Enumeration {
  type ContactType = Value
  val NONE = Value(0)
  val FRIEND = Value(1)
  val GROUP = Value(2)
}