package com.partyfm.radio;

public class Config {

    //Radio Stream UTL
    public static final String RADIO_STREAM_URL_LOW = "http://stream1.partyfm.dk/Party64mobil";
    public static final String RADIO_STREAM_URL_HIGH = "http://stream.partyfm.dk/Party128mobil";

    //Radio Stream Site URL
    public static String stringPartyLink = "http://m.partyfm.dk";

    //Intent Notification Actions
    public static final String MEDIA_NOTIFI_PLAY_PAUSE = "com.partyfm.radio.notification.INTENT_PLAYPAUSE";
    public static final String MEDIA_NOTIFI_MUTE = "com.partyfm.radio.action.INTENT_MUTE";
    public static final String MEDIA_NOTIFI_EXIT = "com.partyfm.radio.action.INTENT_EXIT";
    public static final String MEDIA_NOTIFI_PLAY = "com.partyfm.radio.action.INTENT_PLAY";

    //Intent Actions
    public static final String MEDIA_ACTION_PLAY = "com.partyfm.radio.action.PLAY";
    public static final String MEDIA_ACTION_STOP = "com.partyfm.radio.action.STOP";
    public static final String MEDIA_ACTION_DISMISS = "com.partyfm.radio.action.INTENT_CANCEL";
    public static final String MEDIA_ACTION_MUTE = "com.partyfm.radio.action.MUTE";
    public static final String MEDIA_ACTION_RESUMED = "com.partyfm.radio.action.RESUMED";
    public static final String MEDIA_ACTION_UNMUTE = "com.partyfm.radio.action.UNMUTE";

    //Player Status Actions
    public static final String ACTION_PLAYBACK_STATE_CHANGED = "com.partyfm.radio.action.PLAYBACK_STATE_CHANGED";
    public static final String ACTION_SHOW_PLAYER = "com.partyfm.radio.action.SHOW_PLAYER";
    public static final String ACTION_METADATA_CHANGED = "com.partyfm.radio.action.METADATA_CHANGED";

    //Station Actions
    public static final String EXTRA_LAST_STATION = "LAST_STATION";
    public static final String EXTRA_STREAM_URI = "STREAM_URI";

    public static final String EXTRA_STATION = "STATION";
    public static final String EXTRA_ERROR_OCCURRED = "ERROR_OCCURRED";
    public static final String EXTRA_PLAYBACK_STATE = "PLAYBACK_STATE";

    /* PLAYBACK STATES */
    public static final int PLAYBACK_STATE_LOADING_STATION = 1;
    public static final int PLAYBACK_STATE_STOPPED = 3;

    /* PREFS */
    public static final String PREF_STATION_URL = "prefStationUrl";
    public static final String PREF_STATION_URL_LAST = "prefStationUrlLast";
    public static final String PREF_STATION_URI_SELECTED = "prefStationUriSelected";

    /* MISC */
    public static final int PLAYER_SERVICE_NOTIFICATION_ID = 1;
    public static final String NOTIFICATION_CHANNEL_ID_PLAYBACK_CHANNEL ="notificationChannelIdPlaybackChannel";

}
