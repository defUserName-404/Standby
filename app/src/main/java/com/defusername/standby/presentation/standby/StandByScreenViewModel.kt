package com.defusername.standby.presentation.standby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.defusername.standby.domain.layout.LayoutEngine
import com.defusername.standby.domain.layout.Orientation
import com.defusername.standby.domain.layout.WidgetArrangement
import com.defusername.standby.domain.repository.SettingsRepository
import com.defusername.standby.domain.widget.WidgetSpec
import com.defusername.standby.presentation.widget.WidgetDefinition
import com.defusername.standby.presentation.widget.WidgetRegistry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class StandByScreenUiState(
    val arrangement: WidgetArrangement,
    /** Widget id -> its latest state (null = empty/error per 9.4). */
    val widgetStates: Map<String, Any?>
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StandByScreenViewModel @Inject constructor(
    private val widgetRegistry: WidgetRegistry,
    private val settingsRepository: SettingsRepository,
    private val layoutEngine: LayoutEngine
) : ViewModel() {

    private val _orientation = MutableStateFlow(Orientation.PORTRAIT)
    val orientation: StateFlow<Orientation> = _orientation.asStateFlow()

    fun setOrientation(orientation: Orientation) {
        _orientation.value = orientation
    }

    private val enabledSpecs: Flow<List<WidgetSpec>> = settingsRepository.settings
        .map { settings ->
            if (settings.enabledWidgetIds.isEmpty()) {
                widgetRegistry.specs // empty list = all widgets enabled by default
            } else {
                settings.enabledWidgetIds.mapNotNull { widgetRegistry.spec(it) }
            }
        }
        .distinctUntilChanged()

    private val widgetStates: Flow<Map<String, Any?>> = enabledSpecs
        .flatMapLatest { specs -> combineWidgetStates(specs) }

    private val arrangement: Flow<WidgetArrangement> = combine(
        enabledSpecs,
        _orientation
    ) { specs, orientation -> layoutEngine.arrange(specs, orientation) }

    val uiState: StateFlow<StandByScreenUiState?> = combine(arrangement, widgetStates) { a, s ->
        StandByScreenUiState(a, s)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /**
     * Subscribe to exactly the given widgets' data sources, each isolated so a throwing
     * source becomes a null state instead of crashing siblings (requirement 9.4).
     */
    private fun combineWidgetStates(specs: List<WidgetSpec>): Flow<Map<String, Any?>> {
        if (specs.isEmpty()) return flowOf(emptyMap())
        val flows: List<Flow<Pair<String, Any?>>> = specs.map { spec ->
            val definition: WidgetDefinition = widgetRegistry.definition(spec.id)
                ?: return@map flowOf<Pair<String, Any?>>(spec.id to null)
            definition.dataSource.data
                .map { value -> spec.id to value }
                .catch { emit(spec.id to null) }
        }
        return combine(*flows.toTypedArray()) { pairs -> pairs.toMap() }
    }
}
