package com.partyfm.radio.utilities;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

import com.partyfm.radio.R;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class Utils {

    private static void isNetworkUnavailable(final Context context) {

        if (isNetworkAvailable(context, false)) {
            Toast.makeText(context, context.getResources().getString(R.string.error_retry), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, context.getResources().getString(R.string.alert_network), Toast.LENGTH_SHORT).show();
        }
    }

    public static boolean isNetworkAvailable(Context context, boolean showDialog) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        assert connectivityManager != null;
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected())
            return true;
        else if (showDialog) {
            isNetworkUnavailable(context);
        }
        return false;
    }

    private static String getDataFromUrl(String url) {

        Log.v("INFO", "Requesting: " + url);

        StringBuilder stringBuffer = new StringBuilder("");
        try {
            URL urlCon = new URL(url);

            HttpURLConnection connection = (HttpURLConnection) urlCon.openConnection();
            connection.setRequestProperty("User-Agent", "La Radio Sv");
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            connection.connect();

            InputStream inputStream = connection.getInputStream();

            BufferedReader rd = new BufferedReader(new InputStreamReader(
                    inputStream));
            String line = "";
            while ((line = rd.readLine()) != null) {
                stringBuffer.append(line);
            }

        } catch (IOException ignored) {
        }

        return stringBuffer.toString();
    }

    public static JSONObject getJSONObjectFromUrl(String url) {
        String data = getDataFromUrl(url);

        try {
            return new JSONObject(data);
        } catch (Exception e) {
            Log.e("INFO", "Error parsing JSON. Printing stacktrace now");
        }

        return null;
    }

}
