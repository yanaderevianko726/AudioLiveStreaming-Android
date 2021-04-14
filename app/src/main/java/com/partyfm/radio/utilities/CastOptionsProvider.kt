package com.partyfm.radio.utilities

import android.content.Context
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.partyfm.radio.R

class CastOptionsProvider : OptionsProvider {
    override fun getCastOptions(ctx: Context): CastOptions {
        return CastOptions
                .Builder()
                .setReceiverApplicationId(
                        ctx.getString(R.string.receiver_id)
                )
                .build()
    }

    override fun getAdditionalSessionProviders(ctx: Context) = null
}