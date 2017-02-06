package chat.tox.antox.utils

import android.content.Context
import android.preference.PreferenceManager
import android.text.InputType


object KeyboardOptions {
  def getInputType(ctx: Context): Int = {
    val preferences = PreferenceManager.getDefaultSharedPreferences(ctx)
    val hotkey = preferences.getString("keyboard_hotkey", "emoji")
    var defaultInputTypes = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_CLASS_TEXT
    if (preferences.getBoolean("autocorrect", true)) {
      defaultInputTypes = defaultInputTypes | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
    }

    hotkey match {
      case "emote" =>
        defaultInputTypes | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE

      case "return" =>
        defaultInputTypes | InputType.TYPE_TEXT_FLAG_MULTI_LINE

      case "send" => defaultInputTypes

      case _ => -1

    }
  }
}
