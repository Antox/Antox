package chat.tox.antox.wrapper

import chat.tox.antox.toxme.ToxMeName

class UserInfo(val toxMeName: ToxMeName,
               val password: String,
               val nickname: String,
               val status: String,
               val statusMessage: String,
               val loggingEnabled: Boolean,
               val avatarName: String) {

  def profileName: String = toxMeName.username
}