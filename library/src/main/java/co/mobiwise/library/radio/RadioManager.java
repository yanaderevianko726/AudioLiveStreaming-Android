package co.mobiwise.library.radio;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class RadioManager implements IRadioManager {


    @SuppressLint("StaticFieldLeak")
    private static RadioManager instance = null;
    private static RadioPlayerService mService;

    private Context mContext;
    private List<RadioListener> mRadioListenerQueue;

    private static boolean isLogging = false;
    private boolean isServiceConnected;

    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName arg0, IBinder binder) {

            log("Service Connected.");

            mService = ((RadioPlayerService.LocalBinder) binder).getService();
            mService.setLogging(isLogging);
            isServiceConnected = true;

            if (!mRadioListenerQueue.isEmpty()) {
                for (RadioListener mRadioListener : mRadioListenerQueue) {
                    registerListener(mRadioListener);
                    mRadioListener.onRadioConnected();
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };

    private RadioManager(Context mContext) {
        this.mContext = mContext;
        mRadioListenerQueue = new ArrayList<>();
        isServiceConnected = false;
    }

    public static RadioManager with(Context mContext) {
        if (instance == null)
            instance = new RadioManager(mContext);
        return instance;
    }

    public static RadioPlayerService getService() {
        return mService;
    }

    public boolean isConnected() {
        return isServiceConnected;
    }

    public static void flush() {
        instance = null;
    }

    @Override
    public void startRadio(String streamURL) {
        mService.play(streamURL);
    }

    @Override
    public void stopRadio() {
        mService.stop();
    }

    @Override
    public void registerListener(RadioListener mRadioListener) {
        if (isServiceConnected)
            mService.registerListener(mRadioListener);
        else
            mRadioListenerQueue.add(mRadioListener);
    }

    @Override
    public void connect() {
        log("Requested to connect service.");
        Intent intent = new Intent(mContext, RadioPlayerService.class);
        mContext.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void disconnect() {
        log("Service Disconnected.");
        mContext.unbindService(mServiceConnection);
    }

    private void log(String log) {
        if (isLogging)
            Log.v("RadioManager", "RadioManagerLog : " + log);
    }
}
