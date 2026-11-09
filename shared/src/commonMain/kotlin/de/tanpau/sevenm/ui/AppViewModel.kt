package de.tanpau.sevenm.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.tanpau.sevenm.AppState
import de.tanpau.sevenm.Repository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Tab(val label: String) {
    TABELLE("Tabelle"), SPIELPLAN("Spielplan"), KIRCHHEIM("Kirchheim"), TORE("Torschützen")
}

data class UiState(
    val data: AppState = AppState.EMPTY,
    val loaded: Boolean = false,
    val refreshing: Boolean = false,
    val progress: String = "",
    val error: String? = null,
    val tab: Tab = Tab.TABELLE,
    /** Teamschlüssel ([de.tanpau.sevenm.Aggregator.teamKey]) der Auswahl, null bedeutet „Alle“. */
    val scheduleTeam: String? = null,
    val scorerTeam: String? = null,
    val showAllScorers: Boolean = false,
    val expanded: Set<String> = emptySet(),
    val showInfo: Boolean = false
)

/**
 * Hält Daten und Bedienzustand. Ein ViewModel überlebt das Drehen des Geräts; eine laufende
 * Aktualisierung läuft weiter und landet danach in der neuen Ansicht. Wird die Ansicht endgültig
 * geschlossen, bricht viewModelScope die Arbeit ab.
 */
class AppViewModel(
    private val repo: Repository,
    private val io: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val cached = withContext(io) { repo.loadCached() }
            _state.update { it.copy(data = cached, loaded = true) }
            if (cached.league == null) refresh()
        }
    }

    fun refresh() {
        if (_state.value.refreshing) return
        _state.update { it.copy(refreshing = true, error = null, progress = "Verbinde …") }
        viewModelScope.launch {
            try {
                val s = withContext(io) {
                    repo.refresh { p -> _state.update { it.copy(progress = p) } }
                }
                _state.update { it.copy(data = s, refreshing = false) }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _state.update {
                    it.copy(
                        refreshing = false,
                        error = "Aktualisierung fehlgeschlagen (${e.message ?: e::class.simpleName}). " +
                            "Bitte Internetverbindung prüfen. Angezeigt wird der zuletzt gespeicherte Stand."
                    )
                }
            }
        }
    }

    fun selectTab(tab: Tab) = _state.update { it.copy(tab = tab) }
    fun selectScheduleTeam(key: String?) = _state.update { it.copy(scheduleTeam = key) }
    fun selectScorerTeam(key: String?) = _state.update { it.copy(scorerTeam = key) }
    fun toggleAllScorers() = _state.update { it.copy(showAllScorers = !it.showAllScorers) }
    fun toggleExpanded(gameId: String) = _state.update {
        it.copy(expanded = if (gameId in it.expanded) it.expanded - gameId else it.expanded + gameId)
    }
    fun showInfo(show: Boolean) = _state.update { it.copy(showInfo = show) }
}
