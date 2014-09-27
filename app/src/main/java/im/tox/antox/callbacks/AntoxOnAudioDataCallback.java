package im.tox.antox.callbacks;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import im.tox.antox.utils.AntoxFriend;
import im.tox.jtoxcore.callbacks.OnAudioDataCallback;

/**
 * Created by Mark Winter on 03/09/14.
 */
public class AntoxOnAudioDataCallback implements OnAudioDataCallback<AntoxFriend> {

    private Context ctx;

    public AntoxOnAudioDataCallback(Context ctx) {
        this.ctx = ctx;
    }

    public void execute(int callID, byte[] data) {
        Log.d("OnAudioDataCallback", "Received callback from: " + callID);

        try {
            AudioTrack audioTrack = new  AudioTrack(
                    AudioManager.STREAM_VOICE_CALL,
                    48000,
                    AudioFormat.CHANNEL_CONFIGURATION_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    data.length,
                    AudioTrack.MODE_STREAM);
            audioTrack.play();
            audioTrack.write(data, 0, data.length);
            audioTrack.stop();
            audioTrack.release();
        } catch (Exception e) {
            Log.e("AudioPlayback", e.getMessage());
        }
    }
}
