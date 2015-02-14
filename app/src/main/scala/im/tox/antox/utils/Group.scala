package im.tox.antox.utils

import java.util

class Group(val id: String,
            val groupNumber: Int,
            val title: String,
            val alias: String,
            val topic: String,
            val peers: PeerList) {

  override def toString: String = title
}
