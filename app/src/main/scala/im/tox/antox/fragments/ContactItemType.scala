package im.tox.antox.fragments

//Don't change this order (it will break the DB)
object ContactItemType extends Enumeration {
  type ContactItemType = Value
  val FRIEND_REQUEST = Value(1)
  val FRIEND = Value(2)
  val GROUP_INVITE = Value(3)
  val GROUP = Value(4)
}