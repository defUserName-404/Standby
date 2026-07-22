package com.defusername.standby.domain.model

/**
 * Whether zen mode is on. When enabled, no notification content is shown
 * on the StandBy screen (requirement 5.1). The `scheduledWindows` field is
 * deliberately absent until a future "auto zen at night" feature is built.
 */
data class ZenState(val enabled: Boolean)
