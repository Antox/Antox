package chat.tox.antox.tests

import android.preference.PreferenceManager
import android.support.test.runner.AndroidJUnit4
import android.test.{AndroidTestCase, RenamingDelegatingContext}
import chat.tox.antox.data.AntoxDB
import chat.tox.antox.utils.SelfKey
import chat.tox.antox.wrapper.FriendKey
import im.tox.tox4j.core.enums.ToxMessageType
import org.junit.Assert._
import org.junit.runner.RunWith
import org.junit.{After, Before, Test}

import scala.collection.mutable.ArrayBuffer

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

  val selfKey = new SelfKey("11BB3CCDD46346EAA76FF935F1CB31CDC11C56803F1077745124A1C7C63F7E6C8B286B415682")

  val key = new FriendKey("828435142ACE09E8677427E6180BFB27E38FB589A3B84C24976AE49F80A69C68")
  val name = "Steve Appleseed"
  val alias = "Steve"
  val statusMessage = "This is my status"

  @Test
  def testAddFriend(): Unit = {
    db.addFriend(key, name, alias, statusMessage)

    db.friendInfoList.subscribe(friendList => {
      assert(friendList.size == 1)
      assert(friendList.exists(_.name equals name))
      assert(friendList.exists(_.alias equals alias))
      assert(friendList.exists(_.getAliasOrName equals alias))
      assert(friendList.exists(_.statusMessage equals statusMessage))
      assert(friendList.exists(_.key equals key))
    })
  }

  @Test
  def testLastMessages(): Unit = {
    db.addFriend(key, name, alias, statusMessage)

    val numChanges = 5
    var number = 0
    db.messageListObservable(Some(key))
      .subscribe(messages => {
      assertEquals(messages, ArrayBuffer.empty)
      number += 1
    })

    val numMessages = 1
    db.addMessage(-1, key, selfKey, "asdf", "test", hasBeenReceived = false, hasBeenRead = false, successfullySent = true, ToxMessageType.NORMAL)

    db.lastMessages
      .subscribe(messages => {
      assertEquals(messages.size, numMessages)
      assert(messages.exists(_._1 == "test"))
      
      number += 1
    })

    assertEquals(number, numChanges)
  }

}