package com.partyfm.radio.services;

import android.annotation.SuppressLint;
import android.util.Log;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedList;

public class ParserURL {

    @SuppressLint("DefaultLocale")
    public static String getUrl(String url) {
        String mUrl = url.toUpperCase();
        if (mUrl.endsWith(".FLAC")) {
            return url;
        } else if (mUrl.endsWith(".MP3")) {
            return url;
        } else if (mUrl.endsWith(".WAV")) {
            return url;
        } else if (mUrl.endsWith(".M4A")) {
            return url;
        } else if (mUrl.endsWith(".PLS")) {
            return url;

        } else if (mUrl.endsWith(".M3U")) {
            ParserM3U parserM3U = new ParserM3U();
            LinkedList<String> urls = parserM3U.getRawUrl(url);
            if ((urls.size() > 0)) {
                return urls.get(0);
            }
        } else if (mUrl.endsWith(".ASX")) {
            ParserASX parserASX = new ParserASX();
            LinkedList<String> urls = parserASX.getRawUrl(url);
            if ((urls.size() > 0)) {
                return urls.get(0);
            }
        } else {
            URLConnection urlConnection = getConnection(url);
            if (urlConnection != null) {
                String mContentDisposition = urlConnection.getHeaderField("Content-Disposition");

                Log.v("INFO", "Requesting: " + url + " Headers: " + urlConnection.getHeaderFields());

                String mContentType = urlConnection.getContentType();
                if (mContentType != null) {
                    mContentType = mContentType.toUpperCase();
                }
                if (mContentDisposition != null && mContentDisposition.toUpperCase().endsWith("M3U")) {
                    ParserM3U m3u = new ParserM3U();
                    LinkedList<String> urls = m3u.getRawUrl(urlConnection);
                    if (urls.size() > 0) {
                        return urls.getFirst();
                    }
                } else if (mContentType != null && mContentType.contains("AUDIO/X-SCPLS")) {
                    return url;
                } else if (mContentType != null && mContentType.contains("VIDEO/X-MS-ASF")) {
                    ParserASX asx = new ParserASX();
                    LinkedList<String> urls = asx.getRawUrl(url);
                    if ((urls.size() > 0)) {
                        return urls.get(0);
                    }
                    ParserPLS pls = new ParserPLS();
                    urls = pls.getRawUrl(url);
                    if ((urls.size() > 0)) {
                        return urls.get(0);
                    }
                } else if (mContentType != null && mContentType.contains("AUDIO/MPEG")) {
                    return url;
                } else if (mContentType != null && mContentType.contains("AUDIO/X-MPEGURL")) {
                    ParserM3U m3u = new ParserM3U();
                    LinkedList<String> urls = m3u.getRawUrl(url);
                    if ((urls.size() > 0)) {
                        return urls.get(0);
                    }
                } else {
                    Log.d("LOG", "Not Found");
                }
            }
        }
        return url;
    }

    private static URLConnection getConnection(String url) {
        URLConnection urlConnection;
        try {
            urlConnection = new URL(url).openConnection();
            return urlConnection;
        } catch (IOException ignored) {

        }
        return null;
    }

}
