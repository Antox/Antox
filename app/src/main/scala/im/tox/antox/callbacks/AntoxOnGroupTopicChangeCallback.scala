package im.tox.antox.callbacks

import android.content.Context
import im.tox.antox.tox.ToxSingleton
import im.tox.tox4j.core.callbacks.{GroupTopicChangeCallback, GroupPeerJoinCallback}

class AntoxOnGroupTopicChangeCallback(private var ctx: Context) extends GroupTopicChangeCallback {
  override def groupTopicChange(groupNumber: Int, peerNumber: Int, topic: Array[Byte]): Unit = {
    ToxSingleton.getGroupList.getByGroupNumber(groupNumber).get.topic = new String(topic, "UTF-8")
    ToxSingleton.updateGroupList(ctx)
  }
}