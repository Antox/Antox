import android.support.test.runner.AndroidJUnit4
import android.test.AndroidTestCase
import chat.tox.antox.toxdns.ToxDNS
import org.junit.Assert._
import org.junit.Test
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
}
