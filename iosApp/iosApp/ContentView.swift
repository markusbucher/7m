import SwiftUI
import UIKit
import Shared

/// Bettet die gemeinsame Compose-Oberfläche (shared/src/commonMain/.../ui/App.kt) ein.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        // Compose berücksichtigt die Safe Area selbst (Status- und Home-Leiste).
        ComposeView().ignoresSafeArea()
    }
}
