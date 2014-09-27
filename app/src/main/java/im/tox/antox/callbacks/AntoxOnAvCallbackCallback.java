package im.tox.antox.callbacks;

import android.content.Context;
import android.util.Log;

import im.tox.antox.tox.ToxSingleton;
import im.tox.antox.utils.AntoxFriend;
import im.tox.antox.utils.CaptureAudio;
import im.tox.jtoxcore.ToxAvCallbackID;
import im.tox.jtoxcore.ToxCallType;
import im.tox.jtoxcore.ToxCodecSettings;
import im.tox.jtoxcore.ToxException;
import im.tox.jtoxcore.callbacks.OnAvCallbackCallback;

/**
 * Created by Mark Winter on 03/09/14.
 */
public class AntoxOnAvCallbackCallback implements OnAvCallbackCallback<AntoxFriend> {

    private Context ctx;
    CaptureAudio captureAudio = new CaptureAudio();


    public AntoxOnAvCallbackCallback(Context ctx) {
        this.ctx = ctx;
    }

    public void execute(int callID, ToxAvCallbackID callbackID) {
        Log.d("OnAvCallbackCallback", "Received a callback from: " + callID);
        ToxSingleton toxSingleton = ToxSingleton.getInstance();

        try {
            switch (callbackID) {
            /* Requests */
                case ON_INVITE: // Incoming call request
                    Log.d("OnAvCallbackCallback", "Callback type: ON_INVITE");
                    // Display UI elements to accept or reject the call
                    // For testing auto-accept
                    ToxCodecSettings toxCodecSettings = new ToxCodecSettings(ToxCallType.TYPE_AUDIO, 500, 1280, 720, 64000, 20, 48000, 1);
                    toxSingleton.jTox.avAnswer(callID, toxCodecSettings);
                    break;

                case ON_START: // Incoming call was accepted
                    Log.d("OnAvCallbackCallback", "Callback type: ON_START");
                    // Prepare for transmission
                    toxSingleton.jTox.avPrepareTransmission(0, 3, 40, false);

                    // Start microphone task
                    captureAudio.execute(String.valueOf(callID));

                    break;

                case ON_CANCEL: // Incoming call timed out/stopped
                    Log.d("OnAvCallbackCallback", "Callback type: ON_CANCEL");
                    break;

                case ON_REJECT: // Incoming call was rejected
                    Log.d("OnAvCallbackCallback", "Callback type: ON_REJECT");
                    break;

                case ON_END: // On-going call has now been ended
                    Log.d("OnAvCallbackCallback", "Callback type: ON_END");
                    captureAudio.cancel(true);
                    break;


            /* Responses */
                case ON_RINGING: // Our call has gone through and is ringing
                    Log.d("OnAvCallbackCallback", "Callback type: ON_RINGING");
                    break;

                case ON_STARTING:
                    Log.d("OnAvCallbackCallback", "Callback type: ON_STARTING");
                    break;

                case ON_ENDING:
                    Log.d("OnAvCallbackCallback", "Callback type: ON_ENDING");
                    break;


            /* Protocol */
                case ON_REQUEST_TIMEOUT:
                    Log.d("OnAvCallbackCallback", "Callback type: ON_REQUEST_TIMEOUT");
                    break;

                case ON_PEER_TIMEOUT:
                    Log.d("OnAvCallbackCallback", "Callback type: ON_PEER_TIMEOUT");
                    break;

                case ON_MEDIA_CHANGE:
                    Log.d("OnAvCallbackCallback", "Callback type: ON_MEDIA_CHANGE");
                    break;
            }
        } catch (ToxException e) {
        }
    }
}
