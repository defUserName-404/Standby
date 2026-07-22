package com.defusername.standby.domain.model

data class PermissionState(
    val type: PermissionType,
    val status: PermissionStatus
)

enum class PermissionType {
    NOTIFICATION_LISTENER,
    BATTERY_OPTIMIZATION_EXEMPTION,
    POST_NOTIFICATIONS
}

enum class PermissionStatus {
    GRANTED,
    DENIED,
    SPECIAL_ACCESS_REQUIRED
}
