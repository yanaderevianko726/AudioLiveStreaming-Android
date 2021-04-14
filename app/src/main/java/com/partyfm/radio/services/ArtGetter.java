package com.partyfm.radio.services;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;

import com.partyfm.radio.utilities.Utils;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;

public class ArtGetter {

    public static void getImageForQuery(final String query, final AlbumCallback albumCallback, final Context context) {

        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... unused) {
                JSONObject jsonObject = Utils.getJSONObjectFromUrl("https://api.spotify.com/v1/search?q=" + URLEncoder.encode(query) + "&type=track&limit=1");

                try {
                    if (jsonObject != null
                            && jsonObject.has("tracks")
                            && jsonObject.getJSONObject("tracks").has("items")
                            && jsonObject.getJSONObject("tracks").getJSONArray("items").length() > 0) {
                        JSONObject track = jsonObject.getJSONObject("tracks").getJSONArray("items").getJSONObject(0);
                        JSONObject image = track.getJSONObject("album").getJSONArray("images").getJSONObject(0);
                        return image.getString("url");
                    } else {
                        Log.v("INFO", "No items in Album Art Request");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(final String imageUrl) {
                if (imageUrl != null)
                    Picasso.with(context)
                            .load(imageUrl)
                            .into(new Target() {
                                @Override
                                public void onBitmapLoaded(final Bitmap bitmap, Picasso.LoadedFrom from) {
                                    albumCallback.finished(bitmap);
                                }

                                @Override
                                public void onBitmapFailed(Drawable errorDrawable) {
                                    albumCallback.finished(null);
                                }

                                @Override
                                public void onPrepareLoad(Drawable placeHolderDrawable) {

                                }
                            });
                else
                    albumCallback.finished(null);
            }
        }.execute();

    }

    public interface AlbumCallback {
        void finished(Bitmap b);
    }
}
