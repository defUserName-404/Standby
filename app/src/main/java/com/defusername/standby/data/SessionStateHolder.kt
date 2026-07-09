package com.defusername.standby.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionStateHolder @Inject constructor() {

    private val _isStandByCurrentlyShowing = MutableStateFlow(false)
    val isStandByCurrentlyShowing: StateFlow<Boolean> = _isStandByCurrentlyShowing.asStateFlow()

    fun show() {
        _isStandByCurrentlyShowing.value = true
    }

    fun dismiss() {
        _isStandByCurrentlyShowing.value = false
    }
}
