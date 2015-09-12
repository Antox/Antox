package chat.tox.antox.wrapper

import chat.tox.antox.toxdns.DnsName

class UserInfo(val dnsName: DnsName,
               val password: String,
               val nickname: String,
               val status: String,
               val statusMessage: String,
               val loggingEnabled: Boolean,
               val avatarName: String) {

  def profileName: String = dnsName.username
}