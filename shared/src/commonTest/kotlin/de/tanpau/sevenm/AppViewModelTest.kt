package de.tanpau.sevenm

import de.tanpau.sevenm.ui.AppViewModel
import de.tanpau.sevenm.ui.Tab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val http = FakeHttp()
    private val storage = FakeStorage()
    private val leagueUrl = "https://example.invalid/league"

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.vm() = AppViewModel(
        Repository(Platform(http, storage, TextPdf, { 1L }), leagueUrl = leagueUrl),
        io = dispatcher
    )

    @Test
    fun loadsAutomaticallyWhenNothingIsCached() = runTest(dispatcher) {
        http.responses[leagueUrl] = { Fixtures.LEAGUE_JSON.encodeToByteArray() }
        val vm = vm()
        advanceUntilIdle()
        val s = vm.state.value
        assertTrue(s.loaded)
        assertFalse(s.refreshing)
        assertNotNull(s.data.league)
        assertNull(s.error)
    }

    @Test
    fun doesNotLoadAutomaticallyWhenCached() = runTest(dispatcher) {
        storage.files[Repository.LEAGUE_FILE] = Fixtures.LEAGUE_JSON
        val vm = vm()
        advanceUntilIdle()
        assertNotNull(vm.state.value.data.league)
        assertTrue(http.calls.isEmpty())
    }

    @Test
    fun errorKeepsCachedDataAndShowsMessage() = runTest(dispatcher) {
        storage.files[Repository.LEAGUE_FILE] = Fixtures.LEAGUE_JSON
        val vm = vm()
        advanceUntilIdle()
        vm.refresh()
        advanceUntilIdle()
        val s = vm.state.value
        assertFalse(s.refreshing)
        assertTrue(s.error!!.startsWith("Aktualisierung fehlgeschlagen (HTTP 404)"))
        assertNotNull(s.data.league)
    }

    @Test
    fun secondRefreshWhileRunningIsIgnored() = runTest(dispatcher) {
        storage.files[Repository.LEAGUE_FILE] = Fixtures.LEAGUE_JSON
        http.responses[leagueUrl] = { Fixtures.LEAGUE_JSON.encodeToByteArray() }
        val vm = vm()
        advanceUntilIdle()
        vm.refresh()
        vm.refresh()
        assertTrue(vm.state.value.refreshing)
        advanceUntilIdle()
        assertEquals(1, http.calls.count { it == leagueUrl })
    }

    @Test
    fun uiSelectionsAreKept() = runTest(dispatcher) {
        storage.files[Repository.LEAGUE_FILE] = Fixtures.LEAGUE_JSON
        val vm = vm()
        vm.selectTab(Tab.TORE)
        vm.selectScorerTeam("sghdkirchheim")
        vm.toggleExpanded("808452")
        vm.toggleAllScorers()
        val s = vm.state.value
        assertEquals(Tab.TORE, s.tab)
        assertEquals("sghdkirchheim", s.scorerTeam)
        assertEquals(setOf("808452"), s.expanded)
        assertTrue(s.showAllScorers)
        vm.toggleExpanded("808452")
        assertTrue(vm.state.value.expanded.isEmpty())
    }
}
