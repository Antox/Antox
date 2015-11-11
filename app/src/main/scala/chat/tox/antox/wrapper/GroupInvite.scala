package chat.tox.antox.wrapper

final case class GroupInvite(groupKey: ContactKey, inviter: FriendKey, data: Array[Byte])