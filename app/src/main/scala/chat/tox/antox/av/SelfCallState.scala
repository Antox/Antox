package chat.tox.antox.av

import im.tox.tox4j.av.data.BitRate
import im.tox.tox4j.av.enums.ToxavFriendCallState

object SelfCallState {
  // these bitrates act as the default bitrates
  val DEFAULT = SelfCallState(BitRate.unsafeFromInt(42), BitRate.unsafeFromInt(800), audioMuted = false, loudspeakerEnabled = false, videoHidden = false, receivingAudio = false, receivingVideo = false, ended = false)

  def fromToxCallState(state: Set[ToxavFriendCallState], callState: SelfCallState): SelfCallState = {
    callState.copy(receivingAudio = state.contains(ToxavFriendCallState.SENDING_A),
      receivingVideo = state.contains(ToxavFriendCallState.SENDING_V))
  }
}

case class SelfCallState(audioBitRate: BitRate, videoBitRate: BitRate,
                         audioMuted: Boolean, loudspeakerEnabled: Boolean, videoHidden: Boolean,
                         receivingAudio: Boolean, receivingVideo: Boolean, ended: Boolean) {

  def sendingAudio = audioBitRate.value > 0 && !audioMuted

  def sendingVideo = videoBitRate.value > 0 && !videoHidden
}