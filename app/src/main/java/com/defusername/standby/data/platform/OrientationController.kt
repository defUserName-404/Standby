package com.defusername.standby.data.platform

import android.app.Activity
import android.content.pm.ActivityInfo
import com.defusername.standby.domain.model.OrientationMode
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Applies the user's orientation preference to a given Activity. Pure platform glue
 * (Activity.requestedOrientation) — not unit-testable by design, validated on-device.
 */
@Singleton
class OrientationController @Inject constructor() {

    fun apply(activity: Activity, mode: OrientationMode) {
        activity.requestedOrientation = when (mode) {
            OrientationMode.LANDSCAPE_ONLY -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            OrientationMode.PORTRAIT_ONLY -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            OrientationMode.AUTO -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
}
