import SwiftUI
import Shared

/// Hosts the shared Compose UI. The one PlayerPresenter implements IosPlaybackHost for the Play button.
struct ComposeRootView: UIViewControllerRepresentable {
    private let presenter = PlayerPresenter()

    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(host: presenter)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
