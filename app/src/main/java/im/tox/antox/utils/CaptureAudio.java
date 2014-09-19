package im.tox.antox.utils;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.util.Log;

import im.tox.antox.tox.ToxSingleton;

/**
 * Created by soft on 18/09/14.
 */
public class CaptureAudio extends AsyncTask<String, Void, Void> {
    @Override
    protected Void doInBackground(String... params) {
        ToxSingleton toxSingleton = ToxSingleton.getInstance();
        int bufferSizeBytes = AudioRecord.getMinBufferSize(48000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        AudioRecord audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                48000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSizeBytes);

        audioRecord.startRecording();

        while(true) {
            if(isCancelled())
                break;

            try {
                byte[] buffer = new byte[bufferSizeBytes];
                int read = audioRecord.read(buffer, 0, bufferSizeBytes);
                toxSingleton.jTox.avSendAudio(Integer.parseInt(params[0]), buffer);
                Log.d("Mic", "Sending audio to:" + params[0]);
            } catch (Exception e) {
            }
        }

        audioRecord.stop();
        audioRecord.release();

        return null;
    }
}
