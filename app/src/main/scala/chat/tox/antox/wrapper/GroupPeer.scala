package chat.tox.antox.wrapper

import chat.tox.antox.utils.PeerKey
import im.tox.tox4j.core.data.ToxNickname

class GroupPeer(var key: PeerKey,
                var name: ToxNickname,
                var ignored: Boolean) {
}
