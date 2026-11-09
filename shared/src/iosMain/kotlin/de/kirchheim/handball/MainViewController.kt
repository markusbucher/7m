package de.kirchheim.handball

import androidx.compose.ui.window.ComposeUIViewController
import de.kirchheim.handball.ui.App
import platform.UIKit.UIViewController

/** Wird aus Swift (iosApp/iosApp/ContentView.swift) aufgerufen. */
@Suppress("FunctionName", "unused")
fun MainViewController(): UIViewController {
    val platform = iosPlatform()
    return ComposeUIViewController { App(platform) }
}
