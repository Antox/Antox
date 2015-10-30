package chat.tox.antox.tests

import android.support.test.runner.AndroidJUnit4
import android.test.AndroidTestCase
import chat.tox.antox.tox.MessageHelper
import org.junit.runner.RunWith

@RunWith(classOf[AndroidJUnit4])
class MessageHelperTest extends AndroidTestCase {

  def testSplitMessage(): Unit = {
    val result = MessageHelper.splitMessage("弓形虫移动是未来" * 100)
    println(result)
  }
}
