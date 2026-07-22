package com.defusername.standby.domain.model

import java.time.Instant

data class NotificationEntry(
    /** Stable identity: originating package + system notification key. */
    val id: String,
    val packageName: String,
    val appLabel: String,
    val iconRef: IconRef,
    val title: String,
    val text: String,
    val postedAt: Instant,
    /** From Notification.visibility — reserved for a future "hide sensitive content" toggle. */
    val isSensitive: Boolean
)

/**
 * Opaque icon reference the presentation layer resolves to a Drawable/Painter
 * via PackageManager. Domain stays framework-free; [iconResId] of 0 means "no icon".
 */
data class IconRef(
    val packageName: String,
    val iconResId: Int
)
