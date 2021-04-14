package com.partyfm.radio.utilities;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.media.MediaMetadataCompat;

import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_MEDIA_URI;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_TITLE;
import static com.partyfm.radio.Config.PLAYBACK_STATE_STOPPED;

public final class Station implements Parcelable {

    private String mStationName;
    private String mURL;
    private String mTitle;
    private String mMetaData;
    private int mPlayback;
    private String mMimeType;
    private int mSampleRate ;
    private int mBitrate;

    public Station(String _StationName, String _URL, String _Title, String _Metadata) {
        mStationName = _StationName;
        mURL = _URL;
        mTitle = _Title;
        mMetaData = _Metadata;
        mSampleRate = -1;
        mBitrate = -1;
        mPlayback = PLAYBACK_STATE_STOPPED;
    }

    protected Station(Parcel in) {
        mStationName = in.readString();
        mURL = in.readString();
        mTitle = in.readString();
        mMetaData = in.readString();
        mPlayback = in.readInt();
        mMimeType = in.readString();
        mSampleRate = in.readInt();
        mBitrate = in.readInt();
    }

    public static final Creator<Station> CREATOR = new Creator<Station>() {
        @Override
        public Station createFromParcel(Parcel in) {
            return new Station(in);
        }

        @Override
        public Station[] newArray(int size) {
            return new Station[size];
        }
    };

    /* Initializes variables that are set during playback */
    private void initializePlaybackMetadata() {
        mMetaData = "";
        mMimeType = "";
        mSampleRate = -1;
        mBitrate = -1;
        mPlayback = PLAYBACK_STATE_STOPPED;

    }

    /* Getter for playback state */
    public int getPlaybackState() {
        return mPlayback;
    }
    public String getStationName(){
        return mStationName;
    }

    public String getStreamUri(){
        return mURL;
    }

    /* Constructor when given MediaMetadata (e.g. from Android Auto)  */
    @SuppressLint("WrongConstant")
    public Station (MediaMetadataCompat stationMediaMetadata) {
        mMetaData = stationMediaMetadata.getString(METADATA_KEY_TITLE);
        mURL = stationMediaMetadata.getString(METADATA_KEY_MEDIA_URI);
        mPlayback = PLAYBACK_STATE_STOPPED;
    }

    /* Resets state of station */
    public void resetState() {
        initializePlaybackMetadata();
    }

    /* Setter for playback state */
    public void setPlaybackState(int playback) {
        mPlayback = playback;
    }

    /* Getter for metadata of currently playing song */
    public String getMetadata() {
        return mMetaData;
    }

    /* Setter for Metadata of currently playing media */
    public void setMetadata(String metadata) {
        mMetaData = metadata;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mStationName);
        dest.writeString(mURL);
        dest.writeString(mTitle);
        dest.writeString(mMetaData);
        dest.writeString(mMimeType);
        dest.writeInt(mSampleRate);
        dest.writeInt(mSampleRate);
        dest.writeInt(mBitrate);
    }
}
