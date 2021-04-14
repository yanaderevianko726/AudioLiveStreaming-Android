/*
 * ImageHelper.java
 * Implements the ImageHelper class
 * An ImageHelper formats icons and symbols for use in the app ui
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-19 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package com.partyfm.radio.services;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;

import androidx.palette.graphics.Palette;

import com.partyfm.radio.R;
import com.partyfm.radio.utilities.Station;


/*
 * ImageHelper class
 */
final class ImageHelper {

    /* Main class variables */
    private static Bitmap mInputImage;
    private final Context mContext;


    /* Constructor when given a Bitmap */
    ImageHelper(Station station, Context context) {
        mContext = context;
        if (station != null) {
            mInputImage = getBitmap();
        } else {
            mInputImage = getBitmap();
        }
    }

    /* Creates shortcut icon on radio station shape for Home screen */
    Bitmap createShortcutOnRadioShape() {
        int yOffset = 16;

        // get scaled background bitmap
        Bitmap background = getBitmap();
        assert background != null;
        background = Bitmap.createScaledBitmap(background, 192, 192, false);

        // compose images
        return composeImages(background, yOffset);
    }

    /* Extracts color from station icon */
    private int getStationImageColor() {

        // extract color palette from station image
        Palette palette = Palette.from(mInputImage).generate();
        // get muted and vibrant swatches
        Palette.Swatch vibrantSwatch = palette.getVibrantSwatch();
        Palette.Swatch mutedSwatch = palette.getMutedSwatch();

        if (vibrantSwatch != null) {
            // return vibrant color
            int rgb = vibrantSwatch.getRgb();
            return Color.argb(255, Color.red(rgb), Color.green(rgb), Color.blue(rgb));
        } else if (mutedSwatch != null) {
            // return muted color
            int rgb = mutedSwatch.getRgb();
            return Color.argb(255, Color.red(rgb), Color.green(rgb), Color.blue(rgb));
        } else {
            // default return
            return mContext.getResources().getColor(R.color.playBar);
        }
    }

    /* Creates station image on a square background with the main station image color and option padding for adaptive icons */
    Bitmap createSquareImage() {

        // create background
        int color = getStationImageColor();
        Paint background = new Paint();
        background.setColor(color);
        background.setStyle(Paint.Style.FILL);

        // create empty bitmap and canvas
        Bitmap outputImage = Bitmap.createBitmap(192, 192, Bitmap.Config.ARGB_8888);
        Canvas imageCanvas = new Canvas(outputImage);

        // draw square background
        float right = (float) 192;
        float bottom =  (float) 192;
        imageCanvas.drawRect(0f,0f, right, bottom, background);

        // draw input image onto canvas using transformation matrix
        Paint paint = new Paint();
        paint.setFilterBitmap(true);
        imageCanvas.drawBitmap(mInputImage, createTransformationMatrix(0), paint);

        return outputImage;
    }

    /* Composes foreground bitmap onto background bitmap */
    private Bitmap composeImages(Bitmap background, int yOffset) {

        // compose output image
        Bitmap outputImage = Bitmap.createBitmap(192, 192, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(outputImage);
        canvas.drawBitmap(background, 0, 0, null);
        canvas.drawBitmap(mInputImage, createTransformationMatrix(yOffset), null);

        return outputImage;
    }


    /* Creates a transformation matrix with the given size and optional padding  */
    private Matrix createTransformationMatrix(int yOffset) {
        Matrix matrix = new Matrix();

        // get size of original image and calculate padding
        float inputImageHeight = (float)mInputImage.getHeight();
        float inputImageWidth = (float)mInputImage.getWidth();
        float padding;
        padding = (float) 192 /4f;

        // define variables needed for transformation matrix
        float aspectRatio = 0.0f;
        float xTranslation = 0.0f;
        float yTranslation = 0.0f;

        // landscape format and square
        if (inputImageWidth >= inputImageHeight) {
            aspectRatio = (192 - padding*2) / inputImageWidth;
            xTranslation = 0.0f + padding;
            yTranslation = ((192 - inputImageHeight * aspectRatio)/2.0f ) + yOffset;
        }
        // portrait format
        else if (inputImageHeight > inputImageWidth) {
            aspectRatio = (192 - padding*2) / inputImageHeight;
            yTranslation = 0.0f + padding + yOffset;
            xTranslation = (192 - inputImageWidth * aspectRatio)/2.0f;
        }

        // construct transformation matrix
        matrix.postTranslate(xTranslation, yTranslation);
        matrix.preScale(aspectRatio, aspectRatio);

        return matrix;
    }

/* Calculates parameter needed to scale image down */
    private static int calculateSampleParameter(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // get size of original image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    /* Return a bitmap for a given resource id of a vector drawable */
    private Bitmap getBitmap() {
//        VectorDrawableCompat drawable = VectorDrawableCompat.create(mContext.getResources(), resource, null);
        Drawable drawable = mContext.getResources().getDrawable(R.drawable.ic_icon);
        if (drawable != null) {
            Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            return bitmap;
        } else {
            return null;
        }
    }


}