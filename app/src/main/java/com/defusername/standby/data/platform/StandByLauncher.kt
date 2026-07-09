package com.defusername.standby.data.platform

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION
import com.defusername.standby.data.SessionStateHolder
import com.defusername.standby.presentation.standby.StandByActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StandByLauncher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionStateHolder: SessionStateHolder
) {
    fun launch() {
        if (sessionStateHolder.isStandByCurrentlyShowing.value) return
        val intent = Intent(context, StandByActivity::class.java).apply {
            addFlags(FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_NO_ANIMATION)
        }
        context.startActivity(intent)
        sessionStateHolder.show()
    }
}
