package chat.tox.antox.tests

import android.preference.PreferenceManager
import android.support.test.runner.AndroidJUnit4
import android.test.{AndroidTestCase, RenamingDelegatingContext}
import chat.tox.antox.data.AntoxDB
import chat.tox.antox.wrapper.{FriendKey, SelfKey, ToxKey}
import im.tox.tox4j.core.data.ToxNickname
import im.tox.tox4j.core.enums.ToxMessageType
import org.junit.Assert._
import org.junit.runner.RunWith
import org.junit.{After, Before, Test}

@RunWith(classOf[AndroidJUnit4])
class AntoxDBTest extends AndroidTestCase {

  private var db: AntoxDB = _

  @Before
  override def setUp(): Unit = {
    super.setUp()

    val context = new RenamingDelegatingContext(getContext, "test_")

    val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    db = new AntoxDB(context, "jim", selfKey)
  }

  @After
  override def tearDown(): Unit = {
    super.tearDown()
  }

  val selfKey = new SelfKey("11BB3CCDD46346EAA76FF935F1CB31CDC11C56803F1077745124A1C7C63F7E67")

  val friendKey = new FriendKey("828435142ACE09E8677427E6180BFB27E38FB589A3B84C24976AE49F80A69C68")
  val name = ToxNickname.unsafeFromValue("Steve Appleseed".getBytes)
  val alias = "Steve"
  val statusMessage = "This is my status"

  @Test
  def testAddFriend(): Unit = {
    db.addFriend(friendKey, new String(name.value), alias, statusMessage)

    db.friendInfoList.subscribe(friendList => {
      assert(friendList.size == 1)
      assert(friendList.exists(_.name equals name))
      assert(friendList.exists(_.alias equals alias))
      assert(friendList.exists(_.getDisplayName equals alias))
      assert(friendList.exists(_.statusMessage equals statusMessage))
      assert(friendList.exists(_.key equals friendKey))
    })
  }

  @Test
  def testLastMessages(): Unit = {
    db.addFriend(friendKey, new String(name.value), alias, statusMessage)

    def addMessage(text: String, from: ToxKey): Unit = {
      db.addMessage(friendKey, from, name, text, hasBeenReceived = false, hasBeenRead = false, successfullySent = true, ToxMessageType.NORMAL)
    }
    assertEquals(db.lastMessages.toBlocking.first.get(friendKey), None)
    assertEquals(db.lastMessages.toBlocking.first.get(friendKey), None)

    addMessage("How are you?", friendKey)
    addMessage("Hi, Friend", selfKey)
    val lastSelfMessage: String = "Hello?"
    addMessage(lastSelfMessage, selfKey)

    assertEquals(db.lastMessages.toBlocking.first(friendKey).message, lastSelfMessage)

    val lastFriendMessage: String = "Hello"
    addMessage(lastFriendMessage, friendKey)

    assertEquals(db.lastMessages.toBlocking.first(friendKey).message, lastFriendMessage)
  }

}