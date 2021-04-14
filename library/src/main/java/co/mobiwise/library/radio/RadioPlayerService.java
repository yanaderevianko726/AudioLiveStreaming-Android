package co.mobiwise.library.radio;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.spoledge.aacdecoder.MultiPlayer;
import com.spoledge.aacdecoder.PlayerCallback;

import java.util.ArrayList;
import java.util.List;

public class RadioPlayerService extends Service implements PlayerCallback {

    public final IBinder mLocalBinder = new LocalBinder();
    List<RadioListener> mListenerList;

    private static boolean isLogging = false;
    private State mRadioState;
    private String mRadioUrl;
    private MultiPlayer mRadioPlayer;

    private boolean isSwitching;
    private boolean isInterrupted;
    private boolean mLock;

    PhoneStateListener phoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            if (state == TelephonyManager.CALL_STATE_RINGING) {

                if (isPlaying()) {
                    isInterrupted = true;
                    stop();
                }
            }
            else if (state == TelephonyManager.CALL_STATE_IDLE) {

                if (isInterrupted)
                    play(mRadioUrl);
            }
            else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {

                if (isPlaying()) {
                    isInterrupted = true;
                    stop();
                }
            }
            super.onCallStateChanged(state, incomingNumber);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return mLocalBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mListenerList = new ArrayList<>();

        mRadioState = State.IDLE;
        isSwitching = false;
        isInterrupted = false;
        mLock = false;
        getPlayer();

        TelephonyManager mTelephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if (mTelephonyManager != null)
            mTelephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    public void play(String mRadioUrl) {

        notifyRadioLoading();

        if (checkSuffix(mRadioUrl))
            decodeStremLink(mRadioUrl);
        else {
            this.mRadioUrl = mRadioUrl;
            isSwitching = false;

            if (isPlaying()) {
                log("Switching Radio");
                isSwitching = true;
                stop();
            } else if (!mLock) {
                log("Play requested.");
                mLock = true;
                getPlayer().playAsync(mRadioUrl);
            }
        }
    }

    public void stop() {
        if (!mLock && mRadioState != State.STOPPED) {
            log("Stop requested.");
            mLock = true;
            getPlayer().stop();
        }
    }

    @Override
    public void playerStarted() {
        mRadioState = State.PLAYING;
        mLock = false;
        notifyRadioStarted();

        log("Player started. tate : " + mRadioState);

        if (isInterrupted)
            isInterrupted = false;

    }

    public boolean isPlaying() {
        return State.PLAYING == mRadioState;
    }

    @Override
    public void playerPCMFeedBuffer(boolean b, int i, int i1) {
        //Empty
    }

    @Override
    public void playerStopped(int i) {

        mRadioState = State.STOPPED;

        mLock = false;
        notifyRadioStopped();
        log("Player stopped. State : " + mRadioState);

        if (isSwitching)
            play(mRadioUrl);


    }

    @Override
    public void playerException(Throwable throwable) {
        mLock = false;
        mRadioPlayer = null;
        getPlayer();
        notifyErrorOccured();
        log("ERROR OCCURED.");
    }

    @Override
    public void playerMetadata(String s, String s2) {
        notifyMetaDataChanged(s, s2);
    }

    @Override
    public void playerAudioTrackCreated(AudioTrack audiotrack) {
        int audioSessionId = audiotrack.getAudioSessionId();
        for (RadioListener mRadioListener : mListenerList) {
            mRadioListener.onAudioSessionId(audioSessionId);
        }
    }

    public void registerListener(RadioListener mListener) {
        mListenerList.add(mListener);
    }

    private void notifyRadioStarted() {
        for (RadioListener mRadioListener : mListenerList) {
            mRadioListener.onRadioStarted();
        }
    }

    private void notifyRadioStopped() {
        for (RadioListener mRadioListener : mListenerList)
            mRadioListener.onRadioStopped();
    }

    private void notifyMetaDataChanged(String s, String s2) {
        for (RadioListener mRadioListener : mListenerList)
            mRadioListener.onMetaDataReceived(s, s2);
    }

    private void notifyRadioLoading() {
        for (RadioListener mRadioListener : mListenerList) {
            mRadioListener.onRadioLoading();
        }
    }

    private void notifyErrorOccured() {
        for (RadioListener mRadioListener : mListenerList) {
            mRadioListener.onError();
        }
    }

    private MultiPlayer getPlayer() {
        try {

            java.net.URL.setURLStreamHandlerFactory(new java.net.URLStreamHandlerFactory() {

                public java.net.URLStreamHandler createURLStreamHandler(String protocol) {
                    Log.d("LOG", "Asking for stream handler for protocol: '" + protocol + "'");
                    if ("icy".equals(protocol))
                        return new com.spoledge.aacdecoder.IcyURLStreamHandler();
                    return null;
                }
            });
        } catch (Throwable t) {
            Log.w("LOG", "Cannot set the ICY URLStreamHandler - maybe already set ? - " + t);
        }

        if (mRadioPlayer == null) {
            int AUDIO_BUFFER_CAPACITY_MS = 800;
            int AUDIO_DECODE_CAPACITY_MS = 400;
            mRadioPlayer = new MultiPlayer(this, AUDIO_BUFFER_CAPACITY_MS, AUDIO_DECODE_CAPACITY_MS);
            mRadioPlayer.setResponseCodeCheckEnabled(false);
            mRadioPlayer.setPlayerCallback(this);
        }
        return mRadioPlayer;
    }

    public boolean checkSuffix(String streamUrl) {
        String SUFFIX_PLS = ".pls";
        String SUFFIX_RAM = ".ram";
        String SUFFIX_WAX = ".wax";
        return streamUrl.contains(SUFFIX_PLS) ||
                streamUrl.contains(SUFFIX_RAM) ||
                streamUrl.contains(SUFFIX_WAX);
    }

    public void setLogging(boolean logging) {
        isLogging = logging;
    }

    private void log(String log) {
        if (isLogging)
            Log.v("RadioManager", "RadioPlayerService : " + log);
    }

    @SuppressLint("StaticFieldLeak")
    private void decodeStremLink(String streamLink) {
        new StreamLinkDecoder(streamLink) {
            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                play(s);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public enum State {
        IDLE,
        PLAYING,
        STOPPED,
    }

    class LocalBinder extends Binder {
        RadioPlayerService getService() {
            return RadioPlayerService.this;
        }
    }
}
