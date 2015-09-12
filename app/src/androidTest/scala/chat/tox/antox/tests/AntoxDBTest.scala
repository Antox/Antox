package chat.tox.antox.tests

import android.preference.PreferenceManager
import android.support.test.runner.AndroidJUnit4
import android.test.{AndroidTestCase, RenamingDelegatingContext}
import chat.tox.antox.data.AntoxDB
import chat.tox.antox.wrapper.{MessageType, ToxKey}
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
    db = new AntoxDB(context, "jim")
  }

  @After
  override def tearDown(): Unit = {
    super.tearDown()
  }

  val key = new ToxKey("828435142ACE09E8677427E6180BFB27E38FB589A3B84C24976AE49F80A69C68")
  val name = "Steve Appleseed"
  val alias = "Steve"
  val statusMessage = "This is my status"

  @Test
  def testAddFriend(): Unit = {
    db.addFriend(key, name, alias, statusMessage)

    db.friendList.subscribe(friendList => {
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
      println("GOT A MESSAGE CLLBACK")
      number += 1
    })

    val numMessages = 3
    db.addMessage(-1, key, "asdf", "test", hasBeenReceived = false, hasBeenRead = false, successfullySent = true, MessageType.FRIEND)
    db.addMessage(-1, key, "asdf", "test1", hasBeenReceived = false, hasBeenRead = false, successfullySent = true, MessageType.FRIEND)
    db.addMessage(-1, key, "asdf", "test2", hasBeenReceived = false, hasBeenRead = false, successfullySent = true, MessageType.FRIEND)

    db.lastMessages
      .subscribe(messages => {
      assertEquals(messages.size, numMessages)
      assert(messages.exists(_._2._1 eq "test"))

      println("GOT A MESSAGE CLLBACK")
      number += 1
    })

    assertEquals(number, numChanges)
  }

}