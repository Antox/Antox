import android.support.test.runner.AndroidJUnit4
import android.test.AndroidTestCase
import chat.tox.antox.toxdns.ToxDNS.DNSError
import chat.tox.antox.toxdns.{ToxData, DnsName, ToxDNS}
import chat.tox.antox.utils.Options
import chat.tox.antox.wrapper.ToxAddress
import im.tox.tox4j.core.options.ToxOptions
import im.tox.tox4j.impl.jni.ToxCoreImpl
import org.junit.Assert._
import org.junit.{Ignore, Before, Test}
import org.junit.runner.RunWith

@RunWith(classOf[AndroidJUnit4])
class ToxDNSTest extends AndroidTestCase {

  @Test
  def testLookup(): Unit = {
    val id = ToxDNS.lookup("subliun@toxme.io").toBlocking.first
    assertEquals(id.get, "828435142ACE09E8677427E6180BFB27E38FB589A3B84C24976AE49F80A69C6838D0D7CF0EB6")
  }

  @Test
  def testLookupUnicode(): Unit = {
    val id = ToxDNS.lookup("☠@toxme.io").toBlocking.first
    assertEquals(id.get, "11BB3CCDD46346EAA76FF935F1CB31CDC11C56803F1077745124A1C7C63F7E6C8B286B415682")
  }

  @Test
  def testLookupPublicKey(): Unit = {
    val publicKey = ToxDNS.lookupPublicKey("toxme.io").get
    assertEquals(publicKey, "1A39E7A5D5FA9CF155C751570A32E625698A60A55F6D88028F949F66144F4F25")
  }


  @Test
  def testRegistration(): Unit = {
    val toxData = new ToxData
    val accountName: String = "antoxtest" + (Math.random()*10000).toInt
    val dnsname = DnsName.fromString(accountName)
    val toxOptions = new ToxOptions(Options.ipv6Enabled, Options.udpEnabled)
    val tox = new ToxCoreImpl(toxOptions)
    toxData.address = new ToxAddress(tox.getAddress)
    toxData.fileBytes = tox.getSavedata
    val result = ToxDNS.registerAccount(dnsname, toxData ).toBlocking.first
    result match {
      case Left(error) => {
        val message = DNSError.getDescription(error)
        assertTrue("Could not register, reason: " + message , false)
      }

      case Right(password) => {
        assertTrue(true)
      }
    }
  }

  @Test
  def testDeletion(): Unit = {
    val toxData = new ToxData
    val accountName: String = "antoxtest" + (Math.random()*10000).toInt
    val dnsname = DnsName.fromString(accountName)
    val toxOptions = new ToxOptions(Options.ipv6Enabled, Options.udpEnabled)
    val tox = new ToxCoreImpl(toxOptions)
    toxData.address = new ToxAddress(tox.getAddress)
    toxData.fileBytes = tox.getSavedata
    val registerResult = ToxDNS.registerAccount(dnsname,toxData).toBlocking.first

    registerResult match {
      case Left(error) =>
        val message = DNSError.getDescription(error)
        assertTrue("Could not register, reason: " + message , false)
      case Right(password) =>
        val result = ToxDNS.deleteAccount(dnsname, toxData ).toBlocking.first
        result match {
          case Left(error) =>
            val message = DNSError.getDescription(error)
            assertTrue("Could not delete, reason: " + message,  false)
          case Right(message) =>
            assertTrue(true)
        }
    }
  }
}
