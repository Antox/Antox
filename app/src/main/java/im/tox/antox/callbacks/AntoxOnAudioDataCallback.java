package im.tox.antox.callbacks;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

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
            AudioTrack audioTrack = new AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    48000,
                    AudioFormat.CHANNEL_CONFIGURATION_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    data.length,
                    AudioTrack.MODE_STATIC);

            audioTrack.play();
            audioTrack.write(data, 0, data.length);
            audioTrack.stop();
            audioTrack.flush();

        } catch (Exception e) {
        }
    }
}
