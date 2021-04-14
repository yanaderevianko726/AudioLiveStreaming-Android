/*
 * StationListHelper.java
 * Implements the StationListHelper class
 * A StationListHelper provides methods that can manipulate lists of Station objects
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-19 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package com.partyfm.radio.utilities;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.util.ArrayList;

import static com.partyfm.radio.Config.RADIO_STREAM_URL_HIGH;
import static com.partyfm.radio.Config.RADIO_STREAM_URL_LOW;


/*
 * StationListHelper class
 */
public final class StationListHelper {

    /* Define log tag */
    private static final String LOG_TAG = StationListHelper.class.getSimpleName();


    /* Load list of stations from storage */
    public static ArrayList<Station> loadStationListFromStorage(Context context) {


        // initialize list
        ArrayList<Station> stationList = new ArrayList<>();

        for(int i=0; i<2; i++){
            String stationUrl = RADIO_STREAM_URL_LOW;
            String stationName = "LowStation";
            if(i == 1){
                stationUrl = RADIO_STREAM_URL_HIGH;
                stationName = "HighStation";
            }

            Station mStation = new Station(stationName, stationUrl, "", "");
            stationList.add(mStation);
        }

        Log.v(LOG_TAG, "Finished initial read operation from storage. Stations found: " + stationList.size());
        return stationList;
    }

    /* Finds station when given its Uri */
    public static Station findStation(ArrayList<Station> stationList, Uri streamUri) {

        // make sure list and uri are not null
        if (stationList == null || streamUri == null) {
            return null;
        }

        // traverse list of stations
        for (int i = 0; i < stationList.size(); i++) {
            Station station = stationList.get(i);
            if (station.getStreamUri().equals(streamUri)) {
                return station;
            }
        }

        // return null if nothing was found
        return null;
    }
}
