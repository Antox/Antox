package chat.tox.antox.av

import im.tox.tox4j.av.enums.ToxavFriendCallState

object SelfCallState {
  val DEFAULT = SelfCallState(42, 0, audioMuted = false, videoHidden = false, receivingAudio = false, receivingVideo = false)

  def fromToxCallState(state: Set[ToxavFriendCallState], callState: SelfCallState): SelfCallState = {
    callState.copy(receivingAudio = state.contains(ToxavFriendCallState.SENDING_A),
                   receivingVideo = state.contains(ToxavFriendCallState.SENDING_V))
  }
}

case class SelfCallState(audioBitRate: Int, videoBitRate: Int,
                     audioMuted: Boolean, videoHidden: Boolean,
                     receivingAudio: Boolean, receivingVideo: Boolean) {

  def sendingAudio = audioBitRate > 0 && !audioMuted
  def sendingVideo = videoBitRate > 0 && !videoHidden
}