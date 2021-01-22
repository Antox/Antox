package chat.tox.antox.data

import android.content.Context
import chat.tox.antox.R

sealed abstract class CallEventKind(val kindId: Int, val imageRes: Int, private val messageRes: Int) {
  def message(context: Context): String = context.getResources.getString(messageRes)
}

object CallEventKind {

  case object Invalid extends CallEventKind(-1, 0, R.string.empty)

  case object Incoming extends CallEventKind(0, R.drawable.ic_phone_in_talk_black_18dp, R.string.call_event_incoming)

  case object Outgoing extends CallEventKind(1, R.drawable.ic_call_made_black_18dp, R.string.call_event_outgoing)

  case object Rejected extends CallEventKind(2, R.drawable.ic_call_end_black_18dp, R.string.call_event_rejected)

  case object Unanswered extends CallEventKind(3, R.drawable.ic_phone_missed_black_18dp, R.string.call_event_unanswered)

  case object Missed extends CallEventKind(4, R.drawable.ic_phone_missed_black_18dp, R.string.call_event_missed)

  case object Answered extends CallEventKind(5, R.drawable.ic_call_black_18dp, R.string.call_event_answered)

  case object Ended extends CallEventKind(6, R.drawable.ic_call_end_black_18dp, R.string.call_event_ended)

  case object Cancelled extends CallEventKind(7, R.drawable.ic_call_end_black_18dp, R.string.call_event_cancelled)

  val values: Set[CallEventKind] =
    Set(
      Invalid,
      Incoming,
      Outgoing,
      Rejected,
      Unanswered,
      Missed,
      Answered,
      Ended,
      Cancelled
    )
}
