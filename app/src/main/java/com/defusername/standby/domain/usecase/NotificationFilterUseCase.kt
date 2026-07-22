package com.defusername.standby.domain.usecase

import com.defusername.standby.domain.model.NotificationEntry
import javax.inject.Inject

/**
 * Pure filter deciding what (if anything) to show on the StandBy screen.
 *
 *  - 5.1: zen mode on  → nothing is shown.
 *  - 4.4: excluded packages are never shown.
 *  - 5.2: zen mode off → the single most recent non-excluded notification, if any.
 *
 * Reactive wiring (re-evaluating when settings/notifications change) lives in the
 * presentation layer; this use case is a pure function of its inputs.
 */
class NotificationFilterUseCase @Inject constructor() {

    fun filter(
        rawList: List<NotificationEntry>,
        zenEnabled: Boolean,
        excludedPackages: Set<String>
    ): NotificationEntry? {
        if (zenEnabled) return null
        return rawList
            .filter { it.packageName !in excludedPackages }
            .maxByOrNull { it.postedAt }
    }
}
