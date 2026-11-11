package com.nuvio.app.features.tracking

import com.nuvio.app.core.auth.AuthRepository
import com.nuvio.app.core.auth.AuthState
import com.nuvio.app.features.library.LibrarySourceMode
import com.nuvio.app.features.profiles.ProfileRepository
import com.nuvio.app.features.simkl.DEFAULT_SIMKL_ANIME_ID_PREFERENCE
import com.nuvio.app.features.simkl.SimklAnimeIdPreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

const val CONTINUE_WATCHING_DAYS_CAP_ALL = 0
const val DEFAULT_CONTINUE_WATCHING_DAYS_CAP = 60
const val MIN_CONTINUE_WATCHING_DAYS_CAP = 7
const val MAX_CONTINUE_WATCHING_DAYS_CAP = 365

val ContinueWatchingDaysOptions: List<Int> = listOf(
    14,
    30,
    DEFAULT_CONTINUE_WATCHING_DAYS_CAP,
    90,
    180,
    MAX_CONTINUE_WATCHING_DAYS_CAP,
    CONTINUE_WATCHING_DAYS_CAP_ALL,
)

@Serializable
enum class MoreLikeThisSourcePreference {
    TMDB,
    SIMKL;

    companion object {
        fun fromStorage(value: String?): MoreLikeThisSourcePreference =
            entries.firstOrNull { it.name == value } ?: DEFAULT_MORE_LIKE_THIS_SOURCE
    }
}

val DEFAULT_MORE_LIKE_THIS_SOURCE: MoreLikeThisSourcePreference = MoreLikeThisSourcePreference.TMDB

data class TrackingSettingsUiState(
    val watchProgressSource: WatchProgressSource = DEFAULT_WATCH_PROGRESS_SOURCE,
    val continueWatchingDaysCap: Int = DEFAULT_CONTINUE_WATCHING_DAYS_CAP,
    val librarySourceMode: LibrarySourceMode = DEFAULT_LIBRARY_SOURCE_MODE,
    val moreLikeThisSource: MoreLikeThisSourcePreference = DEFAULT_MORE_LIKE_THIS_SOURCE,
    val simklAnimeIdPreference: SimklAnimeIdPreference = DEFAULT_SIMKL_ANIME_ID_PREFERENCE,
)

@Serializable
private data class StoredTrackingSettings(
    val watchProgressSource: String? = null,
    val continueWatchingDaysCap: Int = DEFAULT_CONTINUE_WATCHING_DAYS_CAP,
    val librarySourceMode: String? = null,
    val moreLikeThisSource: String? = null,
    val simklAnimeIdPreference: String? = null,
)

object TrackingSettingsRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _uiState = MutableStateFlow(TrackingSettingsUiState())
    val uiState: StateFlow<TrackingSettingsUiState> = _uiState.asStateFlow()

    private var hasLoaded = false

    fun ensureLoaded() {
        if (hasLoaded) return
        loadFromDisk()
    }

    fun onProfileChanged() {
        loadFromDisk()
    }

    fun clearLocalState() {
        hasLoaded = false
        _uiState.value = TrackingSettingsUiState()
    }

    internal fun setWatchProgressSource(
        source: WatchProgressSource,
        profileId: Int = ProfileRepository.activeProfileId,
    ) {
        ensureLoaded()
        if (_uiState.value.watchProgressSource == source) return
        val nextState = _uiState.value.copy(watchProgressSource = source)
        persist(nextState)
        val authState = AuthRepository.state.value
        if (authState is AuthState.Authenticated && !authState.isAnonymous) {
            ProfileSettingsWatchSourceOutbox.record(
                accountId = authState.userId,
                profileId = profileId,
                source = source,
            )
        }
        _uiState.value = nextState
    }

    fun setContinueWatchingDaysCap(days: Int) {
        ensureLoaded()
        val normalized = normalizeContinueWatchingDaysCap(days)
        if (_uiState.value.continueWatchingDaysCap == normalized) return
        _uiState.value = _uiState.value.copy(continueWatchingDaysCap = normalized)
        persist()
    }

    fun setLibrarySourceMode(mode: LibrarySourceMode) {
        ensureLoaded()
        if (_uiState.value.librarySourceMode == mode) return
        _uiState.value = _uiState.value.copy(librarySourceMode = mode)
        persist()
    }

    fun setMoreLikeThisSource(source: MoreLikeThisSourcePreference) {
        ensureLoaded()
        if (_uiState.value.moreLikeThisSource == source) return
        _uiState.value = _uiState.value.copy(moreLikeThisSource = source)
        persist()
    }

    fun setSimklAnimeIdPreference(preference: SimklAnimeIdPreference) {
        ensureLoaded()
        if (_uiState.value.simklAnimeIdPreference == preference) return
        _uiState.value = _uiState.value.copy(simklAnimeIdPreference = preference)
        persist()
        com.nuvio.app.features.simkl.SimklSyncRepository.invalidateProjections()
    }

    private fun loadFromDisk() {
        hasLoaded = true

        val payload = TrackingSettingsStorage.loadPayload().orEmpty().trim()
        if (payload.isEmpty()) {
            _uiState.value = TrackingSettingsUiState()
            return
        }

        val stored = runCatching {
            json.decodeFromString<StoredTrackingSettings>(payload)
        }.getOrNull()

        _uiState.value = if (stored != null) {
            TrackingSettingsUiState(
                watchProgressSource = WatchProgressSource.fromStorage(stored.watchProgressSource),
                continueWatchingDaysCap = normalizeContinueWatchingDaysCap(stored.continueWatchingDaysCap),
                librarySourceMode = librarySourceModeFromStorage(stored.librarySourceMode),
                moreLikeThisSource = MoreLikeThisSourcePreference.fromStorage(stored.moreLikeThisSource),
                simklAnimeIdPreference = SimklAnimeIdPreference.fromStorage(stored.simklAnimeIdPreference),
            )
        } else {
            TrackingSettingsUiState()
        }
    }

    private fun persist(state: TrackingSettingsUiState = _uiState.value) {
        TrackingSettingsStorage.savePayload(
            json.encodeToString(
                StoredTrackingSettings(
                    watchProgressSource = state.watchProgressSource.name,
                    continueWatchingDaysCap = state.continueWatchingDaysCap,
                    librarySourceMode = state.librarySourceMode.name,
                    moreLikeThisSource = state.moreLikeThisSource.name,
                    simklAnimeIdPreference = state.simklAnimeIdPreference.name,
                ),
            ),
        )
    }
}

fun normalizeContinueWatchingDaysCap(days: Int): Int =
    if (days == CONTINUE_WATCHING_DAYS_CAP_ALL) {
        CONTINUE_WATCHING_DAYS_CAP_ALL
    } else {
        days.coerceIn(MIN_CONTINUE_WATCHING_DAYS_CAP, MAX_CONTINUE_WATCHING_DAYS_CAP)
    }
