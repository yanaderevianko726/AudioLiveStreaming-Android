package com.partyfm.radio.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.UiModeManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.media.AudioAttributesCompat;
import androidx.media.MediaBrowserServiceCompat;

import com.partyfm.radio.Config;
import com.partyfm.radio.R;
import com.partyfm.radio.fragments.FragmentRadio;
import com.partyfm.radio.utilities.AudioFocusAwarePlayer;
import com.partyfm.radio.utilities.AudioFocusHelper;
import com.partyfm.radio.utilities.AudioFocusRequestCompat;
import com.partyfm.radio.utilities.Station;

import java.util.List;
import java.util.Objects;

import co.mobiwise.library.radio.RadioListener;
import co.mobiwise.library.radio.RadioManager;

import static android.media.AudioAttributes.CONTENT_TYPE_MUSIC;
import static android.media.AudioAttributes.USAGE_MEDIA;
import static android.view.View.VISIBLE;
import static com.partyfm.radio.Config.ACTION_METADATA_CHANGED;
import static com.partyfm.radio.Config.ACTION_PLAYBACK_STATE_CHANGED;
import static com.partyfm.radio.Config.EXTRA_ERROR_OCCURRED;
import static com.partyfm.radio.Config.EXTRA_STATION;
import static com.partyfm.radio.Config.PLAYBACK_STATE_LOADING_STATION;
import static com.partyfm.radio.Config.PLAYBACK_STATE_STOPPED;
import static com.partyfm.radio.Config.PLAYER_SERVICE_NOTIFICATION_ID;
import static com.partyfm.radio.Config.PREF_STATION_URL;
import static com.partyfm.radio.Config.PREF_STATION_URL_LAST;
import static com.partyfm.radio.activities.MainActivity.mCurrentStation;
import static com.partyfm.radio.activities.MainActivity.mStationIndex;
import static com.partyfm.radio.activities.MainActivity.mStationList;
import static com.partyfm.radio.activities.MainActivity.urlToPlay;

public class PlayerService extends MediaBrowserServiceCompat implements RadioListener, AudioFocusAwarePlayer {

    /* Define log tag */
    private static final String LOG_TAG = PlayerService.class.getSimpleName();

    private static Station mStation;

    @SuppressLint("StaticFieldLeak")
    public static RadioManager radioManager;
    static int audioSessionID = 0;

    private static MediaSessionCompat mSession;
    private static MediaControllerCompat mController;
    private AudioFocusHelper mAudioFocusHelper;
    private AudioFocusRequestCompat mAudioFocusRequest;
    private HeadphoneUnplugReceiver mHeadphoneUnplugReceiver;
    private WifiManager.WifiLock mWifiLock;
    private PowerManager.WakeLock mWakeLock;

    private int errorCount = 0;
    private static int RETRY_MAX = 2;
    private static int RETRY_INTERVAL = 7000;

    private AudioManager mAudioManager;

    public static boolean soundMute = false;
    private String mTitle = "";

    private static boolean closedApp = false;
    private boolean isPlaying = false;
    private boolean isClickedOnUI = false;
    private boolean isExitNotifi = false;

    private Bitmap artImage = null;

    private Notification mNotification;
    public static final String NOTIFICATION_CHANNEL_ID_PLAYBACK_CHANNEL = "notificationChannelIdPlaybackChannel";
    private static final int NOTIFICATION_ID = 1;

    private NotificationCompat.Builder buildNotification(String subTitle, boolean isMute, boolean isPlaying) {

        Intent intentPlayPause = new Intent(Config.MEDIA_NOTIFI_PLAY_PAUSE);
        PendingIntent playPausePending = PendingIntent.getService(this, 0, intentPlayPause, 0);

        Intent intentOpenPlayer = new Intent(getPackageManager().getLaunchIntentForPackage(getPackageName()));
        intentOpenPlayer.setAction(Config.MEDIA_NOTIFI_PLAY);
        PendingIntent openPending = PendingIntent.getActivity(this, 0, intentOpenPlayer, 0);

        Intent intentMute = new Intent(Config.MEDIA_NOTIFI_MUTE);
        PendingIntent MutePending = PendingIntent.getService(this, 0, intentMute, 0);

        Intent intentEixt = new Intent(Config.MEDIA_NOTIFI_EXIT);
        PendingIntent exitPending = PendingIntent.getService(this, 0, intentEixt, 0);

        RemoteViews remoteViews = new RemoteViews(this.getPackageName(), R.layout.notification);

        if (artImage == null)
            artImage = BitmapFactory.decodeResource(getResources(), R.drawable.ic_icon);

        remoteViews.setTextViewText(R.id.notification_line_one, getString(R.string.app_name));
        remoteViews.setTextViewText(R.id.notification_line_two, subTitle);
        remoteViews.setImageViewResource(R.id.notification_play, isPlaying ? R.drawable.btn_playback_pause : R.drawable.btn_playback_play);
        remoteViews.setImageViewResource(R.id.notification_mute, soundMute ? R.drawable.ic_lib_mute : R.drawable.ic_lib_unmute);
        remoteViews.setImageViewBitmap(R.id.notification_image, artImage);
        remoteViews.setViewVisibility(R.id.notification_exit, VISIBLE);

        remoteViews.setOnClickPendingIntent(R.id.notification_play, playPausePending);
        remoteViews.setOnClickPendingIntent(R.id.notification_mute, MutePending);
        remoteViews.setOnClickPendingIntent(R.id.notification_exit, exitPending);

        NotificationCompat.Builder notificationBuilder;
        notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID_PLAYBACK_CHANNEL);
        notificationBuilder.setSmallIcon(R.drawable.ic_icon);
        notificationBuilder.setContentIntent(openPending);
        notificationBuilder.setPriority(Notification.PRIORITY_DEFAULT);
        notificationBuilder.setContent(remoteViews);
        notificationBuilder.setOngoing(true);
        notificationBuilder.setShowWhen(false);

        return notificationBuilder;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // API level 26 ("Android O") supports notification channels.
            String id = NOTIFICATION_CHANNEL_ID_PLAYBACK_CHANNEL;
            CharSequence name = getString(R.string.notification_channel_playback_name);
            String description = getString(R.string.notification_channel_playback_description);
            int importance = NotificationManager.IMPORTANCE_LOW;

            // create channel
            NotificationChannel channel = new NotificationChannel(id, name, importance);
            channel.setDescription(description);

            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.createNotificationChannel(channel);
        }
    }

    private void showNotification(String subTitle, boolean isMute, boolean isPlaying){

        createNotificationChannel();

        mNotification = buildNotification(subTitle, isMute, isPlaying).build();
        mNotification.flags = Notification.FLAG_AUTO_CANCEL;

        startForeground(NOTIFICATION_ID, mNotification);
    }

    private void updateNotification(String subTitle, boolean isMute, boolean isPlaying){

        mNotification = buildNotification(subTitle, isMute, isPlaying).build();
        mNotification.flags = Notification.FLAG_AUTO_CANCEL;

        // display updated notification
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, mNotification);
    }

    @Override
    public void onCreate(){
        super.onCreate();

        // set up variables
        mSession = createMediaSession(this);

        // create audio focus helper
        mAudioFocusHelper = new AudioFocusHelper(this);

        // create audio focus request
        mAudioFocusRequest = createFocusRequest();

        // create Wifi and wake locks
        mWifiLock = ((WifiManager) Objects.requireNonNull(this.getSystemService(Context.WIFI_SERVICE))).createWifiLock(WifiManager.WIFI_MODE_FULL, "Transistor:wifi_lock");
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        assert powerManager != null;
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PartyFM:wake_lock");

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        assert mAudioManager != null;
        mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);

        // create media controller
        try {
            mController = new MediaControllerCompat(getApplicationContext(), mSession.getSessionToken());
        } catch (RemoteException e) {
            Log.e(LOG_TAG, "RemoteException: " + e);
            e.printStackTrace();
        }

        createRadioManager();
    }

    private void createRadioManager() {
        radioManager = RadioManager.with(this);
        if (!radioManager.isConnected()) {
            radioManager.connect();
            radioManager.registerListener(this);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if (SERVICE_INTERFACE.equals(intent.getAction())) {
            return super.onBind(intent);
        } else {
            return null;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        isClickedOnUI = true;
        // Checking for empty intent
        if (intent == null) {
            Log.v(LOG_TAG, "Null-Intent received. Stopping self.");
            stopForeground(true); // Remove notification
        }
        // ACTION PLAY
        else if (Objects.equals(intent.getAction(), Config.MEDIA_ACTION_PLAY)) {
            // reset current station if necessary
            if (mStation != null && mStation.getPlaybackState() != PLAYBACK_STATE_STOPPED) {
                mStation.resetState();
                // send local broadcast: stopped
                Intent intentStateBuffering = new Intent();
                intentStateBuffering.setAction(ACTION_PLAYBACK_STATE_CHANGED);
                intentStateBuffering.putExtra(EXTRA_STATION, mStation);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intentStateBuffering);
            }

            mStation = mCurrentStation;
            // update controller - start playback
            mController.getTransportControls().play();

        }
        // ACTION STOP
        else if (Objects.equals(intent.getAction(), Config.MEDIA_ACTION_STOP)) {

            mController.getTransportControls().pause();
        }

        else if(intent.getAction().equals(Config.MEDIA_ACTION_DISMISS)){
            closedApp = true;
        }

        else if(intent.getAction().equals(Config.MEDIA_ACTION_RESUMED)){
            closedApp = false;
        }

        else if(intent.getAction().equals(Config.MEDIA_ACTION_MUTE)){
            mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
            soundMute = true;
            buildNotification(mTitle, soundMute, isPlaying);
        }

        else if(intent.getAction().equals(Config.MEDIA_ACTION_UNMUTE)){
            mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
            soundMute = false;
            buildNotification(mTitle, soundMute, isPlaying);
        }

        else if (intent.getAction().equals(Config.MEDIA_NOTIFI_PLAY_PAUSE)) {

            if (mStation != null && mStation.getPlaybackState() != PLAYBACK_STATE_STOPPED) {
                FragmentRadio.onStopRadioStream();
                if(closedApp){
                    mController.getTransportControls().stop();
                }else{
                    mController.getTransportControls().pause();
                }
                updateNotification(getString(R.string.stopping), soundMute, false);
            } else if (mStation != null){
                FragmentRadio.onStartRadioStream();
                mController.getTransportControls().play();
                updateNotification(getString(R.string.connecting), soundMute, true);
            }
        }

        else if(intent.getAction().equals(Config.MEDIA_NOTIFI_MUTE)){
            if(soundMute){
                FragmentRadio.onMute(false);
                soundMute = false;
            }else{
                FragmentRadio.onMute(true);
                soundMute = true;
            }
            updateNotification(mTitle, soundMute, isPlaying);
        }

        else if(intent.getAction().equals(Config.MEDIA_NOTIFI_EXIT)){


            FragmentRadio.onStopRadioStream();
            updateNotification(getString(R.string.stopping), soundMute, false);
            if(isPlaying()){
                isExitNotifi = true;
                radioManager.stopRadio();
            }else{
                stopForeground(true);
            }

        }

        return START_STICKY;
    }

    /**
     * Inner class: Handles callback from active media session ***
     */
    private final class MediaSessionCallback extends MediaSessionCompat.Callback  {
        @Override
        public void onPlay() {
            // start playback
            if (mStation != null) {
                startPlayback();
            }
        }

        @Override
        public void onPause() {
            // stop playback and keep notification
            stopPlayback(false);
        }

        @Override
        public void onStop() {
            // stop playback and remove notification
            stopPlayback(true);
        }

        @Override
        public void onPlayFromSearch(String query, Bundle extras) {

        }

        @Override
        public void onSkipToNext() {
            if (mStation != null) {
                onNext();
            }
        }

        @Override
        public void onSkipToPrevious() {
            if (mStation != null) {
                onPrevious();
            }
        }
    }

    /* Starts playback */
    private void startPlayback() {

        // check for null - can happen after a crash during playback
        if (mStation == null || radioManager == null ||  mSession == null) {
            saveAppState();
            // send local broadcast: playback stopped
            Intent intent = new Intent();
            intent.setAction(ACTION_PLAYBACK_STATE_CHANGED);
            intent.putExtra(EXTRA_ERROR_OCCURRED, true);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            // stop player service
            stopSelf();
            return;
        }

        // set and save state
        mStation.resetState();
        mStation.setPlaybackState(PLAYBACK_STATE_LOADING_STATION);
        saveAppState();

        // acquire Wifi and wake locks
        if (!mWifiLock.isHeld()) {
            mWifiLock.acquire();
        }
        if (!mWakeLock.isHeld()) {
            mWakeLock.acquire(10*60*1000L /*10 minutes*/); // needs android.permission.WAKE_LOCK
        }

        // request audio focus and initialize media mPlayer
        if (mStation.getStreamUri() != null && mAudioFocusHelper.requestAudioFocus(mAudioFocusRequest)) {
            // initialize player and start playback
            isPlaying = true;
            startPlaying();

            showNotification(getString(R.string.connecting), soundMute, false);

            // update MediaSession
            updateMediaSession(mStation,true);
        }

//        send local broadcast: buffering
        Intent intent = new Intent();
        intent.setAction(ACTION_PLAYBACK_STATE_CHANGED);
        intent.putExtra(EXTRA_STATION, mStation);
        LocalBroadcastManager.getInstance(this.getApplication()).sendBroadcast(intent);
        Log.v(LOG_TAG, "LocalBroadcast: ACTION_PLAYBACK_STATE_CHANGED -> PLAYBACK_STATE_LOADING_STATION");

        // register headphone listener
        IntentFilter headphoneUnplugIntentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        mHeadphoneUnplugReceiver = new HeadphoneUnplugReceiver();
        registerReceiver(mHeadphoneUnplugReceiver, headphoneUnplugIntentFilter);
    }

    /* Stops playback */
    private void stopPlayback(boolean dismissNotification) {
        // check for null - can happen after a crash during playback
        if (mStation == null || radioManager == null || mSession == null) {
            Log.e(LOG_TAG, "Stopping playback. An error occurred. Station is probably NULL.");
            saveAppState();
            // send local broadcast: playback stopped
            Intent intent = new Intent();
            intent.setAction(ACTION_PLAYBACK_STATE_CHANGED);
            intent.putExtra(EXTRA_ERROR_OCCURRED, true);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            // unregister headphone listener
            unregisterHeadphoneUnplugReceiver();
            // stop player service
            stopSelf();
            return;
        }

        // reset and save state
        mStation.resetState();
        saveAppState();

        // release Wifi and wake locks
        if (mWifiLock.isHeld()) {
            mWifiLock.release();
        }
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }

        stopPlaying();

        mAudioFocusHelper.abandonAudioFocus();

        if (dismissNotification) {
            // remove the foreground lock (dismisses notification) and don't keep media session active
            stopForeground(true);
            // update media session
            updateMediaSession(mStation, false);
        } else {
            // remove the foreground lock and update notification (make it swipe-able)
//            NotificationHelper.update(this, mStation, mSession);
            // update media session
            updateMediaSession(mStation, true);
        }

        updateNotification(getString(R.string.stopping), soundMute, false);

        // send local broadcast: playback stopped
        Intent intent = new Intent();
        intent.setAction(ACTION_PLAYBACK_STATE_CHANGED);
        intent.putExtra(EXTRA_STATION, mStation);
        LocalBroadcastManager.getInstance(this.getApplication()).sendBroadcast(intent);
        Log.v(LOG_TAG, "LocalBroadcast: ACTION_PLAYBACK_STATE_CHANGED -> PLAYBACK_STATE_STOPPED");

        // unregister headphone listener
        unregisterHeadphoneUnplugReceiver();
    }

    private void onNext(){
        if(mStationIndex == 0) mStationIndex = 1;
        else mStationIndex = 0;

        mCurrentStation = mStationList.get(mStationIndex);
        mStation = mCurrentStation;

        startPlayback();
    }

    private void onPrevious(){
        if(mStationIndex == 0) mStationIndex = 1;
        else mStationIndex = 0;

        mCurrentStation = mStationList.get(mStationIndex);
        mStation = mCurrentStation;

        startPlayback();
    }

    /* Creates request for AudioFocus */
    private AudioFocusRequestCompat createFocusRequest() {
        // build audio attributes
        @SuppressLint("WrongConstant") AudioAttributesCompat audioAttributes = new AudioAttributesCompat.Builder()
                .setUsage(USAGE_MEDIA)
                .setContentType(CONTENT_TYPE_MUSIC)
                .build();

        // built and return focus request
        return new AudioFocusRequestCompat.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setOnAudioFocusChangeListener(mAudioFocusHelper.getListenerForPlayer(this))
                .setAudioAttributes(audioAttributes)
                .setFocusGain(AudioManager.AUDIOFOCUS_GAIN)
                .setWillPauseWhenDucked(false)
                .setAcceptsDelayedFocusGain(false) // todo check if this flag can be turned on (true)
                .build();
    }

    /* Creates media session */
    private MediaSessionCompat createMediaSession(Context context) {
        // create a media session
        MediaSessionCompat session = new MediaSessionCompat(context, LOG_TAG);
        session.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        session.setPlaybackState(createSessionPlaybackState());
        session.setCallback(new MediaSessionCallback());
        setSessionToken(session.getSessionToken());

        return session;
    }

    /* Creates playback state */
    private PlaybackStateCompat createSessionPlaybackState() {

        long skipActions;
        if (isCarUiMode()) {
            skipActions = PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
        } else {
//            skipActions = 0;
            skipActions = PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
        }

        if (mStation == null || mStation.getPlaybackState() == PLAYBACK_STATE_STOPPED) {
            // define action for playback state to be used in media session callback
            return new PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_STOPPED, 0, 0)
                    .setActions(PlaybackStateCompat.ACTION_PLAY | skipActions)
                    .build();
        } else {
            // define action for playback state to be used in media session callback - car mode version
            return new PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_PLAYING, 0, 0)
                    .setActions(PlaybackStateCompat.ACTION_STOP | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID | skipActions)
                    .build();
        }
    }

    /* Detects car mode */
    private boolean isCarUiMode() {
        UiModeManager uiModeManager = (UiModeManager) this.getSystemService(Context.UI_MODE_SERVICE);
        assert uiModeManager != null;
        if (uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_CAR) {
            Log.v(LOG_TAG, "Running in Car mode");
            return true;
        } else {
            Log.v(LOG_TAG, "Running on a non-Car mode");
            return false;
        }
    }

    /* unregister headphone listener */
    private void unregisterHeadphoneUnplugReceiver() {
        try {
            this.unregisterReceiver(mHeadphoneUnplugReceiver);
        } catch (Exception e) {
            Log.v(LOG_TAG, "Unable to unregister HeadphoneUnplugReceiver");
            // e.printStackTrace();
        }
    }

    /* Saves state of playback */
    private void saveAppState() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplication());
        SharedPreferences.Editor editor = settings.edit();
        if (mStation == null) {
            editor.putString(PREF_STATION_URL, "");
        } else if (mStation.getPlaybackState() == PLAYBACK_STATE_STOPPED) {
            editor.putString(PREF_STATION_URL, "");
            editor.putString(PREF_STATION_URL_LAST, mStation.getStreamUri());
        } else {
            editor.putString(PREF_STATION_URL, mStation.getStreamUri());
            editor.putString(PREF_STATION_URL_LAST, mStation.getStreamUri());
        }
        editor.apply();
        Log.v(LOG_TAG, "Saving state.");
    }

    /* Updates station in MediaSession and state of MediaSession */
    private void updateMediaSession(Station station, boolean activeState) {
        mSession.setPlaybackState(createSessionPlaybackState());
        mSession.setMetadata(getSessionMetadata(getApplicationContext(), station));
        mSession.setActive(activeState);
    }

    /* Creates the metadata needed for MediaSession */
    private MediaMetadataCompat getSessionMetadata(Context context, Station station) {
        Bitmap stationImage = null;
        String albumTitle = context.getResources().getString(R.string.app_name);

        // log metadata change
        Log.v(LOG_TAG, "New Metadata available.");

        String stationName = getString(R.string.station_name_low);
        if(station.getStreamUri().equals(Config.RADIO_STREAM_URL_HIGH)){
            stationName = getString(R.string.station_name_high);
        }

        return new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, stationName)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, station.getMetadata())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, albumTitle)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, "")
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, stationImage)
                .build();
    }

    public boolean isPlaying() {
        return (null != radioManager && null != RadioManager.getService() && RadioManager.getService().isPlaying());
    }

    @Override
    public void play() {

    }

    @Override
    public void pause() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void setVolume(float volume) {

    }

    public class HeadphoneUnplugReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (mStation.getPlaybackState() != PLAYBACK_STATE_STOPPED) {
                Log.v(LOG_TAG, "Headphones unplugged. Stopping playback.");
                // stop playback
                mController.getTransportControls().pause();
                FragmentRadio.onStopRadioStream();
            }
        }
    }

    private void startPlaying() {
        AsyncTask.execute(() -> {
            radioManager.startRadio(urlToPlay);
        });
    }

    private void stopPlaying() {
        AsyncTask.execute(() -> {
            radioManager.stopRadio();
            isClickedOnUI = false;
            resetRadioManager();
        });
    }

    private void resetRadioManager() {
        try {
            radioManager.disconnect();
        } catch (Exception ignored) {
        }
        RadioManager.flush();
        radioManager = RadioManager.with(this);
        radioManager.connect();
        radioManager.registerListener(this);
    }

    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {

        return null;
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {

    }

    @Override
    public void onRadioLoading() {

    }

    @Override
    public void onRadioConnected() {

    }

    @Override
    public void onRadioStarted() {
        new Handler(Looper.getMainLooper()).post(() -> {
            isPlaying = true;
            updateNotification(mTitle, soundMute, isPlaying);
            FragmentRadio.onStartRadioStream();
        });
    }

    @Override
    public void onRadioStopped() {
        new Handler(Looper.getMainLooper()).post(() -> {
            isPlaying = false;
            if(!isExitNotifi){
                updateNotification(getString(R.string.stopped), soundMute, isPlaying);
            }else{
                isExitNotifi = false;
                stopForeground(true);
            }

            if(!isClickedOnUI){
                isClickedOnUI = true;
                FragmentRadio.onStopRadioStream();
            }
        });
    }

    @Override
    public void onMetaDataReceived(final String mKey, final String mTitle) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (mKey != null && (mKey.equals("StreamTitle") || mKey.equals("title")) && !mTitle.equals("")) {
                updateMediaInfoFromBackground(mTitle);
            }
        });
    }

    @Override
    public void onAudioSessionId(int i) {
        audioSessionID = i;
    }

    @Override
    public void onError() {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (errorCount < RETRY_MAX) {
                Handler handler = new Handler();
                handler.postDelayed(() -> {
                    errorCount += 1;
                    startPlaying();
                }, RETRY_INTERVAL);
            } else {

                resetRadioManager();
                FragmentRadio.loadingTV.setText("");
            }
        });
    }

    public void updateMediaInfoFromBackground(String title) {

        mTitle = title;

        FragmentRadio.setTitle(title);

        if (title == null || title.length() > 0 ) {
            mStation.setMetadata(title);
        } else {
            mStation.setMetadata(mStation.getMetadata());
        }

        // send local broadcast
        Intent i = new Intent();
        i.setAction(ACTION_METADATA_CHANGED);
        i.putExtra(EXTRA_STATION,  mStation);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);

        // update media session metadata
        mSession.setMetadata(getSessionMetadata(getApplicationContext(), mStation));

        updateNotification(mTitle, soundMute, isPlaying);
    }

}
