package de.tanpau.sevenm

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import de.tanpau.sevenm.ui.AppScreen
import de.tanpau.sevenm.ui.AppTheme
import de.tanpau.sevenm.ui.AppViewModel
import de.tanpau.sevenm.ui.Tab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Rendert jeden Reiter in Handygröße (393×852 dp, wie ein iPhone 15/16), in normaler und großer Schrift, nach
 * build/screenshots. Dient zur Sichtprüfung des Layouts ohne Gerät; geprüft wird nur, dass
 * das Rendern klappt.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ScreenshotTest {
    @Test
    fun renderAllTabs() {
        val dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
        try {
            val storage = FakeStorage().apply {
                files[Repository.LEAGUE_FILE] = Fixtures.LEAGUE_JSON
                files[Repository.GOALS_FILE] = GoalsJson.encode(
                    mapOf(
                        "808452" to ReportGoals(
                            mapOf("Max Mustermann-Langname" to PlayerGoals(9, 3), "Paul Probe" to PlayerGoals(6, 0), "Kai Kurz" to PlayerGoals(6, 1)),
                            mapOf("Erika Beispiel" to PlayerGoals(12, 5), "Otto Normal" to PlayerGoals(8, 0)),
                            0, 4, 21, 24
                        )
                    )
                )
            }
            val repo = Repository(Platform(FakeHttp(), storage, TextPdf, { 1L }))
            val vm = AppViewModel(repo, io = dispatcher)
            val out = File("build/screenshots").apply { mkdirs() }
            vm.toggleExpanded("808452")
            // 1.0 = Standardschrift, 1.3 = große Systemschrift (iOS „Größerer Text“, Android „Schriftgröße“)
            for (fontScale in listOf(1f, 1.3f)) {
                for (tab in Tab.entries) {
                    vm.selectTab(tab)
                    val png = render(vm, fontScale)
                    File(out, "${tab.name.lowercase()}-$fontScale.png").writeBytes(png)
                    assertTrue(png.isNotEmpty())
                }
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun render(vm: AppViewModel, fontScale: Float): ByteArray {
        val density = 2f
        val owner = object : LifecycleOwner, ViewModelStoreOwner {
            val registry = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
            override val lifecycle: Lifecycle get() = registry
            override val viewModelStore = ViewModelStore()
        }
        val scene = ImageComposeScene(width = (393 * density).toInt(), height = (852 * density).toInt(), density = Density(density, fontScale)) {
            CompositionLocalProvider(LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner) {
                AppTheme { AppScreen(vm) }
            }
        }
        try {
            scene.render() // erster Durchlauf: Komposition
            return scene.render(1_000_000_000L).encodeToData(EncodedImageFormat.PNG)!!.bytes
        } finally {
            scene.close()
        }
    }
}
