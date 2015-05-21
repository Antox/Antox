package im.tox.antox.callbacks

import android.content.Context
import im.tox.antox.data.AntoxDB
import im.tox.antox.tox.ToxSingleton

class AntoxOnGroupTopicChangeCallback(private var ctx: Context) /* extends GroupTopicChangeCallback */ {
  /* override */ def groupTopicChange(groupNumber: Int, peerNumber: Int, topic: Array[Byte]): Unit = {
    val group = ToxSingleton.getGroup(groupNumber)
    group.topic = new String(topic, "UTF-8")

    val db = new AntoxDB(ctx)
    db.updateContactStatusMessage(group.id, group.topic)
    db.close()
    ToxSingleton.updateGroupList(ctx)
  }
}