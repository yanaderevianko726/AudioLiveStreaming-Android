// credit:
// https://medium.com/google-developers/audio-focus-3-cdc09da9c122 &
// https://gist.github.com/nic0lette/c360dd353c451d727ea017890cbaa521

package com.partyfm.radio.utilities;

import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import java.lang.annotation.Retention;
import java.util.Objects;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.media.AudioAttributesCompat;

import static java.lang.annotation.RetentionPolicy.SOURCE;

/*
 * Compatibility version of an {@link AudioFocusRequest}.
 */
public class AudioFocusRequestCompat {

    @Retention(SOURCE)
    @IntDef({
            AudioManager.AUDIOFOCUS_GAIN,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
    })
    @interface FocusGain {}

    private final int mFocusGain;
    private final OnAudioFocusChangeListener mOnAudioFocusChangeListener;
    private final Handler mFocusChangeHandler;
    private final AudioAttributesCompat mAudioAttributesCompat;

    private final boolean mPauseOnDuck;
    private final boolean mAcceptsDelayedFocusGain;

    private AudioFocusRequestCompat(int focusGain,
                                    OnAudioFocusChangeListener onAudioFocusChangeListener,
                                    Handler focusChangeHandler,
                                    AudioAttributesCompat audioFocusRequestCompat,
                                    boolean pauseOnDuck,
                                    boolean acceptsDelayedFocusGain) {
        mFocusGain = focusGain;
        mOnAudioFocusChangeListener = onAudioFocusChangeListener;
        mFocusChangeHandler = focusChangeHandler;
        mAudioAttributesCompat = audioFocusRequestCompat;
        mPauseOnDuck = pauseOnDuck;
        mAcceptsDelayedFocusGain = acceptsDelayedFocusGain;
    }

    int getFocusGain() {
        return mFocusGain;
    }

    AudioAttributesCompat getAudioAttributesCompat() {
        return mAudioAttributesCompat;
    }

    boolean willPauseWhenDucked() {
        return mPauseOnDuck;
    }

    boolean acceptsDelayedFocusGain() {
        return mAcceptsDelayedFocusGain;
    }

    /* package */ OnAudioFocusChangeListener getOnAudioFocusChangeListener() {
        return mOnAudioFocusChangeListener;
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private
        /* package */ AudioAttributes getAudioAttributes() {
        return (mAudioAttributesCompat != null)
                ? (AudioAttributes) (mAudioAttributesCompat.unwrap())
                : null;
    }

    @RequiresApi(Build.VERSION_CODES.O)
    /* package */ AudioFocusRequest getAudioFocusRequest() {
        return new AudioFocusRequest.Builder(mFocusGain)
                .setAudioAttributes(Objects.requireNonNull(getAudioAttributes()))
                .setAcceptsDelayedFocusGain(mAcceptsDelayedFocusGain)
                .setWillPauseWhenDucked(mPauseOnDuck)
                .setOnAudioFocusChangeListener(mOnAudioFocusChangeListener, mFocusChangeHandler)
                .build();
    }

    /**
     * Builder for an {@link AudioFocusRequestCompat}.
     */
    public static final class Builder {
        private int mFocusGain;
        private OnAudioFocusChangeListener mOnAudioFocusChangeListener;
        private Handler mFocusChangeHandler;
        private AudioAttributesCompat mAudioAttributesCompat;

        // Flags
        private boolean mPauseOnDuck;
        private boolean mAcceptsDelayedFocusGain;

        public Builder(@FocusGain int focusGain) {
            mFocusGain = focusGain;
        }

        public Builder(@NonNull AudioFocusRequestCompat requestToCopy) {
            mFocusGain = requestToCopy.mFocusGain;
            mOnAudioFocusChangeListener = requestToCopy.mOnAudioFocusChangeListener;
            mFocusChangeHandler = requestToCopy.mFocusChangeHandler;
            mAudioAttributesCompat = requestToCopy.mAudioAttributesCompat;
            mPauseOnDuck = requestToCopy.mPauseOnDuck;
            mAcceptsDelayedFocusGain = requestToCopy.mAcceptsDelayedFocusGain;
        }

        @NonNull
        public Builder setFocusGain(@FocusGain int focusGain) {
            mFocusGain = focusGain;
            return this;
        }

        @NonNull
        public Builder setOnAudioFocusChangeListener(@NonNull OnAudioFocusChangeListener listener) {
            return setOnAudioFocusChangeListener(listener, new Handler(Looper.getMainLooper()));
        }

        @NonNull
        Builder setOnAudioFocusChangeListener(@NonNull OnAudioFocusChangeListener listener,
                                              @NonNull Handler handler) {
            mOnAudioFocusChangeListener = listener;
            mFocusChangeHandler = handler;
            return this;
        }

        @NonNull
        public Builder setAudioAttributes(@NonNull AudioAttributesCompat attributes) {
            mAudioAttributesCompat = attributes;
            return this;
        }

        @NonNull
        public Builder setWillPauseWhenDucked(boolean pauseOnDuck) {
            mPauseOnDuck = pauseOnDuck;
            return this;
        }

        @NonNull
        public Builder setAcceptsDelayedFocusGain(boolean acceptsDelayedFocusGain) {
            mAcceptsDelayedFocusGain = acceptsDelayedFocusGain;
            return this;
        }

        public AudioFocusRequestCompat build() {
            return new AudioFocusRequestCompat(mFocusGain,
                    mOnAudioFocusChangeListener,
                    mFocusChangeHandler,
                    mAudioAttributesCompat,
                    mPauseOnDuck,
                    mAcceptsDelayedFocusGain);
        }
    }
}
