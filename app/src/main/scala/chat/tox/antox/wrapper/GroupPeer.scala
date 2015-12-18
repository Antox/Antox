package chat.tox.antox.wrapper

import im.tox.tox4j.core.data.ToxNickname

class GroupPeer(var key: PeerKey,
                var name: ToxNickname,
                var ignored: Boolean) {
}
