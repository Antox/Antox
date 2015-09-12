package chat.tox.antox.wrapper

class UserInfo(val username: String,
               val domain: String,
               val password: String,
               val nickname: String,
               val status: String,
               val statusMessage: String,
               val loggingEnabled: Boolean,
               val avatarName: String) {


  /**
   * @return username@domain
   */
  def getFullAddress: String = username + "@" + domain

}