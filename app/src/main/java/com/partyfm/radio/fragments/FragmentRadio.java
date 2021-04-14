package com.partyfm.radio.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.mediarouter.media.MediaControlIntent;
import androidx.mediarouter.media.MediaRouteSelector;
import androidx.mediarouter.media.MediaRouter;

import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaLoadRequestData;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.Session;
import com.google.android.gms.cast.framework.SessionManager;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.partyfm.radio.R;
import com.partyfm.radio.Config;
import com.partyfm.radio.services.PlayerService;
import com.partyfm.radio.utilities.Station;
import com.partyfm.radio.utilities.StationListHelper;
import com.partyfm.radio.utilities.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import co.mobiwise.library.radio.RadioManager;

import static com.partyfm.radio.Config.ACTION_SHOW_PLAYER;
import static com.partyfm.radio.Config.EXTRA_LAST_STATION;
import static com.partyfm.radio.Config.EXTRA_STATION;
import static com.partyfm.radio.Config.EXTRA_STREAM_URI;
import static com.partyfm.radio.Config.PREF_STATION_URI_SELECTED;
import static com.partyfm.radio.Config.PREF_STATION_URL_LAST;
import static com.partyfm.radio.Config.RADIO_STREAM_URL_HIGH;
import static com.partyfm.radio.Config.stringPartyLink;
import static com.partyfm.radio.activities.MainActivity.mCurrentStation;
import static com.partyfm.radio.activities.MainActivity.mStationIndex;
import static com.partyfm.radio.activities.MainActivity.mStationList;
import static com.partyfm.radio.activities.MainActivity.urlToPlay;
import static com.partyfm.radio.services.PlayerService.radioManager;

public final class FragmentRadio extends Fragment{

    @SuppressLint("StaticFieldLeak")
    private static Activity activity;
    private LinearLayout linearLayout;
    @SuppressLint("StaticFieldLeak")
    public static TextView loadingTV;
    @SuppressLint("StaticFieldLeak")
    private static ImageView buttonPlay, buttonStopPlay;
    @SuppressLint("StaticFieldLeak")
    public static ImageView buttonMute;
    @SuppressLint("StaticFieldLeak")
    private static ImageView buttonLow, buttonHigh;
    @SuppressLint("StaticFieldLeak")
    private static TextView nowPlaying;

    private static boolean isStarted = false;
    public static boolean isMuteed = false;
    private static boolean internetConnection = false;

    private ConnectivityReceiver mConnectivityReceiver = new ConnectivityReceiver();
    private IntentFilter filterNetwork = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);

    private CastSession mCastSession;
    private CastContext castContext;
    static public MediaRouteSelector mediaSelector = null;

    private int castPlayerStatus;
    private RemoteMediaClient remoteMediaClient;

    private static PlayBackState mPlaybackState = PlayBackState.RADIO;
    public enum PlayBackState{
        PLAYING, RADIO, CAST_PLAYING, CAST_IDLE, IDLE
    }

    private SessionManagerListener sessionManagerListener = new SessionManagerListener() {
        @Override
        public void onSessionStarting(Session session) { }

        @Override
        public void onSessionStarted(Session session, String s) {
            mCastSession = (CastSession) session;
            Objects.requireNonNull(getActivity()).invalidateOptionsMenu();
            mPlaybackState = PlayBackState.CAST_IDLE;

            onCastStarted();
        }

        @Override
        public void onSessionStartFailed(Session session, int i) { }

        @Override
        public void onSessionEnding(Session session) { }

        @Override
        public void onSessionEnded(Session session, int i) {

            remoteMediaClient.stop();
            Objects.requireNonNull(getActivity()).invalidateOptionsMenu();

            onCastStopped();
        }

        @Override
        public void onSessionResuming(Session session, String s) {

        }

        @Override
        public void onSessionResumed(Session session, boolean b) {

        }

        @Override
        public void onSessionResumeFailed(Session session, int i) {

        }

        @Override
        public void onSessionSuspended(Session session, int i) {

        }
    };

    private void onCastStarted() {
        if (mCastSession != null) {

            if(isPlaying()){

                mPlaybackState = PlayBackState.CAST_PLAYING;

                Intent intent = new Intent(activity, PlayerService.class);
                intent.setAction(Config.MEDIA_ACTION_STOP);
                activity.startService(intent);

                loadRemoteMedia(true);
            }else{
                loadingTV.setText(R.string.ready_to_cast);
                loadRemoteMedia(false);
            }
        }
    }

    private void onCastStopped(){
        mPlaybackState = PlayBackState.IDLE;

        buttonPlay.setVisibility(View.VISIBLE);
        buttonStopPlay.setVisibility(View.INVISIBLE);
        buttonHigh.setImageResource(R.drawable.ic_high_bit);
        buttonLow.setImageResource(R.drawable.ic_low_bit);
        loadingTV.setText("");
        nowPlaying.setText("");
    }

    private void onCastPlayerStart(){
        buttonPlay.setVisibility(View.INVISIBLE);
        buttonStopPlay.setVisibility(View.VISIBLE);

        buttonLow.setImageResource(R.drawable.ic_low_bit);
        buttonHigh.setImageResource(R.drawable.ic_high_bit);
        if(mStationIndex == 0)buttonLow.setImageResource(R.drawable.ic_low_bit_active);
        if(mStationIndex == 1)buttonHigh.setImageResource(R.drawable.ic_high_bit_active);

        loadingTV.setText(R.string.casting_to_chromecast);
    }

    private void onCastPlayerStop(){
        buttonPlay.setVisibility(View.VISIBLE);
        buttonStopPlay.setVisibility(View.INVISIBLE);

        buttonLow.setImageResource(R.drawable.ic_low_bit);
        buttonHigh.setImageResource(R.drawable.ic_high_bit);

        loadingTV.setText(R.string.ready_to_cast);
        nowPlaying.setText("");
    }

    private boolean isPlaying() {
        return (null != radioManager && null != RadioManager.getService() && RadioManager.getService().isPlaying());
    }

    private void loadRemoteMedia(boolean autoPlay) {

        if(mCastSession != null){
            remoteMediaClient = mCastSession.getRemoteMediaClient();
            if(remoteMediaClient != null){
                remoteMediaClient.registerCallback(new RemoteMediaClient.Callback() {
                    @Override
                    public void onStatusUpdated() {

                        castPlayerStatus = remoteMediaClient.getPlayerState();
                        if(castPlayerStatus == MediaStatus.PLAYER_STATE_PLAYING){
                            onCastPlayerStart();
                        }else if(castPlayerStatus == MediaStatus.PLAYER_STATE_PAUSED){
                            onCastPlayerStop();
                        }
                    }
                });

                remoteMediaClient.load(new MediaLoadRequestData.Builder()
                        .setMediaInfo(buildMediaInfo())
                        .setAutoplay(autoPlay)
                        .setCurrentTime(0)
                        .build());

            }
        }
    }

    private MediaInfo buildMediaInfo() {
        MediaMetadata streamMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_TV_SHOW);

        streamMetadata.putString(MediaMetadata.KEY_SUBTITLE, "PartyFM");
        streamMetadata.putString(MediaMetadata.KEY_TITLE, "PartyFM");

        return new MediaInfo.Builder(mCurrentStation.getStreamUri())
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType("audio/mpeg")
                .setMetadata(streamMetadata)
                .setStreamDuration(1)
                .build();
    }

    private MediaRouter.Callback mediaRouterCallback = new MediaRouter.Callback() {
    };

    public FragmentRadio() {

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        activity = getActivity();
        Utils.isNetworkAvailable(Objects.requireNonNull(getActivity()), true);

        castContext = CastContext.getSharedInstance(Objects.requireNonNull(getActivity()));
        mCastSession = castContext.getSessionManager().getCurrentCastSession();

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        linearLayout = (LinearLayout) inflater.inflate(R.layout.fragment_radio_player, container, false);
        Objects.requireNonNull(getActivity()).registerReceiver(mConnectivityReceiver, filterNetwork);

        initializeUIElements();

        List<String> controlCategories = new ArrayList<>();
        controlCategories.add(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);

        mediaSelector = new MediaRouteSelector.Builder()
                .addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
                .addControlCategory(CastMediaControlIntent.categoryForCast(Objects.requireNonNull(getContext()).getString(R.string.receiver_id)))
                .build();

        return linearLayout;
    }

    private void initializeUIElements() {
        isStarted = false;

        loadingTV = linearLayout.findViewById(R.id.loadingText);
        loadingTV.setText(R.string.connecting);

        nowPlaying = linearLayout.findViewById(R.id.now_playing);
        nowPlaying.setText("");
        nowPlaying.setSelected(true); // triggers the marquee

        buttonPlay = linearLayout.findViewById(R.id.btn_play);
        buttonPlay.setOnClickListener(v -> {

            if(mPlaybackState == PlayBackState.CAST_IDLE){

                remoteMediaClient.play();
                onCastPlayerStart();

                mPlaybackState = PlayBackState.CAST_PLAYING;
            }else{
                mPlaybackState = PlayBackState.RADIO;

                if (urlToPlay != null) {

                    isStarted = true;

                    if(mStationIndex == 0){
                        urlToPlay = RADIO_STREAM_URL_HIGH;
                        mCurrentStation = mStationList.get(0);
                    }else{
                        mStationIndex = 1;
                        urlToPlay = RADIO_STREAM_URL_HIGH;
                        mCurrentStation = mStationList.get(1);
                    }

                    ConnectivityManager connMgr = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
                    assert connMgr != null;
                    NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

                    if(networkInfo == null){
                        loadingTV.setText(R.string.nonetwork);
                    }else{
                        loadingTV.setText(R.string.connecting);
                        nowPlaying.setText("");

                        buttonStopPlay.setVisibility(View.VISIBLE);
                        buttonPlay.setVisibility(View.INVISIBLE);
                        buttonLow.setImageResource(R.drawable.ic_low_bit);
                        buttonHigh.setImageResource(R.drawable.ic_high_bit);

                        if(mStationIndex == 0) buttonLow.setImageResource(R.drawable.ic_low_bit_active);
                        if(mStationIndex == 1) buttonHigh.setImageResource(R.drawable.ic_high_bit_active);

                        Intent intent = new Intent(activity, PlayerService.class);
                        intent.setAction(Config.MEDIA_ACTION_PLAY);
                        intent.putExtra(EXTRA_STATION, mCurrentStation);
                        activity.startService(intent);
                    }
                } else {
                    Log.d("INFO", "The loading of urlToPlay should happen almost instantly, so this code should never be reached");
                }
            }
        });

        buttonStopPlay = linearLayout.findViewById(R.id.btn_pause);
        buttonStopPlay.setOnClickListener(v -> {

            if(mPlaybackState == PlayBackState.CAST_PLAYING){
                remoteMediaClient.pause();
                onCastPlayerStop();

                mPlaybackState = PlayBackState.CAST_IDLE;
            }else{
                isStarted = false;
                loadingTV.setText("");
                nowPlaying.setText("");

                buttonLow.setImageResource(R.drawable.ic_low_bit);
                buttonHigh.setImageResource(R.drawable.ic_high_bit);
                buttonStopPlay.setVisibility(View.INVISIBLE);
                buttonPlay.setVisibility(View.VISIBLE);

                Intent intent = new Intent(activity, PlayerService.class);
                intent.setAction(Config.MEDIA_ACTION_STOP);
                activity.startService(intent);

                if (mCastSession != null) {
                    CastContext castContext = CastContext.getSharedInstance(Objects.requireNonNull(getContext()));
                    SessionManager mSessionManager = castContext.getSessionManager();
                    mSessionManager.endCurrentSession(true);

                    Objects.requireNonNull(getActivity()).invalidateOptionsMenu();
                }
            }
        });
        buttonStopPlay.setVisibility(View.GONE);

        buttonLow = linearLayout.findViewById(R.id.btn_low);
        buttonHigh = linearLayout.findViewById(R.id.btn_high);

        buttonMute = linearLayout.findViewById(R.id.btn_mute);
        buttonMute.setImageResource(R.drawable.ic_sound);
        buttonMute.setOnClickListener(v -> {

            if(isMuteed){
                isMuteed = false;

                buttonMute.setImageResource(R.drawable.ic_sound);

                Intent playIntent = new Intent(activity, PlayerService.class);
                playIntent.setAction(Config.MEDIA_ACTION_UNMUTE);
                activity.startService(playIntent);
            }else{
                isMuteed = true;

                buttonMute.setImageResource(R.drawable.ic_soundstop);

                Intent playIntent = new Intent(activity, PlayerService.class);
                playIntent.setAction(Config.MEDIA_ACTION_MUTE);
                activity.startService(playIntent);
            }
        });

        ImageView imgLInk = linearLayout.findViewById(R.id.img_partyfm_link);
        imgLInk.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(stringPartyLink));
            startActivity(browserIntent);
        });

        buttonLow.setOnClickListener(v -> {

            if((mStationIndex == 1 && mPlaybackState == PlayBackState.CAST_PLAYING) || mPlaybackState == PlayBackState.CAST_IDLE){

                buttonHigh.setImageResource(R.drawable.ic_high_bit);
                buttonLow.setImageResource(R.drawable.ic_low_bit_active);
                buttonStopPlay.setVisibility(View.VISIBLE);
                buttonPlay.setVisibility(View.INVISIBLE);

                mStationIndex = 0;
                mCurrentStation = mStationList.get(0);

                remoteMediaClient.load(new MediaLoadRequestData.Builder()
                        .setMediaInfo(buildMediaInfo())
                        .setAutoplay(true)
                        .setCurrentTime(0).build());
            }else{
                if(isStarted){

                    if(mStationIndex == 1){

                        mStationIndex = 0;
                        urlToPlay = RADIO_STREAM_URL_HIGH;
                        mCurrentStation = mStationList.get(0);

                        ConnectivityManager connMgr = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
                        assert connMgr != null;
                        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

                        if(networkInfo == null){
                            loadingTV.setText(R.string.nonetwork);
                        }
                        else{
                            loadingTV.setText(R.string.connecting);
                            nowPlaying.setText("");

                            buttonStopPlay.setVisibility(View.VISIBLE);
                            buttonPlay.setVisibility(View.INVISIBLE);
                            buttonLow.setImageResource(R.drawable.ic_low_bit);
                            buttonHigh.setImageResource(R.drawable.ic_high_bit);

                            if(mStationIndex == 0) buttonLow.setImageResource(R.drawable.ic_low_bit_active);
                            if(mStationIndex == 1) buttonHigh.setImageResource(R.drawable.ic_high_bit_active);

                            Intent intentPlay = new Intent(activity, PlayerService.class);
                            intentPlay.setAction(Config.MEDIA_ACTION_PLAY);
                            intentPlay.putExtra(EXTRA_STATION, mCurrentStation);
                            activity.startService(intentPlay);
                        }
                    }
                }else{
                    isStarted = true;

                    mStationIndex = 0;
                    urlToPlay = RADIO_STREAM_URL_HIGH;
                    mCurrentStation = mStationList.get(0);

                    ConnectivityManager connMgr = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
                    assert connMgr != null;
                    NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

                    if(networkInfo == null){
                        loadingTV.setText(R.string.nonetwork);
                    }else{
                        loadingTV.setText(R.string.connecting);
                        nowPlaying.setText("");

                        buttonStopPlay.setVisibility(View.VISIBLE);
                        buttonPlay.setVisibility(View.INVISIBLE);
                        buttonLow.setImageResource(R.drawable.ic_low_bit);
                        buttonHigh.setImageResource(R.drawable.ic_high_bit);

                        if(mStationIndex == 0) buttonLow.setImageResource(R.drawable.ic_low_bit_active);
                        if(mStationIndex == 1) buttonHigh.setImageResource(R.drawable.ic_high_bit_active);

                        Intent intentPlay = new Intent(activity, PlayerService.class);
                        intentPlay.setAction(Config.MEDIA_ACTION_PLAY);
                        intentPlay.putExtra(EXTRA_STATION, mCurrentStation);
                        activity.startService(intentPlay);
                    }
                }
            }
        });

        buttonHigh.setOnClickListener(v -> {

            if(mPlaybackState == PlayBackState.CAST_IDLE || (mStationIndex == 0 && mPlaybackState == PlayBackState.CAST_PLAYING)){

                buttonHigh.setImageResource(R.drawable.ic_high_bit_active);
                buttonLow.setImageResource(R.drawable.ic_low_bit);
                buttonStopPlay.setVisibility(View.VISIBLE);
                buttonPlay.setVisibility(View.INVISIBLE);
                mStationIndex = 1;
                mCurrentStation = mStationList.get(1);

                remoteMediaClient.load(new MediaLoadRequestData.Builder()
                        .setMediaInfo(buildMediaInfo())
                        .setAutoplay(true)
                        .setCurrentTime(0).build());
            }else{
                if(isStarted){
                    if(mStationIndex == 0){
                        mStationIndex = 1;
                        urlToPlay = RADIO_STREAM_URL_HIGH;
                        mCurrentStation = mStationList.get(1);

                        ConnectivityManager connMgr = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
                        assert connMgr != null;
                        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

                        if(networkInfo == null){
                            loadingTV.setText(R.string.nonetwork);
                        }else{
                            loadingTV.setText(R.string.connecting);
                            nowPlaying.setText("");

                            buttonStopPlay.setVisibility(View.VISIBLE);
                            buttonPlay.setVisibility(View.INVISIBLE);
                            buttonLow.setImageResource(R.drawable.ic_low_bit);
                            buttonHigh.setImageResource(R.drawable.ic_high_bit);

                            if(mStationIndex == 0) buttonLow.setImageResource(R.drawable.ic_low_bit_active);
                            if(mStationIndex == 1) buttonHigh.setImageResource(R.drawable.ic_high_bit_active);

                            Intent intentPlay = new Intent(activity, PlayerService.class);
                            intentPlay.setAction(Config.MEDIA_ACTION_PLAY);
                            intentPlay.putExtra(EXTRA_STATION, mCurrentStation);
                            activity.startService(intentPlay);
                        }
                    }
                }else{
                    isStarted = true;

                    mStationIndex = 1;
                    urlToPlay = RADIO_STREAM_URL_HIGH;
                    mCurrentStation = mStationList.get(1);

                    ConnectivityManager connMgr = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
                    assert connMgr != null;
                    NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

                    if(networkInfo == null){
                        loadingTV.setText(R.string.nonetwork);
                    }else{
                        loadingTV.setText(R.string.connecting);
                        nowPlaying.setText("");

                        buttonStopPlay.setVisibility(View.VISIBLE);
                        buttonPlay.setVisibility(View.INVISIBLE);
                        buttonLow.setImageResource(R.drawable.ic_low_bit);
                        buttonHigh.setImageResource(R.drawable.ic_high_bit);

                        if(mStationIndex == 0) buttonLow.setImageResource(R.drawable.ic_low_bit_active);
                        if(mStationIndex == 1) buttonHigh.setImageResource(R.drawable.ic_high_bit_active);

                        Intent intentPlay = new Intent(activity, PlayerService.class);
                        intentPlay.setAction(Config.MEDIA_ACTION_PLAY);
                        intentPlay.putExtra(EXTRA_STATION, mCurrentStation);
                        activity.startService(intentPlay);
                    }
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();


        castContext.getSessionManager().addSessionManagerListener(sessionManagerListener, CastSession.class);
        if (mCastSession == null) {
            mCastSession = castContext.getSessionManager().getCurrentCastSession();
        }
        // refresh app state
        loadAppState(activity);

        // handles the activity's intent
        Intent intent = activity.getIntent();
        if (ACTION_SHOW_PLAYER.equals(intent.getAction())) {
            handleShowPlayer(intent);
        }else if(Config.MEDIA_NOTIFI_PLAY.equals(intent.getAction())){

            loadAppState(getContext());

            Intent playIntent = new Intent(activity, PlayerService.class);
            playIntent.setAction(Config.MEDIA_ACTION_PLAY);
            activity.startService(playIntent);

            onStartRadioStream(); // update buttons for start
        }

        Intent playIntent = new Intent(activity, PlayerService.class);
        playIntent.setAction(Config.MEDIA_ACTION_RESUMED);
        activity.startService(playIntent);
    }

    /* Handles intent to show player from notification or from shortcut */
    private void handleShowPlayer(Intent intent) {
        Station station = null;

        // CASE: user tapped on notification
        if (intent.hasExtra(EXTRA_STATION)) {
            // get station from notification
            station = intent.getParcelableExtra(EXTRA_STATION);
            mCurrentStation = station;
        }
        // CASE: playback requested via homescreen shortcut
        else if (intent.hasExtra(EXTRA_STREAM_URI)) {
            // get Uri of station from home screen shortcut
            station = StationListHelper.findStation(mStationList, Uri.parse(intent.getStringExtra(EXTRA_STREAM_URI)));
        }
        // CASE: transistor received a last station intent
        else if (intent.hasExtra(EXTRA_LAST_STATION)) {
            // try to get last station from SharedPreferences
            String stationUrlLastString = PreferenceManager.getDefaultSharedPreferences(activity).getString(PREF_STATION_URL_LAST, null);
            loadAppState(activity);
            if (stationUrlLastString != null) {
                station = StationListHelper.findStation(mStationList, Uri.parse(stationUrlLastString));
            }
        }

        // clear the intent, show player and start playback if requested
        if (station != null) {
            intent.setAction("");

            buttonPlay.setVisibility(View.INVISIBLE);
            buttonStopPlay.setVisibility(View.VISIBLE);
            loadingTV.setText(R.string.connecting);

            Intent playIntent = new Intent(activity, PlayerService.class);
            playIntent.setAction(Config.MEDIA_ACTION_PLAY);
            playIntent.putExtra(EXTRA_STATION, mCurrentStation);
            activity.startService(playIntent);
        } else {
            Toast.makeText(activity, "Station not found.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onPause() {

//        castContext.getSessionManager().removeSessionManagerListener(sessionManagerListener, CastSession.class);
        mCastSession = null;
        MediaRouter.getInstance(Objects.requireNonNull(getContext())).removeCallback(mediaRouterCallback);

        super.onPause();
        // save state
        saveAppState(activity);
    }

    @Override
    public void onStop() {
        super.onStop();

        Intent playIntent = new Intent(activity, PlayerService.class);
        playIntent.setAction(Config.MEDIA_ACTION_DISMISS);
        activity.startService(playIntent);
    }

    public static void onStopRadioStream(){

        isStarted = false;

        if(!internetConnection){
            loadingTV.setText(R.string.nonetwork);
        }else{
            buttonLow.setImageResource(R.drawable.ic_low_bit);
            buttonHigh.setImageResource(R.drawable.ic_high_bit);
            loadingTV.setText("");
            nowPlaying.setText("");
            buttonStopPlay.setVisibility(View.INVISIBLE);
            buttonPlay.setVisibility(View.VISIBLE);
        }

        if(mPlaybackState == PlayBackState.CAST_PLAYING){
            buttonStopPlay.setVisibility(View.VISIBLE);
            buttonPlay.setVisibility(View.INVISIBLE);
            loadingTV.setText(R.string.ready_to_cast);
            nowPlaying.setText("");
            if(mStationIndex == 0)buttonLow.setImageResource(R.drawable.ic_low_bit_active);
            if(mStationIndex == 1)buttonHigh.setImageResource(R.drawable.ic_high_bit_active);
        }

        mPlaybackState = PlayBackState.IDLE;
    }

    public static void onStartRadioStream(){

        mPlaybackState = PlayBackState.PLAYING;

        loadingTV.setText("");
        nowPlaying.setText("");

        buttonStopPlay.setVisibility(View.VISIBLE);
        buttonPlay.setVisibility(View.INVISIBLE);
        buttonLow.setImageResource(R.drawable.ic_low_bit);
        buttonHigh.setImageResource(R.drawable.ic_high_bit);

        if(mStationIndex == 0) buttonLow.setImageResource(R.drawable.ic_low_bit_active);
        if(mStationIndex == 1) buttonHigh.setImageResource(R.drawable.ic_high_bit_active);
    }

    /* Saves app state to SharedPreferences */
    private void saveAppState(Context context) {
        if (mCurrentStation != null) {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(PREF_STATION_URI_SELECTED, mCurrentStation.getStreamUri());
            editor.apply();
        }
    }

    public static void onMute(boolean mMute){
        if(mMute){
            isMuteed = false;
            buttonMute.setImageResource(R.drawable.ic_soundstop);

            Intent playIntent = new Intent(activity, PlayerService.class);
            playIntent.setAction(Config.MEDIA_ACTION_MUTE);
            activity.startService(playIntent);
        }else{
            isMuteed = false;
            buttonMute.setImageResource(R.drawable.ic_sound);

            Intent playIntent = new Intent(activity, PlayerService.class);
            playIntent.setAction(Config.MEDIA_ACTION_UNMUTE);
            activity.startService(playIntent);
        }
    }

    public static void setTitle(String _title){
        nowPlaying.setText(_title);
    }

    /* Loads app state from preferences */
    private void loadAppState(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        String mCurrentStationUrl = settings.getString(PREF_STATION_URL_LAST, Config.RADIO_STREAM_URL_LOW);
        if(mCurrentStationUrl.equals(Config.RADIO_STREAM_URL_LOW)) mStationIndex = 0;
        if(mCurrentStationUrl.equals(Config.RADIO_STREAM_URL_HIGH)) mStationIndex = 1;

        mCurrentStation = mStationList.get(mStationIndex);
        urlToPlay = mCurrentStation.getStreamUri();
    }

    private class ConnectivityReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager connMgr = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);

            assert connMgr != null;
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            if(networkInfo == null){
                internetConnection = false;
                loadingTV.setText(R.string.nonetwork);
                nowPlaying.setText("");
            }else{
                internetConnection = true;
                loadingTV.setText("");
                if(isStarted && mCurrentStation != null){
                    Intent intentPlay = new Intent(activity, PlayerService.class);
                    intentPlay.setAction(Config.MEDIA_ACTION_PLAY);
                    intentPlay.putExtra(EXTRA_STATION, mCurrentStation);
                    activity.startService(intentPlay);
                }
            }
        }
    }

    @Override
    public void onStart() {
        MediaRouter.getInstance(Objects.requireNonNull(getContext())).addCallback(mediaSelector, mediaRouterCallback,
                MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);

        super.onStart();
    }
}