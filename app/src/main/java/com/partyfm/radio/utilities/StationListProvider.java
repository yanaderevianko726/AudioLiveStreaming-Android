/*
 * StationListProvider.java
 * Implements the StationListProvider class
 * A StationListProvider provides a list of stations as MediaMetadata items
 * Credit: https://github.com/googlesamples/android-MediaBrowserService/ (-> MusicProvider)
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-19 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package com.partyfm.radio.utilities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.media.MediaMetadataCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.TreeMap;

/*
 * StationListProvider.class
 */
public class StationListProvider {

    /* Define log tag */
    private static final String LOG_TAG = StationListProvider.class.getSimpleName();


    /* Callback used by PlayerService */
    public interface Callback {
        void onStationListReady(boolean success);
    }

    /* Main class variables */
    private final TreeMap<String, MediaMetadataCompat> mStationListByName;
    private enum State { NON_INITIALIZED, INITIALIZING, INITIALIZED }
    private volatile State mCurrentState = State.NON_INITIALIZED;


    /* Constructor */
    public StationListProvider() {
        mStationListByName = new TreeMap<>();
    }

    /* Gets list of stations and caches the track information */
    @SuppressLint("StaticFieldLeak")
    public void retrieveMediaAsync(final Context context, final Callback callback) {
        Log.v(LOG_TAG, "retrieveMediaAsync called");
        if (mCurrentState == State.INITIALIZED) {
            // already initialized, so call back immediately.
            callback.onStationListReady(true);
            return;
        }

        new AsyncTask<Void, Void, State>() {
            @Override
            protected State doInBackground(Void... params) {
                retrieveStations(context);
                return mCurrentState;
            }

            @Override
            protected void onPostExecute(State current) {
                if (callback != null) {
                    callback.onStationListReady(current == State.INITIALIZED);
                }
            }
        }.execute();
    }

    /* Retrieves stations as MediaMetadataCompat */
    private synchronized void retrieveStations(Context context) {
        if (mCurrentState == State.NON_INITIALIZED) {
            mCurrentState = State.INITIALIZING;

            ArrayList<Station> stationList = StationListHelper.loadStationListFromStorage(context);
            if (stationList != null) {
                for (Station station : stationList) {
                    MediaMetadataCompat item = buildMediaMetadata(station);
                    String mediaId = item.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
                    mStationListByName.put(mediaId, item);
                }
            }
            mCurrentState = State.INITIALIZED;
        }
    }

    /* Creates MediaMetadata from station */
    @SuppressLint("WrongConstant")
    private MediaMetadataCompat buildMediaMetadata(Station station) {

        return new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, station.getStreamUri())
                .putString(MediaMetadataCompat.METADATA_KEY_GENRE, "Radio")
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE,  station.getStationName())
                .build();
    }

}
