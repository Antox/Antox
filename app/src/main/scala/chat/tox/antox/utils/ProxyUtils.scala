package chat.tox.antox.utils

import java.net.InetSocketAddress

import android.content.SharedPreferences
import im.tox.tox4j.core.options.ProxyOptions
import org.scaloid.common.LoggerTag

object ProxyUtils {
  private object ProxyType extends Enumeration {
    type ProxyType = Value
    val Http = Value(0)
    val Socks5 = Value(1)
  }

  private case class Proxy(proxyType: ProxyType.ProxyType, address: String, port: Int)

  private def getProxyFromPreferences(preferences: SharedPreferences): Option[Proxy] = {
    val TAG = LoggerTag("getProxyFromPreferences")
    AntoxLog.verbose("Reading proxy settings", TAG)

    val proxyEnabled = preferences.getBoolean("enable_proxy", Options.proxyEnabled)
    AntoxLog.verbose("Proxy enabled: " + proxyEnabled.toString, TAG)

    if (proxyEnabled) {
      val proxyAddress = preferences.getString("proxy_address", Options.proxyAddress)
      AntoxLog.verbose("Proxy address: " + proxyAddress, TAG)

      val proxyPort = preferences.getString("proxy_port", Options.proxyPort).toInt
      AntoxLog.verbose("Proxy port: " + proxyPort, TAG)

      val proxyType = preferences.getString("proxy_type", "SOCKS5")
      AntoxLog.verbose("Proxy type: " + proxyType, TAG)

      Some(proxyType match {
        case "HTTP" =>
          Proxy(ProxyType.Http, proxyAddress, proxyPort)
        case "SOCKS5" =>
          Proxy(ProxyType.Socks5, proxyAddress, proxyPort)
      })
    } else {
      None
    }
  }

  def toxProxyFromPreferences(preferences: SharedPreferences): ProxyOptions = {
    getProxyFromPreferences(preferences) match {
      case Some(proxy) =>
        proxy.proxyType match {
          case ProxyType.Http =>
            ProxyOptions.Http(proxy.address, proxy.port)
          case ProxyType.Socks5 =>
            ProxyOptions.Socks5(proxy.address, proxy.port)
        }

      case _ =>
        ProxyOptions.None
    }
  }

  def netProxyFromPreferences(preferences: SharedPreferences): Option[java.net.Proxy] = {
    getProxyFromPreferences(preferences) match {
      case Some(proxy) =>
        val inetSocketAddress = new InetSocketAddress(proxy.address, proxy.port)
        Some(proxy.proxyType match {
          case ProxyType.Http =>
            new java.net.Proxy(java.net.Proxy.Type.HTTP, inetSocketAddress)
          case ProxyType.Socks5 =>
            new java.net.Proxy(java.net.Proxy.Type.SOCKS, inetSocketAddress)
        })

      case _ =>
        None
    }
  }
}
