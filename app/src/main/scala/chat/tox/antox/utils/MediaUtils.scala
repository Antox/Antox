package chat.tox.antox.utils

import android.content.Context
import android.media.{AudioManager, MediaPlayer}

object MediaUtils {
  def setupSound(context: Context, resourceId: Int, streamType: Int, looping: Boolean): MediaPlayer = {
    val sound = new MediaPlayer()
    val assetFileDescriptor = context.getResources.openRawResourceFd(resourceId)
    sound.setDataSource(assetFileDescriptor.getFileDescriptor, assetFileDescriptor.getStartOffset, assetFileDescriptor.getLength)
    assetFileDescriptor.close()
    sound.setAudioStreamType(AudioManager.STREAM_VOICE_CALL)
    sound.setLooping(looping)
    sound.prepareAsync()
    sound
  }
}
