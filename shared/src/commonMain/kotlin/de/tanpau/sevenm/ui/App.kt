package de.tanpau.sevenm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.tanpau.sevenm.Aggregator
import de.tanpau.sevenm.Format
import de.tanpau.sevenm.Game
import de.tanpau.sevenm.League
import de.tanpau.sevenm.Platform
import de.tanpau.sevenm.PlayerGoals
import de.tanpau.sevenm.Repository
import de.tanpau.sevenm.Team
import de.tanpau.sevenm.Wertung
import de.tanpau.sevenm.resources.Res

/** Einstiegspunkt für Android und iOS. */
@Composable
fun App(platform: Platform) {
    val vm: AppViewModel = viewModel {
        AppViewModel(Repository(platform, seed = { Res.readBytes("files/seed_goals.json").decodeToString() }))
    }
    AppTheme { AppScreen(vm) }
}

@Composable
private fun AppScreen(vm: AppViewModel) {
    val s by vm.state.collectAsStateWithLifecycle()
    val c = LocalAppColors.current
    val list = rememberLazyListState()
    LaunchedEffect(s.tab) { list.scrollToItem(0) }

    Column(Modifier.fillMaxSize().background(c.bg).windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))) {
        Header(s, onRefresh = vm::refresh, onInfo = { vm.showInfo(true) })
        LazyColumn(
            state = list,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 24.dp)
        ) {
            s.error?.let { item { Notice(it, warn = true) } }
            val league = s.data.league
            if (league == null) {
                item {
                    Notice(
                        if (s.refreshing || !s.loaded) "Daten werden geladen …"
                        else "Zum Laden „Aktualisieren“ tippen (Internet nötig).",
                        warn = false
                    )
                }
            } else when (s.tab) {
                Tab.TABELLE -> tableItems(league)
                Tab.SPIELPLAN -> scheduleItems(league, s, vm)
                Tab.KIRCHHEIM -> gameItems(league, league.games.filter { it.involvesKirchheim }, true, s, vm)
                Tab.TORE -> scorerItems(league, s, vm)
            }
        }
        TabBar(s.tab, vm::selectTab)
    }
    if (s.showInfo) InfoDialog { vm.showInfo(false) }
}

@Composable
private fun Header(s: UiState, onRefresh: () -> Unit, onInfo: () -> Unit) {
    val c = LocalAppColors.current
    val league = s.data.league
    val subtitle = when {
        s.refreshing -> s.progress
        league == null -> "Noch keine Daten"
        else -> "${league.headline2.ifEmpty { league.name }} · Stand ${league.actualized}"
    }
    Row(
        Modifier.fillMaxWidth().background(c.primary).statusBarsPadding()
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Txt(Team.DISPLAY_NAME, 20.sp, c.onAccent, bold = true)
            Txt(subtitle, 12.sp, c.onAccent)
        }
        TextButton(onClick = onRefresh, enabled = !s.refreshing) {
            Text("Aktualisieren", color = if (s.refreshing) c.onAccent.copy(alpha = 0.5f) else c.onAccent)
        }
        TextButton(onClick = onInfo) { Text("Info", color = c.onAccent) }
    }
}

@Composable
private fun TabBar(current: Tab, onSelect: (Tab) -> Unit) {
    val c = LocalAppColors.current
    Row(Modifier.fillMaxWidth().background(c.card).navigationBarsPadding()) {
        for (t in Tab.entries) {
            val sel = t == current
            Box(
                Modifier.weight(1f)
                    .semantics { selected = sel }
                    .clickable(role = Role.Tab) { onSelect(t) }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Txt(t.label, 13.sp, if (sel) c.accent else c.muted, bold = sel)
            }
        }
    }
}

// ---------- Tabelle ----------

private fun LazyListScope.tableItems(league: League) {
    item { Heading(league.name.ifEmpty { "Tabelle" }) }
    item {
        TableRowLayout(highlight = false) {
            for ((t, w) in listOf("#" to 0.5f, "Mannschaft" to 3.2f, "Sp" to 0.8f, "S" to 0.6f, "U" to 0.6f, "N" to 0.6f, "Tore" to 1.6f, "Pkt" to 1.4f)) {
                Cell(t, w, bold = true, muted = true, left = t == "Mannschaft")
            }
        }
    }
    items(league.table, key = { "t" + it.rank + it.team }) { r ->
        TableRowLayout(highlight = r.isKirchheim) {
            Cell(r.rank.toString(), 0.5f)
            Cell(r.team, 3.2f, bold = r.isKirchheim, left = true)
            Cell(r.played.toString(), 0.8f)
            Cell(r.won.toString(), 0.6f)
            Cell(r.draw.toString(), 0.6f)
            Cell(r.lost.toString(), 0.6f)
            Cell(r.goals, 1.6f)
            Cell(r.points, 1.4f, bold = true)
        }
    }
}

// ---------- Spielplan ----------

/** Gesamtspielplan mit Teamauswahl; ein Team zeigt alle seine Heim- und Auswärtsspiele. */
private fun LazyListScope.scheduleItems(league: League, s: UiState, vm: AppViewModel) {
    val teams = league.games.flatMap { listOf(it.home, it.guest) }
        .filter { it.isNotBlank() }
        .distinctBy { Aggregator.teamKey(it) }
        .sorted()
    val filter = s.scheduleTeam?.takeIf { key -> teams.any { Aggregator.teamKey(it) == key } }
    val games = league.games.filter {
        filter == null || Aggregator.teamKey(it.home) == filter || Aggregator.teamKey(it.guest) == filter
    }
    gameItems(league, games, false, s, vm) { TeamChips(teams, filter, vm::selectScheduleTeam) }
}

private fun LazyListScope.gameItems(
    league: League,
    games: List<Game>,
    kirchheim: Boolean,
    s: UiState,
    vm: AppViewModel,
    chips: (@Composable () -> Unit)? = null
) {
    item { Heading(if (kirchheim) "Spielplan ${Team.DISPLAY_NAME}" else "Gesamtspielplan") }
    chips?.let { item { Box(Modifier.padding(bottom = 8.dp)) { it() } } }
    if (kirchheim) {
        item {
            val sieg = games.count { it.outcomeForKirchheim() == 'S' }
            val ud = games.count { it.outcomeForKirchheim() == 'U' }
            val nd = games.count { it.outcomeForKirchheim() == 'N' }
            Notice("Bilanz: $sieg Siege · $ud Unentschieden · $nd Niederlagen · ${games.count { !it.played }} offen", false)
        }
    }
    if (games.isEmpty()) item { Notice("Keine Spiele gefunden.", false) }
    items(games, key = { it.id }) { g ->
        GameCard(league, g, kirchheim, s, vm)
    }
}

@Composable
private fun GameCard(league: League, g: Game, kirchheim: Boolean, s: UiState, vm: AppViewModel) {
    val c = LocalAppColors.current
    val uri = LocalUriHandler.current
    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp).background(c.card).padding(horizontal = 12.dp, vertical = 10.dp)) {
        val meta = buildString {
            append(Format.date(g.date)); if (g.time.isNotEmpty()) append(" · ${g.time}")
            if (g.hall.isNotEmpty() || g.hallName.isNotEmpty()) append(" · ${g.hallName.ifEmpty { "Halle ${g.hall}" }}")
        }
        Txt(meta, 12.sp, c.muted)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Txt(g.home, 15.sp, c.fg, bold = g.homeIsKirchheim)
                Txt(g.guest, 15.sp, c.fg, bold = g.guestIsKirchheim)
            }
            Column(horizontalAlignment = Alignment.End) {
                Txt(g.shownHome?.toString() ?: "–", 15.sp, c.fg, bold = true)
                Txt(g.shownGuest?.toString() ?: "–", 15.sp, c.fg, bold = true)
            }
            if (kirchheim) {
                g.outcomeForKirchheim()?.let { o ->
                    val col = when (o) { 'S' -> c.win; 'U' -> c.draw; else -> c.loss }
                    Txt(o.toString(), 14.sp, col, bold = true, modifier = Modifier.padding(start = 14.dp, end = 4.dp))
                }
            }
        }
        if (g.wertung != Wertung.KEINE) {
            Txt("Wertung am grünen Tisch (2:0)" + if (g.note.isNotEmpty()) " · ${g.note}" else "", 12.sp, c.muted)
        } else if (g.note.isNotEmpty()) {
            Txt(g.note, 12.sp, c.muted)
        }

        val rg = s.data.goals[g.id]
        if (g.played && (g.sgid.isNotEmpty() || rg != null)) {
            val open = g.id in s.expanded
            Txt(
                if (open) "Details ausblenden ▲" else "Torschützen & Spielbericht ▼", 13.sp, c.accent,
                modifier = Modifier.fillMaxWidth().clickable(role = Role.Button) { vm.toggleExpanded(g.id) }.padding(top = 8.dp)
            )
            if (open) {
                if (rg != null) {
                    ScorerBlock(g.home, rg.home, rg.unassignedHome)
                    ScorerBlock(g.guest, rg.guest, rg.unassignedGuest)
                } else {
                    Txt("Torverteilung noch nicht ausgewertet – „Aktualisieren“ tippen.", 12.sp, c.muted)
                }
                if (g.sgid.isNotEmpty()) {
                    Txt(
                        "PDF-Spielbericht öffnen ↗", 13.sp, c.accent, bold = true,
                        modifier = Modifier.clickable(role = Role.Button) {
                            try { uri.openUri(league.reportUrlBase + g.sgid) } catch (e: Exception) { /* kein Browser */ }
                        }.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ScorerBlock(team: String, goals: Map<String, PlayerGoals>, unassigned: Int) {
    val c = LocalAppColors.current
    val parts = goals.entries.sortedByDescending { it.value.goals }.map {
        "${it.key} ${it.value.goals}" + if (it.value.sevenMeter > 0) " (${it.value.sevenMeter}×7m)" else ""
    }.toMutableList()
    if (unassigned > 0) parts.add("ohne Zuordnung $unassigned")
    Column(Modifier.padding(top = 8.dp)) {
        Txt(team, 12.sp, c.muted, bold = true)
        Txt(if (parts.isEmpty()) "keine Tore" else parts.joinToString(", "), 13.sp, c.fg)
    }
}

// ---------- Torschützen ----------

private fun LazyListScope.scorerItems(league: League, s: UiState, vm: AppViewModel) {
    item { Heading("Torschützenliste") }
    val all = Aggregator.scorers(league.games, s.data.goals)
    val teams = all.map { it.team }.distinctBy { Aggregator.teamKey(it) }.sorted()
    val filter = s.scorerTeam?.takeIf { key -> teams.any { Aggregator.teamKey(it) == key } }
    item { TeamChips(teams, filter, vm::selectScorerTeam) }

    val list = all.filter { filter == null || Aggregator.teamKey(it.team) == filter }
    val shown = if (s.showAllScorers) list else list.take(15)
    item {
        TableRowLayout(highlight = false) {
            for ((t, w) in listOf("#" to 0.6f, "Spieler" to 3.2f, "Tore" to 0.9f, "7m" to 0.8f, "Sp" to 0.8f)) {
                Cell(t, w, bold = true, muted = true, left = t == "Spieler")
            }
        }
    }
    items(shown, key = { "s" + Aggregator.teamKey(it.team) + "|" + it.name }) { sc ->
        val c = LocalAppColors.current
        TableRowLayout(highlight = Team.isOwn(sc.team)) {
            Cell(sc.rank.toString(), 0.6f)
            Column(Modifier.weight(3.2f)) {
                Txt(sc.name, 14.sp, c.fg, bold = true)
                Txt(sc.team, 11.sp, c.muted)
            }
            Cell(sc.goals.toString(), 0.9f, bold = true)
            Cell(sc.sevenMeter.toString(), 0.8f)
            Cell(sc.games.toString(), 0.8f)
        }
    }
    if (list.size > 15) item {
        val c = LocalAppColors.current
        Txt(
            if (s.showAllScorers) "Nur Top 15 zeigen" else "Alle ${list.size} anzeigen", 14.sp, c.accent, bold = true,
            modifier = Modifier.clickable(role = Role.Button) { vm.toggleAllScorers() }.padding(top = 12.dp, bottom = 4.dp)
        )
    }
    item {
        val played = league.games.count { it.played }
        val evaluated = league.games.count { it.id in s.data.goals }
        val unassigned = Aggregator.unassigned(league.games, s.data.goals)
        Notice(
            "Ausgewertet: $evaluated von $played gespielten Spielen. " +
                (if (unassigned > 0) "$unassigned Tore sind im Spielbericht keinem Spieler zugeordnet. " else "") +
                "Quelle: PDF-Spielberichte, Siebenmeter-Tore zählen mit. „Sp“ = Spiele mit mindestens einem Tor.",
            false
        )
    }
    items(s.data.warnings) { Notice(it, warn = true) }
}

// ---------- Info ----------

@Composable
private fun InfoDialog(onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = { TextButton(onClick = onClose) { Text("Schließen") } },
        title = { Text("Info") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(INFO_TEXT, fontSize = 13.sp)
            }
        }
    )
}

private const val INFO_TEXT = """Inoffizielle App, ohne Verbindung zum Verband oder zu Handball4All. Alle Angaben ohne Gewähr.

Daten: Tabelle, Spielplan und Spielberichte stammen von Handball4All (spo.handball4all.de) und werden direkt von diesem Gerät abgerufen. Die Auswertung der Spielberichte erfolgt nur auf dem Gerät; es gibt keinen eigenen Server, kein Konto und kein Tracking.

Open-Source-Komponenten (jeweils Apache License 2.0, https://www.apache.org/licenses/LICENSE-2.0):
• PDFBox-Android (Android) – Copyright The Apache Software Foundation, Tom Roush
• Kotlin, kotlinx.coroutines, kotlinx.serialization, kotlinx-datetime, Ktor – JetBrains s.r.o.
• Compose Multiplatform – JetBrains s.r.o. und The Android Open Source Project
• AndroidX – The Android Open Source Project"""

// ---------- kleine UI-Bausteine ----------

/** Waagerechte Chip-Leiste zur Teamauswahl. [current] ist der aktive Teamschlüssel, null bedeutet „Alle“. */
@Composable
private fun TeamChips(teams: List<String>, current: String?, onSelect: (String?) -> Unit) {
    val c = LocalAppColors.current
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        @Composable
        fun chip(label: String, key: String?) {
            val sel = current == key
            Txt(
                label, 13.sp, if (sel) c.onAccent else c.fg, bold = sel,
                modifier = Modifier.semantics { selected = sel }
                    .background(if (sel) c.primary else c.card)
                    .clickable(role = Role.RadioButton) { onSelect(key) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
        chip("Alle", null)
        for (t in teams) chip(t, Aggregator.teamKey(t))
    }
}

@Composable
private fun Txt(t: String, size: TextUnit, color: Color, bold: Boolean = false, modifier: Modifier = Modifier, align: TextAlign? = null) {
    Text(t, modifier = modifier, fontSize = size, color = color, fontWeight = if (bold) FontWeight.Bold else null, textAlign = align)
}

@Composable
private fun Heading(t: String) {
    val c = LocalAppColors.current
    Txt(t, 18.sp, c.fg, bold = true, modifier = Modifier.semantics { heading() }.padding(bottom = 8.dp))
}

@Composable
private fun Notice(t: String, warn: Boolean) {
    val c = LocalAppColors.current
    Txt(t, 12.sp, if (warn) c.loss else c.muted, modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
private fun TableRowLayout(highlight: Boolean, content: @Composable RowScope.() -> Unit) {
    val c = LocalAppColors.current
    Row(
        Modifier.fillMaxWidth().background(if (highlight) c.highlight else Color.Transparent).padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
private fun RowScope.Cell(t: String, weight: Float, bold: Boolean = false, muted: Boolean = false, left: Boolean = false) {
    val c = LocalAppColors.current
    Txt(
        t, 13.sp, if (muted) c.muted else c.fg, bold = bold,
        modifier = Modifier.weight(weight),
        align = if (left) TextAlign.Start else TextAlign.Center
    )
}
